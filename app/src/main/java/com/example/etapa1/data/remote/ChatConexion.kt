package com.example.etapa1.data.remote

import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.domain.esperaAntesDeReintentar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Conexión WebSocket en tiempo real de la aplicación, siguiendo fielmente los principios
 * y mecanismos de resiliencia de la Práctica 12 (Charla):
 *
 * 1. Una sola conexión WebSocket activa a la vez para toda la app.
 * 2. Guarda contra sockets obsoletos: `if (webSocket !== actual) return` en todos los callbacks
 *    para evitar que el cierre de un socket viejo sobreescriba el estado de una nueva conexión.
 * 3. Retroceso exponencial con tope: espera (1s, 2s, 4s, 8s, 16s, 30s) ante desconexiones.
 * 4. Detección de rechazo por HTTP 403 / código 1008: si el token es inválido o caducó, se pasa
 *    al estado [EstadoConexion.Rechazado] sin reintentar en bucle.
 * 5. Dos flujos reactivos hacia afuera: [estado] (StateFlow) y [eventos] (SharedFlow con buffer).
 */
class ChatConexion(
    private val cliente: OkHttpClient,
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconectado)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoServidor>(extraBufferCapacity = 64)
    val eventos: SharedFlow<EventoServidor> = _eventos.asSharedFlow()

    @Volatile
    private var actual: WebSocket? = null
    private var peticion: Request? = null
    private var reintento: Job? = null
    private var intentos = 0

    /**
     * Inicia la conexión WebSocket hacia el servidor proporcionado utilizando el token JWT.
     */
    fun conectar(servidor: String, token: String) {
        desconectar()

        val endpoint = when {
            servidor.contains("/ws/") || servidor.endsWith("/chat") -> servidor
            servidor.endsWith("/api/v1") || servidor.endsWith("/api/v1/") -> "${servidor.trimEnd('/')}/ws/v1/connect"
            else -> "$servidor/chat"
        }

        peticion = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $token")
            .header("ngrok-skip-browser-warning", "true")
            .build()

        abrir()
    }

    /**
     * Cierra limpiamente el WebSocket actual y cancela cualquier tarea de reintento pendiente.
     */
    fun desconectar() {
        reintento?.cancel()
        reintento = null
        peticion = null
        intentos = 0
        actual?.close(1000, "Desconexión manual")
        actual = null
        _estado.value = EstadoConexion.Desconectado
    }

    /**
     * Envía una orden o mensaje tipado a través del WebSocket.
     * Retorna false si no hay un socket conectado activamente.
     */
    fun enviar(orden: OrdenChat): Boolean =
        actual?.send(ChatProtocolo.escribir(orden)) ?: false

    /**
     * Envía texto plano o JSON directamente a través del WebSocket.
     */
    fun enviar(texto: String): Boolean =
        actual?.send(texto) ?: false

    private fun abrir() {
        val p = peticion ?: return
        _estado.value = EstadoConexion.Conectando
        actual = cliente.newWebSocket(p, Escucha())
    }

    private fun reintentar() {
        intentos++
        val espera = esperaAntesDeReintentar(intentos)
        _estado.value = EstadoConexion.Reconectando(espera / 1_000)
        reintento = alcance.launch {
            delay(espera)
            abrir()
        }
    }

    private inner class Escucha : WebSocketListener() {
        // En cada callback de OkHttp se verifica: si este socket ya no es el actual,
        // lo que reporte ya no importa y se descarta inmediatamente.
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket !== actual) return
            intentos = 0
            _estado.value = EstadoConexion.Conectado
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== actual) return
            ChatProtocolo.leer(text)?.let { _eventos.tryEmit(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            // El servidor solicita cerrar el socket: respondemos con cierre recíproco
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== actual) return
            reintentar()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== actual) return
            if (response?.code == 403) {
                // El servidor no reconoce el token: reintentar no solucionará el problema
                actual = null
                _estado.value = EstadoConexion.Rechazado
            } else {
                reintentar()
            }
        }
    }
}
