package com.example.etapa1.data.remote

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NetworkErrorHelperTest {

    @Test
    fun parseException_unknownHost_returnsConnectionMessage() {
        val ex = UnknownHostException("list-testament-sea-workplace.trycloudflare.com")
        val message = NetworkErrorHelper.parseException(ex)
        assertTrue(message.contains("No se pudo conectar con el servidor"))
        assertTrue(message.contains("conexión a internet"))
    }

    @Test
    fun parseException_connectException_returnsServerDownMessage() {
        val ex = ConnectException("Connection refused")
        val message = NetworkErrorHelper.parseException(ex)
        assertTrue(message.contains("No se pudo establecer conexión"))
        assertTrue(message.contains("servidor esté encendido"))
    }

    @Test
    fun parseException_socketTimeout_returnsTimeoutMessage() {
        val ex = SocketTimeoutException("timeout")
        val message = NetworkErrorHelper.parseException(ex)
        assertTrue(message.contains("Tiempo de espera agotado"))
    }

    @Test
    fun parseException_sslException_returnsSecurityMessage() {
        val ex = SSLException("Handshake failed")
        val message = NetworkErrorHelper.parseException(ex)
        assertTrue(message.contains("SSL/HTTPS"))
    }

    @Test
    fun parseException_ioExceptionWithTimeout_returnsTimeoutMessage() {
        val ex = IOException("Read timed out")
        val message = NetworkErrorHelper.parseException(ex)
        assertTrue(message.contains("Tiempo de espera agotado"))
    }

    @Test
    fun parseHttpError_401_withCustomServerJson_returnsServerMessage() {
        val json = """{"error": "El correo o la contraseña no coinciden.", "code": "credenciales_invalidas"}"""
        val message = NetworkErrorHelper.parseHttpError(401, json)
        assertEquals("El correo o la contraseña no coinciden.", message)
    }

    @Test
    fun parseHttpError_401_withoutServerMessage_returnsFriendlyDefault() {
        val message = NetworkErrorHelper.parseHttpError(401, null)
        assertEquals("Correo o contraseña incorrectos.", message)
    }

    @Test
    fun parseHttpError_403_withDetail_returnsServerDetail() {
        val json = """{"detail": "Tu cuenta tiene asignado el rol FAMILIAR"}"""
        val message = NetworkErrorHelper.parseHttpError(403, json)
        assertEquals("Tu cuenta tiene asignado el rol FAMILIAR", message)
    }

    @Test
    fun parseHttpError_403_withoutBody_returnsPermissionsMessage() {
        val message = NetworkErrorHelper.parseHttpError(403, null)
        assertTrue(message.contains("permisos"))
    }

    @Test
    fun parseHttpError_502_returnsGatewayOfflineMessage() {
        val message = NetworkErrorHelper.parseHttpError(502, null)
        assertTrue(message.contains("servidor no está disponible"))
        assertTrue(message.contains("túnel"))
    }

    @Test
    fun parseResponse_handlesErrorResponseBody() {
        val response = Response.error<String>(
            404,
            """{"error": "Alumno no encontrado"}""".toResponseBody()
        )
        val message = NetworkErrorHelper.parseResponse(response)
        assertEquals("Alumno no encontrado", message)
    }
}
