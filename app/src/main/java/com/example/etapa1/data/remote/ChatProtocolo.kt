package com.example.etapa1.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Representa los mensajes o eventos que el servidor WebSocket envía a la aplicación.
 */
sealed interface EventoServidor {
    /** Bienvenida al conectarse: confirmación del identificador de usuario y sala. */
    @Serializable
    data class Bienvenida(
        val yo: String = "",
        val event: String? = null
    ) : EventoServidor

    /** Nuevo mensaje de chat interno (personal o general). */
    @Serializable
    data class NuevoMensajeChat(
        val chatId: String,
        val message: ChatMessagePayload
    ) : EventoServidor

    /** Nuevo mensaje en el canal de bitácora diario entre educadora y familia. */
    @Serializable
    data class NuevoMensajeBitacora(
        val childId: String,
        val chatId: String = "",
        val message: ChatMessagePayload
    ) : EventoServidor

    /** Nueva actividad registrada en la bitácora del alumno. */
    @Serializable
    data class NuevaActividadBitacora(
        val childId: String,
        val chatId: String = "",
        val activity: TimelineItemDto
    ) : EventoServidor

    /** Nuevo evento rápido (ingreso, egreso, etc.) en la línea de tiempo del alumno. */
    @Serializable
    data class NuevoEventoBitacora(
        val childId: String,
        val chatId: String = "",
        val event: TimelineItemDto
    ) : EventoServidor

    /** Nuevo mensaje en un grupo familiar institucional. */
    @Serializable
    data class NuevoMensajeGrupo(
        val groupId: String,
        val message: FamilyGroupMessageDto
    ) : EventoServidor

    /** Nuevo aviso o circular general publicado en el centro. */
    @Serializable
    data class NuevoAviso(
        val id: String = "",
        val title: String = ""
    ) : EventoServidor

    /** Evento no tipado o genérico recibido desde el servidor. */
    data class Desconocido(val raw: String) : EventoServidor
}

@Serializable
data class ChatMessagePayload(
    val id: String = "",
    val chatId: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val time: String = "",
    val date: String? = null,
    val isOutgoing: Boolean = false,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null
)

/**
 * Órdenes enviadas desde la app hacia el servidor WebSocket.
 */
@Serializable
sealed interface OrdenChat {
    @Serializable
    @SerialName("mensaje")
    data class Enviar(
        val texto: String,
        val chatId: String? = null
    ) : OrdenChat

    @Serializable
    @SerialName("ping")
    data object Ping : OrdenChat
}

object ChatProtocolo {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * Interpreta una cadena JSON recibida por el socket y la mapea a un [EventoServidor].
     * Si no se reconoce o ocurre un error de parseo, retorna null o [EventoServidor.Desconocido]
     * sin interrumpir la conexión.
     */
    fun leer(texto: String): EventoServidor? {
        val trimmed = texto.trim()
        if (trimmed == "pong" || trimmed.isEmpty()) return null

        return runCatching {
            val root = json.parseToJsonElement(trimmed)
            if (root !is JsonObject) return EventoServidor.Desconocido(trimmed)

            val tipo = root["tipo"]?.jsonPrimitive?.content ?: root["event"]?.jsonPrimitive?.content
            val dataObj = root["data"]

            when (tipo) {
                "bienvenida", "connection:established" -> {
                    val yo = root["yo"]?.jsonPrimitive?.content
                        ?: (dataObj as? JsonObject)?.get("userId")?.jsonPrimitive?.content
                        ?: ""
                    EventoServidor.Bienvenida(yo = yo, event = tipo)
                }

                "chat:message:new" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeChat>(dataObj)
                    } else {
                        // Puede venir directamente sin encapsular en data
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeChat>(root)
                    }
                }

                "chat:bitacora:new" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeBitacora>(dataObj)
                    } else {
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeBitacora>(root)
                    }
                }

                "chat:bitacora:activity" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevaActividadBitacora>(dataObj)
                    } else {
                        json.decodeFromJsonElement<EventoServidor.NuevaActividadBitacora>(root)
                    }
                }

                "chat:bitacora:event" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevoEventoBitacora>(dataObj)
                    } else {
                        json.decodeFromJsonElement<EventoServidor.NuevoEventoBitacora>(root)
                    }
                }

                "group:message:new" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeGrupo>(dataObj)
                    } else {
                        json.decodeFromJsonElement<EventoServidor.NuevoMensajeGrupo>(root)
                    }
                }

                "announcement:new", "announcement" -> {
                    if (dataObj is JsonObject) {
                        json.decodeFromJsonElement<EventoServidor.NuevoAviso>(dataObj)
                    } else {
                        json.decodeFromJsonElement<EventoServidor.NuevoAviso>(root)
                    }
                }

                // Compatibilidad con formato de la práctica 12
                "mensaje" -> {
                    val de = root["de"]?.jsonPrimitive?.content ?: ""
                    val textoMsg = root["texto"]?.jsonPrimitive?.content ?: ""
                    val idMsg = root["id"]?.jsonPrimitive?.content ?: ""
                    val chatId = root["chatId"]?.jsonPrimitive?.content ?: "global_staff_chat"
                    EventoServidor.NuevoMensajeChat(
                        chatId = chatId,
                        message = ChatMessagePayload(
                            id = idMsg,
                            chatId = chatId,
                            senderId = de,
                            senderName = de,
                            message = textoMsg,
                            time = "Ahora",
                            isOutgoing = false
                        )
                    )
                }

                else -> EventoServidor.Desconocido(trimmed)
            }
        }.getOrElse {
            EventoServidor.Desconocido(trimmed)
        }
    }

    fun escribir(orden: OrdenChat): String {
        return when (orden) {
            is OrdenChat.Enviar -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("tipo", kotlinx.serialization.json.JsonPrimitive("mensaje"))
                    put("texto", kotlinx.serialization.json.JsonPrimitive(orden.texto))
                    orden.chatId?.let { put("chatId", kotlinx.serialization.json.JsonPrimitive(it)) }
                }.toString()
            }
            is OrdenChat.Ping -> "ping"
        }
    }
}
