package com.example.etapa1.data.remote

import com.example.etapa1.domain.EstadoConexion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de ChatConexion contra servidores WebSocket en memoria JVM (MockWebServer),
 * siguiendo la suite de pruebas y mecanismos de resiliencia de la Práctica 12 (Charla).
 */
class ChatConexionTest {

    private val salaA = MockWebServer()
    private val salaB = MockWebServer()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val conexion = ChatConexion(OkHttpClient(), alcance)

    @After
    fun cerrar() {
        conexion.desconectar()
        alcance.cancel()
        salaA.shutdown()
        salaB.shutdown()
    }

    private fun MockWebServer.sala(
        tardaEnDespedirse: Long = 0,
        alAbrir: (WebSocket) -> Unit = {}
    ): String {
        enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = alAbrir(webSocket)
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (tardaEnDespedirse > 0) Thread.sleep(tardaEnDespedirse)
                    webSocket.close(1000, null)
                }
            })
        )
        return url("/").toString().removeSuffix("/")
    }

    private fun esperar(estado: EstadoConexion) {
        runBlocking {
            withTimeout(5_000) {
                conexion.estado.first { it == estado }
            }
        }
    }

    @Test
    fun manda_el_token_y_recibe_la_bienvenida() {
        val url = salaA.sala { ws ->
            ws.send("""{"tipo":"bienvenida","yo":"dani"}""")
        }

        val bienvenida = runBlocking {
            val primero = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5_000) { conexion.eventos.first() }
            }
            conexion.conectar(url, "token-de-dani")
            primero.await()
        }

        assertTrue(bienvenida is EventoServidor.Bienvenida)
        assertEquals("dani", (bienvenida as EventoServidor.Bienvenida).yo)
        assertEquals("Bearer token-de-dani", salaA.takeRequest().getHeader("Authorization"))
        esperar(EstadoConexion.Conectado)
    }

    @Test
    fun el_cierre_de_la_conexion_vieja_no_apaga_la_nueva() {
        conexion.conectar(salaA.sala(tardaEnDespedirse = 300), "uno")
        esperar(EstadoConexion.Conectado)
        conexion.conectar(salaB.sala(), "dos")
        esperar(EstadoConexion.Conectado)

        // El cierre de la sala A llega 300 ms DESPUÉS de que abrió la B. La conexión nueva debe seguir Conectada.
        Thread.sleep(1_000)
        assertEquals(EstadoConexion.Conectado, conexion.estado.value)
    }

    @Test
    fun si_la_sala_no_reconoce_el_token_no_reintenta() {
        salaA.enqueue(MockResponse().setResponseCode(403))
        conexion.conectar(salaA.url("/").toString().removeSuffix("/"), "token-invalido")
        esperar(EstadoConexion.Rechazado)

        Thread.sleep(1_500)
        assertEquals(EstadoConexion.Rechazado, conexion.estado.value)
        assertEquals(1, salaA.requestCount)
    }

    @Test
    fun si_la_sala_se_cae_reintenta_y_vuelve() {
        // La primera conexión se cierra de inmediato simulando un reinicio del servidor
        val url = salaA.sala { it.close(1012, "reinicio") }
        salaA.sala()
        conexion.conectar(url, "token-reconexion")

        esperar(EstadoConexion.Reconectando(1))
        esperar(EstadoConexion.Conectado)
        assertEquals(2, salaA.requestCount)
    }

    @Test
    fun recibe_mensaje_en_tiempo_real() {
        val url = salaA.sala { ws ->
            ws.send("""{"event":"chat:message:new","data":{"chatId":"wchat_1","message":{"id":"m_1","senderId":"u1","senderName":"Laura","message":"Hola a todos","time":"10:00 AM","isOutgoing":false}}}""")
        }

        val evento = runBlocking {
            val primero = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5_000) { conexion.eventos.first() }
            }
            conexion.conectar(url, "token-test")
            primero.await()
        }

        assertTrue(evento is EventoServidor.NuevoMensajeChat)
        val chatMsg = evento as EventoServidor.NuevoMensajeChat
        assertEquals("wchat_1", chatMsg.chatId)
        assertEquals("Hola a todos", chatMsg.message.message)
    }
}
