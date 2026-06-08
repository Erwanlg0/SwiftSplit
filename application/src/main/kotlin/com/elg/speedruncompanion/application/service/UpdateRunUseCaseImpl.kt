package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.UpdateRunUseCase
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import javax.inject.Inject

class UpdateRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : UpdateRunUseCase {
    override suspend fun invoke(run: Run) {
        runRepository.update(run)
    }
}
