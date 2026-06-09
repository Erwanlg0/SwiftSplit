package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.UpdateRunUseCase
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.Run
import javax.inject.Inject

class UpdateRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : UpdateRunUseCase {
    override suspend fun invoke(run: Run) {
        runRepository.update(run)
    }
}
