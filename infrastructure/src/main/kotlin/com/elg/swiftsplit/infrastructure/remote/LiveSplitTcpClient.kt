package com.elg.swiftsplit.infrastructure.remote

import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSplitTcpClient @Inject constructor() : LiveSplitRemotePort {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var coroutineScope: CoroutineScope? = null

    private val responseMutex = Mutex()
    private var pendingResponseDeferred: CompletableDeferred<String?>? = null

    override val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED && socket?.isConnected == true

    override suspend fun connect(host: String, port: Int, timeoutMs: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext Result.success(Unit)

        _connectionState.value = ConnectionState.CONNECTING
        try {
            val address = InetSocketAddress(host, port)
            val newSocket = Socket()
            newSocket.connect(address, timeoutMs)
            
            socket = newSocket
            writer = PrintWriter(newSocket.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
            
            _connectionState.value = ConnectionState.CONNECTED
            
            val newScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            coroutineScope = newScope
            newScope.launch {
                runReaderLoop()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            cleanup()
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        cleanup()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendCommand(command: String): Result<String?> = withContext(Dispatchers.IO) {
        val currentWriter = writer
        if (!isConnected || currentWriter == null) {
            return@withContext Result.failure(Exception("Not connected to LiveSplit Server"))
        }

        try {
            if (LiveSplitProtocol.expectsResponse(command)) {
                responseMutex.withLock {
                    val deferred = CompletableDeferred<String?>()
                    pendingResponseDeferred = deferred
                    currentWriter.print(command + LiveSplitProtocol.TERMINATOR)
                    currentWriter.flush()
                    
                    try {
                        withTimeout(3000) {
                            val response = deferred.await()
                            Result.success(response)
                        }
                    } catch (e: TimeoutCancellationException) {
                        Result.failure(Exception("Timeout waiting for response to command: $command"))
                    } finally {
                        pendingResponseDeferred = null
                    }
                }
            } else {
                currentWriter.print(command + LiveSplitProtocol.TERMINATOR)
                currentWriter.flush()
                Result.success(null)
            }
        } catch (e: Exception) {
            cleanup()
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    private suspend fun runReaderLoop() {
        val currentReader = reader ?: return
        try {
            val scope = coroutineScope ?: return
            while (scope.isActive && isConnected) {
                val line = currentReader.readLine() ?: break
                handleIncomingMessage(line)
            }
        } catch (e: Exception) {
            
        } finally {
            if (isConnected) {
                withContext(NonCancellable) {
                    cleanup()
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(line: String) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return

        
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                
                val event = Json.decodeFromString<LiveSplitEvent>(trimmed)
                _events.emit(trimmed)
                return
            } catch (e: Exception) {
                
            }
        }

        
        val deferred = pendingResponseDeferred
        if (deferred != null && deferred.isActive) {
            deferred.complete(trimmed)
        } else {
            
            _events.emit(trimmed)
        }
    }

    private fun cleanup() {
        try {
            coroutineScope?.cancel()
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            
        } finally {
            coroutineScope = null
            writer = null
            reader = null
            socket = null
            pendingResponseDeferred?.cancel()
            pendingResponseDeferred = null
        }
    }
}

