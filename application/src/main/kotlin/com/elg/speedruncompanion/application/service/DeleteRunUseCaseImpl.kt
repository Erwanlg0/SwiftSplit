package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.DeleteRunUseCase
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.RunId
import javax.inject.Inject

class DeleteRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : DeleteRunUseCase {
    override suspend fun invoke(runId: RunId) {
        runRepository.delete(runId)
    }
}
