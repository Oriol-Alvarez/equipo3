package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.CreateWorkerGroupDto
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SendBitacoraMessageDto
import com.example.etapa1.data.remote.SendWorkerMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.model.Child
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

    private val _workerChats = MutableStateFlow<List<WorkerChat>>(emptyList())
    override val workerChats: StateFlow<List<WorkerChat>> = _workerChats.asStateFlow()

    private val _parentChats = MutableStateFlow<List<ParentChatSummary>>(emptyList())
    override val parentChats: StateFlow<List<ParentChatSummary>> = _parentChats.asStateFlow()

    /** Familia: un chat por hijo con su educadora (es la bitácora del niño, id "fchat_<childId>"). */
    private val _familyChats = MutableStateFlow<List<WorkerChat>>(emptyList())
    override val familyChats: StateFlow<List<WorkerChat>> = _familyChats.asStateFlow()

    private val _staffMembers = MutableStateFlow<List<WorkerMember>>(emptyList())
    override val staffMembers: StateFlow<List<WorkerMember>> = _staffMembers.asStateFlow()

    private val _globalChat = MutableStateFlow<WorkerChat>(
        WorkerChat(
            id = "global_staff_chat",
            title = "Chat Global de Trabajadores",
            subtitle = "Canal institucional",
            lastMessage = "",
            lastMessageTime = "",
            isGlobal = true,
            isGroup = true,
            membersCount = 0,
            messages = emptyList()
        )
    )
    override val globalChat: StateFlow<WorkerChat> = _globalChat.asStateFlow()

    init {
        refresh()
        observarWebSocket()
    }

    override fun refresh() {
        // Los chats internos y con familias son solo del personal; la familia ve el chat con
        // la educadora de cada uno de sus hijos.
        if (esFamilia()) {
            cargarChatsFamilia()
            return
        }
        cargarDatosIniciales()
    }

    override fun limpiar() {
        _workerChats.value = emptyList()
        _parentChats.value = emptyList()
        _familyChats.value = emptyList()
        _staffMembers.value = emptyList()
        _globalChat.update { it.copy(lastMessage = "", lastMessageTime = "", unreadCount = 0, messages = emptyList()) }
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
                // Conserva el contador de no leídos que llega con la lista de chats
                val noLeidos = _globalChat.value.unreadCount
                _globalChat.value = WorkerChat(
                    unreadCount = noLeidos,
                    id = dto.id,
                    title = dto.title,
                    subtitle = "Canal institucional",
                    lastMessage = lastMsg?.message ?: "",
                    lastMessageTime = lastMsg?.time ?: "",
                    isGlobal = true,
                    isGroup = true,
                    membersCount = dto.membersCount,
                    messages = msgs
                )
            }
        }

        RemoteSync.launch("Cargar chats internos entre trabajadoras") {
            api.getWorkerChats().bodyOrLog("GET /chats/workers")?.let { todos ->
                // El chat global se muestra aparte; aquí solo se toma su contador de no leídos
                val (globales, list) = todos.partition { it.isGlobal || it.id == "global_staff_chat" }
                globales.firstOrNull()?.let { g -> _globalChat.update { it.copy(unreadCount = g.unreadCount) } }
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
                    WorkerChat(
                        id = chatDto.id,
                        title = chatDto.title,
                        subtitle = chatDto.subtitle,
                        lastMessage = chatDto.lastMessage ?: msgs.lastOrNull()?.message ?: "",
                        lastMessageTime = chatDto.lastMessageTime ?: msgs.lastOrNull()?.time ?: "",
                        unreadCount = chatDto.unreadCount,
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
            // Una conversación por niño de cada sala, con los datos reales del niño
            val salas = api.getRooms().bodyOrLog("GET /rooms").orEmpty()
            val resumenes = salas.flatMap { sala ->
                val ninos = api.getChildrenByRoom(sala.id).bodyOrLog("GET /rooms/${sala.id}/children")
                    .orEmpty().associateBy { it.id }
                api.getParentChatsByRoom(sala.id).bodyOrLog("GET /rooms/${sala.id}/parent-chats").orEmpty()
                    .map { s ->
                        val dto = ninos[s.childId]
                        val child = Child(
                            id = s.childId,
                            fullName = dto?.fullName ?: s.childName,
                            shortName = dto?.shortName ?: s.childName,
                            ageText = dto?.ageText.orEmpty(),
                            roomText = dto?.roomText ?: sala.name,
                            groupText = dto?.groupText.orEmpty(),
                            statusText = dto?.statusText.orEmpty(),
                            isPresent = dto?.isPresent ?: false,
                            arrivalTime = dto?.arrivalTime,
                            allergyAlert = dto?.allergyAlert,
                            avatarInitials = dto?.avatarInitials ?: s.childName.take(2).uppercase(),
                            avatarBgColor = dto?.avatarBgColor ?: 0xFF64B5F6,
                            birthDate = dto?.birthDate.orEmpty(),
                            roomId = sala.id
                        )
                        ParentChatSummary(
                            child = child,
                            parentName = s.parentName,
                            lastMessage = s.lastMessage ?: "Sin mensajes aún.",
                            lastMessageTime = s.lastMessageTime.orEmpty(),
                            unreadCount = s.unreadCount
                        )
                    }
            }
            _parentChats.value = resumenes
        }
    }

    private fun esFamilia() = tokenManager.userRole?.uppercase() == "FAMILIAR"

    private fun idChatFamilia(childId: String) = "fchat_$childId"

    private fun childIdDeChatFamilia(chatId: String): String? =
        chatId.takeIf { it.startsWith("fchat_") }?.removePrefix("fchat_")?.takeIf { it.isNotEmpty() }

    /** Familia: arma un chat por hijo con el nombre real de su educadora y el último mensaje. */
    private fun cargarChatsFamilia() {
        RemoteSync.launch("Cargar chats de la familia con educadoras") {
            val hijos = api.getMyChildren().bodyOrLog("GET /parents/me/children").orEmpty()
            val chats = hijos.map { hijo ->
                val educadora = api.getChildEducator(hijo.id).bodyOrLog("GET /children/${hijo.id}/educator")
                val mensajes = api.getBitacoraMessages(hijo.id).bodyOrLog("GET bitacora ${hijo.id}").orEmpty()
                val ultimo = mensajes.lastOrNull()
                val previo = _familyChats.value.find { it.id == idChatFamilia(hijo.id) }
                val titulo = educadora?.name ?: hijo.educatorName ?: "Educadora de ${hijo.shortName}"
                WorkerChat(
                    id = idChatFamilia(hijo.id),
                    title = titulo,
                    subtitle = "${educadora?.role ?: hijo.educatorRole ?: "Educadora"} • ${hijo.shortName}",
                    lastMessage = ultimo?.let {
                        if (it.senderId == tokenManager.userId) "Yo: ${it.message}" else "${it.senderName}: ${it.message}"
                    } ?: "Sin mensajes aún.",
                    lastMessageTime = ultimo?.time.orEmpty(),
                    unreadCount = previo?.unreadCount ?: 0,
                    isGlobal = false,
                    isGroup = false,
                    avatarInitials = educadora?.avatarInitials ?: hijo.educatorAvatarInitials ?: titulo.take(2).uppercase(),
                    avatarBgColor = educadora?.avatarBgColor ?: hijo.educatorAvatarBgColor ?: 0xFF00897B,
                    messages = emptyList()
                )
            }
            _familyChats.value = chats
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
                        val childId = evento.childId.ifEmpty { evento.chatId }
                        if (childId.isNotEmpty()) {
                            val isIncoming = evento.message.senderId != tokenManager.userId
                            // Actualiza el resumen y sube el chat al inicio de la lista
                            updateBitacoraChatMessage(
                                childId = childId,
                                text = evento.message.message,
                                time = evento.message.time.ifEmpty { horaActual() },
                                isOutgoing = !isIncoming
                            )
                        }
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

        // Chat de la familia con la educadora: es la bitácora del niño
        val childIdFamilia = childIdDeChatFamilia(chatId)
        if (childIdFamilia != null) {
            _familyChats.update { lista ->
                lista.map { chat ->
                    if (chat.id == chatId) {
                        chat.copy(lastMessage = "Yo: $text", lastMessageTime = hora, messages = chat.messages + localMsg)
                    } else chat
                }
            }
            RemoteSync.launch("Enviar mensaje a la bitácora de $childIdFamilia") {
                api.sendBitacoraMessage(childIdFamilia, SendBitacoraMessageDto(message = text, time = hora))
            }
            return
        }

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
            _workerChats.update { lista ->
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
            RemoteSync.launch("Enviar mensaje a chat trabajador $chatId") {
                api.sendWorkerMessage(chatId, SendWorkerMessageDto(message = text, time = hora))
            }
        }
    }

    override fun createWorkerGroup(title: String, memberIds: List<String>) {
        val titulo = title.trim()
        if (titulo.isEmpty()) return
        val tempId = "temp_${System.nanoTime()}"
        val pendiente = WorkerChat(
            id = tempId,
            title = titulo,
            subtitle = "Creando grupo…",
            lastMessage = "",
            lastMessageTime = "",
            isGroup = true,
            membersCount = memberIds.size + 1,
            avatarInitials = titulo.take(2).uppercase(),
            avatarBgColor = 0xFF00897B,
            messages = emptyList()
        )
        _workerChats.update { listOf(pendiente) + it }
        RemoteSync.launch("Crear grupo del personal") {
            val creado = api.createWorkerGroup(CreateWorkerGroupDto(titulo, memberIds)).bodyOrLog("POST /chats/workers")
            _workerChats.update { lista ->
                if (creado == null) {
                    lista.filterNot { it.id == tempId }
                } else {
                    lista.map {
                        if (it.id == tempId) {
                            it.copy(
                                id = creado.id,
                                subtitle = creado.subtitle,
                                lastMessage = creado.lastMessage.orEmpty(),
                                lastMessageTime = creado.lastMessageTime.orEmpty(),
                                avatarInitials = creado.avatarInitials,
                                avatarBgColor = creado.avatarBgColor
                            )
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    override fun markWorkerChatAsRead(chatId: String) {
        // El chat de la familia con la educadora es la bitácora del niño
        childIdDeChatFamilia(chatId)?.let { childId ->
            markParentChatAsRead(childId)
            return
        }
        if (chatId == _globalChat.value.id || chatId == "global_staff_chat") {
            _globalChat.update { it.copy(unreadCount = 0) }
        } else {
            _workerChats.update { list ->
                list.map { if (it.id == chatId) it.copy(unreadCount = 0) else it }
            }
        }
        RemoteSync.launch("Marcar como leído el chat $chatId") {
            api.markWorkerChatRead(chatId).bodyOrLog("PUT /chats/workers/$chatId/read")
        }
    }

    override fun markParentChatAsRead(childId: String) {
        _parentChats.update { list ->
            list.map { if (it.child.id == childId) it.copy(unreadCount = 0) else it }
        }
        _familyChats.update { list ->
            list.map { if (it.id == idChatFamilia(childId)) it.copy(unreadCount = 0) else it }
        }
        RemoteSync.launch("Marcar como leído el chat de bitácora de $childId") {
            api.markBitacoraChatRead(childId).bodyOrLog("PUT /chats/bitacora/$childId/read")
        }
    }

    override fun startNewWorkerChat(staffMember: WorkerMember): WorkerChat {
        _workerChats.value.find { !it.isGroup && it.title.equals(staffMember.name, ignoreCase = true) }
            ?.let { return it }

        // Se crea en el servidor como conversación con esa persona; mientras tanto se muestra en la lista
        val tempId = "temp_${System.nanoTime()}"
        val pendiente = WorkerChat(
            id = tempId,
            title = staffMember.name,
            subtitle = staffMember.role,
            lastMessage = "Inicia una conversación con ${staffMember.name}.",
            lastMessageTime = "Ahora",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            membersCount = 2,
            avatarInitials = staffMember.avatarInitials,
            avatarBgColor = staffMember.avatarBgColor,
            messages = emptyList()
        )
        _workerChats.update { listOf(pendiente) + it }
        RemoteSync.launch("Crear chat con ${staffMember.name}") {
            val creado = api.createWorkerGroup(CreateWorkerGroupDto(staffMember.name, listOf(staffMember.id)))
                .bodyOrLog("POST /chats/workers")
            _workerChats.update { lista ->
                if (creado == null) {
                    lista.filterNot { it.id == tempId }
                } else {
                    lista.map { if (it.id == tempId) it.copy(id = creado.id, lastMessage = creado.lastMessage.orEmpty()) else it }
                }
            }
        }
        return pendiente
    }

    override fun startNewParentChat(child: Child, parentName: String): ParentChatSummary {
        _parentChats.value.find { it.child.id == child.id }?.let { return it }
        val nuevo = ParentChatSummary(
            child = child,
            parentName = parentName,
            lastMessage = "Sin mensajes aún.",
            lastMessageTime = "",
            unreadCount = 0
        )
        _parentChats.update { listOf(nuevo) + it }
        return nuevo
    }

    override fun startNewFamilyEducatorChat(staffMember: WorkerMember): WorkerChat {
        // La familia habla con la educadora por la bitácora de su hijo
        return _familyChats.value.find { it.title.equals(staffMember.name, ignoreCase = true) }
            ?: _familyChats.value.firstOrNull()
            ?: WorkerChat(
                id = "fchat_",
                title = staffMember.name,
                subtitle = staffMember.role,
                lastMessage = "Sin mensajes aún.",
                lastMessageTime = "",
                avatarInitials = staffMember.avatarInitials,
                avatarBgColor = staffMember.avatarBgColor,
                messages = emptyList()
            )
    }

    override fun updateBitacoraChatMessage(childId: String, text: String, time: String, isOutgoing: Boolean) {
        val hora = time.ifEmpty { horaActual() }
        val resumen = if (isOutgoing) "Yo: $text" else text

        // Personal: el chat con la familia de ese niño sube al inicio
        _parentChats.update { lista ->
            val existente = lista.find { it.child.id == childId } ?: return@update lista
            val actualizado = existente.copy(
                lastMessage = resumen,
                lastMessageTime = hora,
                unreadCount = if (isOutgoing) existente.unreadCount else existente.unreadCount + 1
            )
            listOf(actualizado) + lista.filter { it.child.id != childId }
        }

        // Familia: el chat con la educadora de ese hijo sube al inicio
        _familyChats.update { lista ->
            val existente = lista.find { it.id == idChatFamilia(childId) } ?: return@update lista
            val actualizado = existente.copy(
                lastMessage = if (isOutgoing) "Yo: $text" else "${existente.title}: $text",
                lastMessageTime = hora,
                unreadCount = if (isOutgoing) existente.unreadCount else existente.unreadCount + 1
            )
            listOf(actualizado) + lista.filter { it.id != actualizado.id }
        }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
}
