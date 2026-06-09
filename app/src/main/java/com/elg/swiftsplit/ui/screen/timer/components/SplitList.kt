package com.elg.swiftsplit.ui.screen.timer.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.TimeFormatOptions
import com.elg.swiftsplit.domain.model.TimeSpan
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import com.elg.swiftsplit.domain.model.TimingMethod

@Composable
fun SplitList(
    run: Run,
    currentSegmentIndex: Int,
    splitTimes: List<TimeSpan?>,
    comparisonName: String,
    timingMethod: TimingMethod,
    timeFormat: TimeFormatOptions = TimeFormatOptions.DEFAULT,
    layoutPreferences: TimerLayoutPreferences = TimerLayoutPreferences.DEFAULT,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentSegmentIndex) {
        if (currentSegmentIndex in run.segments.indices) {
            listState.animateScrollToItem(currentSegmentIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(run.segments) { index, segment ->
            val isCompleted = index < currentSegmentIndex
            val previousCurrentSplit = if (index > 0) splitTimes[index - 1] else null
            val previousComparisonSplit = if (index > 0) {
                run.segments[index - 1].splitTimes[com.elg.swiftsplit.domain.model.ComparisonName(comparisonName)]?.getTime(timingMethod)
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
                layoutPreferences = layoutPreferences
            )
        }
    }
}
