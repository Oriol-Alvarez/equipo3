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
    DESCANSO("Descanso"),
    FUNCIONES_EXCRETORAS("Funciones Excretoras"),
    ESTADO_ANIMO("Estado de Ánimo"),
    ACCIDENTES("Accidentes"),
    SALUD("Salud"),
    SIESTA("Descanso"),
    OTROS("Otros")
}

enum class MealType(val label: String) {
    DESAYUNO("Desayuno"),
    COLACION("Colación"),
    COMIDA("Comida")
}

enum class FoodConsumptionOption(val label: String) {
    NADA("Nada"),
    POCO("Poco"),
    TODO("Todo"),
    PIDIO_MAS("Pidió más")
}

enum class MoodState(val label: String, val emoji: String) {
    FELIZ("Feliz", "🙂"),
    TRISTE("Triste", "😢"),
    ENOJADO("Enojado", "😠")
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

    fun getChildFullProfile(child: Child): ChildFullProfile {
        return ChildFullProfile(
            child = child,
            birthDate = when (child.id) {
                "mateo_garcia" -> "14/04/2024"
                "sofia_lopez" -> "18/06/2024"
                "lucas_martinez" -> "02/02/2024"
                "valentina_ruiz" -> "25/03/2024"
                else -> "10/05/2024"
            },
            studentId = "EST-${child.id.take(4).uppercase()}-2026",
            bloodType = when (child.id) {
                "mateo_garcia" -> "O+"
                "sofia_lopez" -> "A+"
                "lucas_martinez" -> "B+"
                else -> "O+"
            },
            pediatrician = "Dra. Sofía Morales",
            pediatricianPhone = "+34 912 334 455",
            emergencyPhone = "+34 612 345 678",
            motherName = "María López Fernández",
            motherPhone = "+34 612 345 678",
            fatherName = "Carlos García Ramos",
            fatherPhone = "+34 611 987 654",
            authorizedPickups = listOf(
                "Rosa Fernández (Abuela materna) - DNI ***4521",
                "Elena García (Tía paterna) - DNI ***9812"
            ),
            medicalNotes = if (child.allergyAlert != null) {
                "${child.allergyAlert}. Se requiere extremo cuidado en las meriendas y almuerzos. Botiquín de emergencia revisado."
            } else {
                "Sin alergias diagnosticadas. Cartilla de vacunación completa y verificada por el centro."
            },
            generalNotes = "Muy sociable y participativo. Se adapta muy bien a las rutinas de la sala y duerme tranquilo con su objeto de apego."
        )
    }

