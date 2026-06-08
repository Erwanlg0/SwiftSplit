package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.Run

interface ImportRunUseCase {
    suspend operator fun invoke(content: ByteArray): Result<Run>
}
