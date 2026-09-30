package com.example.casosdeusopadre.ui.screens.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.casosdeusopadre.data.models.Photo
import com.example.casosdeusopadre.data.repository.ChildcareRepository
import com.example.casosdeusopadre.data.repository.FakeChildcareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PhotosUiState(
    val isLoading: Boolean = true,
    val photos: List<Photo> = emptyList(),
    val selectedPhoto: Photo? = null,
    val showDeleteConfirmation: Boolean = false,
    val actionMessage: String? = null
)

class PhotosViewModel(
    private val repository: ChildcareRepository = FakeChildcareRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = repository.getCurrentUser()
            if (user != null) {
                val children = repository.getChildrenForParent(user.id)
                val allPhotos = mutableSetOf<Photo>()
                for (child in children) {
                    allPhotos.addAll(repository.getPhotos(child.id))
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    photos = allPhotos.toList().sortedByDescending { it.date }
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, actionMessage = "Error de sesión")
            }
        }
    }

    fun selectPhoto(photo: Photo) {
        _uiState.value = _uiState.value.copy(selectedPhoto = photo)
    }

    fun clearSelectedPhoto() {
        _uiState.value = _uiState.value.copy(selectedPhoto = null, showDeleteConfirmation = false)
    }

    fun promptDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    fun confirmDelete() {
        val photo = _uiState.value.selectedPhoto ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showDeleteConfirmation = false, selectedPhoto = null)
            val result = repository.requestPhotoDeletion(photo.id)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(actionMessage = "Foto eliminada permanentemente")
                    loadPhotos() // reload
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        actionMessage = "Error al eliminar la foto: ${it.message}"
                    )
                }
            )
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
