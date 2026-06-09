package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ResetTimerUseCase
import javax.inject.Inject

class ResetTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : ResetTimerUseCase {
    override suspend fun invoke(saveAttempt: Boolean) {
        timerManager.reset(saveAttempt)
    }
}
