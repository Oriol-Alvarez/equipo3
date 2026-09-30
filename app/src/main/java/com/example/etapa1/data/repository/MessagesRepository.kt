package com.example.etapa1.data.repository

import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerMember
import kotlinx.coroutines.flow.StateFlow

interface MessagesRepository {
    val workerChats: StateFlow<List<WorkerChat>>
    val parentChats: StateFlow<List<ParentChatSummary>>
    val staffMembers: StateFlow<List<WorkerMember>>
    val globalChat: StateFlow<WorkerChat>
    fun sendWorkerMessage(chatId: String, text: String)
}
