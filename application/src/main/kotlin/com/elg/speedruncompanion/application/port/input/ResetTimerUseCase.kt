package com.elg.speedruncompanion.application.port.input

interface ResetTimerUseCase {
    suspend operator fun invoke(saveAttempt: Boolean = true)
}
