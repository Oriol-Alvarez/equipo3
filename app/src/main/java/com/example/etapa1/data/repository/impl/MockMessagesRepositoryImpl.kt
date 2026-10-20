package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.model.WorkerMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockMessagesRepositoryImpl : MessagesRepository {
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

    override fun recargar() {
        // Datos mock en memoria ya cargados
    }

    override fun sendWorkerMessage(chatId: String, text: String) {
        val newMsg = WorkerChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "me",
            senderName = "Yo",
            message = text,
            time = "Ahora",
            isOutgoing = true
        )
        if (chatId == _globalChat.value.id) {
            _globalChat.update {
                it.copy(
                    lastMessage = "Yo: $text",
                    lastMessageTime = "Ahora",
                    messages = it.messages + newMsg
                )
            }
        } else {
            var foundInWorkers = false
            _workerChats.update { list ->
                list.map { chat ->
                    if (chat.id == chatId) {
                        foundInWorkers = true
                        chat.copy(
                            lastMessage = text,
                            lastMessageTime = "Ahora",
                            messages = chat.messages + newMsg
                        )
                    } else chat
                }
            }
            if (!foundInWorkers) {
                _familyChats.update { list ->
                    list.map { chat ->
                        if (chat.id == chatId) {
                            chat.copy(
                                lastMessage = text,
                                lastMessageTime = "Ahora",
                                messages = chat.messages + newMsg
                            )
                        } else chat
                    }
                }
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
        val hora = time.ifEmpty { "Ahora" }

        // 1. Actualizar _parentChats (vista educadora)
        _parentChats.update { list ->
            val existing = list.find { it.child.id == childId }
            val updatedSummary = if (existing != null) {
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
            listOf(updatedSummary) + list.filter { it.child.id != childId }
        }

        // 2. Actualizar _familyChats (vista familiar)
        val educator = MockDataRepository.getEducatorForChild(childId)
        val newMsg = WorkerChatMessage(
            id = "bm_${System.currentTimeMillis()}",
            senderId = if (isOutgoing) "me" else educator.id,
            senderName = if (isOutgoing) "Yo" else educator.name,
            message = text,
            time = hora,
            isOutgoing = isOutgoing
        )
        _familyChats.update { list ->
            val existing = list.find {
                it.title.equals(educator.name, ignoreCase = true) ||
                (childId == "mateo_garcia" && it.id == "fchat_1") ||
                (childId == "lucia_garcia" && it.id == "fchat_carlos")
            }
            val updatedChat = if (existing != null) {
                existing.copy(
                    lastMessage = if (isOutgoing) "Yo: $text" else "${educator.name}: $text",
                    lastMessageTime = hora,
                    unreadCount = if (isOutgoing) 0 else existing.unreadCount + 1,
                    messages = existing.messages + newMsg
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
            listOf(updatedChat) + list.filter { it.id != updatedChat.id }
        }
    }
}
