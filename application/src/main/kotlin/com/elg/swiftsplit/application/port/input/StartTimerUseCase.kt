package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.RunId
import com.elg.swiftsplit.domain.model.TimingMethod

interface StartTimerUseCase {
    suspend operator fun invoke(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod)
}
