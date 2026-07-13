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

    data class RemoteSplit(
        val name: String,
        val comparisonTime: String? = null,
        val actualTime: String? = null,
        val delta: String? = null
    )

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

    private val _currentSplitName = MutableStateFlow<String?>(null)
    val currentSplitName: StateFlow<String?> = _currentSplitName.asStateFlow()

    private val _currentSplitIndex = MutableStateFlow(-1)
    val currentSplitIndex: StateFlow<Int> = _currentSplitIndex.asStateFlow()

    private val _splitsList = MutableStateFlow<List<RemoteSplit>>(emptyList())
    val splitsList: StateFlow<List<RemoteSplit>> = _splitsList.asStateFlow()

    private val _isSplitsListSupported = MutableStateFlow(true)
    val isSplitsListSupported: StateFlow<Boolean> = _isSplitsListSupported.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private val _reconnectAttempts = MutableStateFlow(0)
    val reconnectAttempts: StateFlow<Int> = _reconnectAttempts.asStateFlow()

    private var pollingJob: Job? = null
    private var pollCounter = 0
    private var isGameInfoSupported = true
    private var isExplicitlyDisconnected = true
    private var isFetchingSplits = false
    private var reconnectJob: Job? = null
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
            var lastState = ConnectionState.DISCONNECTED
            combine(connectionState, networkPreferences) { state, _ -> state }.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    startPolling()
                } else {
                    stopPolling()
                    if ((state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR) &&
                        lastState == ConnectionState.CONNECTED &&
                        !isExplicitlyDisconnected
                    ) {
                        triggerAutoReconnect()
                    }
                }
                lastState = state
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
            isExplicitlyDisconnected = false
            reconnectJob?.cancel()
            _isReconnecting.value = false
            _reconnectAttempts.value = 0
            _errorMessage.value = null
            val portInt = _port.value.toIntOrNull() ?: 16834
            val result = connectToLiveSplitUseCase(_host.value, portInt)
            if (result.isFailure) {
                _errorMessage.value = "Connexion échouée"
            }
        }
    }

    fun disconnect() {
        isExplicitlyDisconnected = true
        reconnectJob?.cancel()
        _isReconnecting.value = false
        _reconnectAttempts.value = 0
        viewModelScope.launch { disconnectLiveSplitUseCase() }
    }

    private var lastSplitTime = 0L

    fun sendCommand(command: String) {
        val now = System.currentTimeMillis()
        val debounce = timerLayoutPreferences.value.splitDebounceMs
        
        if (command == "split" || command == "startorsplit") {
            if (now - lastSplitTime < debounce) return
            lastSplitTime = now
        }

        viewModelScope.launch {
            sendLiveSplitCommandUseCase(command)
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                pollOnce()
                delay(networkPreferences.value.pollingDelayMs)
            }
        }
    }

    private suspend fun pollOnce() {
        val time = sendLiveSplitCommandUseCase("getcurrenttime").getOrNull()
        if (time != null) {
            val trimmed = time.trim()
            _remoteTime.value = if (trimmed.contains('.')) {
                val parts = trimmed.split('.')
                val millis = parts[1].take(3).padEnd(3, '0')
                "${parts[0]}.$millis"
            } else {
                trimmed
            }
        }

        val phase = sendLiveSplitCommandUseCase("getcurrenttimerphase").getOrNull()
        if (phase != null) {
            val oldPhase = _remotePhase.value
            _remotePhase.value = phase.trim()
            if (oldPhase != "NotRunning" && _remotePhase.value == "NotRunning") {
                clearSplitTimesAndDeltas()
            }
        }

        // Poll current split name and index every 5 ticks (500ms)
        if (pollCounter % 5 == 0) {
            val splitNameResult = sendLiveSplitCommandUseCase("getcurrentsplitname")
            if (splitNameResult.isSuccess) {
                val name = splitNameResult.getOrNull()
                _currentSplitName.value = if (name == "-" || name.isNullOrBlank()) null else name.trim()
            }
            val splitIndexResult = sendLiveSplitCommandUseCase("getsplitindex")
            if (splitIndexResult.isSuccess) {
                val indexStr = splitIndexResult.getOrNull()
                val newIndex = indexStr?.trim()?.toIntOrNull() ?: -1
                val oldIndex = _currentSplitIndex.value
                
                if (newIndex != oldIndex) {
                    _currentSplitIndex.value = newIndex
                    if (newIndex > oldIndex && oldIndex >= 0) {
                        updateCompletedSplitData(oldIndex)
                    }
                }
            }

            if (_remotePhase.value == "Running" && _currentSplitIndex.value >= 0) {
                val deltaResult = sendLiveSplitCommandUseCase("getdelta")
                if (deltaResult.isSuccess) {
                    val delta = deltaResult.getOrNull()?.trim()
                    updateCurrentSplitDelta(delta)
                }
            }
        }

        // Periodically check/fetch full splits list if supported and empty
        if (_isSplitsListSupported.value && _splitsList.value.isEmpty() && pollCounter % 10 == 0) {
            viewModelScope.launch(Dispatchers.IO) {
                fetchSplitsList()
            }
        }

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

    private suspend fun fetchSplitsList() {
        if (!_isSplitsListSupported.value || isFetchingSplits) return
        isFetchingSplits = true
        try {
            val countResult = sendLiveSplitCommandUseCase("getsplitcount")
            if (countResult.isFailure) {
                if (countResult.exceptionOrNull() is java.net.SocketTimeoutException) {
                    _isSplitsListSupported.value = false
                }
                return
            }
            
            val countStr = countResult.getOrNull()?.trim() ?: return
            val count = countStr.toIntOrNull() ?: return
            
            val list = mutableListOf<RemoteSplit>()
            for (i in 0 until count) {
                val nameResult = sendLiveSplitCommandUseCase("getsplitname $i")
                val name = if (nameResult.isSuccess) nameResult.getOrNull()?.trim() ?: "Split $i" else "Split $i"
                
                list.add(RemoteSplit(name = name))
            }
            _splitsList.value = list

            if (_remotePhase.value != "NotRunning" && _currentSplitIndex.value > 0) {
                for (i in 0 until _currentSplitIndex.value) {
                    updateCompletedSplitData(i)
                }
            }
        } finally {
            isFetchingSplits = false
        }
    }

    private suspend fun updateCompletedSplitData(index: Int) {
        val list = _splitsList.value.toMutableList()
        if (index !in list.indices) return

        val timeResult = sendLiveSplitCommandUseCase("getlastsplittime")
        val time = timeResult.getOrNull()?.trim()

        if (time != null && time != "-") {
            val formattedTime = if (time.contains('.')) {
                val parts = time.split('.')
                "${parts[0]}.${parts[1].take(3)}"
            } else {
                time
            }
            list[index] = list[index].copy(actualTime = formattedTime)
            _splitsList.value = list
        }
    }

    private fun updateCurrentSplitDelta(delta: String?) {
        val index = _currentSplitIndex.value
        val list = _splitsList.value.toMutableList()
        if (index in list.indices) {
            val formattedDelta = if (delta != null && delta.contains('.')) {
                val parts = delta.split('.')
                "${parts[0]}.${parts[1].take(3)}"
            } else {
                delta
            }
            list[index] = list[index].copy(delta = formattedDelta)
            _splitsList.value = list
        }
    }

    private fun clearSplitTimesAndDeltas() {
        val list = _splitsList.value.map { it.copy(actualTime = null, delta = null) }
        _splitsList.value = list
    }

    private fun triggerAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            _isReconnecting.value = true
            val hostVal = _host.value
            val portInt = _port.value.toIntOrNull() ?: 16834
            
            for (attempt in 1..3) {
                _reconnectAttempts.value = attempt
                delay(2000)
                if (isExplicitlyDisconnected) break
                
                val result = connectToLiveSplitUseCase(hostVal, portInt)
                if (result.isSuccess) {
                    _isReconnecting.value = false
                    _reconnectAttempts.value = 0
                    return@launch
                }
            }
            _isReconnecting.value = false
            _reconnectAttempts.value = 0
            _errorMessage.value = "Connexion perdue. Reconnexion impossible."
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _remoteTime.value = "00:00:00.000"
        _remotePhase.value = "NotRunning"
        _gameName.value = null
        _categoryName.value = null
        _currentSplitName.value = null
        _currentSplitIndex.value = -1
        _splitsList.value = emptyList()
        pollCounter = 0
        isGameInfoSupported = true
        _isSplitsListSupported.value = true
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
