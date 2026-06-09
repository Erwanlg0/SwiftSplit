package com.elg.swiftsplit.application.port.output

import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.RunId
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    fun observeAll(): Flow<List<Run>>
    suspend fun getById(id: RunId): Run?
    suspend fun save(run: Run)
    suspend fun update(run: Run)
    suspend fun delete(id: RunId)
}
