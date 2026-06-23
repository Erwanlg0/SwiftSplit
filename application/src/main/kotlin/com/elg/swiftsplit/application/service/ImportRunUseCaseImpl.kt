package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ImportRunUseCase
import com.elg.swiftsplit.application.port.output.RunFileParser
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.RunId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportRunUseCaseImpl @Inject constructor(
    private val runRepository: RunRepository,
    private val fileParser: RunFileParser
) : ImportRunUseCase {
    override suspend fun invoke(content: ByteArray): Result<Run> = withContext(Dispatchers.IO) {
        fileParser.parse(content).mapCatching { run ->
            val uniqueRun = run.copy(id = RunId.generate())
            runRepository.save(uniqueRun)
            uniqueRun
        }
    }
}
