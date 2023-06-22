/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "../CompilerConstants.hpp"
#include "../KAssert.h"
#include "../Logging.hpp"
#include "../Utils.hpp"
#include "Porting.h"
#include "PushOnlyAtomicArray.hpp"
#include "SplitSharedList.hpp"
#include "BoundedQueue.hpp"

namespace kotlin {

namespace internal {

enum class ShareOn { kPush, kPop };

} // namespace internal

/**
 * Coordinates a group of workers working in parallel on a large amounts of identical tasks.
 * The dispatcher will try to balance the work among the workers.
 *
 * Requirements:
 * -  Every instantiated worker must execute `tryPop` sooner or later;
 * -  No worker must be instantiated after at least one other worker has executed `tryPop`
 */
template <std::size_t kMaxWorkers, typename ListImpl, std::size_t kMinSizeToShare, std::size_t kMaxSizeToSteal = kMinSizeToShare / 2, internal::ShareOn kShareOn = internal::ShareOn::kPush>
class ParallelProcessor : private Pinned {

    // TODO move inside Batch
    static const std::size_t kBatchCapacity = 256; // TODO choose

    class Batch {
    public:
        bool empty() const noexcept {
            return elems_.empty();
        }

        bool full() const noexcept {
            return size_ == kBatchCapacity;
        }

        bool tryPush(typename ListImpl::reference value) noexcept {
            if (full()) return false; // FIXME replace with assert?

            bool pushed = elems_.try_push_front(value);
            if (pushed) {
                ++size_;
            }
            return pushed;
        }

        typename ListImpl::pointer tryPop() noexcept {
            auto popped = elems_.try_pop_front();
            if (popped) {
                --size_;
            }
            return popped;
        }

        // FIXME replace with methods
    public: // FIXME private
        ListImpl elems_;
        std::size_t size_ = 0;
    };

public:
    static const std::size_t kStealingAttemptCyclesBeforeWait = 4;

    class Worker : private Pinned {
        friend ParallelProcessor;
    public:
        explicit Worker(ParallelProcessor& dispatcher) : dispatcher_(dispatcher) {
            dispatcher_.registerWorker(*this);
        }

        ALWAYS_INLINE bool empty() const noexcept {
            return batch_.empty(); // TODO what about shared?
        }

        ALWAYS_INLINE bool tryPushLocal(typename ListImpl::reference value) noexcept {
            // TODO do better
            return batch_.elems_.try_push_front(value);
        }

        ALWAYS_INLINE bool tryPush(typename ListImpl::reference value) noexcept {
            if (batch_.full()) {
                bool released = dispatcher_.releaseBatch(std::move(batch_));
                if (!released) {
                    overflowList_.splice_after(overflowList_.before_begin(), batch_.elems_.before_begin(), batch_.elems_.end(), kBatchCapacity);
                    RuntimeAssert(batch_.empty(), "must become empty");
                }
                batch_ = Batch{};
            }
            return batch_.tryPush(value);
        }

        ALWAYS_INLINE typename ListImpl::pointer tryPopLocal() noexcept {
            // TODO do better
            return batch_.elems_.try_pop_front();
        }

        ALWAYS_INLINE typename ListImpl::pointer tryPop() noexcept {
            if (batch_.empty()) {
                // FIXME simplify CFG
                while (true) {
                    bool acquired = dispatcher_.acquireBatch(batch_);
                    if (!acquired) {
                        if (!overflowList_.empty()) {
                            auto spliced = batch_.elems_.splice_after(batch_.elems_.before_begin(), overflowList_.before_begin(), overflowList_.end(), kBatchCapacity);
                            // TODO check size
                            batch_.size_ = spliced;
                        } else {
                            bool newWorkAvailable = waitForMoreWork();
                            if (newWorkAvailable) continue;
                            return nullptr;
                        }
                    }
                    RuntimeAssert(!batch_.empty(), "Must acquire smth");
                    break;
                }
            }

            return batch_.tryPop();
        }

