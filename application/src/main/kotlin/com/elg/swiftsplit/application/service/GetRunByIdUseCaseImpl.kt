package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.GetRunByIdUseCase
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.RunId
import javax.inject.Inject

class GetRunByIdUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : GetRunByIdUseCase {
    override suspend fun invoke(runId: RunId): Run? {
        return runRepository.getById(runId)
    }
}
