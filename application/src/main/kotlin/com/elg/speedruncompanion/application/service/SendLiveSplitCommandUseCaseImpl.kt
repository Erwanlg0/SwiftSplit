package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.SendLiveSplitCommandUseCase
import com.elg.speedruncompanion.application.port.output.LiveSplitRemotePort
import javax.inject.Inject

class SendLiveSplitCommandUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort
) : SendLiveSplitCommandUseCase {
    override suspend fun invoke(command: String): Result<String?> {
        return remotePort.sendCommand(command)
    }
}
