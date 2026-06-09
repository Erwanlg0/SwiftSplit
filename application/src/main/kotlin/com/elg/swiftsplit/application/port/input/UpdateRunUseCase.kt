package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.Run

interface UpdateRunUseCase {
    suspend operator fun invoke(run: Run)
}
