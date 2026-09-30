package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.MessagesRepository
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

    private val _staffMembers = MutableStateFlow<List<WorkerMember>>(MockDataRepository.staffMembers)
    override val staffMembers: StateFlow<List<WorkerMember>> = _staffMembers.asStateFlow()

    private val _globalChat = MutableStateFlow<WorkerChat>(MockDataRepository.initialGlobalWorkerChat)
    override val globalChat: StateFlow<WorkerChat> = _globalChat.asStateFlow()

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
            _workerChats.update { list ->
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
