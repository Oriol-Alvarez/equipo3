package com.example.etapa1.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.model.FamilyGroupMessage
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import com.example.etapa1.model.WorkerChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
enum class PendingSyncType {
    BITACORA_ACTIVITY,
    BITACORA_EVENT,
    BITACORA_MESSAGE,
    WORKER_MESSAGE,
    FAMILY_GROUP_MESSAGE,
    LOST_OBJECT,
    SUGGESTION
}

@Serializable
data class PendingSyncItem(
    val id: String,
    val type: PendingSyncType,
    val createdAt: Long = System.currentTimeMillis(),

    // Bitácora común
    val childId: String? = null,
    val time: String? = null,
    val date: String? = null,

    // Bitácora actividad
    val activityCategory: String? = null,
    val activityPortion: String? = null,
    val activityDescription: String? = null,

    // Bitácora evento
    val eventText: String? = null,
    val eventIconType: String? = null,

    // Mensajes (Bitácora, Trabajador, Grupos)
    val chatId: String? = null,
    val messageText: String? = null,
    val isOutgoing: Boolean = true,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val senderRole: String? = null,

    // Mensajes grupo familiar / trabajador
    val groupId: String? = null,
    val senderId: String? = null,
    val senderDisplayName: String? = null,
    val isGlobalChat: Boolean = false,

    // Objeto perdido
    val lostTitle: String? = null,
    val lostDescription: String? = null,
    val lostCategory: String? = null,
    val lostLocation: String? = null,
    val lostRoom: String? = null,
    val lostImageUri: String? = null,

    // Sugerencia
    val suggestionRoomName: String? = null,
    val suggestionCategory: String? = null,
    val suggestionSubject: String? = null,
    val suggestionContent: String? = null,
    val parentName: String? = null,
    val childName: String? = null
)

interface OfflineSyncListener {
    fun onBitacoraItemSynced(childId: String, tempId: String, item: TimelineItem) {}
    fun onBitacoraMessageSynced(childId: String, tempId: String, item: TimelineItem.ChatMessage) {}
    fun onWorkerMessageSynced(chatId: String, tempId: String, newServerId: String) {}
    fun onFamilyGroupMessageSynced(groupId: String, tempId: String, item: FamilyGroupMessage) {}
    fun onLostObjectSynced(tempId: String, item: LostItem) {}
    fun onSuggestionSynced(tempId: String, item: ParentSuggestion) {}
}

/**
 * Gestor central de sincronización offline.
 * Mantiene en cola local persistente (vía [OfflineStore]) las acciones realizadas sin internet
 * (mensajes, actividades, objetos perdidos y sugerencias) y las envía automáticamente al servidor
 * cuando se restablece la conexión.
 */
