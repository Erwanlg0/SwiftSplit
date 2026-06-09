package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.TimerState
import kotlinx.coroutines.flow.Flow

interface ObserveTimerUseCase {
    operator fun invoke(): Flow<TimerState>
}
