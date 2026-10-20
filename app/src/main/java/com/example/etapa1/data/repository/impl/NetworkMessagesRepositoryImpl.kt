package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SendWorkerMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.model.WorkerMember
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
 * Repositorio de mensajería interna y canal global conectado a FastAPI y al WebSocket
 * en tiempo real ([ChatConexion]).
 */
class NetworkMessagesRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager,
    private val chatConexion: ChatConexion,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : MessagesRepository {

    private val _workerChats = MutableStateFlow<List<WorkerChat>>(MockDataRepository.getInitialWorkerChats())
    override val workerChats: StateFlow<List<WorkerChat>> = _workerChats.asStateFlow()

    private val _parentChats = MutableStateFlow<List<ParentChatSummary>>(MockDataRepository.getInitialParentChats())
    override val parentChats: StateFlow<List<ParentChatSummary>> = _parentChats.asStateFlow()

    private val _familyChats = MutableStateFlow<List<WorkerChat>>(MockDataRepository.getInitialFamilyChats())
    override val familyChats: StateFlow<List<WorkerChat>> = _familyChats.asStateFlow()

    private val _staffMembers = MutableStateFlow<List<WorkerMember>>(MockDataRepository.staffMembers)
    override val staffMembers: StateFlow<List<WorkerMember>> = _staffMembers.asStateFlow()

    private val _globalChat = MutableStateFlow<WorkerChat>(MockDataRepository.initialGlobalWorkerChat)
    override val globalChat: StateFlow<WorkerChat> = _globalChat.asStateFlow()

    init {
        cargarDatosIniciales()
        observarWebSocket()
    }

    override fun recargar() {
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        RemoteSync.launch("Cargar personal de la institución") {
            api.getStaffMembers().bodyOrLog("GET /staff/members")?.let { dtos ->
                _staffMembers.value = dtos.map { dto ->
                    WorkerMember(
                        id = dto.id,
                        name = dto.name,
                        role = dto.role,
                        avatarInitials = dto.avatarInitials,
                        avatarBgColor = dto.avatarBgColor,
                        isOnline = dto.isOnline
                    )
                }
            }
        }

        RemoteSync.launch("Cargar chat global institucional") {
            api.getStaffGlobalChat().bodyOrLog("GET /chats/staff-global/messages")?.let { dto ->
                val msgs = dto.messages.map { m ->
                    WorkerChatMessage(
                        id = m.id,
                        senderId = m.senderId,
                        senderName = m.senderName,
                        message = m.message,
                        time = m.time,
                        isOutgoing = m.senderId == tokenManager.userId,
                        fileUri = m.fileUri,
                        fileName = m.fileName,
                        fileMimeType = m.fileMimeType
                    )
                }
                val lastMsg = msgs.lastOrNull()
                _globalChat.value = WorkerChat(
                    id = dto.id,
                    title = dto.title,
                    subtitle = "Canal institucional",
                    lastMessage = lastMsg?.message ?: "Canal activo",
                    lastMessageTime = lastMsg?.time ?: "Hoy",
                    isGlobal = true,
                    isGroup = true,
                    membersCount = dto.membersCount,
                    messages = msgs
                )
            }
        }

        RemoteSync.launch("Cargar chats internos entre trabajadoras") {
            api.getWorkerChats().bodyOrLog("GET /chats/workers")?.let { list ->
                val chatsConHistorial = list.map { chatDto ->
                    val mensajesDto = api.getWorkerChatMessages(chatDto.id)
                        .bodyOrLog("GET mensajes ${chatDto.id}").orEmpty()
                    val msgs = mensajesDto.map { m ->
                        WorkerChatMessage(
                            id = m.id,
                            senderId = m.senderId,
                            senderName = m.senderName,
                            message = m.message,
                            time = m.time,
                            isOutgoing = m.senderId == tokenManager.userId,
                            fileUri = m.fileUri,
                            fileName = m.fileName,
                            fileMimeType = m.fileMimeType
                        )
                    }
                    val localChat = _workerChats.value.find { it.id == chatDto.id }
                    val unread = if (localChat != null && localChat.unreadCount == 0 && chatDto.unreadCount <= 1) 0 else chatDto.unreadCount
                    WorkerChat(
                        id = chatDto.id,
                        title = chatDto.title,
                        subtitle = chatDto.subtitle,
                        lastMessage = chatDto.lastMessage ?: msgs.lastOrNull()?.message ?: "",
                        lastMessageTime = chatDto.lastMessageTime ?: msgs.lastOrNull()?.time ?: "",
                        unreadCount = unread,
                        isGlobal = chatDto.isGlobal,
                        isGroup = chatDto.isGroup,
                        avatarInitials = chatDto.avatarInitials,
                        avatarBgColor = chatDto.avatarBgColor,
                        messages = msgs
                    )
                }
                _workerChats.value = chatsConHistorial
            }
        }

        RemoteSync.launch("Cargar chats con familias") {
            api.getParentChatsByRoom("sala_1a").bodyOrLog("GET /rooms/sala_1a/parent-chats")?.let { summaries ->
                if (summaries.isNotEmpty()) {
                    _parentChats.value = summaries.map { s ->
                        val child = MockDataRepository.childrenSala1A.find { it.id == s.childId }
                            ?: MockDataRepository.parentChildren.find { it.id == s.childId }
                            ?: Child(
                                id = s.childId,
                                fullName = s.childName,
                                shortName = s.childName.split(" ").firstOrNull() ?: s.childName,
                                ageText = "2 años",
                                roomText = "Sala 1A",
                                groupText = "Grupo Sala 1A",
                                statusText = "08:15 AM • Presente",
                                isPresent = true
                            )
                        val localParentChat = _parentChats.value.find { it.child.id == s.childId }
                        val unread = if (localParentChat != null && localParentChat.unreadCount == 0 && s.unreadCount <= 1) 0 else s.unreadCount
                        val lastMsg = if (!s.lastMessage.isNullOrBlank() && s.lastMessage != "Sin mensajes aún.") {
                            s.lastMessage
                        } else {
                            localParentChat?.lastMessage?.ifBlank { "Sin mensajes aún." } ?: "Sin mensajes aún."
                        }
                        val time = if (!s.lastMessageTime.isNullOrBlank()) s.lastMessageTime else (localParentChat?.lastMessageTime ?: "Hoy")
                        ParentChatSummary(
                            child = child,
                            parentName = s.parentName,
                            lastMessage = lastMsg,
                            lastMessageTime = time,
                            unreadCount = unread
                        )
                    }
                }
            }
        }

        RemoteSync.launch("Cargar chats familiares con educadores") {
            for (child in MockDataRepository.parentChildren) {
                api.getBitacoraMessages(child.id).bodyOrLog("GET bitacora ${child.id}")?.let { msgs ->
                    val lastMsg = msgs.lastOrNull()
                    if (lastMsg != null) {
                        val educator = MockDataRepository.getEducatorForChild(child.id)
                        val isOut = lastMsg.isOutgoing ?: false
                        val displayText = if (isOut) "Yo: ${lastMsg.message}" else "${lastMsg.senderName ?: educator.name}: ${lastMsg.message}"
                        _familyChats.update { list ->
                            val existing = list.find {
                                it.title.equals(educator.name, ignoreCase = true) ||
                                (child.id == "mateo_garcia" && it.id == "fchat_1") ||
                                (child.id == "lucia_garcia" && it.id == "fchat_carlos")
                            }
                            if (existing != null) {
                                val unread = if (existing.unreadCount == 0) 0 else if (!isOut) 1 else 0
                                val updated = existing.copy(
                                    lastMessage = displayText,
                                    lastMessageTime = lastMsg.time.ifBlank { existing.lastMessageTime },
                                    unreadCount = unread
                                )
                                listOf(updated) + list.filter { it.id != existing.id }
                            } else {
                                list
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observarWebSocket() {
        scope.launch {
            chatConexion.eventos.collect { evento ->
                when (evento) {
                    is EventoServidor.NuevoMensajeChat -> {
                        val m = evento.message
                        val nuevoMsg = WorkerChatMessage(
                            id = m.id.ifEmpty { "wm_${System.currentTimeMillis()}" },
                            senderId = m.senderId,
                            senderName = m.senderName,
                            message = m.message,
                            time = m.time.ifEmpty { horaActual() },
                            isOutgoing = m.senderId == tokenManager.userId,
                            fileUri = m.fileUri,
                            fileName = m.fileName,
                            fileMimeType = m.fileMimeType
                        )

                        val isIncoming = nuevoMsg.senderId != tokenManager.userId
                        if (evento.chatId == _globalChat.value.id || evento.chatId == "global_staff_chat") {
                            _globalChat.update { actual ->
                                if (actual.messages.any { it.id == nuevoMsg.id }) actual
                                else actual.copy(
                                    lastMessage = "${nuevoMsg.senderName}: ${nuevoMsg.message}",
                                    lastMessageTime = nuevoMsg.time,
                                    unreadCount = if (isIncoming) actual.unreadCount + 1 else actual.unreadCount,
                                    messages = actual.messages + nuevoMsg
                                )
                            }
                        } else {
                            _workerChats.update { lista ->
                                lista.map { chat ->
                                    if (chat.id == evento.chatId) {
                                        if (chat.messages.any { it.id == nuevoMsg.id }) chat
                                        else chat.copy(
                                            lastMessage = nuevoMsg.message,
                                            lastMessageTime = nuevoMsg.time,
                                            unreadCount = if (isIncoming) chat.unreadCount + 1 else chat.unreadCount,
                                            messages = chat.messages + nuevoMsg
                                        )
                                    } else chat
                                }
                            }
                        }
                    }

                    is EventoServidor.NuevoMensajeBitacora -> {
                        val targetChildId = when {
                            !evento.childId.isNullOrEmpty() -> evento.childId
                            !evento.chatId.isNullOrEmpty() -> evento.chatId
                            else -> "mateo_garcia"
                        }
                        val isIncoming = evento.message.senderId != tokenManager.userId
                        updateBitacoraChatMessage(
                            childId = targetChildId,
                            text = evento.message.message,
                            time = evento.message.time.ifEmpty { horaActual() },
                            isOutgoing = !isIncoming
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun sendWorkerMessage(chatId: String, text: String) {
        val hora = horaActual()
        val tempId = "wm_${System.currentTimeMillis()}"
        val myName = tokenManager.userDisplayName ?: "Yo"
        val myId = tokenManager.userId ?: "me"

        val localMsg = WorkerChatMessage(
            id = tempId,
            senderId = myId,
            senderName = myName,
            message = text,
            time = hora,
            isOutgoing = true
        )

        // Actualización optimista inmediata en la UI
        if (chatId == _globalChat.value.id || chatId == "global_staff_chat") {
            _globalChat.update { actual ->
                actual.copy(
                    lastMessage = "Yo: $text",
                    lastMessageTime = hora,
                    messages = actual.messages + localMsg
                )
            }
            RemoteSync.launch("Enviar mensaje a chat global") {
                api.sendStaffGlobalMessage(SendWorkerMessageDto(message = text, time = hora))
            }
        } else {
            var foundInWorkers = false
            _workerChats.update { lista ->
                lista.map { chat ->
                    if (chat.id == chatId) {
                        foundInWorkers = true
                        chat.copy(
                            lastMessage = text,
                            lastMessageTime = hora,
                            messages = chat.messages + localMsg
                        )
                    } else chat
                }
            }
            if (!foundInWorkers) {
                _familyChats.update { lista ->
                    lista.map { chat ->
                        if (chat.id == chatId) {
                            chat.copy(
                                lastMessage = text,
                                lastMessageTime = hora,
                                messages = chat.messages + localMsg
                            )
                        } else chat
                    }
                }
            }
            RemoteSync.launch("Enviar mensaje a chat trabajador $chatId") {
                api.sendWorkerMessage(chatId, SendWorkerMessageDto(message = text, time = hora))
            }
        }
    }

    override fun markWorkerChatAsRead(chatId: String) {
        if (chatId == _globalChat.value.id || chatId == "global_staff_chat") {
            _globalChat.update { it.copy(unreadCount = 0) }
        } else {
            _workerChats.update { list ->
                list.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
            }
            _familyChats.update { list ->
                list.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
            }
            RemoteSync.launch("Marcar chat trabajador $chatId como leído") {
                api.markWorkerChatAsRead(chatId)
            }
        }
    }

    override fun markParentChatAsRead(childId: String) {
        _parentChats.update { list ->
            list.map { if (it.child.id == childId) it.copy(unreadCount = 0) else it }
        }
    }

    override fun startNewWorkerChat(staffMember: WorkerMember): WorkerChat {
        val existing = _workerChats.value.find { it.title.equals(staffMember.name, ignoreCase = true) }
        if (existing != null) return existing

        val newChat = WorkerChat(
            id = "wchat_${staffMember.id}_${System.currentTimeMillis()}",
            title = staffMember.name,
            subtitle = "${staffMember.role} • En línea",
            lastMessage = "Inicia una conversación con ${staffMember.name}.",
            lastMessageTime = "Ahora",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = staffMember.avatarInitials,
            avatarBgColor = staffMember.avatarBgColor,
            messages = listOf(
                WorkerChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    senderId = "system",
                    senderName = "Sistema",
                    message = "Canal directo iniciado con ${staffMember.name}.",
                    time = "Ahora"
                )
            )
        )
        _workerChats.update { listOf(newChat) + it }
        return newChat
    }

    override fun startNewParentChat(child: Child, parentName: String): ParentChatSummary {
        val existing = _parentChats.value.find { it.child.id == child.id }
        if (existing != null) return existing

        val newSummary = ParentChatSummary(
            child = child,
            parentName = parentName,
            lastMessage = "Conversación iniciada con la familia.",
            lastMessageTime = "Ahora",
            unreadCount = 0
        )
        _parentChats.update { listOf(newSummary) + it }
        return newSummary
    }

    override fun startNewFamilyEducatorChat(staffMember: WorkerMember): WorkerChat {
        val existing = _familyChats.value.find { it.title.equals(staffMember.name, ignoreCase = true) }
        if (existing != null) return existing

        val newChat = WorkerChat(
            id = "fchat_${staffMember.id}_${System.currentTimeMillis()}",
            title = staffMember.name,
            subtitle = "${staffMember.role} • En línea",
            lastMessage = "Inicia una consulta o mensaje con ${staffMember.name}.",
            lastMessageTime = "Ahora",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = staffMember.avatarInitials,
            avatarBgColor = staffMember.avatarBgColor,
            messages = listOf(
                WorkerChatMessage(
                    id = "fm_init_${System.currentTimeMillis()}",
                    senderId = "system",
                    senderName = "Sonrisas",
                    message = "Canal directo iniciado con ${staffMember.name} (${staffMember.role}).",
                    time = "Ahora"
                )
            )
        )
        _familyChats.update { listOf(newChat) + it }
        return newChat
    }

    override fun updateBitacoraChatMessage(childId: String, text: String, time: String, isOutgoing: Boolean) {
        val hora = time.ifEmpty { horaActual() }

        // 1. Actualizar _parentChats (vista educadora)
        _parentChats.update { lista ->
            val existing = lista.find { it.child.id == childId }
            val updated = if (existing != null) {
                existing.copy(
                    lastMessage = if (isOutgoing) "Yo: $text" else text,
                    lastMessageTime = hora,
                    unreadCount = if (isOutgoing) 0 else existing.unreadCount + 1
                )
            } else {
                val child = (MockDataRepository.childrenSala1A + MockDataRepository.parentChildren).find { it.id == childId }
                    ?: Child(
                        id = childId,
                        fullName = childId,
                        shortName = childId,
                        ageText = "2 años",
                        roomText = "Sala 1A",
                        groupText = "Grupo Sala 1A",
                        statusText = "08:15 AM • Presente",
                        isPresent = true
                    )
                ParentChatSummary(
                    child = child,
                    parentName = if (childId == "mateo_garcia" || childId == "lucia_garcia") "María López (Mamá)" else "Familiar Tutor",
                    lastMessage = if (isOutgoing) "Yo: $text" else text,
                    lastMessageTime = hora,
                    unreadCount = if (isOutgoing) 0 else 1
                )
            }
            listOf(updated) + lista.filter { it.child.id != childId }
        }

        // 2. Actualizar _familyChats (vista familiar)
        val educator = MockDataRepository.getEducatorForChild(childId)
        val newMsg = WorkerChatMessage(
            id = "bm_${System.currentTimeMillis()}",
            senderId = if (isOutgoing) (tokenManager.userId ?: "me") else educator.id,
            senderName = if (isOutgoing) (tokenManager.userDisplayName ?: "Yo") else educator.name,
            message = text,
            time = hora,
            isOutgoing = isOutgoing
        )
        _familyChats.update { lista ->
            val existing = lista.find {
                it.title.equals(educator.name, ignoreCase = true) ||
                (childId == "mateo_garcia" && it.id == "fchat_1") ||
                (childId == "lucia_garcia" && it.id == "fchat_carlos")
            }
            val updatedChat = if (existing != null) {
                existing.copy(
                    lastMessage = if (isOutgoing) "Yo: $text" else "${educator.name}: $text",
                    lastMessageTime = hora,
                    unreadCount = if (isOutgoing) 0 else existing.unreadCount + 1,
                    messages = if (existing.messages.any { it.message == text && it.time == hora }) existing.messages else existing.messages + newMsg
                )
            } else {
                WorkerChat(
                    id = if (childId == "mateo_garcia") "fchat_1" else if (childId == "lucia_garcia") "fchat_carlos" else "fchat_${educator.id}",
                    title = educator.name,
                    subtitle = "${educator.role} • En línea",
                    lastMessage = if (isOutgoing) "Yo: $text" else "${educator.name}: $text",
                    lastMessageTime = hora,
                    unreadCount = if (isOutgoing) 0 else 1,
                    avatarInitials = educator.avatarInitials,
                    avatarBgColor = educator.avatarBgColor,
                    messages = listOf(newMsg)
                )
            }
            listOf(updatedChat) + lista.filter { it.id != updatedChat.id }
        }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
}
