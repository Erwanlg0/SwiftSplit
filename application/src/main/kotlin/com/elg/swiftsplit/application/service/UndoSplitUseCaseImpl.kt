package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.UndoSplitUseCase
import javax.inject.Inject

class UndoSplitUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : UndoSplitUseCase {
    override suspend fun invoke() {
        timerManager.undoSplit()
    }
}
