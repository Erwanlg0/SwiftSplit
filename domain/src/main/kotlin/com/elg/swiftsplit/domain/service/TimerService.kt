package com.elg.swiftsplit.domain.service

import com.elg.swiftsplit.domain.model.*

class TimerService(private val clock: Clock) {

    fun start(run: Run, comparison: ComparisonName, timingMethod: TimingMethod): ActiveRun {
        return ActiveRun(
            run = run,
            currentSegmentIndex = 0,
            splitTimes = List(run.segments.size) { null },
            startTime = clock.currentTimeMillis(),
            pauseAccumulator = 0L,
            pauseStart = null,
            comparison = comparison,
            timingMethod = timingMethod
        )
    }

    fun split(activeRun: ActiveRun): Pair<ActiveRun, TimerEvent> {
        val currentTimeMillis = clock.currentTimeMillis()
        if (activeRun.startTime == 0L || activeRun.pauseStart != null) return activeRun to TimerEvent.Paused 

        val currentIndex = activeRun.currentSegmentIndex
        
        if (currentIndex >= activeRun.run.segments.size) return activeRun to TimerEvent.Paused

        val elapsed = getElapsedTime(activeRun, currentTimeMillis)

        val newSplitTimes = activeRun.splitTimes.toMutableList()
        newSplitTimes[currentIndex] = elapsed

        val nextIndex = currentIndex + 1
        val isFinished = nextIndex >= activeRun.run.segments.size

        val updatedRun = activeRun.copy(
            currentSegmentIndex = nextIndex,
            splitTimes = newSplitTimes
        )

        val event = if (isFinished) {
            TimerEvent.Finished(elapsed)
        } else {
            TimerEvent.Split(currentIndex, elapsed)
        }

        return updatedRun to event
    }

    fun skip(activeRun: ActiveRun): Pair<ActiveRun, TimerEvent> {
        if (activeRun.startTime == 0L || activeRun.pauseStart != null) return activeRun to TimerEvent.Paused

        val currentIndex = activeRun.currentSegmentIndex
        val newSplitTimes = activeRun.splitTimes.toMutableList()
        newSplitTimes[currentIndex] = null 

        val nextIndex = currentIndex + 1
        val isFinished = nextIndex >= activeRun.run.segments.size

        val updatedRun = activeRun.copy(
            currentSegmentIndex = nextIndex,
            splitTimes = newSplitTimes
        )

        val event = if (isFinished) {
            val lastValidSplit = newSplitTimes.lastOrNull { it != null } ?: TimeSpan.ZERO
            TimerEvent.Finished(lastValidSplit)
        } else {
            TimerEvent.Skipped(currentIndex)
        }

        return updatedRun to event
    }

    fun undoSplit(activeRun: ActiveRun): Pair<ActiveRun, TimerEvent> {
        val currentIndex = activeRun.currentSegmentIndex
        if (currentIndex <= 0) return activeRun to TimerEvent.Started

        val prevIndex = currentIndex - 1
        val newSplitTimes = activeRun.splitTimes.toMutableList()
        newSplitTimes[prevIndex] = null

        val updatedRun = activeRun.copy(
            currentSegmentIndex = prevIndex,
            splitTimes = newSplitTimes
        )

        return updatedRun to TimerEvent.Undone(prevIndex)
    }

    fun pause(activeRun: ActiveRun): Pair<ActiveRun, TimerEvent> {
        val currentTimeMillis = clock.currentTimeMillis()
        if (activeRun.startTime == 0L || activeRun.pauseStart != null) return activeRun to TimerEvent.Paused

        val updatedRun = activeRun.copy(
            pauseStart = currentTimeMillis
        )
        return updatedRun to TimerEvent.Paused
    }

    fun resume(activeRun: ActiveRun): Pair<ActiveRun, TimerEvent> {
        val currentTimeMillis = clock.currentTimeMillis()
        val pauseStart = activeRun.pauseStart ?: return activeRun to TimerEvent.Resumed
        val pauseDuration = currentTimeMillis - pauseStart

        val updatedRun = activeRun.copy(
            pauseStart = null,
            pauseAccumulator = activeRun.pauseAccumulator + pauseDuration
        )
        return updatedRun to TimerEvent.Resumed
    }

    fun reset(activeRun: ActiveRun): TimerEvent {
        return TimerEvent.Reset(saveAttempt = activeRun.currentSegmentIndex > 0)
    }

    fun getElapsedTime(activeRun: ActiveRun, currentTimeMillis: Long = clock.currentTimeMillis()): TimeSpan {
        if (activeRun.startTime == 0L) return TimeSpan.ZERO
        val pauseStart = activeRun.pauseStart
        val activeTime = if (pauseStart != null) {
            pauseStart - activeRun.startTime
        } else {
            currentTimeMillis - activeRun.startTime
        }
        return TimeSpan(activeTime - activeRun.pauseAccumulator)
    }

    fun getCurrentDelta(activeRun: ActiveRun, elapsed: TimeSpan): Delta? {
        val currentIndex = activeRun.currentSegmentIndex
        if (currentIndex < 0 || currentIndex >= activeRun.run.segments.size) return null

        val currentSegment = activeRun.run.segments[currentIndex]
        val compSplit = currentSegment.splitTimes[activeRun.comparison]?.getTime(activeRun.timingMethod) ?: return null

        val previousCurrentSplit = if (currentIndex > 0) activeRun.splitTimes[currentIndex - 1] else null
        val previousComparisonSplit = if (currentIndex > 0) {
            activeRun.run.segments[currentIndex - 1].splitTimes[activeRun.comparison]?.getTime(activeRun.timingMethod)
        } else {
            TimeSpan.ZERO
        }

        return SplitTimeCalculator.getDelta(
            currentSplit = elapsed,
            comparisonSplit = compSplit,
            previousCurrentSplit = previousCurrentSplit,
            previousComparisonSplit = previousComparisonSplit
        )
    }

    fun getPreviousSegmentDelta(activeRun: ActiveRun): Delta? {
        val currentIndex = activeRun.currentSegmentIndex
        if (currentIndex <= 0) return null

        val prevIndex = currentIndex - 1
        val prevSplit = activeRun.splitTimes[prevIndex] ?: return null
        val prevSegment = activeRun.run.segments[prevIndex]
        val compSplit = prevSegment.splitTimes[activeRun.comparison]?.getTime(activeRun.timingMethod) ?: return null

        val prevPrevCurrentSplit = if (prevIndex > 0) activeRun.splitTimes[prevIndex - 1] else null
        val prevPrevComparisonSplit = if (prevIndex > 0) {
            activeRun.run.segments[prevIndex - 1].splitTimes[activeRun.comparison]?.getTime(activeRun.timingMethod)
        } else {
            TimeSpan.ZERO
        }

        return SplitTimeCalculator.getDelta(
            currentSplit = prevSplit,
            comparisonSplit = compSplit,
            previousCurrentSplit = prevPrevCurrentSplit,
            previousComparisonSplit = prevPrevComparisonSplit
        )
    }
}
