package com.elg.speedruncompanion.ui.screen.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.speedruncompanion.application.port.input.DeleteRunUseCase
import com.elg.speedruncompanion.application.port.input.GetRunsUseCase
import com.elg.speedruncompanion.application.port.input.ImportRunUseCase
import com.elg.speedruncompanion.application.port.input.SaveRunUseCase
import com.elg.speedruncompanion.domain.model.GameInfo
import com.elg.speedruncompanion.domain.model.Run
import com.elg.speedruncompanion.domain.model.RunId
import com.elg.speedruncompanion.domain.model.Segment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.elg.speedruncompanion.application.port.output.SettingsPort

sealed interface RunsListUiState {
    data object Loading : RunsListUiState
    data class Success(val runs: List<Run>) : RunsListUiState
    data object Empty : RunsListUiState
    data class Error(val message: String) : RunsListUiState
}

@HiltViewModel
class RunsListViewModel @Inject constructor(
    getRunsUseCase: GetRunsUseCase,
    private val importRunUseCase: ImportRunUseCase,
    private val deleteRunUseCase: DeleteRunUseCase,
    private val saveRunUseCase: SaveRunUseCase,
    private val settingsPort: SettingsPort
) : ViewModel() {

    val uiState: StateFlow<RunsListUiState> = getRunsUseCase()
        .combine(settingsPort.observeSaveQuickRuns()) { runs, saveQuickRuns ->
            if (saveQuickRuns) {
                runs
            } else {
                runs.filterNot { it.gameInfo.gameName == "Quick Run" && it.gameInfo.categoryName == "Stopwatch" }
            }
        }
        .map { runs ->
            if (runs.isEmpty()) RunsListUiState.Empty else RunsListUiState.Success(runs)
        }
        .catch { emit(RunsListUiState.Error(it.message ?: "Unknown Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RunsListUiState.Loading
        )

    fun importRun(content: ByteArray) {
        viewModelScope.launch {
            importRunUseCase(content)
        }
    }

    fun deleteRun(runId: RunId) {
        viewModelScope.launch {
            deleteRunUseCase(runId)
        }
    }

    fun createQuickRun(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val runId = RunId.generate()
            val run = Run(
                id = runId,
                gameInfo = GameInfo(
                    gameName = "Quick Run",
                    categoryName = "Stopwatch"
                ),
                segments = listOf(Segment(name = "Chrono"))
            )
            saveRunUseCase(run)
            onSuccess(runId.value)
        }
    }

    fun createNewRun(gameName: String, categoryName: String, platform: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val runId = RunId.generate()
            val run = Run(
                id = runId,
                gameInfo = GameInfo(
                    gameName = gameName.ifBlank { "New Game" },
                    categoryName = categoryName.ifBlank { "Any%" },
                    platform = platform
                ),
                segments = listOf(Segment(name = "Finish"))
            )
            saveRunUseCase(run)
            onSuccess(runId.value)
        }
    }
}
