package com.elg.speedruncompanion.application.port.output

import kotlinx.coroutines.flow.Flow

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

interface LiveSplitRemotePort {
    fun observeConnectionState(): Flow<ConnectionState>
    suspend fun connect(host: String, port: Int = 16834, timeoutMs: Int = 5000): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Result<String?>
    val isConnected: Boolean
}
