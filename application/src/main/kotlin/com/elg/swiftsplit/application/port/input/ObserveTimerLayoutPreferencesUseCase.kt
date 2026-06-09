package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import kotlinx.coroutines.flow.Flow

interface ObserveTimerLayoutPreferencesUseCase {
    operator fun invoke(): Flow<TimerLayoutPreferences>
}
