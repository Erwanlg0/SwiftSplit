package com.elg.swiftsplit.ui.screen.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.*
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.NetworkPreferences
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
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
    private val settingsPort: SettingsPort,
    private val updateTimerLayoutPreferencesUseCase: UpdateTimerLayoutPreferencesUseCase
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

    private val _remoteSplitName = MutableStateFlow<String?>(null)
    val remoteSplitName = _remoteSplitName.asStateFlow()

    private val _remoteSplitIndex = MutableStateFlow(-1)
    val remoteSplitIndex = _remoteSplitIndex.asStateFlow()

    private val _remoteDelta = MutableStateFlow<String?>(null)
    val remoteDelta = _remoteDelta.asStateFlow()

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

    val timerLayoutPreferences = settingsPort.observeTimerLayoutPreferences().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    init {
        viewModelScope.launch {
            settingsPort.observeRemoteHost().collect { h ->
                _host.value = h
            }
        }
        viewModelScope.launch {
            settingsPort.observeRemotePort().collect { p ->
                _port.value = p
            }
        }
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
        viewModelScope.launch {
            settingsPort.setRemoteHost(h)
        }
    }

    fun updatePort(p: String) {
        _port.value = p
        viewModelScope.launch {
            settingsPort.setRemotePort(p)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setFormatPattern(pattern: com.elg.swiftsplit.domain.model.TimeFormatPattern) {
        viewModelScope.launch {
            val current = timerLayoutPreferences.value
            updateTimerLayoutPreferencesUseCase(current.copy(timeFormat = current.timeFormat.copy(pattern = pattern)))
        }
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
                // Update local state immediately on action to prevent lag/delay:
                val cmdLower = command.trim().lowercase()
                when (cmdLower) {
                    "startorsplit", "split", "resume" -> {
                        _remotePhase.value = "Running"
                    }
                    "pause" -> {
                        _remotePhase.value = "Paused"
                    }
                    "reset" -> {
                        _remotePhase.value = "NotRunning"
                        _remoteTime.value = "00:00:00.000"
                        _remoteSplitIndex.value = -1
                        _remoteSplitName.value = null
                        _remoteDelta.value = null
                    }
                }
            }
        }
    }

    private fun startPolling(pollingDelayMs: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var lastIdx = -2
            while (isActive) {
                val phaseResult = sendLiveSplitCommandUseCase("getcurrenttimerphase")
                val phase = phaseResult.getOrNull()?.trim() ?: "NotRunning"
                _remotePhase.value = phase

                val timeResult = sendLiveSplitCommandUseCase("getcurrenttime")
                val rawTime = timeResult.getOrNull()?.trim() ?: "00:00:00.000"
                _remoteTime.value = formatRemoteTime(rawTime)

                if (phase == "Running" || phase == "Paused") {
                    val idxResult = sendLiveSplitCommandUseCase("getsplitindex")
                    val currentIdx = idxResult.getOrNull()?.trim()?.toIntOrNull() ?: -1
                    _remoteSplitIndex.value = currentIdx

                    if (currentIdx != lastIdx) {
                        val nameResult = sendLiveSplitCommandUseCase("getcurrentsplitname")
                        _remoteSplitName.value = nameResult.getOrNull()?.trim()
                        lastIdx = currentIdx
                    }

                    val deltaResult = sendLiveSplitCommandUseCase("getdelta")
                    _remoteDelta.value = deltaResult.getOrNull()?.trim()
                } else {
                    _remoteSplitIndex.value = -1
                    _remoteSplitName.value = null
                    _remoteDelta.value = null
                    lastIdx = -2
                }

                val actualDelay = if (phase == "Running") pollingDelayMs else maxOf(pollingDelayMs, 1000L)
                delay(actualDelay)
            }
        }
    }

    private fun formatRemoteTime(time: String): String {
        val dotIndex = time.indexOf('.')
        if (dotIndex == -1) return time
        val afterDot = time.substring(dotIndex + 1)
        
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
        _remoteSplitIndex.value = -1
        _remoteSplitName.value = null
        _remoteDelta.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
