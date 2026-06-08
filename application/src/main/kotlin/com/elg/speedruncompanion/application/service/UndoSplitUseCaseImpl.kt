package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.UndoSplitUseCase
import javax.inject.Inject

class UndoSplitUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : UndoSplitUseCase {
    override suspend fun invoke() {
        timerManager.undoSplit()
    }
}
