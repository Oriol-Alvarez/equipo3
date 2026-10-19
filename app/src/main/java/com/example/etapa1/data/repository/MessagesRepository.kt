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
    fun markWorkerChatAsRead(chatId: String)
    fun markParentChatAsRead(childId: String)

    /** Crea un grupo de conversación del personal. */
    fun createWorkerGroup(title: String, memberIds: List<String>) {}

    /** Vuelve a pedir al servidor los chats (al iniciar sesión o al recargar). */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
