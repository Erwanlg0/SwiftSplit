package com.elg.swiftsplit.infrastructure.parser

import android.util.Xml
import com.elg.swiftsplit.application.port.output.RunFileParser
import com.elg.swiftsplit.domain.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LssFileParser @Inject constructor() : RunFileParser {

    override fun supportedExtensions(): List<String> = listOf("lss")

    override suspend fun parse(content: ByteArray): Result<Run> {
        return Result.runCatching {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(content), "UTF-8")

            var gameName = ""
            var categoryName = ""
            var offset = TimeSpan.ZERO
            var attemptCount = 0
            var platform = ""
            var region = ""
            var usesEmulator = false
            var gameIcon = ""
            val variables = mutableMapOf<String, String>()
            val attempts = mutableListOf<Attempt>()
            val segments = mutableListOf<Segment>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "GameIcon" -> gameIcon = parser.nextText()
                        "GameName" -> gameName = parser.nextText()
                        "CategoryName" -> categoryName = parser.nextText()
                        "Offset" -> offset = TimeSpan.fromTimeString(parser.nextText()) ?: TimeSpan.ZERO
                        "AttemptCount" -> attemptCount = parser.nextText().toIntOrNull() ?: 0
                        "Platform" -> {
                            usesEmulator = parser.getAttributeValue(null, "usesEmulator")?.toBoolean() ?: false
                            platform = parser.nextText()
                        }
                        "Region" -> region = parser.nextText()
                        "Variable" -> {
                            val name = parser.getAttributeValue(null, "name") ?: ""
                            val value = parser.nextText()
                            if (name.isNotEmpty()) {
                                variables[name] = value
                            }
                        }
                        "AttemptHistory" -> {
                            attempts.addAll(parseAttemptHistory(parser))
                        }
                        "Segments" -> {
                            segments.addAll(parseSegments(parser))
                        }
                    }
                }
                eventType = parser.next()
            }

            Run(
                gameInfo = GameInfo(
                    gameName = gameName,
                    categoryName = categoryName,
                    platform = platform,
                    region = region,
                    usesEmulator = usesEmulator,
                    variables = variables,
                    iconData = gameIcon.takeIf { it.isNotEmpty() }
                ),
                segments = segments,
                attemptCount = attemptCount,
                attemptHistory = attempts,
                offset = offset
            )
        }
    }

    private fun parseAttemptHistory(parser: XmlPullParser): List<Attempt> {
        val attempts = mutableListOf<Attempt>()
        val depth = parser.depth
        var eventType = parser.next()

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Attempt") {
                val id = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
                val started = parser.getAttributeValue(null, "started")
                val ended = parser.getAttributeValue(null, "ended")
                val isStartedSynced = parser.getAttributeValue(null, "isStartedSynced")?.toBoolean() ?: true
                val isEndedSynced = parser.getAttributeValue(null, "isEndedSynced")?.toBoolean() ?: true

                var realTime: TimeSpan? = null
                var gameTime: TimeSpan? = null
                var pauseTime: TimeSpan? = null

                val attemptDepth = parser.depth
                var attemptEvent = parser.next()
                while (!(attemptEvent == XmlPullParser.END_TAG && parser.depth == attemptDepth)) {
                    if (attemptEvent == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "RealTime" -> realTime = TimeSpan.fromTimeString(parser.nextText())
                            "GameTime" -> gameTime = TimeSpan.fromTimeString(parser.nextText())
                            "PauseTime" -> pauseTime = TimeSpan.fromTimeString(parser.nextText())
                        }
                    }
                    attemptEvent = parser.next()
                }

                attempts.add(
                    Attempt(
                        id = id,
                        startedAt = started,
                        endedAt = ended,
                        isStartedSynced = isStartedSynced,
                        isEndedSynced = isEndedSynced,
                        realTime = realTime,
                        gameTime = gameTime,
                        pauseTime = pauseTime
                    )
                )
            }
            eventType = parser.next()
        }
        return attempts
    }

    private fun parseSegments(parser: XmlPullParser): List<Segment> {
        val segments = mutableListOf<Segment>()
        val depth = parser.depth
        var eventType = parser.next()

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Segment") {
                var name = ""
                var iconData: String? = null
                var splitTimes = mutableMapOf<ComparisonName, SplitTime>()
                var bestSegmentTime: SplitTime? = null
                val segmentHistory = mutableListOf<SegmentHistoryEntry>()

                val segmentDepth = parser.depth
                var segmentEvent = parser.next()
                while (!(segmentEvent == XmlPullParser.END_TAG && parser.depth == segmentDepth)) {
                    if (segmentEvent == XmlPullParser.START_TAG) {
                        when (segmentEventName(parser)) {
                            "Name" -> name = parser.nextText()
                            "Icon" -> iconData = parser.nextText().takeIf { it.isNotEmpty() }
                            "SplitTimes" -> splitTimes.putAll(parseSplitTimes(parser))
                            "BestSegmentTime" -> bestSegmentTime = parseSplitTime(parser)
                            "SegmentHistory" -> segmentHistory.addAll(parseSegmentHistory(parser))
                        }
                    }
                    segmentEvent = parser.next()
                }

                segments.add(
                    Segment(
                        name = name,
                        iconData = iconData,
                        splitTimes = splitTimes,
                        bestSegmentTime = bestSegmentTime,
                        segmentHistory = segmentHistory
                    )
                )
            }
            eventType = parser.next()
        }
        return segments
    }

    private fun segmentEventName(parser: XmlPullParser): String = parser.name

    private fun parseSplitTimes(parser: XmlPullParser): Map<ComparisonName, SplitTime> {
        val splitTimes = mutableMapOf<ComparisonName, SplitTime>()
        val depth = parser.depth
        var eventType = parser.next()

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "SplitTime") {
                val name = parser.getAttributeValue(null, "name") ?: ""
                val splitTime = parseSplitTime(parser)
                if (name.isNotEmpty()) {
                    splitTimes[ComparisonName(name)] = splitTime
                }
            }
            eventType = parser.next()
        }
        return splitTimes
    }

    private fun parseSplitTime(parser: XmlPullParser): SplitTime {
        val depth = parser.depth
        var eventType = parser.next()
        var realTime: TimeSpan? = null
        var gameTime: TimeSpan? = null

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "RealTime" -> realTime = TimeSpan.fromTimeString(parser.nextText())
                    "GameTime" -> gameTime = TimeSpan.fromTimeString(parser.nextText())
                }
            }
            eventType = parser.next()
        }
        return SplitTime(realTime, gameTime)
    }

    private fun parseSegmentHistory(parser: XmlPullParser): List<SegmentHistoryEntry> {
        val history = mutableListOf<SegmentHistoryEntry>()
        val depth = parser.depth
        var eventType = parser.next()

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == depth)) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Time") {
                val id = parser.getAttributeValue(null, "id")?.toIntOrNull() ?: 0
                val splitTime = parseSplitTime(parser)
                history.add(SegmentHistoryEntry(id, splitTime))
            }
            eventType = parser.next()
        }
        return history
    }
}
