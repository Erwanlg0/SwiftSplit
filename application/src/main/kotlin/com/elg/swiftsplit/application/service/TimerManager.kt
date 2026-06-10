package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.output.HapticFeedbackPort
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.*
import com.elg.swiftsplit.domain.service.Clock
import com.elg.swiftsplit.domain.service.TimerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val runRepository: RunRepository,
    private val timerService: TimerService,
    private val settingsPort: SettingsPort,
    private val clock: Clock,
    private val hapticFeedbackPort: HapticFeedbackPort
) {
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: Flow<TimerState> = _timerState.asStateFlow()

    private var activeRun: ActiveRun? = null
    private var lastSplitTime = 0L

    suspend fun start(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod) {
        val run = runRepository.getById(runId) ?: return
        val correctedRun = if (run.segments.isEmpty()) {
            run.copy(segments = listOf(Segment(name = "Finish")))
        } else {
            run
        }
        val newActiveRun = timerService.start(correctedRun, comparison, timingMethod)
        activeRun = newActiveRun
        _timerState.value = TimerState.Running(
            startTime = newActiveRun.startTime,
            pauseAccumulator = newActiveRun.pauseAccumulator,
            currentSegmentIndex = newActiveRun.currentSegmentIndex,
            splitTimes = newActiveRun.splitTimes,
            comparison = comparison
        )
    }

    suspend fun split() {
        val current = activeRun ?: return
        val now = clock.currentTimeMillis()
        
        val layoutPrefs = settingsPort.observeTimerLayoutPreferences().first()
        if (now - lastSplitTime < layoutPrefs.splitDebounceMs) return
        lastSplitTime = now

        val (updated, event) = timerService.split(current)
        activeRun = updated

        if (layoutPrefs.enableVibration) {
            val isGold = event is TimerEvent.Split && current.run.segments[event.segmentIndex].bestSegmentTime?.getTime(current.timingMethod)?.let {
                event.splitTime <= it
            } ?: false
            
            if (isGold) {
                hapticFeedbackPort.vibrate(150) // Longer for gold
            } else {
                hapticFeedbackPort.vibrate(50)
            }
        }

        when (event) {
            is TimerEvent.Finished -> {
                _timerState.value = TimerState.Finished(
                    finalTime = event.finalTime,
                    splitTimes = updated.splitTimes,
                    comparison = updated.comparison
                )
            }
            is TimerEvent.Split -> {
                _timerState.value = TimerState.Running(
                    startTime = updated.startTime,
                    pauseAccumulator = updated.pauseAccumulator,
                    currentSegmentIndex = updated.currentSegmentIndex,
                    splitTimes = updated.splitTimes,
                    comparison = updated.comparison
                )
            }
            else -> {}
        }
    }

    suspend fun undoSplit() {
        val current = activeRun ?: return
        val (updated, _) = timerService.undoSplit(current)
        activeRun = updated
        _timerState.value = TimerState.Running(
            startTime = updated.startTime,
            pauseAccumulator = updated.pauseAccumulator,
            currentSegmentIndex = updated.currentSegmentIndex,
            splitTimes = updated.splitTimes,
            comparison = updated.comparison
        )
    }

    suspend fun skip() {
        val current = activeRun ?: return
        val (updated, event) = timerService.skip(current)
        activeRun = updated

        if (event is TimerEvent.Finished) {
            _timerState.value = TimerState.Finished(
                finalTime = event.finalTime,
                splitTimes = updated.splitTimes,
                comparison = updated.comparison
            )
        } else {
            _timerState.value = TimerState.Running(
                startTime = updated.startTime,
                pauseAccumulator = updated.pauseAccumulator,
                currentSegmentIndex = updated.currentSegmentIndex,
                splitTimes = updated.splitTimes,
                comparison = updated.comparison
            )
        }
    }

    suspend fun pauseResume() {
        val current = activeRun ?: return
        if (current.currentSegmentIndex >= current.run.segments.size) return
        
        if (current.pauseStart == null) {
            val (updated, _) = timerService.pause(current)
            activeRun = updated
            _timerState.value = TimerState.Paused(
                elapsedTime = timerService.getElapsedTime(current),
                currentSegmentIndex = updated.currentSegmentIndex,
                splitTimes = updated.splitTimes,
                comparison = updated.comparison
            )
        } else {
            val (updated, _) = timerService.resume(current)
            activeRun = updated
            _timerState.value = TimerState.Running(
                startTime = updated.startTime,
                pauseAccumulator = updated.pauseAccumulator,
                currentSegmentIndex = updated.currentSegmentIndex,
                splitTimes = updated.splitTimes,
                comparison = updated.comparison
            )
        }
    }

    suspend fun reset(saveAttempt: Boolean) {
        val current = activeRun ?: return
        val timerStateVal = _timerState.value

        if (saveAttempt) {
            val now = clock.currentTimeMillis()
            val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US)
            
            val updatedRun = current.run.withCompletedAttempt(
                attemptId = current.run.attemptCount + 1,
                startedAt = dateFormat.format(Date(current.startTime)),
                endedAt = dateFormat.format(Date(now)),
                pauseTime = TimeSpan.fromMilliseconds(current.pauseAccumulator),
                timingMethod = current.timingMethod,
                finalTime = if (timerStateVal is TimerState.Finished) timerStateVal.finalTime else null,
                splitTimes = current.splitTimes
            )
            runRepository.update(updatedRun)
        }

        activeRun = null
        _timerState.value = TimerState.Idle
    }
}
