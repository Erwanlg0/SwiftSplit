package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.RunId

interface GetRunByIdUseCase {
    suspend operator fun invoke(runId: RunId): Run?
}
