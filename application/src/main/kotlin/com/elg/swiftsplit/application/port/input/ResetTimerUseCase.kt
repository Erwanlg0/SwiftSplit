package com.elg.swiftsplit.application.port.input

interface ResetTimerUseCase {
    suspend operator fun invoke(saveAttempt: Boolean = true)
}
