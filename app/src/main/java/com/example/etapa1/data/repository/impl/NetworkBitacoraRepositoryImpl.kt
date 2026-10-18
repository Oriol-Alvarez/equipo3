package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SendBitacoraMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio de la bitácora individual y chat directo (Educadora <-> Familia)
 * con sincronización en tiempo real vía [ChatConexion].
 */
class NetworkBitacoraRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager,
    private val chatConexion: ChatConexion,
    private val defaultChildId: String = "mateo_garcia",
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : BitacoraRepository {

    private val _mateoTimeline = MutableStateFlow<List<TimelineItem>>(MockDataRepository.getInitialMateoTimeline())
    override val mateoTimeline: StateFlow<List<TimelineItem>> = _mateoTimeline.asStateFlow()

    init {
        cargarLineaDeTiempo()
        observarWebSocket()
    }

    private fun cargarLineaDeTiempo() {
        RemoteSync.launch("Cargar línea de tiempo y mensajes de bitácora") {
            api.getChildTimeline(defaultChildId).bodyOrLog("GET /children/$defaultChildId/timeline")?.let { dtos ->
                if (dtos.isNotEmpty()) {
                    val convertidos = dtos.map { dto ->
                        when (dto.type) {
                            "EVENT_CHIP" -> TimelineItem.EventChip(
                                id = dto.id,
                                time = dto.time,
                                text = dto.text ?: "",
                                iconType = dto.iconType ?: "entry"
                            )
                            "ACTIVITY_CARD" -> TimelineItem.ActivityCard(
                                id = dto.id,
                                time = dto.time,
                                category = dto.category ?: "Otros",
                                portionLabel = dto.portionLabel ?: "",
                                description = dto.description ?: ""
                            )
                            else -> TimelineItem.ChatMessage(
                                id = dto.id,
                                time = dto.time,
                                message = dto.message ?: "",
                                isOutgoing = dto.isOutgoing ?: false,
                                fileUri = dto.fileUri,
                                fileName = dto.fileName,
                                fileMimeType = dto.fileMimeType
                            )
                        }
                    }
                    _mateoTimeline.value = convertidos
                }
            }
        }
    }

    private fun observarWebSocket() {
        scope.launch {
            chatConexion.eventos.collect { evento ->
                when (evento) {
                    is EventoServidor.NuevoMensajeBitacora -> {
                        if (evento.childId == defaultChildId || evento.chatId == defaultChildId) {
                            val payload = evento.message
                            val nuevoMsg = TimelineItem.ChatMessage(
                                id = payload.id.ifEmpty { "msg_${System.currentTimeMillis()}" },
                                time = payload.time.ifEmpty { horaActual() },
                                message = payload.message,
                                isOutgoing = payload.senderId == tokenManager.userId,
                                fileUri = payload.fileUri,
                                fileName = payload.fileName,
                                fileMimeType = payload.fileMimeType
                            )
                            _mateoTimeline.update { actual ->
                                if (actual.any { it.id == nuevoMsg.id }) actual
                                else actual + nuevoMsg
                            }
                        }
                    }
                    is EventoServidor.NuevoMensajeChat -> {
                        if (evento.chatId == defaultChildId) {
                            val payload = evento.message
                            val nuevoMsg = TimelineItem.ChatMessage(
                                id = payload.id.ifEmpty { "msg_${System.currentTimeMillis()}" },
                                time = payload.time.ifEmpty { horaActual() },
                                message = payload.message,
                                isOutgoing = payload.senderId == tokenManager.userId,
                                fileUri = payload.fileUri,
                                fileName = payload.fileName,
                                fileMimeType = payload.fileMimeType
                            )
                            _mateoTimeline.update { actual ->
                                if (actual.any { it.id == nuevoMsg.id }) actual
                                else actual + nuevoMsg
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun addChatMessage(
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) {
        val hora = horaActual()
        val tempId = "msg_${System.currentTimeMillis()}"
        val nuevoMsg = TimelineItem.ChatMessage(
            id = tempId,
            time = hora,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType
        )

        // Actualización reactiva instantánea en la interfaz
        _mateoTimeline.update { it + nuevoMsg }

        // Envío asíncrono al servidor FastAPI
        RemoteSync.launch("Enviar mensaje bitácora a $defaultChildId") {
            api.sendBitacoraMessage(
                defaultChildId,
                SendBitacoraMessageDto(
                    message = text,
                    fileUri = fileUri,
                    fileName = fileName,
                    fileMimeType = fileMimeType
                )
            )
        }
    }

    override fun addEventChip(time: String, text: String, iconType: String) {
        val chip = TimelineItem.EventChip(
            id = "chip_${System.currentTimeMillis()}",
            time = time,
            text = text,
            iconType = iconType
        )
        _mateoTimeline.update { it + chip }
    }

    override fun addActivityCard(time: String, category: String, portionLabel: String, description: String) {
        val card = TimelineItem.ActivityCard(
            id = "act_${System.currentTimeMillis()}",
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        _mateoTimeline.update { it + card }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
}
