package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ExportRunUseCase
import com.elg.swiftsplit.application.port.output.RunFileExporter
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.RunId
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
