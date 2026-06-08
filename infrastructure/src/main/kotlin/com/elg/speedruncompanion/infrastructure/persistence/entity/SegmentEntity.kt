package com.elg.speedruncompanion.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "segments",
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
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val orderIndex: Int,
    val name: String,
    val iconData: String?,
    val bestRealTimeMs: Long?,
    val bestGameTimeMs: Long?
)
