package com.elg.speedruncompanion.application.port.input

import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences

interface UpdateTimerLayoutPreferencesUseCase {
    suspend operator fun invoke(preferences: TimerLayoutPreferences)
}
