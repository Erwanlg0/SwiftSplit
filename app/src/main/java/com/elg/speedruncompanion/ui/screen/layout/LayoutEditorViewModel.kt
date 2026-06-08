package com.elg.speedruncompanion.ui.screen.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.speedruncompanion.application.port.input.ObserveTimerLayoutPreferencesUseCase
import com.elg.speedruncompanion.application.port.input.UpdateTimerLayoutPreferencesUseCase
import com.elg.speedruncompanion.domain.model.TimerColorMode
import com.elg.speedruncompanion.domain.model.TimerLayoutPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LayoutEditorViewModel @Inject constructor(
    observeTimerLayoutPreferencesUseCase: ObserveTimerLayoutPreferencesUseCase,
    private val updateTimerLayoutPreferencesUseCase: UpdateTimerLayoutPreferencesUseCase
) : ViewModel() {

    val layoutPreferences = observeTimerLayoutPreferencesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerLayoutPreferences.DEFAULT
    )

    fun setShowLeadingZeros(enabled: Boolean) {
        update { it.copy(timeFormat = it.timeFormat.copy(showLeadingZeros = enabled)) }
    }

    fun setDecimalPlaces(places: Int) {
        update { it.copy(timeFormat = it.timeFormat.copy(decimalPlaces = places.coerceIn(1, 3))) }
    }

    fun setShowFraction(enabled: Boolean) {
        update { it.copy(timeFormat = it.timeFormat.copy(showFraction = enabled)) }
    }

    fun setColorMode(mode: TimerColorMode) {
        update { it.copy(colorMode = mode) }
    }

    private fun update(transform: (TimerLayoutPreferences) -> TimerLayoutPreferences) {
        viewModelScope.launch {
            val current = layoutPreferences.value
            updateTimerLayoutPreferencesUseCase(transform(current))
        }
    }
}
