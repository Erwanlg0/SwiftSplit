package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.SplitUseCase
import javax.inject.Inject

class SplitUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : SplitUseCase {
    override suspend fun invoke() {
        timerManager.split()
    }
}
