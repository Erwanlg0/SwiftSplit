package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.DisconnectLiveSplitUseCase
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import javax.inject.Inject

class DisconnectLiveSplitUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort
) : DisconnectLiveSplitUseCase {
    override suspend fun invoke() {
        remotePort.disconnect()
    }
}
