package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.StartTimerUseCase
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.RunId
import com.elg.swiftsplit.domain.model.TimingMethod
import javax.inject.Inject

class StartTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : StartTimerUseCase {
    override suspend fun invoke(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod) {
        timerManager.start(runId, comparison, timingMethod)
    }
}
