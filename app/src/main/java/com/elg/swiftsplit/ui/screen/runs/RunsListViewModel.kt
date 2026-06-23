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
import com.elg.swiftsplit.infrastructure.remote.SpeedrunClient
import com.elg.swiftsplit.infrastructure.remote.SpeedrunGame
import com.elg.swiftsplit.infrastructure.remote.SpeedrunCategory
import com.elg.swiftsplit.infrastructure.remote.SpeedrunRunPlacement

sealed interface RunsListUiState {
    data object Loading : RunsListUiState
    data class Success(val runs: List<Run>) : RunsListUiState
    data object Empty : RunsListUiState
    data class Error(val message: String) : RunsListUiState
}

@HiltViewModel
class RunsListViewModel @Inject constructor(
    private val getRunsUseCase: GetRunsUseCase,
    private val importRunUseCase: ImportRunUseCase,
    private val deleteRunUseCase: DeleteRunUseCase,
    private val saveRunUseCase: SaveRunUseCase,
    private val settingsPort: SettingsPort,
    private val speedrunClient: SpeedrunClient
) : ViewModel() {

    // Speedrun.com Integration States
    private val _speedrunGames = MutableStateFlow<List<SpeedrunGame>>(emptyList())
    val speedrunGames = _speedrunGames.asStateFlow()

    private val _speedrunCategories = MutableStateFlow<List<SpeedrunCategory>>(emptyList())
    val speedrunCategories = _speedrunCategories.asStateFlow()

    private val _speedrunRuns = MutableStateFlow<List<SpeedrunRunPlacement>>(emptyList())
    val speedrunRuns = _speedrunRuns.asStateFlow()

    private val _isSpeedrunLoading = MutableStateFlow(false)
    val isSpeedrunLoading = _isSpeedrunLoading.asStateFlow()

    fun searchSpeedrunGames(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isSpeedrunLoading.value = true
            try {
                _speedrunGames.value = speedrunClient.searchGames(query)
                _speedrunCategories.value = emptyList()
                _speedrunRuns.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("RunsListViewModel", "Error searching games", e)
                _speedrunGames.value = emptyList()
            } finally {
                _isSpeedrunLoading.value = false
            }
        }
    }

    fun selectSpeedrunGame(gameId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isSpeedrunLoading.value = true
            try {
                _speedrunCategories.value = speedrunClient.getCategories(gameId)
                _speedrunRuns.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("RunsListViewModel", "Error selecting game", e)
                _speedrunCategories.value = emptyList()
            } finally {
                _isSpeedrunLoading.value = false
            }
        }
    }

    fun selectSpeedrunCategory(gameId: String, categoryId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isSpeedrunLoading.value = true
            try {
                val placements = speedrunClient.getLeaderboard(gameId, categoryId)
                _speedrunRuns.value = placements.take(15)
            } catch (e: Exception) {
                android.util.Log.e("RunsListViewModel", "Error selecting category", e)
                _speedrunRuns.value = emptyList()
            } finally {
                _isSpeedrunLoading.value = false
            }
        }
    }

    fun clearSpeedrunSearch() {
        _speedrunGames.value = emptyList()
        _speedrunCategories.value = emptyList()
        _speedrunRuns.value = emptyList()
    }

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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            importRunUseCase(content)
        }
    }

    fun cleanUrlForDownload(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        if (clean.contains("splits.io/")) {
            val uriWithoutQuery = clean.split("?")[0]
            if (!uriWithoutQuery.endsWith("/download/livesplit") && !uriWithoutQuery.endsWith("/export/livesplit")) {
                clean = if (uriWithoutQuery.endsWith("/")) {
                    "${uriWithoutQuery}download/livesplit"
                } else {
                    "${uriWithoutQuery}/download/livesplit"
                }
            }
        }
        return clean
    }

    fun importRunFromUrl(url: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var currentUrl = speedrunClient.resolveUrl(cleanUrlForDownload(url))

                var connection: java.net.HttpURLConnection? = null
                var status = -1
                var redirects = 0
                val maxRedirects = 5

                while (redirects < maxRedirects) {
                    val conn = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.requestMethod = "GET"
                    conn.instanceFollowRedirects = false // Manually follow to allow cross-protocol redirect

                    status = conn.responseCode
                    if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                        status == 307 || status == 308
                    ) {
                        val newUrl = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (newUrl == null) {
                            throw Exception("Redirection sans header Location")
                        }
                        currentUrl = if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                            val base = java.net.URL(currentUrl)
                            java.net.URL(base, newUrl).toString()
                        } else {
                            newUrl
                        }
                        redirects++
                    } else {
                        connection = conn
                        break
                    }
                }

                val finalConnection = connection ?: throw Exception("Trop de redirections")
                if (status == 200) {
                    val bytes = finalConnection.inputStream.use { it.readBytes() }
                    finalConnection.disconnect()
                    val result = importRunUseCase(bytes)
                    if (result.isSuccess) {
                        launch(kotlinx.coroutines.Dispatchers.Main) { onSuccess() }
                    } else {
                        val exception = result.exceptionOrNull() ?: Exception("Impossible de lire le fichier .lss")
                        launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(exception) }
                    }
                } else {
                    finalConnection.disconnect()
                    val error = Exception("Code HTTP d'erreur : $status")
                    launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(error) }
                }
            } catch (e: Exception) {
                android.util.Log.e("RunsListViewModel", "Error importing run from URL: $url", e)
                launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(e) }
            }
        }
    }

    fun importSpeedrunRun(runId: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isSpeedrunLoading.value = true
                val runDetails = speedrunClient.getRun(runId)
                val splitsUri = runDetails.splits?.uri
                if (splitsUri.isNullOrEmpty()) {
                    throw Exception("Ce run n'a pas de splits associés sur splits.io")
                }
                var currentUrl = speedrunClient.resolveUrl(cleanUrlForDownload(splitsUri))
                var connection: java.net.HttpURLConnection? = null
                var status = -1
                var redirects = 0
                val maxRedirects = 5

                while (redirects < maxRedirects) {
                    val conn = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.requestMethod = "GET"
                    conn.instanceFollowRedirects = false

                    status = conn.responseCode
                    if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                        status == 307 || status == 308
                    ) {
                        val newUrl = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (newUrl == null) {
                            throw Exception("Redirection sans header Location")
                        }
                        currentUrl = if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                            val base = java.net.URL(currentUrl)
                            java.net.URL(base, newUrl).toString()
                        } else {
                            newUrl
                        }
                        redirects++
                    } else {
                        connection = conn
                        break
                    }
                }

                val finalConnection = connection ?: throw Exception("Trop de redirections")
                if (status == 200) {
                    val bytes = finalConnection.inputStream.use { it.readBytes() }
                    finalConnection.disconnect()
                    val result = importRunUseCase(bytes)
                    if (result.isSuccess) {
                        launch(kotlinx.coroutines.Dispatchers.Main) { onSuccess() }
                    } else {
                        val exception = result.exceptionOrNull() ?: Exception("Impossible de lire le fichier .lss")
                        launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(exception) }
                    }
                } else {
                    finalConnection.disconnect()
                    val error = Exception("Code HTTP d'erreur : $status")
                    launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(error) }
                }
            } catch (e: Exception) {
                android.util.Log.e("RunsListViewModel", "Error importing speedrun run: $runId", e)
                launch(kotlinx.coroutines.Dispatchers.Main) { onFailure(e) }
            } finally {
                _isSpeedrunLoading.value = false
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
