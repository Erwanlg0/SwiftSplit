package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.DeleteRunUseCase
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.RunId
import javax.inject.Inject

class DeleteRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : DeleteRunUseCase {
    override suspend fun invoke(runId: RunId) {
        runRepository.delete(runId)
    }
}
