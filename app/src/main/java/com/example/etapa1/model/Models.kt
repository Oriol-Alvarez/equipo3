package com.example.etapa1.model

enum class UserRole(val label: String) {
    EDUCADORA("Educadora"),
    FAMILIAR("Familiar")
}

data class Room(
    val id: String,
    val name: String,
    val level: String,
    val presentCount: Int,
    val totalCount: Int
)

data class Child(
    val id: String,
    val fullName: String,
    val shortName: String,
    val ageText: String,
    val roomText: String,
    val groupText: String,
    val statusText: String,
    val isPresent: Boolean,
    val arrivalTime: String? = null,
    val allergyAlert: String? = null,
    val avatarInitials: String = "MG",
    val avatarBgColor: Long = 0xFF64B5F6
)

data class LostItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String? = null,
    val imageUri: String? = null,
    val room: String = "Sala 1A",
    val isClaimed: Boolean = false
)

enum class ActivityCategory(val label: String) {
    ALIMENTACION("Alimentación"),
    SIESTA("Siesta"),
    ESTADO_ANIMO("Estado de Ánimo"),
    OTROS("Otros")
}

enum class PortionOption(val label: String, val fractionText: String) {
    POCO("Poco", "1/3"),
    MEDIO("Medio", "1/2"),
    TODO("Todo", "1/1")
}

enum class MedicationPrescriptionOption(val label: String) {
    SI("Sí"),
    NO("No"),
    NO_APLICA("No aplica")
}

data class AttendanceRecord(
    val id: String = "att_${System.currentTimeMillis()}",
    val childId: String,
    val isIngreso: Boolean, // true = Ingreso (Entrada), false = Egreso (Salida)
    val date: String,
    val dayOfWeek: String, // L, M, M, J, V
    val time: String,
    val observations: String = ""
)

sealed interface TimelineItem {
    val id: String
    val time: String

    data class EventChip(
        override val id: String,
        override val time: String,
        val text: String,
        val iconType: String // "entry", "mood", "nap"
    ) : TimelineItem

    data class ChatMessage(
        override val id: String,
        override val time: String,
        val message: String,
        val isOutgoing: Boolean
    ) : TimelineItem

    data class ActivityCard(
        override val id: String,
        override val time: String,
        val category: String,
        val portionLabel: String,
        val description: String
    ) : TimelineItem
}

object MockDataRepository {
    val rooms = listOf(
        Room(
            id = "sala_1a",
            name = "Sala 1A",
            level = "Lactantes",
            presentCount = 12,
            totalCount = 15
        ),
        Room(
            id = "sala_2b",
            name = "Sala 2B",
            level = "Maternales A",
            presentCount = 8,
            totalCount = 12
        ),
        Room(
            id = "sala_3c",
            name = "Sala 3C",
            level = "Maternales B",
            presentCount = 10,
            totalCount = 14
        )
    )

    val mateoGarcia = Child(
        id = "mateo_garcia",
        fullName = "Mateo García",
        shortName = "Mateo G.",
        ageText = "2 años",
        roomText = "Sala 1A",
        groupText = "Grupo Sala 1A",
        statusText = "08:15 AM • Presente",
        isPresent = true,
        arrivalTime = "08:15 AM",
        allergyAlert = "Alergia a Nuez (No Certificado)",
        avatarInitials = "MG",
        avatarBgColor = 0xFFFFB74D
    )

