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
    /** Vuelve a pedir los chats al servidor (al abrir la pantalla de mensajes). */
    fun recargar() = refresh()
    fun sendWorkerMessage(chatId: String, text: String)
    fun markWorkerChatAsRead(chatId: String)
    fun markParentChatAsRead(childId: String)

    /** Crea un grupo de conversación del personal. */
    fun createWorkerGroup(title: String, memberIds: List<String>) {}

    /** Vuelve a pedir al servidor los chats (al iniciar sesión o al recargar). */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}

    /** Abre (o reutiliza) un chat individual con alguien del personal. */
    fun startNewWorkerChat(staffMember: WorkerMember): WorkerChat

    /** Abre (o reutiliza) la conversación con la familia de un niño. */
    fun startNewParentChat(child: Child, parentName: String = "Familiar Tutor"): ParentChatSummary

    /** Familia: abre (o reutiliza) un chat con una educadora. */
    fun startNewFamilyEducatorChat(staffMember: WorkerMember): WorkerChat

    /** Pone el último mensaje de la bitácora de un niño en la lista de chats y la sube al inicio. */
    fun updateBitacoraChatMessage(childId: String, text: String, time: String, isOutgoing: Boolean)
}
