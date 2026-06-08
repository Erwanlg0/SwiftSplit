package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.ImportRunUseCase
import com.elg.speedruncompanion.application.port.output.RunFileParser
import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.RunId
import javax.inject.Inject

class ImportRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository,
    private val fileParser: RunFileParser
) : ImportRunUseCase {
    override suspend fun invoke(content: ByteArray): Result<Run> {
        return fileParser.parse(content).mapCatching { run ->
            val uniqueRun = run.copy(id = RunId.generate())
            runRepository.save(uniqueRun)
            uniqueRun
        }
    }
}
