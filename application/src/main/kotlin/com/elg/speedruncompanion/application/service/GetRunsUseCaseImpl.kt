package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.GetRunsUseCase
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRunsUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : GetRunsUseCase {
    override fun invoke(): Flow<List<Run>> {
        return runRepository.observeAll()
    }
}
