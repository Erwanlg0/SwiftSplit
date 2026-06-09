package com.elg.swiftsplit.ui.screen.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.DeleteRunUseCase
import com.elg.swiftsplit.application.port.input.GetRunsUseCase
import com.elg.swiftsplit.application.port.input.ImportRunUseCase
import com.elg.swiftsplit.application.port.input.SaveRunUseCase
import com.elg.swiftsplit.domain.model.GameInfo
import com.elg.swiftsplit.domain.model.Run
import com.elg.swiftsplit.domain.model.RunId
import com.elg.swiftsplit.domain.model.Segment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.elg.swiftsplit.application.port.output.SettingsPort

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

    fun importRunFromUrl(url: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                if (connection.responseCode == 200) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val result = importRunUseCase(bytes)
                    if (result.isSuccess) {
                        launch(kotlinx.coroutines.Dispatchers.Main) { onSuccess() }
                    } else {
                        val exception = result.exceptionOrNull() ?: Exception("Failed to parse splits")
                        launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(exception) }
                    }
                } else {
                    val error = Exception("HTTP error: ${connection.responseCode}")
                    launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(error) }
                }
            } catch (e: Exception) {
                launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(e) }
            }
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
