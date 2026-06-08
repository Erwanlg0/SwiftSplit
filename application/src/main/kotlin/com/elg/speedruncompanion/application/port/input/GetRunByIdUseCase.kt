package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.RunId

interface GetRunByIdUseCase {
    suspend operator fun invoke(runId: RunId): Run?
}
