package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.TimerState
import kotlinx.coroutines.flow.Flow

interface ObserveTimerUseCase {
    operator fun invoke(): Flow<TimerState>
}
