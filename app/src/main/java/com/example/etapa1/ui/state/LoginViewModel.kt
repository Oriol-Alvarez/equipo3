package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val selectedRole: UserRole = UserRole.EDUCADORA,
    val email: String = "correo@ejemplo.com",
    val password: String = "••••••••",
    val passwordVisible: Boolean = false,
    val hasError: Boolean = false,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, hasError = false) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, hasError = false) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submitLogin(): UserRole? {
        val state = _uiState.value
        val isValid = state.email.isNotBlank() && state.password.isNotBlank()
        if (!isValid) {
            _uiState.update { it.copy(hasError = true) }
            return null
        }
        authRepository.login(state.email, state.password, state.selectedRole)
        _uiState.update { it.copy(isSuccess = true) }
        return state.selectedRole
    }
}