    private:
        bool waitForMoreWork() noexcept {
            RuntimeAssert(batch_.empty(), "Must be empty");
            std::unique_lock lock(dispatcher_.waitMutex_);

            auto nowWaiting = dispatcher_.waitingWorkers_.fetch_add(1, std::memory_order_relaxed) + 1;
            RuntimeLogDebug({ "balancing" }, "Worker goes to sleep (now sleeping %zu of %zu)",
                            nowWaiting, dispatcher_.registeredWorkers_.size());

            if (dispatcher_.allDone_) {
                dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return false;
            }

            if (nowWaiting == dispatcher_.registeredWorkers_.size()) {
                // we are the last ones awake
                RuntimeLogDebug({ "balancing" }, "Worker has detected termination");
                dispatcher_.allDone_ = true;
                lock.unlock();
                dispatcher_.waitCV_.notify_all();
                dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return false;
            }

            dispatcher_.waitCV_.wait(lock);
            dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
            if (dispatcher_.allDone_) {
                return false;
            }
            RuntimeLogDebug({ "balancing" }, "Worker woke up");

            return true;
        }

        const int carrierThreadId_ = konan::currentThreadId();
        ParallelProcessor& dispatcher_;

        Batch batch_;
        ListImpl overflowList_;
    };

    ParallelProcessor() = default;

    ~ParallelProcessor() {
        RuntimeAssert(waitingWorkers_.load() == 0, "All the workers must terminate before dispatcher destruction");
    }

    size_t registeredWorkers() {
        return registeredWorkers_.size(std::memory_order_relaxed);
    }

private:
    void registerWorker(Worker& worker) {
        RuntimeAssert(worker.empty(), "Work list of an unregistered worker must be empty (e.g. fully depleted earlier)");
        RuntimeAssert(!allDone_, "Dispatcher must wait for every possible worker to register before finishing the work");
        RuntimeAssert(!isRegistered(worker), "Task registration is not idempotent");

        registeredWorkers_.push(&worker);
        RuntimeLogDebug({ "balancing" }, "Worker registered");
    }

    // Primarily to be used in assertions
    bool isRegistered(const Worker& worker) const {
        for (size_t i = 0; i < registeredWorkers_.size(std::memory_order_acquire); ++i) {
            if (registeredWorkers_[i] == &worker) return true;
        }
        return false;
    }

    void onShare(std::size_t sharedAmount) {
        RuntimeAssert(sharedAmount > 0, "Must have shared something");
        RuntimeLogDebug({ "balancing" }, "Worker has shared %zu tasks", sharedAmount);
        if (waitingWorkers_.load(std::memory_order_relaxed) > 0) {
            waitCV_.notify_all();
        }
    }

    PushOnlyAtomicArray<Worker*, kMaxWorkers, nullptr> registeredWorkers_;
    std::atomic<size_t> waitingWorkers_ = 0;

    std::atomic<bool> allDone_ = false;
    mutable std::mutex waitMutex_;
    mutable std::condition_variable waitCV_;

    bool releaseBatch(Batch&& batch) {
        RuntimeAssert(!batch.empty(), "must not be empty"); // TODO assert msgs
        auto size = batch.size_; // TODO remove?
        bool enqueued = batches_.enqueue(std::move(batch)); // TODO forward?
        if (!enqueued) {
            return false;
//            std::unique_lock guard(overflowMutex_);
//            overflowSet_.splice_after(overflowSet_.before_begin(), batch.elems_.before_begin(), batch.elems_.end(), std::numeric_limits<std::size_t>::max()); // FIXME max?
        }
        onShare(size);
        return true;
    }

    bool acquireBatch(Batch& dst) {
        RuntimeAssert(dst.empty(), "must be empty");
        bool dequeued = batches_.dequeue(dst);
        return dequeued;

//        if (dequeued) return true;
//        RuntimeAssert(dst.empty(), "must be empty");
//        // TODO if overflow is empty?
//        std::unique_lock guard(overflowMutex_);
//        std::size_t spliced = dst.elems_.splice_after(dst.elems_.before_begin(),
//                                                      overflowSet_.before_begin(),
//                                                      overflowSet_.end(),
//                                                      kBatchCapacity);
//        dst.size_ = spliced;
//        return spliced > 0;
    }

    mpmc_bounded_queue<Batch, 1024 * 4> batches_; // TODO choose

//    std::mutex overflowMutex_;
//    ListImpl overflowSet_;
};

}
