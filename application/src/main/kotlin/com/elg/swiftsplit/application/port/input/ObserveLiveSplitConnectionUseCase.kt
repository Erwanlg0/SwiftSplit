package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.application.port.output.ConnectionState
import kotlinx.coroutines.flow.Flow

interface ObserveLiveSplitConnectionUseCase {
    operator fun invoke(): Flow<ConnectionState>
}
