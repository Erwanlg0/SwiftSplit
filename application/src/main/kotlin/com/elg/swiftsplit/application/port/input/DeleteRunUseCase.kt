package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.RunId

interface DeleteRunUseCase {
    suspend operator fun invoke(runId: RunId)
}
