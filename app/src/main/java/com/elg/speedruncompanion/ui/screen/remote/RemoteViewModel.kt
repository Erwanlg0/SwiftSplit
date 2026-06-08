package com.elg.speedruncompanion.ui.screen.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.speedruncompanion.application.port.input.*
import com.elg.speedruncompanion.application.port.output.ConnectionState
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.domain.model.NetworkPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val connectToLiveSplitUseCase: ConnectToLiveSplitUseCase,
    private val disconnectLiveSplitUseCase: DisconnectLiveSplitUseCase,
    private val sendLiveSplitCommandUseCase: SendLiveSplitCommandUseCase,
    observeLiveSplitConnectionUseCase: ObserveLiveSplitConnectionUseCase,
    settingsPort: SettingsPort
) : ViewModel() {

    private val _host = MutableStateFlow("192.168.1.10")
    val host = _host.asStateFlow()

    private val _port = MutableStateFlow("16834")
    val port = _port.asStateFlow()

    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse = _lastResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _remoteTime = MutableStateFlow("00:00:00.000")
    val remoteTime = _remoteTime.asStateFlow()

    private val _remotePhase = MutableStateFlow("NotRunning")
    val remotePhase = _remotePhase.asStateFlow()

    private var pollingJob: Job? = null

    val connectionState = observeLiveSplitConnectionUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionState.DISCONNECTED
    )

    private val networkPreferences = settingsPort.observeNetworkPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkPreferences.DEFAULT
    )

    init {
        viewModelScope.launch {
            combine(connectionState, networkPreferences) { state, prefs ->
                state to prefs.pollingDelayMs
            }.collect { (state, pollingDelayMs) ->
                if (state == ConnectionState.CONNECTED) {
                    _errorMessage.value = null
                    startPolling(pollingDelayMs)
                } else {
                    stopPolling()
                }
            }
        }
    }

    fun updateHost(h: String) {
        _host.value = h
    }

    fun updatePort(p: String) {
        _port.value = p
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun connect() {
        viewModelScope.launch {
            _errorMessage.value = null
            val portInt = _port.value.toIntOrNull() ?: 16834
            val result = connectToLiveSplitUseCase(_host.value, portInt)
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                _errorMessage.value = "Connexion échouée : ${e?.localizedMessage ?: "Impossible de joindre le serveur."}"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectLiveSplitUseCase()
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            val result = sendLiveSplitCommandUseCase(command)
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                _errorMessage.value = "Erreur commande ($command) : ${e?.localizedMessage ?: "Échec d'envoi."}"
            } else {
                _lastResponse.value = result.getOrNull()
            }
        }
    }

    private fun startPolling(pollingDelayMs: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val phaseResult = sendLiveSplitCommandUseCase("getcurrenttimerphase")
                _remotePhase.value = phaseResult.getOrNull()?.trim() ?: "NotRunning"

                val timeResult = sendLiveSplitCommandUseCase("getcurrenttime")
                val rawTime = timeResult.getOrNull()?.trim() ?: "00:00:00.000"
                _remoteTime.value = formatRemoteTime(rawTime)

                delay(pollingDelayMs)
            }
        }
    }

    private fun formatRemoteTime(time: String): String {
        val dotIndex = time.indexOf('.')
        if (dotIndex == -1) return time
        val afterDot = time.substring(dotIndex + 1)
        // Garde maximum 3 chiffres après la virgule comme demandé
        return if (afterDot.length > 3) {
            time.substring(0, dotIndex + 4)
        } else {
            time
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _remoteTime.value = "00:00:00.000"
        _remotePhase.value = "NotRunning"
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
