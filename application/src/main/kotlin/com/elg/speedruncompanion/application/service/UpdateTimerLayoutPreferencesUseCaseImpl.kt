package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.UpdateTimerLayoutPreferencesUseCase
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import javax.inject.Inject

class UpdateTimerLayoutPreferencesUseCaseImpl @Inject constructor(
    private val settingsPort: SettingsPort
) : UpdateTimerLayoutPreferencesUseCase {
    override suspend fun invoke(preferences: TimerLayoutPreferences) {
        settingsPort.setTimerLayoutPreferences(preferences)
    }
}
