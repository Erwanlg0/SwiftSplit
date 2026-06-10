package com.elg.swiftsplit.infrastructure

import com.elg.swiftsplit.domain.service.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClockImpl @Inject constructor() : Clock {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
