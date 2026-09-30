package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepositoryImpl : AuthRepository {
    private val _currentUserRole = MutableStateFlow(UserRole.EDUCADORA)
    override val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    override fun login(email: String, password: String, role: UserRole): Result<UserRole> {
        _currentUserRole.value = role
        return Result.success(role)
    }

    override fun setRole(role: UserRole) {
        _currentUserRole.value = role
    }

    override fun logout() {
        _currentUserRole.value = UserRole.EDUCADORA
    }
}
