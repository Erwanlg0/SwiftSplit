package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import kotlinx.coroutines.flow.Flow

interface ObserveTimerLayoutPreferencesUseCase {
    operator fun invoke(): Flow<TimerLayoutPreferences>
}
