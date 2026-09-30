package com.example.casosdeusopadre.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.casosdeusopadre.data.models.User
import com.example.casosdeusopadre.data.repository.ChildcareRepository
import com.example.casosdeusopadre.data.repository.FakeChildcareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class NoChildrenError(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val repository: ChildcareRepository = FakeChildcareRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    // Verify if user has children
                    val children = repository.getChildrenForParent(user.id)
                    if (children.isEmpty()) {
                        _uiState.value = LoginUiState.NoChildrenError("No tienes niños vinculados. Por favor contacta a la dirección.")
                    } else {
                        _uiState.value = LoginUiState.Success(user)
                    }
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Error desconocido")
                }
            )
        }
    }
}
