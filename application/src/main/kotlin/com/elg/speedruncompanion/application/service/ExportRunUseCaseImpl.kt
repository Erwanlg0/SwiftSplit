package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.ExportRunUseCase
import com.elg.speedruncompanion.application.port.output.RunFileExporter
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.RunId
import javax.inject.Inject

class ExportRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository,
    private val fileExporter: RunFileExporter
) : ExportRunUseCase {
    override suspend fun invoke(runId: RunId): Result<ByteArray> {
        val run = runRepository.getById(runId) ?: return Result.failure(Exception("Run not found"))
        return Result.runCatching {
            fileExporter.export(run)
        }
    }
}
