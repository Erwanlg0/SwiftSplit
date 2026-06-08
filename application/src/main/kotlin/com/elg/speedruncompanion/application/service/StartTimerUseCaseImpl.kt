package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.StartTimerUseCase
import com.elg.speedruncompanion.domain.model.ComparisonName
import com.elg.speedruncompanion.domain.model.RunId
import com.elg.speedruncompanion.domain.model.TimingMethod
import javax.inject.Inject

class StartTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : StartTimerUseCase {
    override suspend fun invoke(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod) {
        timerManager.start(runId, comparison, timingMethod)
    }
}
