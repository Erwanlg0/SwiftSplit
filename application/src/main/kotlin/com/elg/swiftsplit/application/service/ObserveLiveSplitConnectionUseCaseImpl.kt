package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ObserveLiveSplitConnectionUseCase
import com.elg.swiftsplit.application.port.output.ConnectionState
import com.elg.swiftsplit.application.port.output.LiveSplitRemotePort
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLiveSplitConnectionUseCaseImpl @Inject constructor(
    private val remotePort: LiveSplitRemotePort
) : ObserveLiveSplitConnectionUseCase {
    override fun invoke(): Flow<ConnectionState> {
        return remotePort.observeConnectionState()
    }
}
