package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ConnectToLiveSplitUseCase
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import com.elg.swiftsplit.application.port.output.SettingsPort
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ConnectToLiveSplitUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort,
    private val settingsPort: SettingsPort
) : ConnectToLiveSplitUseCase {
    override suspend fun invoke(host: String, port: Int): Result<Unit> {
        val timeoutMs = settingsPort.observeNetworkPreferences().first().networkTimeoutMs.toInt()
        return remotePort.connect(host, port, timeoutMs)
    }
}
