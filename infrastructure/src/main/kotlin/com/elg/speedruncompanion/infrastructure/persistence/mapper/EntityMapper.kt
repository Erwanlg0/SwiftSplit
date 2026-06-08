package com.elg.speedruncompanion.infrastructure.persistence.mapper

import com.elg.speedruncompanion.domain.model.*
import com.elg.speedruncompanion.infrastructure.persistence.entity.*
import com.elg.speedruncompanion.infrastructure.persistence.relation.RunWithDetails
import com.elg.speedruncompanion.infrastructure.persistence.relation.SegmentWithDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EntityMapper {

    fun toDomain(runDetails: RunWithDetails): Run {
        val runEntity = runDetails.run
        val variables = try {
            Json.decodeFromString<Map<String, String>>(runEntity.variablesJson)
        } catch (e: Exception) {
            emptyMap()
        }

        val gameInfo = GameInfo(
            gameName = runEntity.gameName,
            categoryName = runEntity.categoryName,
            platform = runEntity.platform,
            region = runEntity.region,
            usesEmulator = runEntity.usesEmulator,
            variables = variables
        )

        val attempts = runDetails.attempts.map { toDomain(it) }.sortedBy { it.id }
        
        val segments = runDetails.segments
            .sortedBy { it.segment.orderIndex }
            .map { toDomain(it) }

        return Run(
            id = RunId(runEntity.id),
            gameInfo = gameInfo,
            segments = segments,
            attemptCount = runEntity.attemptCount,
            attemptHistory = attempts,
            offset = TimeSpan(runEntity.offsetMs),
            layoutPath = runEntity.layoutPath,
            autoSplitterSettings = runEntity.autoSplitterSettings
        )
    }

    private fun toDomain(entity: AttemptEntity): Attempt {
        return Attempt(
            id = entity.attemptId,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            isStartedSynced = entity.isStartedSynced,
            isEndedSynced = entity.isEndedSynced,
            realTime = entity.realTimeMs?.let { TimeSpan(it) },
            gameTime = entity.gameTimeMs?.let { TimeSpan(it) },
            pauseTime = entity.pauseTimeMs?.let { TimeSpan(it) }
        )
    }

    private fun toDomain(details: SegmentWithDetails): Segment {
        val seg = details.segment
        val splitTimes = details.splitTimes.associate { entity ->
            ComparisonName(entity.comparisonName) to SplitTime(
                realTime = entity.realTimeMs?.let { TimeSpan(it) },
                gameTime = entity.gameTimeMs?.let { TimeSpan(it) }
            )
        }

        val bestSegment = if (seg.bestRealTimeMs != null || seg.bestGameTimeMs != null) {
            SplitTime(
                realTime = seg.bestRealTimeMs?.let { TimeSpan(it) },
                gameTime = seg.bestGameTimeMs?.let { TimeSpan(it) }
            )
        } else {
            null
        }

        val history = details.segmentHistory.map { entity ->
            SegmentHistoryEntry(
                attemptId = entity.attemptId,
                time = SplitTime(
                    realTime = entity.realTimeMs?.let { TimeSpan(it) },
                    gameTime = entity.gameTimeMs?.let { TimeSpan(it) }
                )
            )
        }.sortedBy { it.attemptId }

        return Segment(
            name = seg.name,
            iconData = seg.iconData,
            splitTimes = splitTimes,
            bestSegmentTime = bestSegment,
            segmentHistory = history
        )
    }

    // === Database Mappings ===

    data class DatabaseStructure(
        val runEntity: RunEntity,
        val segmentEntities: List<SegmentEntity>,
        val splitTimeEntities: List<List<SplitTimeEntity>>,
        val attemptEntities: List<AttemptEntity>,
        val segmentHistoryEntities: List<List<SegmentHistoryEntity>>
    )

    fun toDatabase(run: Run): DatabaseStructure {
        val variablesJson = try {
            Json.encodeToString(run.gameInfo.variables)
        } catch (e: Exception) {
            "{}"
        }

        val runEntity = RunEntity(
            id = run.id.value,
            gameName = run.gameInfo.gameName,
            categoryName = run.gameInfo.categoryName,
            platform = run.gameInfo.platform,
            region = run.gameInfo.region,
            usesEmulator = run.gameInfo.usesEmulator,
            attemptCount = run.attemptCount,
            offsetMs = run.offset.totalMilliseconds,
            layoutPath = run.layoutPath,
            autoSplitterSettings = run.autoSplitterSettings,
            variablesJson = variablesJson
        )

        val segmentEntities = mutableListOf<SegmentEntity>()
        val splitTimeEntitiesList = mutableListOf<List<SplitTimeEntity>>()
        val segmentHistoryEntitiesList = mutableListOf<List<SegmentHistoryEntity>>()

        run.segments.forEachIndexed { index, segment ->
            segmentEntities.add(
                SegmentEntity(
                    runId = run.id.value,
                    orderIndex = index,
                    name = segment.name,
                    iconData = segment.iconData,
                    bestRealTimeMs = segment.bestSegmentTime?.realTime?.totalMilliseconds,
                    bestGameTimeMs = segment.bestSegmentTime?.gameTime?.totalMilliseconds
                )
            )

            val splits = segment.splitTimes.map { (compName, splitTime) ->
                SplitTimeEntity(
                    segmentId = 0, // set by dao transaction
                    comparisonName = compName.name,
                    realTimeMs = splitTime.realTime?.totalMilliseconds,
                    gameTimeMs = splitTime.gameTime?.totalMilliseconds
                )
            }
            splitTimeEntitiesList.add(splits)

            val history = segment.segmentHistory.map { entry ->
                SegmentHistoryEntity(
                    segmentId = 0, // set by dao transaction
                    attemptId = entry.attemptId,
                    realTimeMs = entry.time.realTime?.totalMilliseconds,
                    gameTimeMs = entry.time.gameTime?.totalMilliseconds
                )
            }
            segmentHistoryEntitiesList.add(history)
        }

        val attemptEntities = run.attemptHistory.map { attempt ->
            AttemptEntity(
                runId = run.id.value,
                attemptId = attempt.id,
                startedAt = attempt.startedAt,
                endedAt = attempt.endedAt,
                isStartedSynced = attempt.isStartedSynced,
                isEndedSynced = attempt.isEndedSynced,
                realTimeMs = attempt.realTime?.totalMilliseconds,
                gameTimeMs = attempt.gameTime?.totalMilliseconds,
                pauseTimeMs = attempt.pauseTime?.totalMilliseconds
            )
        }

        return DatabaseStructure(
            runEntity = runEntity,
            segmentEntities = segmentEntities,
            splitTimeEntities = splitTimeEntitiesList,
            attemptEntities = attemptEntities,
            segmentHistoryEntities = segmentHistoryEntitiesList
        )
    }
}
