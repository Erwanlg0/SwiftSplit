package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.PauseResumeTimerUseCase
import javax.inject.Inject

class PauseResumeTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : PauseResumeTimerUseCase {
    override suspend fun invoke() {
        timerManager.pauseResume()
    }
}
