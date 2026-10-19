package com.example.etapa1.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.etapa1.SonrisasApplication

/**
 * Nombre de la persona con la sesión iniciada (el que manda el servidor al entrar).
 * En el modo sin servidor, o si aún no se conoce, devuelve [siNoHay].
 */
@Composable
fun nombreDeSesion(siNoHay: String): String {
    val app = LocalContext.current.applicationContext as? SonrisasApplication
    val container = app?.container ?: return siNoHay
    if (container.useMocks) return siNoHay
    return container.tokenManager.userDisplayName?.takeIf { it.isNotBlank() } ?: siNoHay
}

/** Nombre, correo y rol de la sesión actual para mostrarlos al usuario; null si no hay datos. */
data class DatosSesion(val nombre: String, val correo: String, val rol: String)

@Composable
fun datosDeSesion(): DatosSesion? {
    val app = LocalContext.current.applicationContext as? SonrisasApplication
    val tokens = app?.container?.tokenManager ?: return null
    val rol = when (tokens.userRole?.uppercase()) {
        "FAMILIAR" -> "Familia"
        "EDUCADORA" -> "Educadora"
        "DIRECCION" -> "Dirección"
        else -> tokens.userRole.orEmpty()
    }
    val nombre = tokens.userDisplayName.orEmpty()
    val correo = tokens.userEmail.orEmpty()
    if (nombre.isBlank() && correo.isBlank()) return null
    return DatosSesion(nombre, correo, rol)
}
