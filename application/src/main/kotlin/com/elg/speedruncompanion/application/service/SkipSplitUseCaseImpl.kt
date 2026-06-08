package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.SkipSplitUseCase
import javax.inject.Inject

class SkipSplitUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : SkipSplitUseCase {
    override suspend fun invoke() {
        timerManager.skip()
    }
}
