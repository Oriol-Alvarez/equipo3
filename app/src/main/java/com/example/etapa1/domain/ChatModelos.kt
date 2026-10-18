package com.example.etapa1.domain

/**
 * Representa el estado de conexión del WebSocket en tiempo real,
 * siguiendo la arquitectura de Práctica 12 (Charla).
 */
sealed interface EstadoConexion {
    /** La app no está conectada al servidor (o se cerró la sesión voluntariamente). */
    data object Desconectado : EstadoConexion

    /** Se está negociando la conexión HTTP Upgrade / WebSocket con el servidor. */
    data object Conectando : EstadoConexion

    /** Conexión establecida y lista para enviar y recibir eventos en tiempo real. */
    data object Conectado : EstadoConexion

    /**
     * Se perdió la conexión temporalmente y se reintentará en [enSegundos] s
     * mediante retroceso exponencial (exponential backoff).
     */
    data class Reconectando(val enSegundos: Long) : EstadoConexion

    /**
     * El servidor rechazó la conexión (HTTP 403 / código 1008 de violación de política).
     * El token no es válido o caducó; no tiene sentido reintentar en bucle.
     */
    data object Rechazado : EstadoConexion
}

/**
 * Cuánto esperar antes del intento número [intento] de reconectar:
 * 1 s, 2 s, 4 s, 8 s, 16 s, y de ahí 30 s siempre.
 *
 * El retroceso exponencial evita saturar el servidor ante caídas o reinicios.
 */
fun esperaAntesDeReintentar(intento: Int): Long =
    (1_000L shl (intento - 1).coerceIn(0, 5)).coerceAtMost(30_000L)
