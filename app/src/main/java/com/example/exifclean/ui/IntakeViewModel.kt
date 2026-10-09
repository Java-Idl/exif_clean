package com.example.exifclean.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.exifclean.data.IntakeCopier
import com.example.exifclean.data.IntakeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IntakeUiState {
    object Idle : IntakeUiState()
    object Loading : IntakeUiState()
    data class Success(val items: List<IntakeItem>) : IntakeUiState()
    data class Error(val message: String) : IntakeUiState()
}

class IntakeViewModel(application: Application) : AndroidViewModel(application) {

    private val intakeCopier = IntakeCopier(application)

    private val _uiState = MutableStateFlow<IntakeUiState>(IntakeUiState.Idle)
    val uiState: StateFlow<IntakeUiState> = _uiState.asStateFlow()

    fun processUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = IntakeUiState.Loading
            try {
                val currentItems = when (val state = _uiState.value) {
                    is IntakeUiState.Success -> state.items
                    else -> emptyList()
                }
                val newItems = intakeCopier.copyUrisToIntakeCache(uris)
                val combined = currentItems + newItems
                _uiState.value = IntakeUiState.Success(combined)
            } catch (e: Exception) {
                _uiState.value = IntakeUiState.Error(e.localizedMessage ?: "Failed to import images")
            }
        }
    }

    fun removeItem(item: IntakeItem) {
        val currentItems = when (val state = _uiState.value) {
            is IntakeUiState.Success -> state.items
            else -> return
        }
        intakeCopier.deleteItem(item)
        val updated = currentItems.filterNot { it.id == item.id }
        _uiState.value = if (updated.isEmpty()) IntakeUiState.Idle else IntakeUiState.Success(updated)
    }

    fun clearAll() {
        intakeCopier.clearIntakeCache()
        _uiState.value = IntakeUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure intake cache is cleaned up if viewmodel is destroyed
        intakeCopier.clearIntakeCache()
    }
}
