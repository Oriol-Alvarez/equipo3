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

    private val timelines = mutableMapOf<String, MutableStateFlow<List<TimelineItem>>>(
        "mateo_garcia" to MutableStateFlow(MockDataRepository.getInitialMateoTimeline()),
        "lucia_garcia" to MutableStateFlow(MockDataRepository.getInitialLuciaTimeline())
    )

    override val mateoTimeline: StateFlow<List<TimelineItem>>
        get() = getTimelineForChild("mateo_garcia")

    override fun getTimelineForChild(childId: String): StateFlow<List<TimelineItem>> {
        val flow = synchronized(timelines) {
            timelines.getOrPut(childId) {
                MutableStateFlow(
                    if (childId == "lucia_garcia") MockDataRepository.getInitialLuciaTimeline()
                    else emptyList()
                )
            }
        }
        return flow.asStateFlow()
    }

    init {
        cargarLineaDeTiempo("mateo_garcia")
        cargarLineaDeTiempo("lucia_garcia")
        observarWebSocket()
    }

    override fun recargar(childId: String) {
        cargarLineaDeTiempo(childId)
    }

    private fun cargarLineaDeTiempo(childId: String = defaultChildId) {
        RemoteSync.launch("Cargar línea de tiempo y mensajes de bitácora para $childId") {
            api.getChildTimeline(childId).bodyOrLog("GET /children/$childId/timeline")?.let { dtos ->
                if (dtos.isNotEmpty()) {
                    val convertidos = dtos.map { dto ->
                        val itemDate = dto.date ?: "12/10/2026"
                        when (dto.type) {
                            "EVENT_CHIP" -> TimelineItem.EventChip(
                                id = dto.id,
                                time = dto.time,
                                date = itemDate,
                                text = dto.text ?: "",
                                iconType = dto.iconType ?: "entry"
                            )
                            "ACTIVITY_CARD" -> TimelineItem.ActivityCard(
                                id = dto.id,
                                time = dto.time,
                                date = itemDate,
                                category = dto.category ?: "Otros",
                                portionLabel = dto.portionLabel ?: "",
                                description = dto.description ?: ""
                            )
                            else -> TimelineItem.ChatMessage(
                                id = dto.id,
                                time = dto.time,
                                date = itemDate,
                                message = dto.message ?: "",
                                isOutgoing = dto.isOutgoing ?: false,
                                fileUri = dto.fileUri,
                                fileName = dto.fileName,
                                fileMimeType = dto.fileMimeType
                            )
                        }
                    }
                    val flow = synchronized(timelines) {
                        timelines.getOrPut(childId) { MutableStateFlow(emptyList()) }
                    }
                    flow.value = convertidos.ordenadaCronologicamente()
                }
            }
        }
    }

    private fun observarWebSocket() {
        scope.launch {
            chatConexion.eventos.collect { evento ->
                when (evento) {
                    is EventoServidor.NuevoMensajeBitacora -> {
                        val targetChildId = when {
                            !evento.childId.isNullOrEmpty() -> evento.childId
                            !evento.chatId.isNullOrEmpty() -> evento.chatId
                            else -> defaultChildId
                        }
                        val payload = evento.message
                        val nuevoMsg = TimelineItem.ChatMessage(
                            id = payload.id.ifEmpty { "msg_${System.currentTimeMillis()}" },
                            time = payload.time.ifEmpty { horaActual() },
                            date = payload.date ?: fechaActual(),
                            message = payload.message,
                            isOutgoing = payload.senderId == tokenManager.userId,
                            fileUri = payload.fileUri,
                            fileName = payload.fileName,
                            fileMimeType = payload.fileMimeType
                        )
                        val flow = synchronized(timelines) {
                            timelines.getOrPut(targetChildId) { MutableStateFlow(emptyList()) }
                        }
                        flow.update { actual ->
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
                    is EventoServidor.NuevaActividadBitacora -> {
                        val targetChildId = when {
                            !evento.childId.isNullOrEmpty() -> evento.childId
                            !evento.chatId.isNullOrEmpty() -> evento.chatId
                            else -> defaultChildId
                        }
                        val act = evento.activity
                        val card = TimelineItem.ActivityCard(
                            id = act.id,
                            time = act.time,
                            date = act.date ?: fechaActual(),
                            category = act.category ?: "Actividad",
                            portionLabel = act.portionLabel ?: "",
                            description = act.description ?: ""
                        )
                        val flow = synchronized(timelines) {
                            timelines.getOrPut(targetChildId) { MutableStateFlow(emptyList()) }
                        }
                        flow.update { actual ->
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
        senderRole: com.example.etapa1.model.UserRole?,
        date: String?
    ) {
        addChatMessageForChild(defaultChildId, text, isOutgoing, fileUri, fileName, fileMimeType, senderRole, date)
    }

    override fun addChatMessageForChild(
        childId: String,
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?,
        senderRole: com.example.etapa1.model.UserRole?,
        date: String?
    ) {
        val targetChildId = childId.ifEmpty { defaultChildId }
        val hora = horaActual()
        val fecha = date ?: fechaActual()
        val tempId = "temp_${System.currentTimeMillis()}"
        val nuevoMsg = TimelineItem.ChatMessage(
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

        // Actualización reactiva instantánea en la interfaz
        val flow = synchronized(timelines) {
            timelines.getOrPut(targetChildId) { MutableStateFlow(emptyList()) }
        }
        flow.update { (it + nuevoMsg).ordenadaCronologicamente() }

        // Envío asíncrono al servidor FastAPI
        RemoteSync.launch("Enviar mensaje bitácora a $targetChildId") {
            val response = api.sendBitacoraMessage(
                targetChildId,
                SendBitacoraMessageDto(
                    message = text,
                    fileUri = fileUri,
                    fileName = fileName,
                    fileMimeType = fileMimeType,
                    time = hora,
                    date = fecha
                )
            )
            response.body()?.let { serverDto ->
                flow.update { actual ->
                    val idx = actual.indexOfFirst { it.id == tempId }
                    if (idx >= 0) {
                        val finalMsg = nuevoMsg.copy(id = serverDto.id, date = serverDto.date ?: fecha)
                        actual.toMutableList().apply { set(idx, finalMsg) }.ordenadaCronologicamente()
                    } else {
                        actual
                    }
                }
            }
        }
    }

    override fun addEventChip(time: String, text: String, iconType: String, date: String?) {
        addEventChipForChild(defaultChildId, time, text, iconType, date)
    }

    override fun addEventChipForChild(childId: String, time: String, text: String, iconType: String, date: String?) {
        val targetChildId = childId.ifEmpty { defaultChildId }
        val fecha = date ?: fechaActual()
        val chip = TimelineItem.EventChip(
            id = "chip_${System.currentTimeMillis()}",
            time = time,
            date = fecha,
            text = text,
            iconType = iconType
        )
        val flow = synchronized(timelines) {
            timelines.getOrPut(targetChildId) { MutableStateFlow(emptyList()) }
        }
        flow.update { (it + chip).ordenadaCronologicamente() }
    }

    override fun addActivityCard(
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        childId: String,
        date: String?
    ) {
        val targetChildId = childId.ifEmpty { defaultChildId }
        val tempId = "temp_${System.currentTimeMillis()}"
        val fecha = date ?: fechaActual()
        val card = TimelineItem.ActivityCard(
            id = tempId,
            time = time,
            date = fecha,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        val flow = synchronized(timelines) {
            timelines.getOrPut(targetChildId) { MutableStateFlow(emptyList()) }
        }
        flow.update { (it + card).ordenadaCronologicamente() }

        RemoteSync.launch("Registrar actividad para $targetChildId") {
            val response = api.createActivity(
                childId = targetChildId,
                request = CreateActivityDto(
                    time = time,
                    date = fecha,
                    category = category,
                    title = "$category: $portionLabel".trim(),
                    description = description,
                    portionOption = portionLabel
                )
            )
            response.body()?.let { res ->
                flow.update { actual ->
                    val idx = actual.indexOfFirst { it.id == tempId }
                    if (idx >= 0) {
                        val finalCard = card.copy(id = res.id, date = res.timelineItem.date ?: fecha)
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

    private fun fechaActual(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

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

    private fun parseItemTimestamp(dateStr: String, timeStr: String): Long {
        val cleanTime = timeStr.trim().uppercase(Locale.US)
            .replace(".", "")
            .replace("A M", "AM")
            .replace("P M", "PM")
        val cleanDate = dateStr.trim().ifEmpty { "12/10/2026" }

        val dateFormats = listOf(
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy"
        )
        val timeFormats = listOf(
            "hh:mm a",
            "h:mm a",
            "HH:mm",
            "H:mm"
        )
        for (df in dateFormats) {
            for (tf in timeFormats) {
                try {
                    val sdf = SimpleDateFormat("$df $tf", Locale.US)
                    val parsed = sdf.parse("$cleanDate $cleanTime")
                    if (parsed != null) {
                        return parsed.time
                    }
                } catch (_: Exception) {
                }
            }
            try {
                val sdfDate = SimpleDateFormat(df, Locale.US)
                val parsedDate = sdfDate.parse(cleanDate)
                if (parsedDate != null) {
                    val minutes = parseTimeToMinutes(cleanTime)
                    return parsedDate.time + (minutes * 60L * 1000L)
                }
            } catch (_: Exception) {
            }
        }

        return parseTimeToMinutes(cleanTime).toLong() * 60L * 1000L
    }

    private fun List<TimelineItem>.ordenadaCronologicamente(): List<TimelineItem> =
        this.sortedBy { parseItemTimestamp(it.date, it.time) }
}
