package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.Run

interface UpdateRunUseCase {
    suspend operator fun invoke(run: Run)
}
