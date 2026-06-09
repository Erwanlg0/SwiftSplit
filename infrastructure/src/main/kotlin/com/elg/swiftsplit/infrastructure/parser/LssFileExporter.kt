package com.elg.swiftsplit.infrastructure.parser

import android.util.Xml
import com.elg.swiftsplit.application.port.output.RunFileExporter
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.TimeSpan
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LssFileExporter @Inject constructor() : RunFileExporter {

    override suspend fun export(run: Run): ByteArray {
        val writer = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        serializer.startTag(null, "Run")
        serializer.attribute(null, "version", "1.7.0")

        serializer.startTag(null, "GameIcon")
        serializer.text("")
        serializer.endTag(null, "GameIcon")

        serializer.startTag(null, "GameName")
        serializer.text(run.gameInfo.gameName)
        serializer.endTag(null, "GameName")

        serializer.startTag(null, "CategoryName")
        serializer.text(run.gameInfo.categoryName)
        serializer.endTag(null, "CategoryName")

        serializer.startTag(null, "LayoutPath")
        serializer.text(run.layoutPath ?: "")
        serializer.endTag(null, "LayoutPath")

        serializer.startTag(null, "Offset")
        serializer.text(formatTimeSpan(run.offset))
        serializer.endTag(null, "Offset")

        serializer.startTag(null, "AttemptCount")
        serializer.text(run.attemptCount.toString())
        serializer.endTag(null, "AttemptCount")

        
        serializer.startTag(null, "Metadata")
        serializer.startTag(null, "Run")
        serializer.attribute(null, "id", "")
        serializer.endTag(null, "Run")

        serializer.startTag(null, "Platform")
        serializer.attribute(null, "usesEmulator", run.gameInfo.usesEmulator.toString())
        serializer.text(run.gameInfo.platform)
        serializer.endTag(null, "Platform")

        serializer.startTag(null, "Region")
        serializer.text(run.gameInfo.region)
        serializer.endTag(null, "Region")

        serializer.startTag(null, "Variables")
        for ((key, value) in run.gameInfo.variables) {
            serializer.startTag(null, "Variable")
            serializer.attribute(null, "name", key)
            serializer.text(value)
            serializer.endTag(null, "Variable")
        }
        serializer.endTag(null, "Variables")
        serializer.endTag(null, "Metadata")

        
        serializer.startTag(null, "AttemptHistory")
        for (attempt in run.attemptHistory) {
            serializer.startTag(null, "Attempt")
            serializer.attribute(null, "id", attempt.id.toString())
            if (attempt.startedAt != null) serializer.attribute(null, "started", attempt.startedAt)
            if (attempt.endedAt != null) serializer.attribute(null, "ended", attempt.endedAt)
            serializer.attribute(null, "isStartedSynced", attempt.isStartedSynced.toString())
            serializer.attribute(null, "isEndedSynced", attempt.isEndedSynced.toString())

            if (attempt.realTime != null || attempt.gameTime != null || attempt.pauseTime != null) {
                if (attempt.realTime != null) {
                    serializer.startTag(null, "RealTime")
                    serializer.text(formatTimeSpan(attempt.realTime))
                    serializer.endTag(null, "RealTime")
                }
                if (attempt.gameTime != null) {
                    serializer.startTag(null, "GameTime")
                    serializer.text(formatTimeSpan(attempt.gameTime))
                    serializer.endTag(null, "GameTime")
                }
                if (attempt.pauseTime != null) {
                    serializer.startTag(null, "PauseTime")
                    serializer.text(formatTimeSpan(attempt.pauseTime))
                    serializer.endTag(null, "PauseTime")
                }
            }
            serializer.endTag(null, "Attempt")
        }
        serializer.endTag(null, "AttemptHistory")

        
        serializer.startTag(null, "Segments")
        for (segment in run.segments) {
            serializer.startTag(null, "Segment")
            serializer.startTag(null, "Name")
            serializer.text(segment.name)
            serializer.endTag(null, "Name")

            serializer.startTag(null, "Icon")
            serializer.text(segment.iconData ?: "")
            serializer.endTag(null, "Icon")

            
            serializer.startTag(null, "SplitTimes")
            for ((compName, splitTime) in segment.splitTimes) {
                serializer.startTag(null, "SplitTime")
                serializer.attribute(null, "name", compName.name)
                if (splitTime.realTime != null) {
                    serializer.startTag(null, "RealTime")
                    serializer.text(formatTimeSpan(splitTime.realTime))
                    serializer.endTag(null, "RealTime")
                }
                if (splitTime.gameTime != null) {
                    serializer.startTag(null, "GameTime")
                    serializer.text(formatTimeSpan(splitTime.gameTime))
                    serializer.endTag(null, "GameTime")
                }
                serializer.endTag(null, "SplitTime")
            }
            serializer.endTag(null, "SplitTimes")

            
            val bestSegmentTime = segment.bestSegmentTime
            if (bestSegmentTime != null) {
                serializer.startTag(null, "BestSegmentTime")
                if (bestSegmentTime.realTime != null) {
                    serializer.startTag(null, "RealTime")
                    serializer.text(formatTimeSpan(bestSegmentTime.realTime))
                    serializer.endTag(null, "RealTime")
                }
                if (bestSegmentTime.gameTime != null) {
                    serializer.startTag(null, "GameTime")
                    serializer.text(formatTimeSpan(bestSegmentTime.gameTime))
                    serializer.endTag(null, "GameTime")
                }
                serializer.endTag(null, "BestSegmentTime")
            }

            
            serializer.startTag(null, "SegmentHistory")
            for (historyEntry in segment.segmentHistory) {
                serializer.startTag(null, "Time")
                serializer.attribute(null, "id", historyEntry.attemptId.toString())
                if (historyEntry.time.realTime != null) {
                    serializer.startTag(null, "RealTime")
                    serializer.text(formatTimeSpan(historyEntry.time.realTime))
                    serializer.endTag(null, "RealTime")
                }
                if (historyEntry.time.gameTime != null) {
                    serializer.startTag(null, "GameTime")
                    serializer.text(formatTimeSpan(historyEntry.time.gameTime))
                    serializer.endTag(null, "GameTime")
                }
                serializer.endTag(null, "Time")
            }
            serializer.endTag(null, "SegmentHistory")

            serializer.endTag(null, "Segment")
        }
        serializer.endTag(null, "Segments")

        serializer.startTag(null, "AutoSplitterSettings")
        serializer.text(run.autoSplitterSettings ?: "")
        serializer.endTag(null, "AutoSplitterSettings")

        serializer.endTag(null, "Run")
        serializer.endDocument()

        return writer.toString().toByteArray(Charsets.UTF_8)
    }

    private fun formatTimeSpan(ts: TimeSpan?): String {
        if (ts == null) return ""
        val ms = ts.totalMilliseconds
        val sign = if (ms < 0) "-" else ""
        val absMs = kotlin.math.abs(ms)
        val h = absMs / 3600000
        val m = (absMs % 3600000) / 60000
        val s = (absMs % 60000) / 1000
        val remMs = absMs % 1000
        val ns100 = remMs * 10000
        return String.format(java.util.Locale.US, "%s%02d:%02d:%02d.%07d", sign, h, m, s, ns100)
    }
}
