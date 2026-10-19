package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.CreateActivityDto
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.QuickEventDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SendBitacoraMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.SubidaArchivos
import com.example.etapa1.data.remote.TimelineItemDto
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
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
 * Bitácora individual y chat directo (educadora <-> familia) del niño abierto en pantalla,
 * con sincronización en tiempo real vía [ChatConexion].
 *
 * Reglas para no mezclar niños:
 * - [abrir] limpia la línea de tiempo al cambiar de niño y solo aplica la respuesta del
 *   servidor si ese niño sigue abierto.
 * - Todo lo que se envía lleva el id del niño; solo se agrega a la pantalla si es el abierto.
 * - Los eventos en tiempo real de otros niños se ignoran.
 */
class NetworkBitacoraRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager?,
    private val chatConexion: ChatConexion,
    private val subidaArchivos: SubidaArchivos? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : BitacoraRepository {

    private val _childIdActual = MutableStateFlow<String?>(null)
    override val childIdActual: StateFlow<String?> = _childIdActual.asStateFlow()

    private val _timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    override val timeline: StateFlow<List<TimelineItem>> = _timeline.asStateFlow()

    init {
        observarWebSocket()
    }

    override fun abrir(childId: String) {
        if (_childIdActual.value != childId) {
            _childIdActual.value = childId
            _timeline.value = emptyList()
        }
        RemoteSync.launch("Cargar línea de tiempo de $childId") {
            val dtos = api.getChildTimeline(childId).bodyOrLog("GET /children/$childId/timeline")
                ?: return@launch
            if (_childIdActual.value != childId) return@launch
            val delServidor = dtos.map { it.toDomain() }
            _timeline.update { actual ->
                // Conserva lo que se está enviando (id temporal) para no "perderlo" al recargar
                val pendientes = actual.filter { it.id.startsWith("temp_") }
                (delServidor + pendientes).ordenadaCronologicamente()
            }
        }
    }

    override fun limpiar() {
        _childIdActual.value = null
        _timeline.value = emptyList()
    }

    private fun esActual(childId: String) = _childIdActual.value == childId

    private fun agregarSiEsActual(childId: String, item: TimelineItem) {
        if (!esActual(childId)) return
        _timeline.update { actual ->
            if (actual.any { it.id == item.id }) actual else (actual + item).ordenadaCronologicamente()
        }
    }

    /** Reemplaza el elemento temporal por el definitivo del servidor (o lo agrega si no estaba). */
    private fun confirmar(childId: String, tempId: String, definitivo: TimelineItem) {
        if (!esActual(childId)) return
        _timeline.update { actual ->
            val sinDuplicados = actual.filter { it.id != definitivo.id }
            val idx = sinDuplicados.indexOfFirst { it.id == tempId }
            if (idx >= 0) {
                sinDuplicados.toMutableList().apply { set(idx, definitivo) }.ordenadaCronologicamente()
            } else {
                (sinDuplicados + definitivo).ordenadaCronologicamente()
            }
        }
    }

    private fun quitar(childId: String, tempId: String) {
        if (!esActual(childId)) return
        _timeline.update { actual -> actual.filter { it.id != tempId } }
    }

    private fun observarWebSocket() {
        scope.launch {
            chatConexion.eventos.collect { evento ->
                when (evento) {
                    is EventoServidor.NuevoMensajeBitacora -> {
                        val childId = evento.childId.ifEmpty { evento.chatId }
                        if (!esActual(childId)) return@collect
                        val payload = evento.message
                        val nuevo = TimelineItem.ChatMessage(
                            id = payload.id.ifEmpty { "msg_${System.currentTimeMillis()}" },
                            time = payload.time.ifEmpty { horaActual() },
                            date = payload.date ?: fechaActual(),
                            message = payload.message,
                            isOutgoing = payload.senderId == tokenManager?.userId,
                            fileUri = payload.fileUri?.let(RemoteSync::absoluteUrl),
                            fileName = payload.fileName,
                            fileMimeType = payload.fileMimeType
                        )
                        // Si es un mensaje propio que aún está como temporal, se reemplaza
                        _timeline.update { actual ->
                            if (actual.any { it.id == nuevo.id }) return@update actual
                            val pendiente = actual.indexOfFirst {
                                it is TimelineItem.ChatMessage && it.id.startsWith("temp_") &&
                                    it.message == nuevo.message && nuevo.isOutgoing
                            }
                            if (pendiente >= 0) {
                                actual.toMutableList().apply { set(pendiente, nuevo) }.ordenadaCronologicamente()
                            } else {
                                (actual + nuevo).ordenadaCronologicamente()
                            }
                        }
                    }
                    is EventoServidor.NuevaActividadBitacora -> {
                        val childId = evento.childId.ifEmpty { evento.chatId }
                        val card = evento.activity.toDomain()
                        if (!esActual(childId)) return@collect
                        _timeline.update { actual ->
                            if (actual.any { it.id == card.id }) return@update actual
                            val pendiente = actual.indexOfFirst {
                                it is TimelineItem.ActivityCard && it.id.startsWith("temp_") &&
                                    card is TimelineItem.ActivityCard &&
                                    it.category == card.category && it.description == card.description
                            }
                            if (pendiente >= 0) {
                                actual.toMutableList().apply { set(pendiente, card) }.ordenadaCronologicamente()
                            } else {
                                (actual + card).ordenadaCronologicamente()
                            }
                        }
                    }
                    is EventoServidor.NuevoEventoBitacora -> {
                        val childId = evento.childId.ifEmpty { evento.chatId }
                        val chip = evento.event.toDomain()
                        if (!esActual(childId)) return@collect
                        _timeline.update { actual ->
                            if (actual.any { it.id == chip.id }) return@update actual
                            val pendiente = actual.indexOfFirst {
                                it is TimelineItem.EventChip && it.id.startsWith("temp_") &&
                                    chip is TimelineItem.EventChip && it.text == chip.text
                            }
                            if (pendiente >= 0) {
                                actual.toMutableList().apply { set(pendiente, chip) }.ordenadaCronologicamente()
                            } else {
                                (actual + chip).ordenadaCronologicamente()
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun addChatMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?,
        senderRole: UserRole?,
        date: String?
    ) {
        val hora = horaActual()
        val fecha = date ?: fechaActual()
        val tempId = "temp_${System.nanoTime()}"
        val pendiente = TimelineItem.ChatMessage(
            id = tempId,
            time = hora,
            date = fecha,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType,
            senderRole = senderRole
        )
        agregarSiEsActual(childId, pendiente)

        RemoteSync.launch("Enviar mensaje de bitácora a $childId") {
            // Un archivo del teléfono se sube primero para que la otra persona lo pueda abrir
            val uriServidor = if (fileUri != null) {
                subidaArchivos?.subir(fileUri, fileName, fileMimeType, "bitacora")
                    ?: run {
                        quitar(childId, tempId)
                        return@launch
                    }
            } else {
                null
            }
            val enviado = api.sendBitacoraMessage(
                childId,
                SendBitacoraMessageDto(
                    message = text,
                    fileUri = uriServidor,
                    fileName = fileName,
                    fileMimeType = fileMimeType,
                    time = hora,
                    date = fecha
                )
            ).bodyOrLog("POST /chats/bitacora/$childId/messages")
            if (enviado == null) {
                quitar(childId, tempId)
                return@launch
            }
            confirmar(
                childId,
                tempId,
                pendiente.copy(
                    id = enviado.id,
                    date = enviado.date ?: fecha,
                    fileUri = uriServidor?.let(RemoteSync::absoluteUrl)
                )
            )
        }
    }

    override fun addEventChip(childId: String, time: String, text: String, iconType: String, date: String?) {
        val fecha = date ?: fechaActual()
        val tempId = "temp_${System.nanoTime()}"
        val pendiente = TimelineItem.EventChip(id = tempId, time = time, date = fecha, text = text, iconType = iconType)
        agregarSiEsActual(childId, pendiente)

        RemoteSync.launch("Registrar evento en la bitácora de $childId") {
            val creado = api.createTimelineEvent(childId, QuickEventDto(time, text, iconType, fecha))
                .bodyOrLog("POST /children/$childId/timeline/events")
            if (creado == null) {
                quitar(childId, tempId)
            } else {
                confirmar(childId, tempId, creado.toDomain())
            }
        }
    }

    override fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String?
    ) {
        val tempId = "temp_${System.nanoTime()}"
        val fecha = date ?: fechaActual()
        val pendiente = TimelineItem.ActivityCard(
            id = tempId,
            time = time,
            date = fecha,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        agregarSiEsActual(childId, pendiente)

        RemoteSync.launch("Registrar actividad para $childId") {
            val res = api.createActivity(
                childId = childId,
                request = CreateActivityDto(
                    time = time,
                    date = fecha,
                    category = category,
                    title = "$category: $portionLabel".trim().removeSuffix(":"),
                    description = description,
                    portionOption = portionLabel
                )
            ).bodyOrLog("POST /children/$childId/activities")
            if (res == null) {
                quitar(childId, tempId)
            } else {
                confirmar(childId, tempId, pendiente.copy(id = res.id, date = res.timelineItem.date ?: fecha))
            }
        }
    }

    private fun TimelineItemDto.toDomain(): TimelineItem {
        val fecha = date ?: fechaActual()
        return when (type) {
            "EVENT_CHIP" -> TimelineItem.EventChip(
                id = id,
                time = time,
                date = fecha,
                text = text ?: "",
                iconType = iconType ?: "entry"
            )
            "ACTIVITY_CARD" -> TimelineItem.ActivityCard(
                id = id,
                time = time,
                date = fecha,
                category = category ?: "Otros",
                portionLabel = portionLabel ?: "",
                description = description ?: ""
            )
            else -> TimelineItem.ChatMessage(
                id = id,
                time = time,
                date = fecha,
                message = message ?: "",
                isOutgoing = isOutgoing ?: false,
                fileUri = fileUri?.let(RemoteSync::absoluteUrl),
                fileName = fileName,
                fileMimeType = fileMimeType
            )
        }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    private fun fechaActual(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

    private fun List<TimelineItem>.ordenadaCronologicamente(): List<TimelineItem> =
        OrdenLineaDeTiempo.ordenar(this)
}
