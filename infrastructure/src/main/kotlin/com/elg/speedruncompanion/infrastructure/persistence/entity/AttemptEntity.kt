package com.elg.speedruncompanion.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("runId")]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val attemptId: Int,
    val startedAt: String?,
    val endedAt: String?,
    val isStartedSynced: Boolean,
    val isEndedSynced: Boolean,
    val realTimeMs: Long?,
    val gameTimeMs: Long?,
    val pauseTimeMs: Long?
)
