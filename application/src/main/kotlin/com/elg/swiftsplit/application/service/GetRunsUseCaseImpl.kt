package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.GetRunsUseCase
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.Run
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRunsUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : GetRunsUseCase {
    override fun invoke(): Flow<List<Run>> {
        return runRepository.observeAll()
    }
}
