package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.domain.model.*
import com.elg.speedruncompanion.domain.service.SplitTimeCalculator
import com.elg.speedruncompanion.domain.service.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val settingsPort: SettingsPort
) {
    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: Flow<TimerState> = _timerState.asStateFlow()

    private var activeRun: ActiveRun? = null

    suspend fun start(runId: RunId, comparison: ComparisonName, timingMethod: TimingMethod) {
        val run = runRepository.getById(runId) ?: return
        val newActiveRun = timerService.start(run, comparison, timingMethod)
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
        val now = System.currentTimeMillis()
        val (updated, event) = timerService.split(current, now)
        activeRun = updated

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
        val now = System.currentTimeMillis()
        if (current.pauseStart == null) {
            val (updated, _) = timerService.pause(current, now)
            activeRun = updated
            _timerState.value = TimerState.Paused(
                elapsedTime = timerService.getElapsedTime(current, now),
                currentSegmentIndex = updated.currentSegmentIndex,
                splitTimes = updated.splitTimes,
                comparison = updated.comparison
            )
        } else {
            val (updated, _) = timerService.resume(current, now)
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

        val isQuickRun = current.run.gameInfo.gameName == "Quick Run" && current.run.gameInfo.categoryName == "Stopwatch"
        val shouldSaveQuickRun = settingsPort.observeSaveQuickRuns().first()

        if (isQuickRun && !shouldSaveQuickRun) {
            runRepository.delete(current.run.id)
        } else if (saveAttempt) {
            val nowStr = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(Date())
            val attemptId = current.run.attemptCount + 1

            val isCompleted = timerStateVal is TimerState.Finished
            val finalTime = if (timerStateVal is TimerState.Finished) timerStateVal.finalTime else null

            val attempt = Attempt(
                id = attemptId,
                startedAt = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(Date(current.startTime)),
                endedAt = nowStr,
                realTime = if (current.timingMethod == TimingMethod.REAL_TIME) finalTime else null,
                gameTime = if (current.timingMethod == TimingMethod.GAME_TIME) finalTime else null,
                pauseTime = TimeSpan.fromMilliseconds(current.pauseAccumulator)
            )

            val updatedSegments = current.run.segments.mapIndexed { idx, segment ->
                val splitVal = current.splitTimes[idx]
                val segmentVal = SplitTimeCalculator.getSegmentTime(current.splitTimes, idx)

                if (segmentVal != null) {
                    val newHistoryEntry = SegmentHistoryEntry(
                        attemptId = attemptId,
                        time = SplitTime(
                            realTime = if (current.timingMethod == TimingMethod.REAL_TIME) segmentVal else null,
                            gameTime = if (current.timingMethod == TimingMethod.GAME_TIME) segmentVal else null
                        )
                    )

                    val bestTimeSpan = segment.bestSegmentTime?.getTime(current.timingMethod)
                    val newBest = if (bestTimeSpan == null || segmentVal < bestTimeSpan) {
                        SplitTime(
                            realTime = if (current.timingMethod == TimingMethod.REAL_TIME) segmentVal else segment.bestSegmentTime?.realTime,
                            gameTime = if (current.timingMethod == TimingMethod.GAME_TIME) segmentVal else segment.bestSegmentTime?.gameTime
                        )
                    } else {
                        segment.bestSegmentTime
                    }

                    val newSplitTimes = segment.splitTimes.toMutableMap()
                    if (isCompleted && finalTime != null) {
                        val pbTime = current.run.personalBest?.getTime(current.timingMethod)
                        if (pbTime == null || finalTime < pbTime) {
                            newSplitTimes[ComparisonName.PERSONAL_BEST] = SplitTime(
                                realTime = if (current.timingMethod == TimingMethod.REAL_TIME) splitVal else segment.splitTimes[ComparisonName.PERSONAL_BEST]?.realTime,
                                gameTime = if (current.timingMethod == TimingMethod.GAME_TIME) splitVal else segment.splitTimes[ComparisonName.PERSONAL_BEST]?.gameTime
                            )
                        }
                    }

                    segment.copy(
                        bestSegmentTime = newBest,
                        segmentHistory = segment.segmentHistory + newHistoryEntry,
                        splitTimes = newSplitTimes
                    )
                } else {
                    segment
                }
            }

            val updatedRun = current.run.copy(
                attemptCount = attemptId,
                attemptHistory = current.run.attemptHistory + attempt,
                segments = updatedSegments
            )
            runRepository.update(updatedRun)
        }

        activeRun = null
        _timerState.value = TimerState.Idle
    }
}
