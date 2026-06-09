package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.Run

interface SaveRunUseCase {
    suspend operator fun invoke(run: Run)
}
