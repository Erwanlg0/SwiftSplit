package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.Run
import kotlinx.coroutines.flow.Flow

interface GetRunsUseCase {
    operator fun invoke(): Flow<List<Run>>
}
