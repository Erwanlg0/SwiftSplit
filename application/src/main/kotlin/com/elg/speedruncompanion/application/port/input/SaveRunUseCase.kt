package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.Run

interface SaveRunUseCase {
    suspend operator fun invoke(run: Run)
}
