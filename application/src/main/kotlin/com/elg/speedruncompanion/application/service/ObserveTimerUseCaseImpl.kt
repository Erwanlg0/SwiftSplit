package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.ObserveTimerUseCase
import com.elg.speedruncompanion.domain.model.TimerState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : ObserveTimerUseCase {
    override fun invoke(): Flow<TimerState> = timerManager.timerState
}
