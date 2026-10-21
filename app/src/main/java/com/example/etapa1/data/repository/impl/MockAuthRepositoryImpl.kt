package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepositoryImpl(
    initialIsLoggedIn: Boolean = false
) : AuthRepository {
    private val _currentUserRole = MutableStateFlow(UserRole.EDUCADORA)
    override val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(initialIsLoggedIn)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun login(email: String, password: String, role: UserRole): Result<UserRole> {
        val resolvedRole = when {
            email.contains("familiar", ignoreCase = true) -> UserRole.FAMILIAR
            email.contains("admin", ignoreCase = true) || email.contains("direccion", ignoreCase = true) -> UserRole.ADMINISTRADOR
            else -> role
        }
        _currentUserRole.value = resolvedRole
        _isLoggedIn.value = true
        return Result.success(resolvedRole)
    }

    override fun setRole(role: UserRole) {
        _currentUserRole.value = role
        _isLoggedIn.value = true
    }

    override fun logout() {
        _currentUserRole.value = UserRole.EDUCADORA
        _isLoggedIn.value = false
    }
}
