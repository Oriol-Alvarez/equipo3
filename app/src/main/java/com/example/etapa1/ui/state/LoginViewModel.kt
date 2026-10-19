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
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    /** Esperando la respuesta del servidor. */
    val isLoading: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, hasError = false, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, hasError = false, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, hasError = false, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /** Valida y pide al servidor iniciar sesión. Devuelve el rol si entró, o null. */
    suspend fun submitLogin(): UserRole? {
        val state = _uiState.value
        if (state.isLoading) return null
        val isValid = state.email.isNotBlank() && state.password.isNotBlank()
        if (!isValid) {
            _uiState.update {
                it.copy(
                    hasError = true,
                    errorMessage = "Por favor, introduce tu correo y contraseña."
                )
            }
            return null
        }
        _uiState.update { it.copy(isLoading = true, hasError = false, errorMessage = null) }
        val result = authRepository.login(state.email, state.password, state.selectedRole)
        return if (result.isSuccess) {
            _uiState.update { it.copy(isSuccess = true, isLoading = false, hasError = false, errorMessage = null) }
            result.getOrNull() ?: state.selectedRole
        } else {
            val msg = result.exceptionOrNull()?.message ?: "Error al iniciar sesión."
            _uiState.update { it.copy(hasError = true, isLoading = false, errorMessage = msg) }
            null
        }
    }

    fun toggleError() {
        _uiState.update { it.copy(hasError = !it.hasError) }
    }
}
