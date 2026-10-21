package com.example.etapa1.data.repository

import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserRole: StateFlow<UserRole>
    val isLoggedIn: StateFlow<Boolean>
    /** Habla con el servidor: se llama desde una corrutina y no bloquea la pantalla. */
    suspend fun login(email: String, password: String, role: UserRole): Result<UserRole>
    fun setRole(role: UserRole)
    fun logout()
}
