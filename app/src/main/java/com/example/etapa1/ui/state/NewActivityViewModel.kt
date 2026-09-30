package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NewActivityUiState(
    val category: String = "Alimentación",
    val portionLabel: String = "Todo el plato",
    val description: String = "",
    val time: String = "12:00 PM",
    val isSaved: Boolean = false
)

class NewActivityViewModel(
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewActivityUiState())
    val uiState: StateFlow<NewActivityUiState> = _uiState.asStateFlow()

    fun onCategoryChanged(cat: String) {
        _uiState.update { it.copy(category = cat) }
    }

    fun onPortionChanged(portion: String) {
        _uiState.update { it.copy(portionLabel = portion) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onTimeChanged(time: String) {
        _uiState.update { it.copy(time = time) }
    }

    fun saveActivity(time: String, category: String, portionLabel: String, description: String) {
        bitacoraRepository.addActivityCard(time, category, portionLabel, description)
        _uiState.update { it.copy(isSaved = true) }
    }
}
