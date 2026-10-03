package com.example.etapa1.domain

import com.example.etapa1.model.UserRole


data class Sesion(
    val role: UserRole,
    //Por el momento no se usa
    val usuario: String = if (role == UserRole.FAMILIAR) "familiar@sonrisas.edu" else "educadora@sonrisas.edu",
    val token: String = "token_jwt_simulado"
) {
    val isEducadora: Boolean get() = role == UserRole.EDUCADORA
    val isFamiliar: Boolean get() = role == UserRole.FAMILIAR
}
