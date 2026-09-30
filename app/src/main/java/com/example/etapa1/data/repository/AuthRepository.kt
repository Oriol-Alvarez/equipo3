package com.example.etapa1.data.repository

import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserRole: StateFlow<UserRole>
    fun login(email: String, password: String, role: UserRole): Result<UserRole>
    fun setRole(role: UserRole)
    fun logout()
}
