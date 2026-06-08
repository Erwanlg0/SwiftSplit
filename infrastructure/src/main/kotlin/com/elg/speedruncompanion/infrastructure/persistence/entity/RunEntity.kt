package com.elg.speedruncompanion.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val id: String,
    val gameName: String,
    val categoryName: String,
    val platform: String,
    val region: String,
    val usesEmulator: Boolean,
    val attemptCount: Int,
    val offsetMs: Long,
    val layoutPath: String?,
    val autoSplitterSettings: String?,
    val variablesJson: String
)