    val childrenSala1A = listOf(
        mateoGarcia,
        Child(
            id = "sofia_lopez",
            fullName = "Sofía López",
            shortName = "Sofía L.",
            ageText = "1 año y 8 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:30 AM • Presente",
            isPresent = true,
            arrivalTime = "08:30 AM",
            allergyAlert = null,
            avatarInitials = "SL",
            avatarBgColor = 0xFF81C784
        ),
        Child(
            id = "lucas_martinez",
            fullName = "Lucas Martínez",
            shortName = "Lucas M.",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "No ha ingresado",
            isPresent = false,
            arrivalTime = null,
            allergyAlert = null,
            avatarInitials = "LM",
            avatarBgColor = 0xFF90A4AE
        ),
        Child(
            id = "valentina_ruiz",
            fullName = "Valentina Ruiz",
            shortName = "Valentina R.",
            ageText = "1 año y 11 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:20 AM • Presente",
            isPresent = true,
            arrivalTime = "08:20 AM",
            allergyAlert = null,
            avatarInitials = "VR",
            avatarBgColor = 0xFFBA68C8
        ),
        Child(
            id = "santiago_gomez",
            fullName = "Santiago Gómez",
            shortName = "Santiago G.",
            ageText = "2 años y 1 mes",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:05 AM • Presente",
            isPresent = true,
            arrivalTime = "08:05 AM",
            allergyAlert = null,
            avatarInitials = "SG",
            avatarBgColor = 0xFF4DD0E1
        ),
        Child(
            id = "camila_hernandez",
            fullName = "Camila Hernández",
            shortName = "Camila H.",
            ageText = "1 año y 9 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:10 AM • Presente",
            isPresent = true,
            arrivalTime = "08:10 AM",
            allergyAlert = null,
            avatarInitials = "CH",
            avatarBgColor = 0xFFF06292
        ),
        Child(
            id = "diego_fernandez",
            fullName = "Diego Fernández",
            shortName = "Diego F.",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:25 AM • Presente",
            isPresent = true,
            arrivalTime = "08:25 AM",
            allergyAlert = null,
            avatarInitials = "DF",
            avatarBgColor = 0xFF4FC3F7
        ),
        Child(
            id = "isabella_torres",
            fullName = "Isabella Torres",
            shortName = "Isabella T.",
            ageText = "1 año y 10 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:35 AM • Presente",
            isPresent = true,
            arrivalTime = "08:35 AM",
            allergyAlert = "Alergia a la Lactosa",
            avatarInitials = "IT",
            avatarBgColor = 0xFFAED581
        ),
        Child(
            id = "thiago_diaz",
            fullName = "Thiago Díaz",
            shortName = "Thiago D.",
            ageText = "2 años y 2 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:40 AM • Presente",
            isPresent = true,
            arrivalTime = "08:40 AM",
            allergyAlert = null,
            avatarInitials = "TD",
            avatarBgColor = 0xFFFFD54F
        ),
        Child(
            id = "emma_morales",
            fullName = "Emma Morales",
            shortName = "Emma M.",
            ageText = "1 año y 7 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:42 AM • Presente",
            isPresent = true,
            arrivalTime = "08:42 AM",
            allergyAlert = null,
            avatarInitials = "EM",
            avatarBgColor = 0xFFE57373
        ),
        Child(
            id = "leo_navarro",
            fullName = "Leo Navarro",
            shortName = "Leo N.",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:45 AM • Presente",
            isPresent = true,
            arrivalTime = "08:45 AM",
            allergyAlert = null,
            avatarInitials = "LN",
            avatarBgColor = 0xFF7986CB
        ),
        Child(
            id = "mia_ramirez",
            fullName = "Mia Ramírez",
            shortName = "Mia R.",
            ageText = "1 año y 11 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:50 AM • Presente",
            isPresent = true,
            arrivalTime = "08:50 AM",
            allergyAlert = null,
            avatarInitials = "MR",
            avatarBgColor = 0xFF4DB6AC
        ),
        Child(
            id = "gael_castro",
            fullName = "Gael Castro",
            shortName = "Gael C.",
            ageText = "2 años y 1 mes",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "08:55 AM • Presente",
            isPresent = true,
            arrivalTime = "08:55 AM",
            allergyAlert = null,
            avatarInitials = "GC",
            avatarBgColor = 0xFFFF8A65
        ),
        Child(
            id = "lucia_vargas",
            fullName = "Lucía Vargas",
            shortName = "Lucía V.",
            ageText = "1 año y 10 meses",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "Ausente",
            isPresent = false,
            arrivalTime = null,
            allergyAlert = null,
            avatarInitials = "LV",
            avatarBgColor = 0xFFA1887F
        ),
        Child(
            id = "bruno_benitez",
            fullName = "Bruno Benítez",
            shortName = "Bruno B.",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Grupo Sala 1A",
            statusText = "No ha ingresado",
            isPresent = false,
            arrivalTime = null,
            allergyAlert = null,
            avatarInitials = "BB",
            avatarBgColor = 0xFF9E9E9E
        )
    )

    fun getInitialMateoTimeline(): List<TimelineItem> = listOf(
        TimelineItem.EventChip(
            id = "chip_1",
            time = "08:15 AM",
            text = "Ingreso: 08:15 AM",
            iconType = "entry"
        ),
        TimelineItem.EventChip(
            id = "chip_2",
            time = "10:15 AM",
            text = "Estado: Feliz",
            iconType = "mood"
        ),
        TimelineItem.ChatMessage(
            id = "msg_1",
            time = "11:30 AM",
            message = "¡Buenos días! Muchas gracias por el aviso, ¿cómo fue su noche?",
            isOutgoing = false
        ),
        TimelineItem.ActivityCard(
            id = "act_1",
            time = "12:20 PM",
            category = "Alimentación",
            portionLabel = "Medio plato",
            description = "Rechazó el brócoli y le gustó mucho la sopa de verduras con pollo."
        ),
        TimelineItem.EventChip(
            id = "chip_3",
            time = "01:15 PM",
            text = "Siesta: 45 min",
            iconType = "nap"
        ),
        TimelineItem.ChatMessage(
            id = "msg_2",
            time = "01:30 PM",
            message = "Sí, tomó toda su leche. Ahora acaba de despertar de su siesta muy tranquilo.",
            isOutgoing = true
        )
    )

    fun getInitialLostItems(): List<LostItem> = listOf(
        LostItem(
            id = "lost_1",
            title = "Chaqueta Azul",
            description = "Chaqueta de invierno infantil, marca geoxrosa. Encontrada en la zona de recreo ayer por la tarde.",
            category = "Ropa",
            location = "Zona de recreo"
        ),
        LostItem(
            id = "lost_2",
            title = "Botella de Agua Roja",
            description = "Botella de agua de plástico rojo con tapa deportiva. Sin nombre marcado. Encontrada en el comedor.",
            category = "Otros",
            location = "Comedor"
        ),
        LostItem(
            id = "lost_3",
            title = "Oso de Peluche Marrón",
            description = "Pequeño oso de peluche marrón claro muy desgastado. Encontrado en la zona de descanso de los pequeños.",
            category = "Juguetes",
            location = "Zona de descanso"
        ),
        LostItem(
            id = "lost_4",
            title = "Zapatilla Rosa (Derecha)",
            description = "Zapatilla deportiva rosa con luces en la suela, talla 24. Encontrada cerca de la entrada principal.",
            category = "Ropa",
            location = "Entrada principal"
        )
    )
}

