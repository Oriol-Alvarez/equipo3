package com.example.etapa1.model

/**
 * Valores neutros para mostrar mientras llegan los datos del servidor.
 * No son datos de ejemplo: no tienen nombres ni información inventada.
 */
object SinDatos {
    val nino = Child(
        id = "",
        fullName = "",
        shortName = "",
        ageText = "",
        roomText = "",
        groupText = "",
        statusText = "",
        isPresent = false,
        avatarInitials = "",
        avatarBgColor = 0xFFB0BEC5
    )

    val sala = Room(id = "", name = "", level = "", presentCount = 0, totalCount = 0)
}