    fun getDailyBitacorasForChild(child: Child): List<DailyBitacora> {
        return listOf(
            DailyBitacora(
                date = "29/09/2026",
                dayLabel = "Hoy, 29 Sep",
                isToday = true,
                ingreso = if (child.isPresent) {
                    DailyIngresoInfo(
                        time = child.arrivalTime ?: "08:15 AM",
                        delivererName = "María López (Madre)",
                        receiverName = "Laura Gómez (Educadora)",
                        physicalCondition = "Buen estado general, alegre y sin lesiones.",
                        hasSymptoms = false,
                        observations = "Durmió 9 horas continuas. Desayunó un tazón de leche en casa."
                    )
                } else null,
                egreso = null,
                activities = if (child.isPresent) {
                    listOf(
                        DailyActivityInfo(
                            id = "act_today_1",
                            time = "09:30 AM",
                            title = "Asamblea y cantos matutinos",
                            category = ActivityCategory.ESTADO_ANIMO,
                            description = "Participó entusiasmado en el círculo de bienvenida e interactuó amistosamente con sus compañeros."
                        ),
                        DailyActivityInfo(
                            id = "act_today_2",
                            time = "10:30 AM",
                            title = "Merienda matutina",
                            category = ActivityCategory.ALIMENTACION,
                            portion = PortionOption.TODO,
                            description = "Comió toda su porción de plátano y pera cortada. Tomó agua con autonomía."
                        ),
                        DailyActivityInfo(
                            id = "act_today_3",
                            time = "12:15 PM",
                            title = "Almuerzo - Menú del día",
                            category = ActivityCategory.ALIMENTACION,
                            portion = PortionOption.MEDIO,
                            description = "Tomó la crema de calabaza completa. Del plato de pollo con patatas consumió la mitad."
                        ),
                        DailyActivityInfo(
                            id = "act_today_4",
                            time = "13:00 - 14:15 PM",
                            title = "Siesta y descanso",
                            category = ActivityCategory.SIESTA,
                            description = "Descanso reparador de 1 hora y 15 minutos en su cuna habitual sin interrupciones."
                        )
                    )
                } else emptyList()
            ),
            DailyBitacora(
                date = "28/09/2026",
                dayLabel = "Ayer, 28 Sep",
                isToday = false,
                ingreso = DailyIngresoInfo(
                    time = "08:20 AM",
                    delivererName = "Carlos García (Padre)",
                    receiverName = "Laura Gómez (Educadora)",
                    physicalCondition = "Buen estado físico, sin rasguños ni temperatura.",
                    hasSymptoms = false,
                    observations = "Trae ropa de recambio limpia en su mochila."
                ),
                egreso = DailyEgresoInfo(
                    time = "16:30 PM",
                    collectorName = "María López (Madre)",
                    delivererName = "Laura Gómez (Educadora)",
                    physicalCondition = "Excelente estado general, aseado y tranquilo.",
                    observations = "Jornada muy agradable. Merendó compota de frutas completa a las 15:45."
                ),
                activities = listOf(
                    DailyActivityInfo(
                        id = "act_yest_1",
                        time = "10:15 AM",
                        title = "Juego sensorial y psicomotricidad",
                        category = ActivityCategory.OTROS,
                        description = "Exploró texturas con plastilina no tóxica y armó torres de bloques de madera."
                    ),
                    DailyActivityInfo(
                        id = "act_yest_2",
                        time = "12:30 PM",
                        title = "Almuerzo - Lentejas con arroz",
                        category = ActivityCategory.ALIMENTACION,
                        portion = PortionOption.TODO,
                        description = "Almorzó lentejas con arroz y puré de zanahorias. Comió todo el plato con gran apetito."
                    ),
                    DailyActivityInfo(
                        id = "act_yest_3",
                        time = "13:10 - 14:30 PM",
                        title = "Siesta",
                        category = ActivityCategory.SIESTA,
                        description = "Descanso reparador de 1 hora y 20 minutos."
                    ),
                    DailyActivityInfo(
                        id = "act_yest_4",
                        time = "15:45 PM",
                        title = "Merienda de la tarde",
                        category = ActivityCategory.ALIMENTACION,
                        portion = PortionOption.TODO,
                        description = "Yogur natural con cereales blandos y vaso de agua."
                    )
                )
            ),
            DailyBitacora(
                date = "25/09/2026",
                dayLabel = "Viernes, 25 Sep",
                isToday = false,
                ingreso = DailyIngresoInfo(
                    time = "08:10 AM",
                    delivererName = "Rosa Fernández (Abuela)",
                    receiverName = "Laura Gómez (Educadora)",
                    physicalCondition = "Buen estado general.",
                    hasSymptoms = false,
                    observations = "Entregó autorización médica para actividades al aire libre."
                ),
                egreso = DailyEgresoInfo(
                    time = "16:25 PM",
                    collectorName = "Carlos García (Padre)",
                    delivererName = "Laura Gómez (Educadora)",
                    physicalCondition = "Entregado en perfecto estado, despierto y sonriente.",
                    observations = "Lleva en su mochila los dibujos y manualidades de la semana."
                ),
                activities = listOf(
                    DailyActivityInfo(
                        id = "act_fri_1",
                        time = "11:00 AM",
                        title = "Taller de pintura con dedos",
                        category = ActivityCategory.ESTADO_ANIMO,
                        description = "Pintó con pintura al agua no tóxica sobre mural de papel continuo."
                    ),
                    DailyActivityInfo(
                        id = "act_fri_2",
                        time = "12:20 PM",
                        title = "Almuerzo - Menú de pescado",
                        category = ActivityCategory.ALIMENTACION,
                        portion = PortionOption.TODO,
                        description = "Pescado blanco al vapor con patatas hervidas y calabacín. Comió todo sin problema."
                    ),
                    DailyActivityInfo(
                        id = "act_fri_3",
                        time = "13:00 - 14:10 PM",
                        title = "Siesta",
                        category = ActivityCategory.SIESTA,
                        description = "Siesta tranquila de 1 hora y 10 minutos."
                    )
                )
            )
        )
    }

