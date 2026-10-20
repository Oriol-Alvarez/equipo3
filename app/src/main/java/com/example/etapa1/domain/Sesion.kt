package com.example.etapa1.domain

import com.example.etapa1.model.UserRole


data class Sesion(
    val role: UserRole,
    val usuario: String = when (role) {
        UserRole.FAMILIAR -> "familiar@sonrisas.edu"
        UserRole.ADMINISTRADOR -> "direccion@sonrisas.edu"
        else -> "educadora@sonrisas.edu"
    },
    val token: String = "token_jwt_simulado"
) {
    val isEducadora: Boolean get() = role == UserRole.EDUCADORA
    val isFamiliar: Boolean get() = role == UserRole.FAMILIAR
    val isAdmin: Boolean get() = role == UserRole.ADMINISTRADOR
}
