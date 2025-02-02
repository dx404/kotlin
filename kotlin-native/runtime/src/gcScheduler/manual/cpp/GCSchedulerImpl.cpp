/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCSchedulerImpl.hpp"

using namespace kotlin;

void gcScheduler::GCSchedulerThreadData::OnSafePointRegular(size_t weight) noexcept {}

gcScheduler::GCScheduler::GCScheduler() noexcept : gcData_(std_support::make_unique<internal::GCSchedulerDataManual>()) {}
