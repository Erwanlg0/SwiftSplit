package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.elg.swiftsplit.domain.model.ComparisonName
import com.elg.swiftsplit.domain.model.Delta
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimingMethod
import kotlin.math.abs

internal fun shouldShowLiveDeltaInSplit(
    currentElapsed: TimeSpan,
    currentSegmentIndex: Int,
    run: Run,
    comparisonName: String,
    timingMethod: TimingMethod,
    thresholdSeconds: Int,
    isTimerRunning: Boolean
): Boolean {
    if (!isTimerRunning || thresholdSeconds <= 0) return false
    if (currentSegmentIndex !in run.segments.indices) return false

    val compSplit = run.segments[currentSegmentIndex]
        .splitTimes[ComparisonName(comparisonName)]
        ?.getTime(timingMethod)
        ?: return false

    val diffMs = abs((currentElapsed - compSplit).totalMilliseconds)
    return diffMs <= thresholdSeconds * 1000L
}

@Composable
fun SplitList(
    run: Run,
    currentSegmentIndex: Int,
    splitTimes: List<TimeSpan?>,
    comparisonName: String,
    timingMethod: TimingMethod,
    timeFormat: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    layoutPreferences: TimerLayoutPreferences = TimerLayoutPreferences.DEFAULT,
    modifier: Modifier = Modifier,
    completedSplitsVisible: Int = 0,
    currentElapsed: TimeSpan? = null,
    activeSegmentDelta: Delta? = null,
    isTimerRunning: Boolean = false,
    splitApproachThresholdSeconds: Int = layoutPreferences.splitApproachThresholdSeconds
) {
    val listState = rememberLazyListState()

    val scrollTarget = if (completedSplitsVisible > 0) {
        (currentSegmentIndex - completedSplitsVisible).coerceAtLeast(0)
    } else {
        currentSegmentIndex
    }

    LaunchedEffect(scrollTarget) {
        if (scrollTarget in run.segments.indices) {
            listState.animateScrollToItem(scrollTarget)
        }
    }

    val showLiveDelta = currentElapsed != null && activeSegmentDelta != null &&
        shouldShowLiveDeltaInSplit(
            currentElapsed = currentElapsed,
            currentSegmentIndex = currentSegmentIndex,
            run = run,
            comparisonName = comparisonName,
            timingMethod = timingMethod,
            thresholdSeconds = splitApproachThresholdSeconds,
            isTimerRunning = isTimerRunning
        )

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(run.segments) { index, segment ->
            val isCompleted = index < currentSegmentIndex
            val previousCurrentSplit = if (index > 0) splitTimes[index - 1] else null
            val previousComparisonSplit = if (index > 0) {
                run.segments[index - 1].splitTimes[ComparisonName(comparisonName)]?.getTime(timingMethod)
            } else {
                TimeSpan.ZERO
            }

            SplitRow(
                segment = segment,
                isActive = index == currentSegmentIndex,
                isCompleted = isCompleted,
                elapsedSplit = splitTimes.getOrNull(index),
                comparisonName = comparisonName,
                timingMethod = timingMethod,
                previousCurrentSplit = previousCurrentSplit,
                previousComparisonSplit = previousComparisonSplit,
                timeFormat = timeFormat,
                layoutPreferences = layoutPreferences,
                liveDelta = if (index == currentSegmentIndex && showLiveDelta) activeSegmentDelta else null,
                liveElapsed = if (index == currentSegmentIndex && showLiveDelta) currentElapsed else null
            )
        }
    }
}
