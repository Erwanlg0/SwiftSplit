package com.elg.swiftsplit.ui.screen.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.GetRunByIdUseCase
import com.elg.swiftsplit.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunStatsUiState(
    val run: Run? = null,
    val totalAttempts: Int = 0,
    val completedAttemptsCount: Int = 0,
    val resetCount: Int = 0,
    val completionRate: Float = 0f,
    val sumOfBest: TimeSpan = TimeSpan.ZERO,
    val pbProgression: List<Pair<Int, TimeSpan>> = emptyList(), // Attempt ID to PB Time
    val segmentStats: List<SegmentStat> = emptyList()
)

data class SegmentStat(
    val name: String,
    val bestTime: TimeSpan?,
    val averageTime: TimeSpan?
)

@HiltViewModel
class RunStatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRunByIdUseCase: GetRunByIdUseCase
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    private val _uiState = MutableStateFlow(RunStatsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val run = getRunByIdUseCase(RunId(runId)) ?: return@launch
            val history = run.attemptHistory
            val total = history.size
            val completed = history.filter { it.realTime != null || it.gameTime != null }
            val completedCount = completed.size
            val resets = total - completedCount
            val rate = if (total > 0) (completedCount.toFloat() / total.toFloat() * 100f) else 0f

            // 1. Sum of Best
            var sobMs = 0L
            run.segments.forEach { seg ->
                val best = seg.bestSegmentTime?.realTime ?: seg.bestSegmentTime?.gameTime
                if (best != null) {
                    sobMs += best.totalMilliseconds
                }
            }
            val sob = TimeSpan(sobMs)

            // 2. PB Progression
            val pbs = mutableListOf<Pair<Int, TimeSpan>>()
            var currentBest: TimeSpan? = null
            completed.sortedBy { it.id }.forEach { attempt ->
                val time = attempt.realTime ?: attempt.gameTime
                if (time != null) {
                    if (currentBest == null || time < currentBest!!) {
                        currentBest = time
                        pbs.add(attempt.id to time)
                    }
                }
            }

            // 3. Segment Averages
            val segStats = run.segments.map { segment ->
                val best = segment.bestSegmentTime?.realTime ?: segment.bestSegmentTime?.gameTime
                val times = segment.segmentHistory.mapNotNull { it.time.realTime ?: it.time.gameTime }
                val avg = if (times.isNotEmpty()) {
                    TimeSpan(times.map { it.totalMilliseconds }.average().toLong())
                } else null

                SegmentStat(segment.name, best, avg)
            }

            _uiState.value = RunStatsUiState(
                run = run,
                totalAttempts = total,
                completedAttemptsCount = completedCount,
                resetCount = resets,
                completionRate = rate,
                sumOfBest = sob,
                pbProgression = pbs,
                segmentStats = segStats
            )
        }
    }
}
