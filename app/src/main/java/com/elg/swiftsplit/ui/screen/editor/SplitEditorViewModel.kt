package com.elg.swiftsplit.ui.screen.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elg.swiftsplit.application.port.input.GetRunByIdUseCase
import com.elg.swiftsplit.application.port.input.UpdateRunUseCase
import com.elg.swiftsplit.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRunByIdUseCase: GetRunByIdUseCase,
    private val updateRunUseCase: UpdateRunUseCase
) : ViewModel() {

    private val runId: String = checkNotNull(savedStateHandle["runId"])

    private val _run = MutableStateFlow<Run?>(null)
    val run = _run.asStateFlow()

    private val _segments = MutableStateFlow<List<Segment>>(emptyList())
    val segments = _segments.asStateFlow()

    init {
        viewModelScope.launch {
            val r = getRunByIdUseCase(RunId(runId))
            _run.value = r
            _segments.value = r?.segments ?: emptyList()
        }
    }

    fun updateGameName(name: String) {
        val current = _run.value ?: return
        _run.value = current.copy(gameInfo = current.gameInfo.copy(gameName = name))
    }

    fun updateCategoryName(name: String) {
        val current = _run.value ?: return
        _run.value = current.copy(gameInfo = current.gameInfo.copy(categoryName = name))
    }

    fun updateSegmentName(index: Int, name: String) {
        val list = _segments.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(name = name)
            _segments.value = list
        }
    }

    fun addSegment() {
        val list = _segments.value.toMutableList()
        list.add(Segment(name = "New Split"))
        _segments.value = list
    }

    fun removeSegment(index: Int) {
        val list = _segments.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _segments.value = list
        }
    }

    fun moveSegment(fromIndex: Int, toIndex: Int) {
        val list = _segments.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _segments.value = list
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val current = _run.value ?: return@launch
            val updated = current.copy(segments = _segments.value)
            updateRunUseCase(updated)
            onSuccess()
        }
    }
}
