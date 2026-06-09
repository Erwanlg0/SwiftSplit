package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.input.ObserveTimerLayoutPreferencesUseCase
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.TimerLayoutPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTimerLayoutPreferencesUseCaseImpl @Inject constructor(
    private val settingsPort: SettingsPort
) : ObserveTimerLayoutPreferencesUseCase {
    override fun invoke(): Flow<TimerLayoutPreferences> {
        return settingsPort.observeTimerLayoutPreferences()
    }
}
