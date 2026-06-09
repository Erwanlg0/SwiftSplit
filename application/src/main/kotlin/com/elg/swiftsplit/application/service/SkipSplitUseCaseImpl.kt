package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.SkipSplitUseCase
import javax.inject.Inject

class SkipSplitUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : SkipSplitUseCase {
    override suspend fun invoke() {
        timerManager.skip()
    }
}
