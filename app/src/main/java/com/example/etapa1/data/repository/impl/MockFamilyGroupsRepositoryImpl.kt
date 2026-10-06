package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.FamilyGroupMessage
import com.example.etapa1.model.FamilyGroupsSampleData
import com.example.etapa1.model.GroupParticipant
import com.example.etapa1.model.MessageAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementación en memoria: los grupos se pierden al cerrar el proceso de la app.
 * [currentTime] se puede reemplazar en pruebas para obtener horas fijas.
 */
class MockFamilyGroupsRepositoryImpl(
    initialGroups: List<FamilyGroupChat> = FamilyGroupsSampleData.initialGroups(),
    private val currentTime: () -> String = {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }
) : FamilyGroupsRepository {

    private val _groups = MutableStateFlow(initialGroups)
    override val groups: StateFlow<List<FamilyGroupChat>> = _groups.asStateFlow()

    private var idCounter = 0

    private fun nextId(prefix: String): String {
        idCounter += 1
        return "${prefix}_${System.currentTimeMillis()}_$idCounter"
    }

    override fun createGroup(
        title: String,
        memberChildIds: List<String>,
        creator: GroupParticipant
    ): Result<FamilyGroupChat> {
        val cleanTitle = title.trim()
        val members = memberChildIds.distinct()

        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe un nombre para el grupo."))
        }
        if (cleanTitle.length > MAX_TITLE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("El nombre del grupo puede tener hasta $MAX_TITLE_LENGTH caracteres.")
            )
        }
        if (members.isEmpty()) {
            return Result.failure(IllegalArgumentException("Elige al menos una familia para el grupo."))
        }

        val group = FamilyGroupChat(
            id = nextId("fgroup"),
            title = cleanTitle,
            memberChildIds = members,
            createdById = creator.id,
            avatarBgColor = AVATAR_COLORS[_groups.value.size % AVATAR_COLORS.size],
            messages = listOf(
                FamilyGroupMessage(
                    id = nextId("fgm"),
                    senderId = creator.id,
                    senderName = creator.displayName,
                    text = "Grupo «$cleanTitle» creado.",
                    time = currentTime()
                )
            )
        )
        _groups.update { listOf(group) + it }
        return Result.success(group)
    }

    override fun sendMessage(
        groupId: String,
        sender: GroupParticipant,
        text: String,
        attachment: MessageAttachment?
    ): Boolean {
        val cleanText = text.trim()
        if (cleanText.isEmpty() && attachment == null) return false
        val group = _groups.value.find { it.id == groupId } ?: return false

        val message = FamilyGroupMessage(
            id = nextId("fgm"),
            senderId = sender.id,
            senderName = sender.displayName,
            text = cleanText,
            time = currentTime(),
            attachment = attachment
        )
        val updated = group.copy(messages = group.messages + message)
        // Igual que en WhatsApp: el grupo con actividad reciente sube al inicio.
        _groups.update { list -> listOf(updated) + list.filter { it.id != groupId } }
        return true
    }

    companion object {
        const val MAX_TITLE_LENGTH = 40

        private val AVATAR_COLORS = listOf(
            0xFF00897B, 0xFF5E35B1, 0xFFD81B60, 0xFF0288D1, 0xFFFB8C00, 0xFF43A047
        )
    }
}
