package com.elg.swiftsplit.infrastructure.persistence.dao

import androidx.room.*
import com.elg.swiftsplit.infrastructure.persistence.entity.*
import com.elg.swiftsplit.infrastructure.persistence.relation.RunWithDetails
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
        
        val allSplitTimes = mutableListOf<SplitTimeEntity>()
        val allSegmentHistory = mutableListOf<SegmentHistoryEntity>()
        
        for (i in segments.indices) {
            val segmentId = segmentIds[i]
            allSplitTimes.addAll(splitTimes[i].map { it.copy(segmentId = segmentId) })
            allSegmentHistory.addAll(segmentHistory[i].map { it.copy(segmentId = segmentId) })
        }
        
        if (allSplitTimes.isNotEmpty()) {
            insertSplitTimes(allSplitTimes)
        }
        if (allSegmentHistory.isNotEmpty()) {
            insertSegmentHistory(allSegmentHistory)
        }
        
        val attemptEntities = attempts.map { it.copy(runId = run.id) }
        insertAttempts(attemptEntities)
    }
}
