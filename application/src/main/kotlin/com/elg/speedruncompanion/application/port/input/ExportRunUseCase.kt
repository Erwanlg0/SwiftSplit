package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.RunId

interface ExportRunUseCase {
    suspend operator fun invoke(runId: RunId): Result<ByteArray>
}
