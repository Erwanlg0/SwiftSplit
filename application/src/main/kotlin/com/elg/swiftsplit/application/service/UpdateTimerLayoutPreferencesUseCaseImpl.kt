package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.UpdateTimerLayoutPreferencesUseCase
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import javax.inject.Inject

class UpdateTimerLayoutPreferencesUseCaseImpl @Inject constructor(
    private val settingsPort: SettingsPort
) : UpdateTimerLayoutPreferencesUseCase {
    override suspend fun invoke(preferences: TimerLayoutPreferences) {
        settingsPort.setTimerLayoutPreferences(preferences)
    }
}
