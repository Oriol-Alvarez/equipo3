package com.example.etapa1.ui.state

import com.example.etapa1.model.Child
import com.example.etapa1.model.FamilyGroupCandidate
import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.FamilyGroupMessage
import com.example.etapa1.model.GroupViewer
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage

/**
 * Reglas de presentación de los grupos con familias, sin dependencias de Android
 * para poder probarlas con pruebas unitarias.
 */
object FamilyGroupsPresenter {

    /** La educadora ve todos los grupos; una familia solo los que incluyen a alguno de sus hijos. */
    fun isVisibleTo(group: FamilyGroupChat, viewer: GroupViewer): Boolean = when (viewer) {
        is GroupViewer.Educadora -> true
        is GroupViewer.Familia -> group.memberChildIds.any { it in viewer.childIds }
    }

    fun groupsVisibleTo(groups: List<FamilyGroupChat>, viewer: GroupViewer): List<FamilyGroupChat> =
        groups.filter { isVisibleTo(it, viewer) }

    /** Solo la educadora crea grupos; las familias aparecen en los grupos donde las agregan. */
    fun canCreateGroups(viewer: GroupViewer): Boolean = viewer is GroupViewer.Educadora

    /** Familias de la sala a partir de los chats directos (incluye el nombre del papá o la mamá). */
    fun candidatesFrom(parentChats: List<ParentChatSummary>): List<FamilyGroupCandidate> =
        parentChats.map { summary ->
            FamilyGroupCandidate(
                childId = summary.child.id,
                childName = summary.child.fullName,
                parentName = summary.parentName,
                avatarInitials = summary.child.avatarInitials,
                avatarBgColor = summary.child.avatarBgColor
            )
        }

    /**
     * Convierte el grupo al formato de chat que ya usa la pantalla de mensajes
     * ([WorkerChat]), para reutilizar la lista y la conversación existentes.
     */
    fun toChat(
        group: FamilyGroupChat,
        viewer: GroupViewer,
        childrenById: Map<String, Child>
    ): WorkerChat {
        val viewerId = viewer.participant.id
        val childNames = group.memberChildIds.map { id ->
            childrenById[id]?.fullName?.substringBefore(" ") ?: id
        }
        val familiesText = if (group.memberChildIds.size == 1) "1 familia" else "${group.memberChildIds.size} familias"
        val last = group.messages.lastOrNull()

        return WorkerChat(
            id = group.id,
            title = group.title,
            subtitle = "$familiesText • ${childNames.joinToString(", ")}",
            lastMessage = last?.let { previewOf(it, viewerId) } ?: "Sin mensajes todavía",
            lastMessageTime = last?.time.orEmpty(),
            unreadCount = 0,
            isGlobal = false,
            isGroup = true,
            // Familias + la educadora
            membersCount = group.memberChildIds.size + 1,
            avatarInitials = group.title.take(2).uppercase(),
            avatarBgColor = group.avatarBgColor,
            messages = group.messages.map { toChatMessage(it, viewerId) }
        )
    }

    fun toChatMessage(message: FamilyGroupMessage, viewerId: String): WorkerChatMessage =
        WorkerChatMessage(
            id = message.id,
            senderId = message.senderId,
            senderName = message.senderName,
            message = message.text,
            time = message.time,
            isOutgoing = message.senderId == viewerId,
            fileUri = message.attachment?.uri,
            fileName = message.attachment?.name,
            fileMimeType = message.attachment?.mimeType
        )

    fun previewOf(message: FamilyGroupMessage, viewerId: String): String {
        val author = if (message.senderId == viewerId) "Tú" else message.senderName.substringBefore(" (")
        val body = when {
            message.text.isNotBlank() -> message.text
            message.attachment?.isImage == true -> "📷 Foto"
            message.attachment != null -> "📎 ${message.attachment.name ?: "Archivo"}"
            else -> ""
        }
        return "$author: $body"
    }

    /** Búsqueda por nombre del grupo, niños incluidos o último mensaje. */
    fun filterByQuery(chats: List<WorkerChat>, query: String): List<WorkerChat> {
        val q = query.trim()
        if (q.isEmpty()) return chats
        return chats.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.lastMessage.contains(q, ignoreCase = true)
        }
    }
}
