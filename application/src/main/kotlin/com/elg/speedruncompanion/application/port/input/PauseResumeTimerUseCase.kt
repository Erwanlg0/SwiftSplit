package com.elg.speedruncompanion.application.port.input

interface PauseResumeTimerUseCase {
    suspend operator fun invoke()
}
