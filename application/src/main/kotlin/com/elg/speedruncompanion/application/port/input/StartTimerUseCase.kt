package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.ComparisonName
import com.elg.speedruncompanion.domain.model.RunId
import com.elg.speedruncompanion.domain.model.TimingMethod

interface StartTimerUseCase {
    suspend operator fun invoke(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod)
}
