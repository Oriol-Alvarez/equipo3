package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PublishLostObjectUiState(
    val selectedImageUri: String? = null,
    val description: String = "",
    val location: String = "",
    val selectedCategory: String = "Ropa",
    val photoError: Boolean = false,
    val descriptionError: Boolean = false,
    val categoryError: Boolean = false,
    val isPublished: Boolean = false
)

class PublishLostObjectViewModel(
    private val lostObjectsRepository: LostObjectsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishLostObjectUiState())
    val uiState: StateFlow<PublishLostObjectUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: String?) {
        _uiState.update { it.copy(selectedImageUri = uri, photoError = false) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description, descriptionError = false) }
    }

    fun onLocationChanged(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = false) }
    }

    fun publish(roomName: String): LostItem? {
        val state = _uiState.value
        val photoErr = state.selectedImageUri == null
        val descErr = state.description.isBlank()
        val catErr = state.selectedCategory.isBlank()

        if (photoErr || descErr || catErr) {
            _uiState.update {
                it.copy(
                    photoError = photoErr,
                    descriptionError = descErr,
                    categoryError = catErr
                )
            }
            return null
        }

        val title = if (state.description.contains(",")) {
            state.description.substringBefore(",").trim()
        } else {
            state.description.take(25)
        }

        val newItem = LostItem(
            id = "lost_${System.currentTimeMillis()}",
            title = title.ifBlank { "Objeto Encontrado" },
            description = state.description.trim(),
            category = state.selectedCategory,
            location = state.location.trim().ifBlank { null },
            imageUri = state.selectedImageUri,
            room = roomName
        )

        lostObjectsRepository.publishLostItem(newItem)
        _uiState.update { it.copy(isPublished = true) }
        return newItem
    }

    fun publishItem(item: LostItem) {
        lostObjectsRepository.publishLostItem(item)
        _uiState.update { it.copy(isPublished = true) }
    }
}
