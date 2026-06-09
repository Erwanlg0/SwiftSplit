package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ObserveTimerUseCase
import com.elg.swiftsplit.domain.model.TimerState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTimerUseCaseImpl @Inject constructor(
    private val timerManager: TimerManager
) : ObserveTimerUseCase {
    override fun invoke(): Flow<TimerState> = timerManager.timerState
}
