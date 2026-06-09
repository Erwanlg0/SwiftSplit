package com.elg.swiftsplit.infrastructure.persistence.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.elg.swiftsplit.infrastructure.persistence.entity.*

data class SegmentWithDetails(
    @Embedded val segment: SegmentEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "segmentId"
    )
    val splitTimes: List<SplitTimeEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "segmentId"
    )
    val segmentHistory: List<SegmentHistoryEntity>
)

data class RunWithDetails(
    @Embedded val run: RunEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "runId",
        entity = SegmentEntity::class
    )
    val segments: List<SegmentWithDetails>,

    @Relation(
        parentColumn = "id",
        entityColumn = "runId"
    )
    val attempts: List<AttemptEntity>
)
