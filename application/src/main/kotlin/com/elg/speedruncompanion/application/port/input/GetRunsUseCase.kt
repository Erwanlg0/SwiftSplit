package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.Run
import kotlinx.coroutines.flow.Flow

interface GetRunsUseCase {
    operator fun invoke(): Flow<List<Run>>
}
