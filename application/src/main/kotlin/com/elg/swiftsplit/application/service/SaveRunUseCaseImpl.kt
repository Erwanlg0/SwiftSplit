package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.SaveRunUseCase
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.Run
import javax.inject.Inject

class SaveRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository
) : SaveRunUseCase {
    override suspend fun invoke(run: Run) {
        runRepository.save(run)
    }
}
