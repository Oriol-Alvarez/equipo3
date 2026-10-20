package com.example.etapa1.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Utilidad centralizada para mapear excepciones y respuestas HTTP de error
 * en mensajes amigables, precisos y en español para la interfaz de usuario.
 */
object NetworkErrorHelper {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Convierte una excepción (problema de red, timeout, host desconocido, etc.)
     * en un mensaje de error claro y comprensible.
     */
    fun parseException(throwable: Throwable?): String {
        if (throwable == null) return "Ha ocurrido un error inesperado."

        return when (throwable) {
            is UnknownHostException ->
                "No se pudo conectar con el servidor. Verifica tu conexión a internet o si la dirección del servidor ha cambiado."
            is ConnectException ->
                "No se pudo establecer conexión con el servidor. Asegúrate de que el servidor esté encendido y accesible."
            is SocketTimeoutException ->
                "Tiempo de espera agotado al conectar con el servidor. Comprueba tu conexión a internet."
            is SSLException ->
                "Error de seguridad en la conexión segura (SSL/HTTPS)."
            is IOException -> {
                val msg = throwable.message.orEmpty().lowercase()
                when {
                    msg.contains("timeout") || msg.contains("time out") || msg.contains("timed out") ->
                        "Tiempo de espera agotado al conectar con el servidor."
                    msg.contains("failed to connect") || msg.contains("connection refused") || msg.contains("unreachable") ->
                        "No se pudo conectar con el servidor. Verifica tu conexión o el estado del servidor."
                    msg.contains("canceled") ->
                        "Petición cancelada."
                    else ->
                        "Error de conexión: No se pudo comunicar con el servidor."
                }
            }
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: "Error inesperado."
        }
    }

    /**
     * Convierte una respuesta de error HTTP en un mensaje amigable según el código y el cuerpo JSON.
     */
    fun parseHttpError(code: Int, errorBody: String?): String {
        val serverMessage = parseServerErrorMessage(errorBody)

        return when (code) {
            401 -> serverMessage ?: "Correo o contraseña incorrectos."
            403 -> serverMessage ?: "Tu cuenta no tiene permisos para realizar esta acción o usar este rol."
            404 -> serverMessage ?: "El recurso solicitado no fue encontrado en el servidor."
            422 -> serverMessage ?: "Los datos introducidos son inválidos o están incompletos."
            500 -> serverMessage ?: "Error interno del servidor. Inténtalo de nuevo más tarde."
            502, 503, 504 -> "El servidor no está disponible en este momento (Código $code). Verifica si el contenedor o el túnel están encendidos."
            else -> serverMessage ?: "Error del servidor (Código $code)."
        }
    }

    /**
     * Parsea una respuesta Retrofit que no fue exitosa.
     */
    fun <T> parseResponse(response: Response<T>): String {
        val errorBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        return parseHttpError(response.code(), errorBody)
    }

    private fun parseServerErrorMessage(jsonStr: String?): String? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val element = json.parseToJsonElement(jsonStr)
            val obj = element.jsonObject
            obj["error"]?.jsonPrimitive?.content
                ?: obj["detail"]?.jsonPrimitive?.content
                ?: obj["mensaje"]?.jsonPrimitive?.content
                ?: obj["message"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }
}
