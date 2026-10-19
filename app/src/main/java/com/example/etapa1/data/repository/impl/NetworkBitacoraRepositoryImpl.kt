package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.CreateActivityDto
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

    override fun recargar(childId: String) {
        cargarLineaDeTiempo(childId)
    }

    private fun cargarLineaDeTiempo(childId: String = defaultChildId) {
        RemoteSync.launch("Cargar línea de tiempo y mensajes de bitácora") {
            api.getChildTimeline(childId).bodyOrLog("GET /children/$childId/timeline")?.let { dtos ->
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
                    _mateoTimeline.value = convertidos.ordenadaCronologicamente()
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
                                if (actual.any { it.id == nuevoMsg.id }) return@update actual
                                val pendingIdx = actual.indexOfFirst {
                                    it is TimelineItem.ChatMessage &&
                                    it.id.startsWith("temp_") &&
                                    it.message == nuevoMsg.message
                                }
                                if (pendingIdx >= 0) {
                                    actual.toMutableList().apply { set(pendingIdx, nuevoMsg) }.ordenadaCronologicamente()
                                } else {
                                    (actual + nuevoMsg).ordenadaCronologicamente()
                                }
                            }
                        }
                    }
                    is EventoServidor.NuevaActividadBitacora -> {
                        if (evento.childId == defaultChildId || evento.chatId == defaultChildId) {
                            val act = evento.activity
                            val card = TimelineItem.ActivityCard(
                                id = act.id,
                                time = act.time,
                                category = act.category ?: "Actividad",
                                portionLabel = act.portionLabel ?: "",
                                description = act.description ?: ""
                            )
                            _mateoTimeline.update { actual ->
                                if (actual.any { it.id == card.id }) return@update actual
                                val pendingIdx = actual.indexOfFirst {
                                    it is TimelineItem.ActivityCard &&
                                    it.id.startsWith("temp_") &&
                                    it.category == card.category &&
                                    it.description == card.description
                                }
                                if (pendingIdx >= 0) {
                                    actual.toMutableList().apply { set(pendingIdx, card) }.ordenadaCronologicamente()
                                } else {
                                    (actual + card).ordenadaCronologicamente()
                                }
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
        fileMimeType: String?,
        senderRole: com.example.etapa1.model.UserRole?
    ) {
        val hora = horaActual()
        val tempId = "temp_${System.currentTimeMillis()}"
        val nuevoMsg = TimelineItem.ChatMessage(
            id = tempId,
            time = hora,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType,
            senderRole = senderRole
        )

        // Actualización reactiva instantánea en la interfaz
        _mateoTimeline.update { (it + nuevoMsg).ordenadaCronologicamente() }

        // Envío asíncrono al servidor FastAPI
        RemoteSync.launch("Enviar mensaje bitácora a $defaultChildId") {
            val response = api.sendBitacoraMessage(
                defaultChildId,
                SendBitacoraMessageDto(
                    message = text,
                    fileUri = fileUri,
                    fileName = fileName,
                    fileMimeType = fileMimeType,
                    time = hora
                )
            )
            response.body()?.let { serverDto ->
                _mateoTimeline.update { actual ->
                    val idx = actual.indexOfFirst { it.id == tempId }
                    if (idx >= 0) {
                        val finalMsg = nuevoMsg.copy(id = serverDto.id)
                        actual.toMutableList().apply { set(idx, finalMsg) }.ordenadaCronologicamente()
                    } else {
                        actual
                    }
                }
            }
        }
    }

    override fun addEventChip(time: String, text: String, iconType: String) {
        val chip = TimelineItem.EventChip(
            id = "chip_${System.currentTimeMillis()}",
            time = time,
            text = text,
            iconType = iconType
        )
        _mateoTimeline.update { (it + chip).ordenadaCronologicamente() }
    }

    override fun addActivityCard(
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        childId: String
    ) {
        val targetChildId = childId.ifEmpty { defaultChildId }
        val tempId = "temp_${System.currentTimeMillis()}"
        val card = TimelineItem.ActivityCard(
            id = tempId,
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        _mateoTimeline.update { (it + card).ordenadaCronologicamente() }

        RemoteSync.launch("Registrar actividad para $targetChildId") {
            val response = api.createActivity(
                childId = targetChildId,
                request = CreateActivityDto(
                    time = time,
                    category = category,
                    title = "$category: $portionLabel".trim(),
                    description = description,
                    portionOption = portionLabel
                )
            )
            response.body()?.let { res ->
                _mateoTimeline.update { actual ->
                    val idx = actual.indexOfFirst { it.id == tempId }
                    if (idx >= 0) {
                        val finalCard = card.copy(id = res.id)
                        actual.toMutableList().apply { set(idx, finalCard) }.ordenadaCronologicamente()
                    } else {
                        actual
                    }
                }
            }
        }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    private fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim().uppercase(Locale.US)
            .replace(".", "")
            .replace("A M", "AM")
            .replace("P M", "PM")
        val formats = listOf(
            SimpleDateFormat("hh:mm a", Locale.US),
            SimpleDateFormat("h:mm a", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US),
            SimpleDateFormat("H:mm", Locale.US)
        )
        for (sdf in formats) {
            try {
                val date = sdf.parse(clean)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                }
            } catch (_: Exception) {
            }
        }
        return 9999
    }

    private fun List<TimelineItem>.ordenadaCronologicamente(): List<TimelineItem> =
        this.sortedBy { parseTimeToMinutes(it.time) }
}
