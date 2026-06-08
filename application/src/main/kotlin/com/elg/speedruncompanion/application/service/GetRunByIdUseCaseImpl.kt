package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.GetRunByIdUseCase
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.RunId
import javax.inject.Inject

class GetRunByIdUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : GetRunByIdUseCase {
    override suspend fun invoke(runId: RunId): Run? {
        return runRepository.getById(runId)
    }
}
