/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TODO check license!!!!

#pragma once

#include <atomic>

#include "Utils.hpp"

template<typename T, std::size_t Capacity>
class mpmc_bounded_queue {
public:
    mpmc_bounded_queue() {
        // TODO static assert
        RuntimeAssert((Capacity >= 2) && ((Capacity & (Capacity - 1)) == 0), "Must be a power of 2");

        for (size_t i = 0; i < Capacity; ++i) {
            buffer_[i].sequence_.store(i, std::memory_order_relaxed);
        }
        enqueue_pos_.store(0, std::memory_order_relaxed);
        dequeue_pos_.store(0, std::memory_order_relaxed);
        // TODO release smth?
    }

    bool enqueue(T&& data) {
        // TODO refactor?
        Cell* cell;
        size_t pos = enqueue_pos_.load(std::memory_order_relaxed);
        while (true) {
            cell = &buffer_[pos & (Capacity - 1)];
            size_t seq = cell->sequence_.load(std::memory_order_acquire);
            intptr_t dif = (intptr_t) seq - (intptr_t) pos;
            if (dif == 0) {
                if (enqueue_pos_.compare_exchange_weak(pos, pos + 1, std::memory_order_relaxed)) {
                    break;
                }
            } else if (dif < 0) {
                return false;
            } else {
                pos = enqueue_pos_.load(std::memory_order_relaxed);
            }
        }
        cell->data_ = std::move(data);
        cell->sequence_.store(pos + 1, std::memory_order_release);
        return true;
    }

    bool dequeue(T& data) {
        // TODO refactor?
        Cell* cell;
        size_t pos = dequeue_pos_.load(std::memory_order_relaxed);
        while (true) {
            cell = &buffer_[pos & (Capacity - 1)];
            size_t seq = cell->sequence_.load(std::memory_order_acquire);
            intptr_t dif = (intptr_t) seq - (intptr_t) (pos + 1);
            if (dif == 0) {
                if (dequeue_pos_.compare_exchange_weak(pos, pos + 1, std::memory_order_relaxed)) {
                    break;
                }
            } else if (dif < 0) {
                return false;
            } else {
                pos = dequeue_pos_.load(std::memory_order_relaxed);
            }
        }
        data = std::move(cell->data_); // FIXME move?
        cell->sequence_.store(pos + Capacity, std::memory_order_release);
        return true;
    }

private:
    struct Cell {
        // TODO describe
        std::atomic<size_t> sequence_;
        T data_;
    };

    // TODO use alignment?
    static size_t const cacheline_size = 64;
    typedef char cacheline_pad_t[cacheline_size];

    cacheline_pad_t pad0_;

    Cell buffer_[Capacity];

    cacheline_pad_t pad1_;

    std::atomic<size_t> enqueue_pos_;

    cacheline_pad_t pad2_;

    std::atomic<size_t> dequeue_pos_;

    cacheline_pad_t pad3_;

    // TODO =delete?
    mpmc_bounded_queue(mpmc_bounded_queue const&);
    void operator=(mpmc_bounded_queue const&);
};
