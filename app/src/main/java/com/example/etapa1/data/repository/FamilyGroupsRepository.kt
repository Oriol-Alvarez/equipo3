package com.example.etapa1.data.repository

import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.GroupParticipant
import com.example.etapa1.model.MessageAttachment
import kotlinx.coroutines.flow.StateFlow

interface FamilyGroupsRepository {
    /** Todos los grupos, del más reciente al más antiguo según su última actividad. */
    val groups: StateFlow<List<FamilyGroupChat>>

    /**
     * Crea un grupo con las familias de los niños indicados.
     * Falla con un mensaje en español si falta el nombre o no hay familias.
     */
    fun createGroup(
        title: String,
        memberChildIds: List<String>,
        creator: GroupParticipant
    ): Result<FamilyGroupChat>

    /**
     * Envía texto, un adjunto o ambos. Devuelve false si el grupo no existe
     * o si el mensaje está vacío.
     */
    fun sendMessage(
        groupId: String,
        sender: GroupParticipant,
        text: String,
        attachment: MessageAttachment? = null
    ): Boolean
}