class OfflineSyncManager(
    private val context: Context,
    private val api: SonrisasApi,
    private val offlineStore: OfflineStore,
    private val subidaArchivos: SubidaArchivos?,
    private val chatConexion: ChatConexion,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "OfflineSyncManager"
        private const val PREFS_KEY_PENDING = "pending_sync_items_v1"
    }

    private val listeners = CopyOnWriteArrayList<OfflineSyncListener>()
    private val syncMutex = Mutex()

    private val _pendingItems = MutableStateFlow<List<PendingSyncItem>>(emptyList())
    val pendingItems: StateFlow<List<PendingSyncItem>> = _pendingItems.asStateFlow()

    init {
        loadPendingFromDisk()
        startNetworkMonitoring()
    }

    fun addListener(listener: OfflineSyncListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OfflineSyncListener) {
        listeners.remove(listener)
    }

    private fun loadPendingFromDisk() {
        try {
            val list = offlineStore.load<List<PendingSyncItem>>(PREFS_KEY_PENDING) ?: emptyList()
            _pendingItems.value = list
            Log.d(TAG, "Cargados ${list.size} elementos pendientes de sincronización desde disco.")
        } catch (e: Exception) {
            Log.w(TAG, "Error cargando pendientes desde disco: ${e.message}")
        }
    }

    private fun savePendingToDisk(items: List<PendingSyncItem>) {
        try {
            offlineStore.save(PREFS_KEY_PENDING, items)
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando pendientes a disco: ${e.message}")
        }
    }

    private fun enqueue(item: PendingSyncItem) {
        _pendingItems.update { current ->
            val updated = current.filterNot { it.id == item.id } + item
            savePendingToDisk(updated)
            updated
        }
        triggerSync()
    }

    fun removePending(id: String) {
        _pendingItems.update { current ->
            val updated = current.filterNot { it.id == id }
            savePendingToDisk(updated)
            updated
        }
    }

    // =========================================================================
    // Métodos para encolar cada tipo de elemento offline
    // =========================================================================

    fun queueBitacoraActivity(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String?,
        tempId: String
    ) {
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.BITACORA_ACTIVITY,
            childId = childId,
            time = time,
            date = date,
            activityCategory = category,
            activityPortion = portionLabel,
            activityDescription = description
        )
        enqueue(item)
    }

    fun queueBitacoraEvent(
        childId: String,
        time: String,
        text: String,
        iconType: String,
        date: String?,
        tempId: String
    ) {
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.BITACORA_EVENT,
            childId = childId,
            time = time,
            date = date,
            eventText = text,
            eventIconType = iconType
        )
        enqueue(item)
    }

    fun queueBitacoraMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?,
        senderRole: UserRole?,
        time: String,
        date: String?,
        tempId: String
    ) {
        val cachedFileUri = cacheFileLocallyIfNeeded(fileUri, "bitacora")
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.BITACORA_MESSAGE,
            childId = childId,
            time = time,
            date = date,
            messageText = text,
            isOutgoing = isOutgoing,
            fileUri = cachedFileUri,
            fileName = fileName,
            fileMimeType = fileMimeType,
            senderRole = senderRole?.name
        )
        enqueue(item)
    }

    fun queueWorkerMessage(
        chatId: String,
        text: String,
        time: String,
        tempId: String,
        isGlobal: Boolean,
        childId: String? = null
    ) {
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.WORKER_MESSAGE,
            chatId = chatId,
            childId = childId,
            messageText = text,
            time = time,
            isGlobalChat = isGlobal
        )
        enqueue(item)
    }

    fun queueFamilyGroupMessage(
        groupId: String,
        senderId: String,
        senderDisplayName: String,
        text: String,
        time: String,
        tempId: String,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null
    ) {
        val cachedAttachment = cacheFileLocallyIfNeeded(attachmentUri, "group")
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.FAMILY_GROUP_MESSAGE,
            groupId = groupId,
            senderId = senderId,
            senderDisplayName = senderDisplayName,
            messageText = text,
            time = time,
            fileUri = cachedAttachment,
            fileName = attachmentName,
            fileMimeType = attachmentMime
        )
        enqueue(item)
    }

    fun queueLostObject(
        title: String,
        description: String,
        category: String,
        location: String?,
        room: String?,
        imageUri: String?,
        tempId: String
    ) {
        val cachedImage = cacheFileLocallyIfNeeded(imageUri, "lost")
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.LOST_OBJECT,
            lostTitle = title,
            lostDescription = description,
            lostCategory = category,
            lostLocation = location,
            lostRoom = room,
            lostImageUri = cachedImage
        )
        enqueue(item)
    }

    fun queueSuggestion(
        childId: String,
        roomName: String?,
        category: String,
        subject: String,
        content: String,
        tempId: String,
        parentName: String? = null,
        childName: String? = null,
        date: String? = null,
        time: String? = null
    ) {
        val item = PendingSyncItem(
            id = tempId,
            type = PendingSyncType.SUGGESTION,
            childId = childId,
            suggestionRoomName = roomName,
            suggestionCategory = category,
            suggestionSubject = subject,
            suggestionContent = content,
            parentName = parentName,
            childName = childName,
            date = date,
            time = time
        )
        enqueue(item)
    }

    // =========================================================================
    // Recuperación de elementos pendientes para poblar UI localmente
    // =========================================================================

    fun getPendingTimelineItems(childId: String): List<TimelineItem> {
        return _pendingItems.value
            .filter { it.childId == childId }
            .mapNotNull { it.toTimelineItem() }
    }

    fun getPendingLostObjects(): List<LostItem> {
        return _pendingItems.value
            .filter { it.type == PendingSyncType.LOST_OBJECT }
            .map { it.toLostItem() }
    }

    fun getPendingSuggestions(): List<ParentSuggestion> {
        return _pendingItems.value
            .filter { it.type == PendingSyncType.SUGGESTION }
            .map { it.toParentSuggestion() }
    }

    fun getPendingWorkerMessages(chatId: String): List<WorkerChatMessage> {
        return _pendingItems.value
            .filter { it.type == PendingSyncType.WORKER_MESSAGE && (it.chatId == chatId || (it.isGlobalChat && chatId == "global_staff_chat")) }
            .map { it.toWorkerChatMessage() }
    }

    fun getPendingFamilyGroupMessages(groupId: String): List<FamilyGroupMessage> {
        return _pendingItems.value
            .filter { it.type == PendingSyncType.FAMILY_GROUP_MESSAGE && it.groupId == groupId }
            .map { it.toFamilyGroupMessage() }
    }

    // =========================================================================
    // Monitoreo de conectividad y Sincronización Automática
    // =========================================================================

    private fun isConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val active = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(active) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun startNetworkMonitoring() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Conexión a internet detectada por ConnectivityManager, iniciando sincronización...")
                        triggerSync()
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar callback de red: ${e.message}")
        }

        scope.launch {
            chatConexion.estado.collect { estado ->
                if (estado == EstadoConexion.Conectado) {
                    Log.d(TAG, "ChatConexion conectado al servidor, iniciando sincronización de pendientes...")
                    triggerSync()
                }
            }
        }
    }

    fun triggerSync() {
        scope.launch {
            if (!syncMutex.tryLock()) return@launch
            try {
                syncInternal()
            } finally {
                syncMutex.unlock()
            }
        }
    }

    private suspend fun syncInternal() {
        if (!isConnected()) {
            Log.d(TAG, "Sin conexión activa. Sincronización pospuesta.")
            return
        }

        var pendientes = _pendingItems.value
        if (pendientes.isEmpty()) return

        Log.d(TAG, "Iniciando sincronización de ${pendientes.size} elementos pendientes...")

        for (item in pendientes) {
            if (!isConnected()) {
                Log.d(TAG, "Se perdió la conexión durante la sincronización. Deteniendo.")
                break
            }

            try {
                val exito = syncSingleItem(item)
                if (exito) {
                    removePending(item.id)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Error de red al sincronizar item ${item.id}: ${e.message}. Se reintentará luego.")
                break // No hay conexión fiable con el backend; se detiene hasta el próximo evento de red
            } catch (e: Exception) {
                Log.e(TAG, "Error no recuperable sincronizando item ${item.id}: ${e.message}", e)
                // En caso de error no recuperable (ej. validación 400), se retira para no bloquear la cola
                removePending(item.id)
            }
        }
    }

    private suspend fun syncSingleItem(item: PendingSyncItem): Boolean {
        when (item.type) {
            PendingSyncType.BITACORA_ACTIVITY -> {
                val childId = item.childId ?: return true
                val response = api.createActivity(
                    childId = childId,
                    request = CreateActivityDto(
                        time = item.time ?: "",
                        date = item.date,
                        category = item.activityCategory ?: "Otros",
                        title = "${item.activityCategory ?: "Otros"}: ${item.activityPortion ?: ""}".trim().removeSuffix(":"),
                        description = item.activityDescription ?: "",
                        portionOption = item.activityPortion ?: ""
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val domainItem = TimelineItem.ActivityCard(
                        id = body?.id ?: "act_${System.currentTimeMillis()}",
                        time = item.time ?: "",
                        date = body?.timelineItem?.date ?: item.date ?: "",
                        category = item.activityCategory ?: "Otros",
                        portionLabel = item.activityPortion ?: "",
                        description = item.activityDescription ?: ""
                    )
                    listeners.forEach { it.onBitacoraItemSynced(childId, item.id, domainItem) }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }

            PendingSyncType.BITACORA_EVENT -> {
                val childId = item.childId ?: return true
                val response = api.createTimelineEvent(
                    childId = childId,
                    request = QuickEventDto(
                        time = item.time ?: "",
                        text = item.eventText ?: "",
                        iconType = item.eventIconType ?: "entry",
                        date = item.date
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val domainItem = TimelineItem.EventChip(
                        id = body?.id ?: "evt_${System.currentTimeMillis()}",
                        time = body?.time ?: item.time ?: "",
                        date = body?.date ?: item.date ?: "",
                        text = body?.text ?: item.eventText ?: "",
                        iconType = body?.iconType ?: item.eventIconType ?: "entry"
                    )
                    listeners.forEach { it.onBitacoraItemSynced(childId, item.id, domainItem) }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }

            PendingSyncType.BITACORA_MESSAGE -> {
                val childId = item.childId ?: return true
                val uriServidor = if (item.fileUri != null && !item.fileUri.startsWith("http") && !item.fileUri.startsWith("/api/")) {
                    subidaArchivos?.subir(item.fileUri, item.fileName, item.fileMimeType, "bitacora")
                } else {
                    item.fileUri
                }

                val response = api.sendBitacoraMessage(
                    childId = childId,
                    request = SendBitacoraMessageDto(
                        message = item.messageText ?: "",
                        fileUri = uriServidor,
                        fileName = item.fileName,
                        fileMimeType = item.fileMimeType,
                        time = item.time,
                        date = item.date
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val domainItem = TimelineItem.ChatMessage(
                        id = body?.id ?: "msg_${System.currentTimeMillis()}",
                        time = body?.time ?: item.time ?: "",
                        date = body?.date ?: item.date ?: "",
                        message = body?.message ?: item.messageText ?: "",
                        isOutgoing = item.isOutgoing,
                        fileUri = uriServidor?.let(RemoteSync::absoluteUrl),
                        fileName = item.fileName,
                        fileMimeType = item.fileMimeType,
                        senderRole = item.senderRole?.let { roleStr -> UserRole.entries.firstOrNull { it.name == roleStr } }
                    )
                    listeners.forEach { it.onBitacoraMessageSynced(childId, item.id, domainItem) }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }

            PendingSyncType.WORKER_MESSAGE -> {
                val text = item.messageText ?: ""
                val hora = item.time ?: ""
                val (isSuccess, serverId, httpCode) = when {
                    item.isGlobalChat || item.chatId == "global_staff_chat" -> {
                        val res = api.sendStaffGlobalMessage(SendWorkerMessageDto(message = text, time = hora))
                        Triple(res.isSuccessful, res.body()?.id, res.code())
                    }
                    item.childId != null -> {
                        val res = api.sendBitacoraMessage(item.childId, SendBitacoraMessageDto(message = text, time = hora))
                        Triple(res.isSuccessful, res.body()?.id, res.code())
                    }
                    else -> {
                        val res = api.sendWorkerMessage(item.chatId ?: return true, SendWorkerMessageDto(message = text, time = hora))
                        Triple(res.isSuccessful, res.body()?.id, res.code())
                    }
                }
                if (isSuccess) {
                    val finalId = serverId ?: "wm_${System.currentTimeMillis()}"
                    listeners.forEach { it.onWorkerMessageSynced(item.chatId ?: item.childId ?: "", item.id, finalId) }
                    return true
                }
                if (httpCode in 400..499) return true
                return false
            }

            PendingSyncType.FAMILY_GROUP_MESSAGE -> {
                val groupId = item.groupId ?: return true
                val uriServidor = if (item.fileUri != null && !item.fileUri.startsWith("http") && !item.fileUri.startsWith("/api/")) {
                    subidaArchivos?.subir(item.fileUri, item.fileName, item.fileMimeType, "group-attachments")
                } else {
                    item.fileUri
                }
                val attachmentDto = if (uriServidor != null) {
                    AttachmentDto(
                        uri = uriServidor,
                        name = item.fileName ?: "archivo",
                        mimeType = item.fileMimeType ?: "application/octet-stream"
                    )
                } else null

                val response = api.sendFamilyGroupMessage(
                    groupId = groupId,
                    request = SendFamilyGroupMessageDto(
                        text = item.messageText ?: "",
                        attachment = attachmentDto,
                        time = item.time ?: ""
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val domainMsg = FamilyGroupMessage(
                            id = body.id,
                            senderId = body.senderId,
                            senderName = body.senderName,
                            text = body.text,
                            time = body.time,
                            attachment = body.attachment?.let { MessageAttachment(RemoteSync.absoluteUrl(it.uri), it.name, it.mimeType) }
                        )
                        listeners.forEach { it.onFamilyGroupMessageSynced(groupId, item.id, domainMsg) }
                    }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }

            PendingSyncType.LOST_OBJECT -> {
                val foto = if (item.lostImageUri != null && !item.lostImageUri.startsWith("http") && !item.lostImageUri.startsWith("/api/")) {
                    subidaArchivos?.subir(item.lostImageUri, "objeto-perdido.jpg", "image/jpeg", "lost-objects")
                } else {
                    item.lostImageUri
                }
                val response = api.createLostObject(
                    CreateLostObjectDto(
                        title = item.lostTitle ?: "",
                        description = item.lostDescription ?: "",
                        category = item.lostCategory ?: "Otros",
                        location = item.lostLocation ?: "",
                        room = item.lostRoom ?: "General",
                        imageUri = foto
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val domainItem = LostItem(
                            id = body.id,
                            title = body.title,
                            description = body.description,
                            category = body.category,
                            location = body.location,
                            room = body.room ?: "Sala 1A",
                            imageUri = body.imageUri?.let(RemoteSync::absoluteUrl),
                            isClaimed = body.isClaimed
                        )
                        listeners.forEach { it.onLostObjectSynced(item.id, domainItem) }
                    }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }

            PendingSyncType.SUGGESTION -> {
                val response = api.createSuggestion(
                    CreateSuggestionDto(
                        childId = item.childId ?: "",
                        roomName = item.suggestionRoomName ?: "General",
                        category = item.suggestionCategory ?: "GENERAL",
                        subject = item.suggestionSubject ?: "",
                        content = item.suggestionContent ?: ""
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val cat = SuggestionCategory.entries.firstOrNull { it.name == body.category.uppercase() } ?: SuggestionCategory.GENERAL
                        val stat = SuggestionStatus.entries.firstOrNull { it.name == body.status.uppercase() } ?: SuggestionStatus.PENDIENTE
                        val domainItem = ParentSuggestion(
                            id = body.id,
                            parentName = body.parentName,
                            childName = body.childName,
                            roomName = body.roomName,
                            date = body.date,
                            time = body.time,
                            category = cat,
                            subject = body.subject,
                            content = body.content,
                            status = stat,
                            response = body.response,
                            responseDate = body.responseDate,
                            responderName = body.responderName
                        )
                        listeners.forEach { it.onSuggestionSynced(item.id, domainItem) }
                    }
                    return true
                }
                if (response.code() in 400..499) return true
                return false
            }
        }
    }

    // =========================================================================
    // Conversiones a entidades de Dominio
    // =========================================================================

    private fun PendingSyncItem.toTimelineItem(): TimelineItem? {
        val fDate = date ?: SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(createdAt))
        val fTime = time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date(createdAt))
        return when (type) {
            PendingSyncType.BITACORA_ACTIVITY -> TimelineItem.ActivityCard(
                id = id,
                time = fTime,
                date = fDate,
                category = activityCategory ?: "Otros",
                portionLabel = activityPortion ?: "",
                description = activityDescription ?: ""
            )
            PendingSyncType.BITACORA_EVENT -> TimelineItem.EventChip(
                id = id,
                time = fTime,
                date = fDate,
                text = eventText ?: "",
                iconType = eventIconType ?: "entry"
            )
            PendingSyncType.BITACORA_MESSAGE -> TimelineItem.ChatMessage(
                id = id,
                time = fTime,
                date = fDate,
                message = messageText ?: "",
                isOutgoing = isOutgoing,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType,
                senderRole = senderRole?.let { roleStr -> UserRole.entries.firstOrNull { it.name == roleStr } }
            )
            else -> null
        }
    }

    private fun PendingSyncItem.toLostItem(): LostItem {
        return LostItem(
            id = id,
            title = lostTitle ?: "",
            description = lostDescription ?: "",
            category = lostCategory ?: "Otros",
            location = lostLocation,
            room = lostRoom ?: "Sala 1A",
            imageUri = lostImageUri,
            isClaimed = false
        )
    }

    private fun PendingSyncItem.toParentSuggestion(): ParentSuggestion {
        val cat = SuggestionCategory.entries.firstOrNull { it.name == suggestionCategory } ?: SuggestionCategory.GENERAL
        val fDate = date ?: SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(createdAt))
        val fTime = time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date(createdAt))
        return ParentSuggestion(
            id = id,
            parentName = parentName ?: "",
            childName = childName ?: "",
            roomName = suggestionRoomName ?: "",
            date = fDate,
            time = fTime,
            category = cat,
            subject = suggestionSubject ?: "",
            content = suggestionContent ?: "",
            status = SuggestionStatus.PENDIENTE
        )
    }

    private fun PendingSyncItem.toWorkerChatMessage(): WorkerChatMessage {
        return WorkerChatMessage(
            id = id,
            senderId = senderId ?: "me",
            senderName = senderDisplayName ?: "Yo",
            message = messageText ?: "",
            time = time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date(createdAt)),
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType
        )
    }

    private fun PendingSyncItem.toFamilyGroupMessage(): FamilyGroupMessage {
        return FamilyGroupMessage(
            id = id,
            senderId = senderId ?: "me",
            senderName = senderDisplayName ?: "Yo",
            text = messageText ?: "",
            time = time ?: SimpleDateFormat("hh:mm a", Locale.US).format(Date(createdAt)),
            attachment = if (fileUri != null) MessageAttachment(fileUri, fileName, fileMimeType) else null
        )
    }

    private fun cacheFileLocallyIfNeeded(uriString: String?, prefix: String): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("http") || uriString.startsWith("/api/")) return uriString
        if (uriString.startsWith("file://")) return uriString
        return try {
            val uri = Uri.parse(uriString)
            val dir = File(context.cacheDir, "offline_uploads").apply { mkdirs() }
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "bin"
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            uriString
        }
    }
}
