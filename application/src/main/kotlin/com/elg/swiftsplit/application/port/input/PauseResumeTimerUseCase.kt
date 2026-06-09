package com.elg.swiftsplit.application.port.input

interface PauseResumeTimerUseCase {
    suspend operator fun invoke()
}
