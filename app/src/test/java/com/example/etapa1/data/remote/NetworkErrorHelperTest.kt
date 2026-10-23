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
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseException_connectException_returnsServerDownMessage() {
        val ex = ConnectException("Connection refused")
        val message = NetworkErrorHelper.parseException(ex)
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseException_socketTimeout_returnsTimeoutMessage() {
        val ex = SocketTimeoutException("timeout")
        val message = NetworkErrorHelper.parseException(ex)
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseException_sslException_returnsSecurityMessage() {
        val ex = SSLException("Handshake failed")
        val message = NetworkErrorHelper.parseException(ex)
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseException_ioExceptionWithTimeout_returnsTimeoutMessage() {
        val ex = IOException("Read timed out")
        val message = NetworkErrorHelper.parseException(ex)
        assertEquals("Error de conexión.", message)
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
    fun parseHttpError_502_returnsConnectionErrorMessage() {
        val message = NetworkErrorHelper.parseHttpError(502, null)
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseHttpError_500_withCustomServerJson_returnsConnectionError() {
        val json = """{"error": "Internal Database Error", "detail": "traceback..."}"""
        val message = NetworkErrorHelper.parseHttpError(500, json)
        assertEquals("Error de conexión.", message)
    }

    @Test
    fun parseHttpError_503_returnsConnectionError() {
        val message = NetworkErrorHelper.parseHttpError(503, null)
        assertEquals("Error de conexión.", message)
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
