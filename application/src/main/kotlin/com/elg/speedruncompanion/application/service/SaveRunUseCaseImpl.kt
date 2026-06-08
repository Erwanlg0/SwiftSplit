package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.SaveRunUseCase
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import javax.inject.Inject

class SaveRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : SaveRunUseCase {
    override suspend fun invoke(run: Run) {
        runRepository.save(run)
    }
}
