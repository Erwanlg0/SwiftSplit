package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.SendLiveSplitCommandUseCase
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import javax.inject.Inject

class SendLiveSplitCommandUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort
) : SendLiveSplitCommandUseCase {
    override suspend fun invoke(command: String): Result<String?> {
        return remotePort.sendCommand(command)
    }
}
