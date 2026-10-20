package com.example.etapa1.data.repository

import com.example.etapa1.model.Child
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerMember
import kotlinx.coroutines.flow.StateFlow

interface MessagesRepository {
    val workerChats: StateFlow<List<WorkerChat>>
    val parentChats: StateFlow<List<ParentChatSummary>>
    val familyChats: StateFlow<List<WorkerChat>>
    val staffMembers: StateFlow<List<WorkerMember>>
    val globalChat: StateFlow<WorkerChat>
    fun recargar()
    fun sendWorkerMessage(chatId: String, text: String)
    fun markWorkerChatAsRead(chatId: String)
    fun markParentChatAsRead(childId: String)
    fun startNewWorkerChat(staffMember: WorkerMember): WorkerChat
    fun startNewParentChat(child: Child, parentName: String = "Familiar Tutor"): ParentChatSummary
    fun startNewFamilyEducatorChat(staffMember: WorkerMember): WorkerChat
    fun updateBitacoraChatMessage(childId: String, text: String, time: String, isOutgoing: Boolean)
}
