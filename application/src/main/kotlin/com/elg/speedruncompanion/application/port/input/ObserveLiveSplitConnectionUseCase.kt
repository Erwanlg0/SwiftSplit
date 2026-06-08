package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.application.port.output.ConnectionState
import kotlinx.coroutines.flow.Flow

interface ObserveLiveSplitConnectionUseCase {
    operator fun invoke(): Flow<ConnectionState>
}
