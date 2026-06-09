package com.elg.speedruncompanion.ui.screen.timer

import com.elg.speedruncompanion.application.port.output.SettingsPort
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.speedruncompanion.application.port.input.*
import com.elg.speedruncompanion.domain.model.*
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
    private val observeTimerUseCase: ObserveTimerUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val splitUseCase: SplitUseCase,
    private val undoSplitUseCase: UndoSplitUseCase,
    private val skipSplitUseCase: SkipSplitUseCase,
    private val pauseResumeTimerUseCase: PauseResumeTimerUseCase,
    private val resetTimerUseCase: ResetTimerUseCase,
    private val deleteRunUseCase: DeleteRunUseCase,
    private val settingsPort: SettingsPort,
    observeTimerLayoutPreferencesUseCase: ObserveTimerLayoutPreferencesUseCase
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    private val _run = MutableStateFlow<Run?>(null)
    val run = _run.asStateFlow()

    private val _currentElapsed = MutableStateFlow(TimeSpan.ZERO)
    val currentElapsed = _currentElapsed.asStateFlow()

    val timerState = observeTimerUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerState.Idle
    )

    val layoutPreferences = observeTimerLayoutPreferencesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    private var tickJob: Job? = null

    init {
        loadRun()
        
        viewModelScope.launch {
            observeTimerUseCase().collect { state ->
                when (state) {
                    is TimerState.Idle -> {
                        stopTicking()
                        _currentElapsed.value = TimeSpan.ZERO
                        // Reload run to update attempt counts and PB
                        loadRun()
                    }
                    is TimerState.Running -> {
                        startTicking(state.startTime, state.pauseAccumulator)
                    }
                    is TimerState.Paused -> {
                        stopTicking()
                        _currentElapsed.value = state.elapsedTime
                    }
                    is TimerState.Finished -> {
                        stopTicking()
                        _currentElapsed.value = state.finalTime
                    }
                }
            }
        }
    }

    private fun loadRun() {
        viewModelScope.launch {
            _run.value = getRunByIdUseCase(RunId(runId))
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

    private fun startTicking(startTime: Long, pauseAccumulator: Long) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime - pauseAccumulator
                _currentElapsed.value = TimeSpan(elapsed)
                delay(16) // ~60fps
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTicking()
        viewModelScope.launch {
            val currentRun = _run.value
            if (currentRun != null &&
                currentRun.gameInfo.gameName == "Quick Run" &&
                currentRun.gameInfo.categoryName == "Stopwatch"
            ) {
                val shouldSaveQuickRun = settingsPort.observeSaveQuickRuns().first()
                if (!shouldSaveQuickRun) {
                    deleteRunUseCase(RunId(runId))
                }
            }
        }
    }
}
