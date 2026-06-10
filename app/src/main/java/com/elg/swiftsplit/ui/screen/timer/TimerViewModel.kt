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
    private val getRunByIdUseCase: GetRunByIdUseCase,
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
    observeTimerLayoutPreferencesUseCase: ObserveTimerLayoutPreferencesUseCase
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    private val _run = MutableStateFlow<Run?>(null)
    val run = _run.asStateFlow()

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
                    val now = System.currentTimeMillis()
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

    init {
        viewModelScope.launch {
            _run.value = getRunByIdUseCase(RunId(runId))?.let {
                if (it.segments.isEmpty()) {
                    it.copy(segments = listOf(Segment(name = "Finish")))
                } else {
                    it
                }
            }

            // Also keep it updated if the DB changes
            getRunsUseCase().collect { runs ->
                val loaded = runs.firstOrNull { it.id.value == runId }
                if (loaded != null) {
                    _run.value = if (loaded.segments.isEmpty()) {
                        loaded.copy(segments = listOf(Segment(name = "Finish")))
                    } else {
                        loaded
                    }
                }
            }
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
}
