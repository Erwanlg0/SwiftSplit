package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.input.ObserveTimerLayoutPreferencesUseCase
import com.elg.speedruncompanion.application.port.output.SettingsPort
import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTimerLayoutPreferencesUseCaseImpl @Inject constructor(
    private val settingsPort: SettingsPort
) : ObserveTimerLayoutPreferencesUseCase {
    override fun invoke(): Flow<TimerLayoutPreferences> {
        return settingsPort.observeTimerLayoutPreferences()
    }
}
