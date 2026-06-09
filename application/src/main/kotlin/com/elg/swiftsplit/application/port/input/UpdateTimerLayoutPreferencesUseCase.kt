package com.elg.swiftsplit.application.port.input

import com.elg.swiftsplit.domain.model.TimerLayoutPreferences

interface UpdateTimerLayoutPreferencesUseCase {
    suspend operator fun invoke(preferences: TimerLayoutPreferences)
}
