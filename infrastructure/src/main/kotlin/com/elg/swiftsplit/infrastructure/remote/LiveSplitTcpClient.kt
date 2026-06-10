package com.elg.swiftsplit.infrastructure.remote

import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private val ioMutex = Mutex()

    override val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED && socket?.isConnected == true

    override suspend fun connect(host: String, port: Int, timeoutMs: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext Result.success(Unit)

        _connectionState.value = ConnectionState.CONNECTING
        try {
            val address = InetSocketAddress(host, port)
            val newSocket = Socket()
            newSocket.connect(address, timeoutMs)
            newSocket.soTimeout = timeoutMs
            
            socket = newSocket
            writer = PrintWriter(newSocket.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
            
            _connectionState.value = ConnectionState.CONNECTED
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
        val currentReader = reader
        if (!isConnected || currentWriter == null || currentReader == null) {
            return@withContext Result.failure(Exception("Not connected to LiveSplit Server"))
        }

        try {
            ioMutex.withLock {
                currentWriter.print(command + LiveSplitProtocol.TERMINATOR)
                currentWriter.flush()

                if (LiveSplitProtocol.expectsResponse(command)) {
                    val response = currentReader.readLine()
                    if (response == null) {
                        cleanup()
                        _connectionState.value = ConnectionState.DISCONNECTED
                        Result.failure(Exception("Connection closed by server"))
                    } else {
                        Result.success(response.trim())
                    }
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            cleanup()
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    private fun cleanup() {
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            writer = null
            reader = null
            socket = null
        }
    }
}
