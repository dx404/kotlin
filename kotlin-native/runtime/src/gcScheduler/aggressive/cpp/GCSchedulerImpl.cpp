/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

#include "GlobalData.hpp"
#include "Memory.h"
#include "Logging.hpp"
#include "Porting.h"

using namespace kotlin;

ALWAYS_INLINE void gcScheduler::GCScheduler::OnSafePoint() noexcept {
    static_cast<internal::GCSchedulerDataAggressive&>(gcData()).OnSafePoint();
}

gcScheduler::GCScheduler::GCScheduler() noexcept :
    gcData_(std_support::make_unique<internal::GCSchedulerDataAggressive>(config_, []() noexcept {
        // This call acquires a lock, so we need to ensure that we're in the safe state.
        NativeOrUnregisteredThreadGuard guard(/* reentrant = */ true);
        mm::GlobalData::Instance().gc().Schedule();
    })) {}
