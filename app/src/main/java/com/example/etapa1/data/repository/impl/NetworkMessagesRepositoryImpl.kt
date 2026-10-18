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

    private val _workerChats = MutableStateFlow<List<WorkerChat>>(emptyList())
    override val workerChats: StateFlow<List<WorkerChat>> = _workerChats.asStateFlow()

    private val _parentChats = MutableStateFlow<List<ParentChatSummary>>(emptyList())
    override val parentChats: StateFlow<List<ParentChatSummary>> = _parentChats.asStateFlow()

    private val _staffMembers = MutableStateFlow<List<WorkerMember>>(emptyList())
    override val staffMembers: StateFlow<List<WorkerMember>> = _staffMembers.asStateFlow()

    private val _globalChat = MutableStateFlow<WorkerChat>(
        WorkerChat(
            id = "global_staff_chat",
            title = "Chat Global de Trabajadores",
            subtitle = "Canal institucional",
            lastMessage = "Cargando mensajes...",
            lastMessageTime = "",
            isGlobal = true,
            isGroup = true,
            membersCount = 18,
            messages = emptyList()
        )
    )
    override val globalChat: StateFlow<WorkerChat> = _globalChat.asStateFlow()

    init {
        cargarDatosIniciales()
        observarWebSocket()
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
            api.getParentChatsByRoom("sala_1a").bodyOrLog("GET /rooms/sala_1a/parent-chats")?.let { summaries ->
                _parentChats.value = summaries.map { s ->
                    val child = MockDataRepository.childrenSala1A.find { it.id == s.childId }
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
                    ParentChatSummary(
                        child = child,
                        parentName = s.parentName,
                        lastMessage = s.lastMessage ?: "Sin mensajes aún.",
                        lastMessageTime = s.lastMessageTime ?: "Hoy",
                        unreadCount = s.unreadCount
                    )
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

                        if (evento.chatId == _globalChat.value.id || evento.chatId == "global_staff_chat") {
                            _globalChat.update { actual ->
                                if (actual.messages.any { it.id == nuevoMsg.id }) actual
                                else actual.copy(
                                    lastMessage = "${nuevoMsg.senderName}: ${nuevoMsg.message}",
                                    lastMessageTime = nuevoMsg.time,
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
                                            messages = chat.messages + nuevoMsg
                                        )
                                    } else chat
                                }
                            }
                        }
                    }

                    is EventoServidor.NuevoMensajeBitacora -> {
                        // Actualizar resumen en la lista de chats de padres
                        _parentChats.update { lista ->
                            lista.map { item ->
                                if (item.child.id == evento.childId) {
                                    item.copy(
                                        lastMessage = evento.message.message,
                                        lastMessageTime = evento.message.time.ifEmpty { horaActual() }
                                    )
                                } else item
                            }
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
                api.sendStaffGlobalMessage(SendWorkerMessageDto(message = text))
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
                api.sendWorkerMessage(chatId, SendWorkerMessageDto(message = text))
            }
        }
    }

    private fun horaActual(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
}