    fun getWeeklySummaryForChild(child: Child): WeeklySummary {
        val allergyText = if (child.allergyAlert != null) {
            "Protocolo estricto para ${child.allergyAlert}. Menú especial de cocina cumplido al 100% sin ninguna reacción adversa durante la semana."
        } else {
            "Sin incidentes de salud ni alergias registradas. Temperatura y estado físico óptimos en todas las jornadas."
        }

        return WeeklySummary(
            weekRangeText = "Semana del 22 al 29 de Septiembre, 2026",
            attendanceDaysCount = if (child.isPresent) 5 else 4,
            totalSchoolDays = 5,
            attendancePercentageText = if (child.isPresent) "100%" else "80%",
            averageArrivalTime = child.arrivalTime ?: "08:20 AM",
            foodIntakePercentage = if (child.isPresent) "92%" else "85%",
            totalNapHours = "6h 15m",
            averageNapDaily = "1h 15m/día",
            moodSummary = if (child.isPresent) "Muy participativo y sociable" else "Tranquilo y afectuoso",
            executiveSummary = "Semana sumamente positiva para ${child.fullName}. Ha mantenido una asistencia constante con horario puntual de ingreso. En alimentación mostró gran apetito y excelente autonomía con los cubiertos. En el descanso mantuvo siestas continuas de más de una hora. Su interacción con educadoras y compañeros en actividades sensoriales fue sobresaliente.",
            foodHighlights = listOf(
                "Gran aceptación en cremas de verduras, sopa de pollo con fideos y lentejas con arroz.",
                "Excelente autonomía: come sin ayuda y pide agua con naturalidad cuando la necesita.",
                "Oportunidad de refuerzo: Introducción gradual de verduras enteras (rechazó el brócoli hervido, aunque lo tomó en puré)."
            ),
            napHighlights = "Ritmo biológico de descanso muy estable. Siesta diaria entre las 13:00 y las 14:15 h. Duerme tranquilo con su objeto de apego y despierta con excelente disposición.",
            pedagogicalHighlights = listOf(
                "Motricidad fina: Modelado con plastilina y construcción de torres de bloques de madera.",
                "Sensorial y creativo: Taller de pintura con dedos sobre mural grande y exploración de texturas.",
                "Socioemocional: Participación activa en las canciones de asamblea matutina y juego cooperativo en patio."
            ),
            healthAndSafetySummary = allergyText
        )
    }
}

data class DailyBitacora(
    val date: String,
    val dayLabel: String,
    val isToday: Boolean = false,
    val ingreso: DailyIngresoInfo?,
    val egreso: DailyEgresoInfo?,
    val activities: List<DailyActivityInfo>
)

data class DailyIngresoInfo(
    val time: String,
    val delivererName: String,
    val receiverName: String,
    val physicalCondition: String,
    val hasSymptoms: Boolean,
    val symptomsDetail: String = "",
    val observations: String = ""
)

data class DailyEgresoInfo(
    val time: String,
    val collectorName: String,
    val delivererName: String,
    val physicalCondition: String,
    val observations: String = ""
)

data class DailyActivityInfo(
    val id: String,
    val time: String,
    val title: String,
    val category: ActivityCategory,
    val portion: PortionOption? = null,
    val description: String
)

data class ChildFullProfile(
    val child: Child,
    val birthDate: String = "12/03/2024",
    val enrollmentDate: String = "01/09/2025",
    val studentId: String = "AL-2025-042",
    val bloodType: String = "A+",
    val pediatrician: String = "Dra. Sofía Morales",
    val pediatricianPhone: String = "+34 912 334 455",
    val emergencyPhone: String = "+34 612 345 678",
    val motherName: String = "María López Fernández",
    val motherPhone: String = "+34 612 345 678",
    val fatherName: String = "Carlos García Ramos",
    val fatherPhone: String = "+34 611 987 654",
    val authorizedPickups: List<String> = listOf("Rosa Fernández (Abuela)", "Elena García (Tía)"),
    val medicalNotes: String = "Vacunación al día. No presenta intolerancias alimentarias adicionales.",
    val generalNotes: String = "Se adapta con facilidad a las rutinas de la sala. Duerme con su mantita en la siesta."
)

data class WeeklySummary(
    val weekRangeText: String = "Semana del 22 al 29 de Septiembre, 2026",
    val attendanceDaysCount: Int = 5,
    val totalSchoolDays: Int = 5,
    val attendancePercentageText: String = "100%",
    val averageArrivalTime: String = "08:18 AM",
    val foodIntakePercentage: String = "92%",
    val totalNapHours: String = "6h 15m",
    val averageNapDaily: String = "1h 15m/día",
    val moodSummary: String = "Feliz y participativo",
    val executiveSummary: String,
    val foodHighlights: List<String>,
    val napHighlights: String,
    val pedagogicalHighlights: List<String>,
    val healthAndSafetySummary: String
)



