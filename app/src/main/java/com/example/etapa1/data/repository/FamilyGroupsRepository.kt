package com.example.etapa1.data.repository

import com.example.etapa1.model.FamilyGroupCandidate
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

    /**
     * Quién tiene la sesión iniciada según el servidor (id y nombre reales). La versión en memoria
     * devuelve null y la pantalla usa los participantes de ejemplo.
     */
    fun currentParticipant(): GroupParticipant? = null

    /** Familias que se pueden agregar a un grupo según el servidor; null en la versión en memoria. */
    val serverCandidates: StateFlow<List<FamilyGroupCandidate>>? get() = null

    /** Vuelve a cargar los grupos del servidor. En la versión en memoria no hace nada. */
    fun refresh() {}
    
    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
