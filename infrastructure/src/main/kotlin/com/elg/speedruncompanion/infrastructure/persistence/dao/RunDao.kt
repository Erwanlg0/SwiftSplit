package com.elg.speedruncompanion.infrastructure.persistence.dao

import androidx.room.*
import com.elg.speedruncompanion.infrastructure.persistence.entity.*
import com.elg.speedruncompanion.infrastructure.persistence.relation.RunWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {

    @Transaction
    @Query("SELECT * FROM runs")
    fun observeAll(): Flow<List<RunWithDetails>>

    @Transaction
    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getById(id: String): RunWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<SegmentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitTimes(splitTimes: List<SplitTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<AttemptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegmentHistory(segmentHistory: List<SegmentHistoryEntity>)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRunById(id: String)

    @Transaction
    suspend fun saveRun(
        run: RunEntity,
        segments: List<SegmentEntity>,
        splitTimes: List<List<SplitTimeEntity>>,
        attempts: List<AttemptEntity>,
        segmentHistory: List<List<SegmentHistoryEntity>>
    ) {
        deleteRunById(run.id)
        
        insertRun(run)
        val segmentIds = insertSegments(segments)
        
        for (i in segments.indices) {
            val segmentId = segmentIds[i]
            val segmentSplits = splitTimes[i].map { it.copy(segmentId = segmentId) }
            val segmentHist = segmentHistory[i].map { it.copy(segmentId = segmentId) }
            insertSplitTimes(segmentSplits)
            insertSegmentHistory(segmentHist)
        }
        
        val attemptEntities = attempts.map { it.copy(runId = run.id) }
        insertAttempts(attemptEntities)
    }
}
