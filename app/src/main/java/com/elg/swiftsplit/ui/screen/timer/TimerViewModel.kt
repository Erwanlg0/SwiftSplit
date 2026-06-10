package com.elg.swiftsplit.ui.screen.timer

import com.elg.swiftsplit.application.port.output.SettingsPort
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.*
import com.elg.swiftsplit.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRunsUseCase: GetRunsUseCase,
    private val observeTimerUseCase: ObserveTimerUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val splitUseCase: SplitUseCase,
    private val undoSplitUseCase: UndoSplitUseCase,
    private val skipSplitUseCase: SkipSplitUseCase,
    private val pauseResumeTimerUseCase: PauseResumeTimerUseCase,
    private val resetTimerUseCase: ResetTimerUseCase,
    private val deleteRunUseCase: DeleteRunUseCase,
    private val exportRunUseCase: ExportRunUseCase,
    private val settingsPort: SettingsPort,
    private val clock: com.elg.swiftsplit.domain.service.Clock,
    observeTimerLayoutPreferencesUseCase: ObserveTimerLayoutPreferencesUseCase
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    val run: StateFlow<Run?> = getRunsUseCase()
        .map { runs ->
            val loaded = runs.firstOrNull { it.id.value == runId }
            loaded?.let {
                if (it.segments.isEmpty()) {
                    it.copy(segments = listOf(Segment(name = "Finish")))
                } else {
                    it
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val timerState = observeTimerUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerState.Idle
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentElapsed: StateFlow<TimeSpan> = timerState.flatMapLatest { state ->
        when (state) {
            is TimerState.Running -> flow {
                while (true) {
                    val now = clock.currentTimeMillis()
                    val elapsed = now - state.startTime - state.pauseAccumulator
                    emit(TimeSpan(elapsed))
                    delay(16)
                }
            }
            is TimerState.Paused -> flowOf(state.elapsedTime)
            is TimerState.Finished -> flowOf(state.finalTime)
            is TimerState.Idle -> flowOf(TimeSpan.ZERO)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimeSpan.ZERO
    )

    val layoutPreferences = observeTimerLayoutPreferencesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    fun exportCurrentRun(onSuccess: (ByteArray) -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            exportRunUseCase(RunId(runId))
                .onSuccess(onSuccess)
                .onFailure(onFailure)
        }
    }


    fun startTimer() {
        viewModelScope.launch {
            startTimerUseCase(RunId(runId), ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        }
    }

    fun split() {
        viewModelScope.launch {
            splitUseCase()
        }
    }

    fun undoSplit() {
        viewModelScope.launch {
            undoSplitUseCase()
        }
    }

    fun skipSplit() {
        viewModelScope.launch {
            skipSplitUseCase()
        }
    }

    fun pauseResume() {
        viewModelScope.launch {
            pauseResumeTimerUseCase()
        }
    }

    fun reset(saveAttempt: Boolean = true) {
        viewModelScope.launch {
            resetTimerUseCase(saveAttempt)
        }
    }

    fun cycleComparison() {
        viewModelScope.launch {
            val current = timerState.value
            val currentComp = when (current) {
                is TimerState.Running -> current.comparison
                is TimerState.Paused -> current.comparison
                is TimerState.Finished -> current.comparison
                else -> ComparisonName.PERSONAL_BEST
            }
            
            val comparisons = listOf(
                ComparisonName.PERSONAL_BEST,
                ComparisonName.BEST_SEGMENTS,
                ComparisonName.AVERAGE_SEGMENTS
            )
            
            val nextComp = comparisons[(comparisons.indexOf(currentComp) + 1) % comparisons.size]
            settingsPort.setComparison(nextComp)
            // The timer state will update automatically if ObserveTimerUseCase reacts to settings
        }
    }
}
