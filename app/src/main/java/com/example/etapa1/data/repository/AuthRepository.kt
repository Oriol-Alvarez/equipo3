package com.example.etapa1.data.repository

import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserRole: StateFlow<UserRole>
    /** Habla con el servidor: se llama desde una corrutina y no bloquea la pantalla. */
    suspend fun login(email: String, password: String, role: UserRole): Result<UserRole>
    fun setRole(role: UserRole)
    fun logout()

    /**
     * Rol de la sesión guardada en el teléfono, o null si no hay sesión.
     * Permite abrir la app sin internet (RF-11) pidiendo la huella en vez de la contraseña (RF-13).
     */
    fun sesionGuardada(): UserRole? = null

    /** Nombre de quien tiene la sesión (para la pantalla de bloqueo). */
    fun nombreSesion(): String? = null
}
