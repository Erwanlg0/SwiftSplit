package com.elg.swiftsplit.ui.screen.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.*
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val connectToLiveSplitUseCase: ConnectToLiveSplitUseCase,
    private val disconnectLiveSplitUseCase: DisconnectLiveSplitUseCase,
    private val sendLiveSplitCommandUseCase: SendLiveSplitCommandUseCase,
    observeLiveSplitConnectionUseCase: ObserveLiveSplitConnectionUseCase,
    private val settingsPort: SettingsPort,
    private val updateTimerLayoutPreferencesUseCase: UpdateTimerLayoutPreferencesUseCase
) : ViewModel() {

    private val _host = MutableStateFlow("192.168.1.10")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _port = MutableStateFlow("16834")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _remoteTime = MutableStateFlow("00:00:00.000")
    val remoteTime: StateFlow<String> = _remoteTime.asStateFlow()

    private val _remotePhase = MutableStateFlow("NotRunning")
    val remotePhase: StateFlow<String> = _remotePhase.asStateFlow()

    private val _gameName = MutableStateFlow<String?>(null)
    val gameName: StateFlow<String?> = _gameName.asStateFlow()

    private val _categoryName = MutableStateFlow<String?>(null)
    val categoryName: StateFlow<String?> = _categoryName.asStateFlow()

    private var pollingJob: Job? = null
    private var pollCounter = 0
    private var isGameInfoSupported = true

    val connectionState: StateFlow<ConnectionState> = observeLiveSplitConnectionUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionState.DISCONNECTED
    )

    private val networkPreferences = settingsPort.observeNetworkPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkPreferences.DEFAULT
    )

    val timerLayoutPreferences: StateFlow<TimerLayoutPreferences> = settingsPort.observeTimerLayoutPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    init {
        viewModelScope.launch { settingsPort.observeRemoteHost().collect { _host.value = it } }
        viewModelScope.launch { settingsPort.observeRemotePort().collect { _port.value = it } }
        
        viewModelScope.launch {
            combine(connectionState, networkPreferences) { state, _ -> state }.collect { state ->
                if (state == ConnectionState.CONNECTED) startPolling() else stopPolling()
            }
        }
    }

    fun updateHost(h: String) {
        _host.value = h
        viewModelScope.launch { settingsPort.setRemoteHost(h) }
    }

    fun updatePort(p: String) {
        _port.value = p
        viewModelScope.launch { settingsPort.setRemotePort(p) }
    }

    fun clearError() { _errorMessage.value = null }

    fun connect() {
        if (connectionState.value == ConnectionState.CONNECTING || connectionState.value == ConnectionState.CONNECTED) return
        viewModelScope.launch {
            _errorMessage.value = null
            val portInt = _port.value.toIntOrNull() ?: 16834
            val result = connectToLiveSplitUseCase(_host.value, portInt)
            if (result.isFailure) {
                _errorMessage.value = "Connexion échouée"
            }
        }
    }

    fun disconnect() { viewModelScope.launch { disconnectLiveSplitUseCase() } }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            sendLiveSplitCommandUseCase(command)
            pollOnce()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                pollOnce()
                delay(100)
            }
        }
    }

    private suspend fun pollOnce() {
        val time = sendLiveSplitCommandUseCase("getcurrenttime").getOrNull()
        if (time != null) _remoteTime.value = time.trim()

        val phase = sendLiveSplitCommandUseCase("getcurrenttimerphase").getOrNull()
        if (phase != null) _remotePhase.value = phase.trim()

        if (isGameInfoSupported && pollCounter % 20 == 0) {
            val gameResult = sendLiveSplitCommandUseCase("getgamename")
            if (gameResult.isSuccess) {
                val game = gameResult.getOrNull()
                if (game != null) _gameName.value = game.trim()
            } else if (gameResult.exceptionOrNull() is java.net.SocketTimeoutException) {
                isGameInfoSupported = false
            }

            if (isGameInfoSupported) {
                val categoryResult = sendLiveSplitCommandUseCase("getcategoryname")
                if (categoryResult.isSuccess) {
                    val category = categoryResult.getOrNull()
                    if (category != null) _categoryName.value = category.trim()
                } else if (categoryResult.exceptionOrNull() is java.net.SocketTimeoutException) {
                    isGameInfoSupported = false
                }
            }
        }
        pollCounter++
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _remoteTime.value = "00:00:00.000"
        _remotePhase.value = "NotRunning"
        _gameName.value = null
        _categoryName.value = null
        pollCounter = 0
        isGameInfoSupported = true
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
