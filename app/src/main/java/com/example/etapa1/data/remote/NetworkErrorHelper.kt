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
        if (throwable == null) return "Error de conexión."

        return when (throwable) {
            is UnknownHostException -> "Error de conexión."
            is ConnectException -> "Error de conexión."
            is SocketTimeoutException -> "Error de conexión."
            is SSLException -> "Error de conexión."
            is IOException -> {
                val msg = throwable.message.orEmpty().lowercase()
                when {
                    msg.contains("canceled") -> "Petición cancelada."
                    else -> "Error de conexión."
                }
            }
            else -> "Error de conexión."
        }
    }

    /**
     * Convierte una respuesta de error HTTP en un mensaje amigable según el código y el cuerpo JSON.
     */
    fun parseHttpError(code: Int, errorBody: String?): String {
        val serverMessage = parseServerErrorMessage(errorBody)

        return when {
            code == 401 -> serverMessage ?: "Correo o contraseña incorrectos."
            code == 403 -> serverMessage ?: "Tu cuenta no tiene permisos para realizar esta acción o usar este rol."
            code == 404 -> serverMessage ?: "El recurso solicitado no fue encontrado."
            code == 422 -> serverMessage ?: "Los datos introducidos son inválidos o están incompletos."
            code in 500..599 -> "Error de conexión."
            else -> "Error de conexión."
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
