package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.DisconnectLiveSplitUseCase
import com.elg.speedruncompanion.application.port.output.LiveSplitRemotePort
import javax.inject.Inject

class DisconnectLiveSplitUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort
) : DisconnectLiveSplitUseCase {
    override suspend fun invoke() {
        remotePort.disconnect()
    }
}
