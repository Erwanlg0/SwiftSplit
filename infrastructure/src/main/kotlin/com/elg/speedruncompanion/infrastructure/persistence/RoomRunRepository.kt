package com.elg.speedruncompanion.infrastructure.persistence

import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.RunId
import com.elg.speedruncompanion.infrastructure.persistence.dao.RunDao
import com.elg.speedruncompanion.infrastructure.persistence.mapper.EntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRunRepository @Inject constructor(
    private val runDao: RunDao
) : RunRepository {

    override fun observeAll(): Flow<List<Run>> {
        return runDao.observeAll().map { list ->
            list.map { EntityMapper.toDomain(it) }
        }
    }

    override suspend fun getById(id: RunId): Run? {
        return runDao.getById(id.value)?.let { EntityMapper.toDomain(it) }
    }

    override suspend fun save(run: Run) {
        val dbStruct = EntityMapper.toDatabase(run)
        runDao.saveRun(
            run = dbStruct.runEntity,
            segments = dbStruct.segmentEntities,
            splitTimes = dbStruct.splitTimeEntities,
            attempts = dbStruct.attemptEntities,
            segmentHistory = dbStruct.segmentHistoryEntities
        )
    }

    override suspend fun update(run: Run) {
        save(run)
    }

    override suspend fun delete(id: RunId) {
        runDao.deleteRunById(id.value)
    }
}
