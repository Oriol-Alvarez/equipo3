package com.example.etapa1.data.repository.impl

import android.util.Log
import com.example.etapa1.data.remote.BitacoraMessageDto
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.ColaSinConexion
import com.example.etapa1.data.remote.CreateActivityDto
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.NetworkClient
import com.example.etapa1.data.remote.NetworkErrorHelper
import com.example.etapa1.data.remote.NewActivityResponseDto
import com.example.etapa1.data.remote.OfflineStore
import com.example.etapa1.data.remote.Pendiente
import com.example.etapa1.data.remote.QuickEventDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.ResultadoEnvio
import com.example.etapa1.data.remote.SendBitacoraMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.SubidaArchivos
import com.example.etapa1.data.remote.TimelineItemDto
import com.example.etapa1.data.remote.TipoPendiente
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
import kotlinx.serialization.encodeToString
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
 *
 * Sin conexión (RF-11):
 * - Actividades, eventos y mensajes de texto pasan por [ColaSinConexion]: se ven al momento,
 *   se guardan en el teléfono y se mandan cuando hay red, sin duplicarse.
 * - La última línea de tiempo de cada niño se guarda cifrada para poder verla sin internet.
 */
class NetworkBitacoraRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager?,
    private val chatConexion: ChatConexion,
    private val subidaArchivos: SubidaArchivos? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val cola: ColaSinConexion? = null,
    private val cache: OfflineStore? = null
) : BitacoraRepository {

    private val json = NetworkClient.json

    private val _childIdActual = MutableStateFlow<String?>(null)
    override val childIdActual: StateFlow<String?> = _childIdActual.asStateFlow()

    private val _timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    override val timeline: StateFlow<List<TimelineItem>> = _timeline.asStateFlow()

    init {
        observarWebSocket()
        observarCola()
    }

    private fun claveCache(childId: String) = "linea_${tokenManager?.userId.orEmpty()}_$childId"

    override fun abrir(childId: String) {
        if (_childIdActual.value != childId) {
            _childIdActual.value = childId
            // Lo último guardado en el teléfono + lo que sigue pendiente de enviar
            val guardado = cache?.load<List<TimelineItemDto>>(claveCache(childId))?.map { it.toDomain() }.orEmpty()
            _timeline.value = (guardado + pendientesComoElementos(childId)).ordenadaCronologicamente()
        }
        RemoteSync.launch("Cargar línea de tiempo de $childId") {
            val dtos = api.getChildTimeline(childId).bodyOrLog("GET /children/$childId/timeline")
                ?: return@launch
            cache?.save(claveCache(childId), dtos)
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

    /** Cuando la cola logra mandar algo, el elemento temporal se cambia por el del servidor. */
    private fun observarCola() {
        val c = cola ?: return
        scope.launch {
            c.resultados.collect { r ->
                val p = r.pendiente
                val tempId = "temp_${p.id}"
                when (r) {
                    is ResultadoEnvio.Rechazado -> quitar(p.childId, tempId)
                    is ResultadoEnvio.Enviado -> when (p.tipo) {
                        TipoPendiente.ACTIVIDAD -> {
                            val res = r.respuesta as? NewActivityResponseDto ?: return@collect
                            val enviado = json.decodeFromString<CreateActivityDto>(p.cuerpo)
                            confirmar(
                                p.childId, tempId,
                                TimelineItem.ActivityCard(
                                    id = res.id,
                                    time = enviado.time,
                                    date = res.timelineItem.date ?: enviado.date.orEmpty(),
                                    category = enviado.category,
                                    portionLabel = enviado.portionOption.orEmpty(),
                                    description = enviado.description
                                )
                            )
                            onActivityCreated?.invoke(p.childId)
                        }
                        TipoPendiente.EVENTO -> {
                            val res = r.respuesta as? TimelineItemDto ?: return@collect
                            confirmar(p.childId, tempId, res.toDomain())
                        }
                        TipoPendiente.MENSAJE_BITACORA -> {
                            val res = r.respuesta as? BitacoraMessageDto ?: return@collect
                            confirmar(p.childId, tempId, res.toChatMessage())
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    /** Pendientes de la cola de ese niño, como elementos temporales para mostrarlos en pantalla. */
    private fun pendientesComoElementos(childId: String): List<TimelineItem> {
        val c = cola ?: return emptyList()
        return c.pendientesDe(childId).mapNotNull { p ->
            try {
                when (p.tipo) {
                    TipoPendiente.ACTIVIDAD -> json.decodeFromString<CreateActivityDto>(p.cuerpo).let {
                        TimelineItem.ActivityCard(
                            id = "temp_${p.id}", time = it.time, date = it.date.orEmpty(),
                            category = it.category, portionLabel = it.portionOption.orEmpty(), description = it.description
                        )
                    }
                    TipoPendiente.EVENTO -> json.decodeFromString<QuickEventDto>(p.cuerpo).let {
                        TimelineItem.EventChip(
                            id = "temp_${p.id}", time = it.time, date = it.date.orEmpty(),
                            text = it.text, iconType = it.iconType
                        )
                    }
                    TipoPendiente.MENSAJE_BITACORA -> json.decodeFromString<SendBitacoraMessageDto>(p.cuerpo).let {
                        TimelineItem.ChatMessage(
                            id = "temp_${p.id}", time = it.time.orEmpty(), date = it.date.orEmpty(),
                            message = it.message, isOutgoing = true
                        )
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun nuevoPendiente(tipo: TipoPendiente, childId: String, cuerpo: String) =
        Pendiente(tipo = tipo, userId = tokenManager?.userId.orEmpty(), childId = childId, cuerpo = cuerpo)

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

        // Mensaje de solo texto: va por la cola (funciona sin internet y no se duplica)
        val c = cola
        if (fileUri == null && c != null) {
            val dto = SendBitacoraMessageDto(message = text, time = hora, date = fecha)
            val p = nuevoPendiente(TipoPendiente.MENSAJE_BITACORA, childId, json.encodeToString(dto))
            agregarSiEsActual(
                childId,
                TimelineItem.ChatMessage(
                    id = "temp_${p.id}", time = hora, date = fecha, message = text,
                    isOutgoing = isOutgoing, senderRole = senderRole
                )
            )
            c.agregar(p)
            return
        }

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
        val dto = QuickEventDto(time, text, iconType, fecha)
        val c = cola
        if (c != null) {
            val p = nuevoPendiente(TipoPendiente.EVENTO, childId, json.encodeToString(dto))
            agregarSiEsActual(
                childId,
                TimelineItem.EventChip(id = "temp_${p.id}", time = time, date = fecha, text = text, iconType = iconType)
            )
            c.agregar(p)
            return
        }
        val tempId = "temp_${System.nanoTime()}"
        agregarSiEsActual(childId, TimelineItem.EventChip(id = tempId, time = time, date = fecha, text = text, iconType = iconType))
        RemoteSync.launch("Registrar evento en la bitácora de $childId") {
            val creado = api.createTimelineEvent(childId, dto).bodyOrLog("POST /children/$childId/timeline/events")
            if (creado == null) quitar(childId, tempId) else confirmar(childId, tempId, creado.toDomain())
        }
    }

    /** Callback notificado cuando se crea una actividad en el backend para refrescar resúmenes. */
    var onActivityCreated: ((String) -> Unit)? = null

    override fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String?
    ) {
        addActivityCard(
            childId = childId,
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description,
            date = date,
            mealType = null,
            consumptionOption = null,
            napMinutes = null,
            moodState = null,
            excretoryFunction = null
        )
    }

    override fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String?,
        mealType: String?,
        consumptionOption: String?,
        napMinutes: Int?,
        moodState: String?,
        excretoryFunction: String?
    ) {
        val fecha = date ?: fechaActual()
        val dto = CreateActivityDto(
            time = time,
            date = fecha,
            category = category,
            title = "$category: $portionLabel".trim().removeSuffix(":"),
            description = description,
            portionOption = portionLabel,
            mealType = mealType,
            consumptionOption = consumptionOption,
            napMinutes = napMinutes,
            moodState = moodState,
            excretoryFunction = excretoryFunction
        )
        val c = cola
        val p = nuevoPendiente(TipoPendiente.ACTIVIDAD, childId, json.encodeToString(dto))
        val tempId = if (c != null) "temp_${p.id}" else "temp_${System.nanoTime()}"
        val pendiente = TimelineItem.ActivityCard(
            id = tempId,
            time = time,
            date = fecha,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        agregarSiEsActual(childId, pendiente)
        if (c != null) {
            c.agregar(p)
            return
        }

        RemoteSync.launch("Registrar actividad para $childId") {
            val res = api.createActivity(childId = childId, request = dto).bodyOrLog("POST /children/$childId/activities")
            if (res == null) {
                quitar(childId, tempId)
            } else {
                confirmar(childId, tempId, pendiente.copy(id = res.id, date = res.timelineItem.date ?: fecha))
                onActivityCreated?.invoke(childId)
            }
        }
    }

    // ---------------------------------------------------------------- RF-10: borrar una foto

    override fun eliminarFoto(
        childId: String,
        itemId: String,
        fileUri: String,
        motivo: String,
        alTerminar: (Result<Unit>) -> Unit
    ) {
        val fileId = fileUri.substringAfterLast("/media/", "").substringBefore("?")
        if (fileId.isBlank()) {
            alTerminar(Result.failure(IllegalArgumentException("Esta foto no está guardada en el servidor")))
            return
        }
        scope.launch {
            val resultado = try {
                val r = api.eliminarArchivo(fileId, motivo)
                if (r.isSuccessful) {
                    _timeline.update { actual ->
                        actual.map { item ->
                            if (item is TimelineItem.ChatMessage && item.fileUri?.contains("/media/$fileId") == true) {
                                item.copy(
                                    fileUri = null, fileName = null, fileMimeType = null,
                                    message = if (item.message.isBlank()) FOTO_ELIMINADA
                                    else "${item.message} (${FOTO_ELIMINADA.lowercase()})"
                                )
                            } else {
                                item
                            }
                        }
                    }
                    cache?.remove(claveCache(childId))
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(NetworkErrorHelper.parseResponse(r)))
                }
            } catch (e: Exception) {
                Log.w("Bitacora", "No se pudo borrar la foto $fileId: ${e.message}")
                Result.failure(Exception(NetworkErrorHelper.parseException(e)))
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) { alTerminar(resultado) }
        }
    }

    // ---------------------------------------------------------------- conversiones

    private fun BitacoraMessageDto.toChatMessage() = TimelineItem.ChatMessage(
        id = id,
        time = time,
        date = date ?: fechaActual(),
        message = message,
        isOutgoing = senderId == tokenManager?.userId || isOutgoing,
        fileUri = fileUri?.let(RemoteSync::absoluteUrl),
        fileName = fileName,
        fileMimeType = fileMimeType
    )

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

    private companion object {
        const val FOTO_ELIMINADA = "Foto eliminada a solicitud de la familia"
    }
}
