package com.example.etapa1.model

/**
 * Grupos de mensajes entre la educadora y las familias (estilo WhatsApp).
 *
 * Cada familia participa en un grupo a través de su hijo o hija: si el niño está en
 * [FamilyGroupChat.memberChildIds], su familia ve el grupo y puede escribir en él.
 * Los datos son simulados y viven solo mientras el proceso de la app está activo.
 */

/** Persona que escribe en un grupo: la educadora o una familia. */
data class GroupParticipant(
    val id: String,
    val displayName: String
)

/** Archivo o foto adjunta a un mensaje de grupo. */
data class MessageAttachment(
    val uri: String,
    val name: String?,
    val mimeType: String?
) {
    val isImage: Boolean get() = mimeType?.startsWith("image/") == true
}

data class FamilyGroupMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val time: String,
    val attachment: MessageAttachment? = null
)

data class FamilyGroupChat(
    val id: String,
    val title: String,
    val memberChildIds: List<String>,
    val createdById: String,
    val avatarBgColor: Long = 0xFF00897B,
    val messages: List<FamilyGroupMessage> = emptyList()
)

/** Familia que la educadora puede elegir al crear un grupo. */
data class FamilyGroupCandidate(
    val childId: String,
    val childName: String,
    val parentName: String,
    val avatarInitials: String,
    val avatarBgColor: Long
)

/** Quién está viendo los grupos; define qué grupos ve y con qué nombre escribe. */
sealed interface GroupViewer {
    val participant: GroupParticipant

    data class Educadora(override val participant: GroupParticipant) : GroupViewer

    data class Familia(
        override val participant: GroupParticipant,
        val childIds: List<String>
    ) : GroupViewer
}

object FamilyGroupsSampleData {

    val EDUCADORA = GroupParticipant(
        id = "educadora_sala_1a",
        displayName = "Educadora Sala 1A"
    )

    /** Familia con la que se inicia sesión en el perfil Familiar (padres de Mateo y Lucía). */
    val FAMILIA_GARCIA = GroupParticipant(
        id = "familia_garcia",
        displayName = "María López (Mamá de Mateo)"
    )

    fun initialGroups(): List<FamilyGroupChat> = listOf(
        FamilyGroupChat(
            id = "fgroup_paseo_sala_1a",
            title = "Paseo al parque",
            memberChildIds = listOf("mateo_garcia", "sofia_lopez", "lucas_martinez"),
            createdById = EDUCADORA.id,
            avatarBgColor = 0xFF00897B,
            messages = listOf(
                FamilyGroupMessage(
                    id = "fgm_1",
                    senderId = EDUCADORA.id,
                    senderName = EDUCADORA.displayName,
                    text = "¡Hola, familias! Creé este grupo para organizar el paseo del viernes.",
                    time = "09:00 AM"
                ),
                FamilyGroupMessage(
                    id = "fgm_2",
                    senderId = "familia_sofia_lopez",
                    senderName = "Marcos Rossi (Papá de Sofía)",
                    text = "Perfecto, ¿a qué hora salen?",
                    time = "09:12 AM"
                ),
                FamilyGroupMessage(
                    id = "fgm_3",
                    senderId = EDUCADORA.id,
                    senderName = EDUCADORA.displayName,
                    text = "Salimos a las 10:00 AM. Manden gorra y bloqueador, por favor.",
                    time = "09:25 AM"
                )
            )
        )
    )
}
