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
        val isOutgoing: Boolean,
        val fileUri: String? = null,
        val fileName: String? = null,
        val fileMimeType: String? = null
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

    val luciaGarcia = Child(
        id = "lucia_garcia",
        fullName = "Lucía García López",
        shortName = "Lucía G.",
        ageText = "1 año y 2 meses",
        roomText = "Sala Bebés",
        groupText = "Lactantes A",
        statusText = "08:45 AM • Presente",
        isPresent = true,
        arrivalTime = "08:45 AM",
        allergyAlert = null,
        avatarInitials = "LG",
        avatarBgColor = 0xFFF06292
    )

    val parentChildren: List<Child> = listOf(mateoGarcia, luciaGarcia)

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
                // Fechas aproximadas a la edad de cada ficha; se usan para los avisos de cumpleaños.
                "lucia_garcia" -> "22/07/2025"
                "santiago_gomez" -> "20/08/2024"
                "camila_hernandez" -> "12/12/2024"
                "diego_fernandez" -> "28/09/2024"
                "isabella_torres" -> "05/11/2024"
                "thiago_diaz" -> "15/07/2024"
                "emma_morales" -> "14/02/2025"
                "leo_navarro" -> "02/09/2024"
                "mia_ramirez" -> "09/10/2024"
                "gael_castro" -> "03/08/2024"
                "lucia_vargas" -> "21/11/2024"
                "bruno_benitez" -> "11/10/2024"
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
            pediatricianPhone = "+52 555 111 222",
            emergencyPhone = "+52 555 333 444",
            motherName = "María López Fernández",
            motherPhone = "+52 555 123 456",
            fatherName = "Carlos García Ramos",
            fatherPhone = "+52 555 987 654",
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

    fun getWeeklySummariesForChild(child: Child): List<WeeklySummary> {
        val allergyText = if (child.allergyAlert != null) {
            "Protocolo estricto para ${child.allergyAlert}. Menú especial de cocina cumplido al 100% sin ninguna reacción adversa durante la semana."
        } else {
            "Sin incidentes de salud ni alergias registradas. Temperatura y estado físico óptimos en todas las jornadas."
        }

        val currentWeek = WeeklySummary(
            weekLabel = "Semana actual",
            weekRangeText = "Semana del 22 al 26 de Septiembre, 2026",
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

        val previousWeek1 = WeeklySummary(
            weekLabel = "15 - 19 Sep",
            weekRangeText = "Semana del 15 al 19 de Septiembre, 2026",
            attendanceDaysCount = 5,
            totalSchoolDays = 5,
            attendancePercentageText = "100%",
            averageArrivalTime = "08:25 AM",
            foodIntakePercentage = "88%",
            totalNapHours = "5h 45m",
            averageNapDaily = "1h 09m/día",
            moodSummary = "Curioso y alegre",
            executiveSummary = "Durante la semana del 15 al 19 de septiembre, ${child.fullName} consolidó su integración al grupo. Mostró mucho entusiasmo en el taller de psicomotricidad gruesa y gateo/marcha, y participó con gran alegría en la asamblea de música. La relación con sus compañeros fue muy armónica.",
            foodHighlights = listOf(
                "Probó frutas de temporada en la merienda (plátano y compota de manzana con canela).",
                "Buen manejo del vaso de aprendizaje sin apenas derrames.",
                "Terminó por completo la ración de arroz con verduras y pavo."
            ),
            napHighlights = "Siestas regulares tras el almuerzo de 1h y 10m en promedio. Descanso profundo y sin despertares bruscos.",
            pedagogicalHighlights = listOf(
                "Circuito de psicomotricidad con colchonetas, rampa y túnel blando.",
                "Cuentacuentos participativo sobre animales de la granja y onomatopeyas.",
                "Iniciación al orden: ayuda entusiasmado a guardar los juguetes en sus cajas."
            ),
            healthAndSafetySummary = if (child.allergyAlert != null) "Control de ${child.allergyAlert} cumplido sin incidencias." else "Salud excelente, sin cuadros febriles ni síntomas."
        )

        val previousWeek2 = WeeklySummary(
            weekLabel = "8 - 12 Sep",
            weekRangeText = "Semana del 8 al 12 de Septiembre, 2026",
            attendanceDaysCount = 4,
            totalSchoolDays = 5,
            attendancePercentageText = "80%",
            averageArrivalTime = "08:35 AM",
            foodIntakePercentage = "84%",
            totalNapHours = "5h 15m",
            averageNapDaily = "1h 18m/día",
            moodSummary = "Adaptación progresiva",
            executiveSummary = "Semana de reingreso y adaptación al ritmo escolar. ${child.fullName} tuvo momentos iniciales de apego a la llegada el lunes, pero se tranquilizó rápidamente con los juegos de encaje y el afecto de las educadoras.",
            foodHighlights = listOf(
                "Comió muy bien purés y papillas; algo más selectivo con el pescado blanco.",
                "Buena hidratación ofrecida a lo largo de toda la jornada."
            ),
            napHighlights = "Le costó conciliar el sueño el primer día, pero a partir del miércoles durmió profundamente con su mantita.",
            pedagogicalHighlights = listOf(
                "Juegos de integración y bienvenida con canciones y títeres.",
                "Estimulación táctil con telas de diferentes texturas y pelotas sensoriales.",
                "Pintura con esponjas y témperas al agua hipoalergénicas."
            ),
            healthAndSafetySummary = "Una jornada de ausencia justificada por revisión pediátrica programada."
        )

        val previousWeek3 = WeeklySummary(
            weekLabel = "1 - 5 Sep",
            weekRangeText = "Semana del 1 al 5 de Septiembre, 2026",
            attendanceDaysCount = 5,
            totalSchoolDays = 5,
            attendancePercentageText = "100%",
            averageArrivalTime = "08:30 AM",
            foodIntakePercentage = "86%",
            totalNapHours = "5h 30m",
            averageNapDaily = "1h 06m/día",
            moodSummary = "Activo y comunicativo",
            executiveSummary = "Comienzo del ciclo escolar. ${child.fullName} demostró una gran curiosidad explorando cada rincón del aula, familiarizándose con sus percheros y disfrutando del patio de juegos.",
            foodHighlights = listOf(
                "Excelente aceptación de compotas naturales y legumbres suaves.",
                "Interés activo por usar la cuchara de forma autónoma."
            ),
            napHighlights = "Siestas en colchoneta individual con luz tenue y música relajante ambiental.",
            pedagogicalHighlights = listOf(
                "Conocimiento del aula y reconocimiento de su perchero y pertenencias.",
                "Juegos de coordinación visomotora con bloques apilables.",
                "Canciones infantiles de saludo y rutinas diarias."
            ),
            healthAndSafetySummary = "Ficha médica y cartilla de vacunación revisadas y archivadas en secretaría médica."
        )

        return listOf(currentWeek, previousWeek1, previousWeek2, previousWeek3)
    }

    fun getWeeklySummaryForChild(child: Child): WeeklySummary {
        return getWeeklySummariesForChild(child).first()
    }

    val staffMembers = listOf(
        WorkerMember("staff_1", "Laura Méndez", "Dirección General", "LM", 0xFF8E24AA, isOnline = true),
        WorkerMember("staff_2", "Carlos Ruiz", "Educador Lactantes", "CR", 0xFF0288D1, isOnline = true),
        WorkerMember("staff_3", "Dra. Elena Vega", "Servicio Médico / Pediatría", "EV", 0xFFD81B60, isOnline = false),
        WorkerMember("staff_4", "Marta Sánchez", "Coordinadora Pedagógica", "MS", 0xFF00897B, isOnline = true),
        WorkerMember("staff_5", "Lucía Martín", "Auxiliar Sala 1A", "LM", 0xFFFB8C00, isOnline = true),
        WorkerMember("staff_6", "Javier Ortiz", "Mantenimiento y Cocina", "JO", 0xFF546E7A, isOnline = false),
        WorkerMember("staff_7", "Sara Navarro", "Psicopedagoga", "SN", 0xFF3949AB, isOnline = true)
    )

    val initialGlobalWorkerChat = WorkerChat(
        id = "global_staff_chat",
        title = "Chat Global de Trabajadores",
        subtitle = "📌 Canal general del centro • 18 miembros",
        lastMessage = "Laura Méndez: Recordad registrar los partes de descanso y alimentación antes de las 14:30.",
        lastMessageTime = "12:45 PM",
        unreadCount = 2,
        isGlobal = true,
        isGroup = true,
        membersCount = 18,
        avatarInitials = "SC",
        avatarBgColor = 0xFF1565C0,
        messages = listOf(
            WorkerChatMessage("gm_1", "staff_1", "Laura Méndez", "Buenos días equipo. Hoy a las 16:30 reunión rápida de coordinación en sala polivalente.", "09:00 AM"),
            WorkerChatMessage("gm_2", "staff_2", "Carlos Ruiz", "Entendido Laura, Sala 1A tendrá listos los informes de avance.", "09:15 AM"),
            WorkerChatMessage("gm_3", "staff_3", "Dra. Elena Vega", "Recordatorio: Se han revisado las fichas de alergias de los nuevos ingresos de este mes.", "11:20 AM"),
            WorkerChatMessage("gm_4", "staff_1", "Laura Méndez", "Recordad registrar los partes de descanso y alimentación antes de las 14:30.", "12:45 PM")
        )
    )

    fun getInitialWorkerChats(): List<WorkerChat> = listOf(
        WorkerChat(
            id = "wchat_1",
            title = "Laura Méndez",
            subtitle = "Dirección General • En línea",
            lastMessage = "¿Cómo va la adaptación de Mateo García en el grupo?",
            lastMessageTime = "11:30 AM",
            unreadCount = 1,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "LM",
            avatarBgColor = 0xFF8E24AA,
            messages = listOf(
                WorkerChatMessage("wm_1", "staff_1", "Laura Méndez", "Hola Oriol, cuando tengas un momento confírmame si llegó la autorización médica de Mateo.", "10:15 AM"),
                WorkerChatMessage("wm_2", "me", "Yo", "Hola Laura, sí, la mamá la entregó a primera hora en secretaría.", "10:30 AM", isOutgoing = true),
                WorkerChatMessage("wm_3", "staff_1", "Laura Méndez", "¿Cómo va la adaptación de Mateo García en el grupo?", "11:30 AM")
            )
        ),
        WorkerChat(
            id = "wchat_2",
            title = "Carlos Ruiz",
            subtitle = "Educador Sala 2B • En línea",
            lastMessage = "Te dejé el material sensorial en el casillero común.",
            lastMessageTime = "10:15 AM",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "CR",
            avatarBgColor = 0xFF0288D1,
            messages = listOf(
                WorkerChatMessage("cm_1", "staff_2", "Carlos Ruiz", "Te dejé el material sensorial en el casillero común.", "10:15 AM")
            )
        ),
        WorkerChat(
            id = "wchat_3",
            title = "Dra. Elena Vega",
            subtitle = "Servicio Médico • Desconectada",
            lastMessage = "La revisión de Isabella Torres está lista, sin molestias.",
            lastMessageTime = "Ayer",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "EV",
            avatarBgColor = 0xFFD81B60,
            messages = listOf(
                WorkerChatMessage("em_1", "staff_3", "Dra. Elena Vega", "La revisión de Isabella Torres está lista, sin molestias.", "Ayer")
            )
        ),
        WorkerChat(
            id = "wchat_4",
            title = "Marta Sánchez",
            subtitle = "Coordinadora Pedagógica • En línea",
            lastMessage = "¿Revisaste el plan de psicomotricidad para el jueves?",
            lastMessageTime = "Ayer",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "MS",
            avatarBgColor = 0xFF00897B,
            messages = listOf(
                WorkerChatMessage("mm_1", "staff_4", "Marta Sánchez", "¿Revisaste el plan de psicomotricidad para el jueves?", "Ayer")
            )
        ),
        WorkerChat(
            id = "wchat_5",
            title = "Lucía Martín",
            subtitle = "Auxiliar Sala 1A • En línea",
            lastMessage = "Pañales y toallitas reposicionados en el cambiador.",
            lastMessageTime = "Lun",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "LM",
            avatarBgColor = 0xFFFB8C00,
            messages = listOf(
                WorkerChatMessage("lm_1", "staff_5", "Lucía Martín", "Pañales y toallitas reposicionados en el cambiador.", "Lun")
            )
        )
    )

    fun getInitialFamilyChats(): List<WorkerChat> = listOf(
        WorkerChat(
            id = "fchat_1",
            title = "Laura Gómez",
            subtitle = "Educadora Principal • Sala 1A",
            lastMessage = "Laura Gómez: Mateo comió muy bien hoy y descansó toda su siesta.",
            lastMessageTime = "02:15 PM",
            unreadCount = 1,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "LG",
            avatarBgColor = 0xFF8E24AA,
            messages = listOf(
                WorkerChatMessage("fm_1", "staff_educadora", "Laura Gómez", "¡Hola! Les comparto que Mateo tuvo un excelente día hoy.", "02:10 PM"),
                WorkerChatMessage("fm_2", "staff_educadora", "Laura Gómez", "Mateo comió muy bien hoy y descansó toda su siesta.", "02:15 PM")
            )
        ),
        WorkerChat(
            id = "fchat_2",
            title = "Dirección General",
            subtitle = "Laura Méndez • En línea",
            lastMessage = "Laura Méndez: Confirmada la recepción del documento de autorización.",
            lastMessageTime = "11:00 AM",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "DG",
            avatarBgColor = 0xFF1565C0,
            messages = listOf(
                WorkerChatMessage("fm_3", "staff_1", "Laura Méndez", "Confirmada la recepción del documento de autorización.", "11:00 AM")
            )
        ),
        WorkerChat(
            id = "fchat_3",
            title = "Servicio Médico",
            subtitle = "Dra. Elena Vega • Desconectada",
            lastMessage = "Dra. Elena Vega: Todo en orden con la ficha médica de Mateo.",
            lastMessageTime = "Ayer",
            unreadCount = 0,
            isGlobal = false,
            isGroup = false,
            avatarInitials = "SM",
            avatarBgColor = 0xFFD81B60,
            messages = listOf(
                WorkerChatMessage("fm_4", "staff_3", "Dra. Elena Vega", "Todo en orden con la ficha médica de Mateo.", "Ayer")
            )
        )
    )

    fun getInitialParentChats(): List<ParentChatSummary> = listOf(
        ParentChatSummary(
            child = mateoGarcia,
            parentName = "María López (Mamá)",
            lastMessage = "Mateo disfrutó mucho su comida de hoy y pidió un poco más de fruta.",
            lastMessageTime = "01:50 PM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[1],
            parentName = "Marcos Rossi (Papá)",
            lastMessage = "Siesta completa de 1h 15m. Se despertó de muy buen humor.",
            lastMessageTime = "01:15 PM",
            unreadCount = 1
        ),
        ParentChatSummary(
            child = childrenSala1A[2],
            parentName = "Andrea Benítez (Mamá)",
            lastMessage = "Ingreso registrado a las 08:00 AM con su padre.",
            lastMessageTime = "08:00 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[3],
            parentName = "Carmen Soto (Mamá)",
            lastMessage = "Actividad sensorial de pintura de dedos completada.",
            lastMessageTime = "11:45 AM",
            unreadCount = 2
        ),
        ParentChatSummary(
            child = childrenSala1A[4],
            parentName = "Jorge Hernández (Papá)",
            lastMessage = "Colación de la mañana consumida en su totalidad.",
            lastMessageTime = "10:30 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[5],
            parentName = "Laura Ramos (Mamá)",
            lastMessage = "Ingreso registrado con normalidad a las 08:25 AM.",
            lastMessageTime = "08:25 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[6],
            parentName = "Andrés Torres (Papá)",
            lastMessage = "⚠️ Menú adaptado sin lactosa aplicado con éxito.",
            lastMessageTime = "12:30 PM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[7],
            parentName = "Valentina Pérez (Mamá)",
            lastMessage = "Juegos matutinos en el patio y asamblea participativa.",
            lastMessageTime = "09:20 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[8],
            parentName = "Patricia Morales (Mamá)",
            lastMessage = "Ingreso a las 08:45 AM en perfectas condiciones.",
            lastMessageTime = "08:45 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[9],
            parentName = "Roberto Castro (Papá)",
            lastMessage = "Almorzó puré de verduras con buena aceptación.",
            lastMessageTime = "12:50 PM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[10],
            parentName = "Claudia Vargas (Mamá)",
            lastMessage = "Entrada puntual a las 08:55 AM.",
            lastMessageTime = "08:55 AM",
            unreadCount = 0
        ),
        ParentChatSummary(
            child = childrenSala1A[11],
            parentName = "Gabriel Ortiz (Papá)",
            lastMessage = "Siesta reparadora de 45 minutos.",
            lastMessageTime = "01:40 PM",
            unreadCount = 0
        )
    )

    fun getInitialAnnouncements(): List<Announcement> = listOf(
        Announcement(
            id = "ann_1",
            title = "Reunión de Padres y Madres - Inicio de Curso",
            date = "Hoy, 10:00 AM",
            summary = "Recordatorio sobre la reunión de este viernes en el salón principal.",
            fullContent = "Estimadas familias,\n\nLes recordamos que este viernes 2 de Octubre a las 18:00h tendremos nuestra primera reunión general de curso en el salón de usos múltiples.\n\nRepasaremos normativas, protocolos de seguridad y presentaremos las nuevas metodologías pedagógicas. ¡Esperamos contar con su presencia!",
            isUnread = true,
            author = "Dirección General"
        ),
        Announcement(
            id = "ann_2",
            title = "Menú Escolar de Octubre",
            date = "Ayer, 14:30 PM",
            summary = "Ya está disponible el menú escolar detallado para el próximo mes.",
            fullContent = "Queridas familias,\n\nEl equipo de nutrición ha preparado el menú escolar del mes de Octubre con ingredientes de temporada y enfoque en alimentación equilibrada. Pueden descargarlo desde la sección de Documentos o solicitar una copia física en secretaría.\n\nRecuerden que para los niños con alergias registradas, la adaptación se aplicará automáticamente.",
            isUnread = true,
            author = "Coordinación de Comedor"
        ),
        Announcement(
            id = "ann_3",
            title = "Campaña de Vacunación contra la Gripe",
            date = "25 Sep, 09:15 AM",
            summary = "Información sobre la próxima campaña de salud en el centro.",
            fullContent = "Estimados padres,\n\nEn colaboración con el centro de salud de la zona, la próxima semana iniciaremos la campaña voluntaria de vacunación antigripal infantil.\n\nSi desean que sus hijos sean vacunados en nuestro centro, por favor, entreguen la autorización médica firmada a sus respectivas educadoras antes del martes.",
            isUnread = false,
            author = "Servicio Médico"
        )
    )

    fun getInitialSuggestions(): List<ParentSuggestion> = listOf(
        ParentSuggestion(
            id = "sug_1",
            parentName = "Elena Ramos",
            childName = "Mateo García",
            roomName = "Sala 1A",
            date = "Hoy",
            time = "09:30 AM",
            category = SuggestionCategory.ALIMENTACION,
            subject = "Variedad de frutas frescas en merienda matutina",
            content = "Buenos días equipo. Nos gustaría proponer si es posible incluir mayor variedad de frutas de temporada (como plátano maduro o mandarina) en el tentempié de media mañana para los pequeños de Sala 1A.",
            status = SuggestionStatus.PENDIENTE
        ),
        ParentSuggestion(
            id = "sug_2",
            parentName = "Marcos López",
            childName = "Sofía López",
            roomName = "Sala 1A",
            date = "Ayer",
            time = "17:15 PM",
            category = SuggestionCategory.INSTALACIONES,
            subject = "Toldo de sombra para el arenero del patio",
            content = "Hola a la dirección. Notamos que en los días soleados la zona del arenero exterior recibe sol directo al mediodía. ¿Podría valorarse instalar una lona o vela de sombra para cuidar su piel mientras juegan?",
            status = SuggestionStatus.EN_REVISION,
            response = "Muchas gracias por la observación, Marcos. El área de mantenimiento ya está evaluando presupuesto y anclajes para instalarlo esta misma semana.",
            responseDate = "Ayer, 18:00 PM",
            responderName = "Laura Méndez (Dirección)"
        ),
        ParentSuggestion(
            id = "sug_3",
            parentName = "Claudia Torres",
            childName = "Lucas Martínez",
            roomName = "Sala 1A",
            date = "28 Sep",
            time = "14:20 PM",
            category = SuggestionCategory.ACTIVIDADES,
            subject = "Taller familiar de psicomotricidad o música",
            content = "Sería una experiencia fantástica poder compartir un taller abierto un viernes al mes donde los padres podamos participar en dinámicas de estimulación temprana junto a nuestros hijos.",
            status = SuggestionStatus.ATENDIDA,
            response = "¡Excelente iniciativa Claudia! Estamos coordinando el primer taller conjunto para el mes de octubre. Les enviaremos la circular con fechas.",
            responseDate = "28 Sep, 16:30 PM",
            responderName = "Marta Sánchez (Coord. Pedagógica)"
        ),
        ParentSuggestion(
            id = "sug_4",
            parentName = "Javier Ruiz",
            childName = "Valentina Ruiz",
            roomName = "Sala 1A",
            date = "25 Sep",
            time = "11:05 AM",
            category = SuggestionCategory.GENERAL,
            subject = "Campaña de marcado de prendas exteriores",
            content = "Sería muy útil enviar una recomendación general a las familias para marcar chaquetas y zapatos con nombres visibles, ya que a veces con el cambio de tiempo coinciden modelos similares.",
            status = SuggestionStatus.ATENDIDA,
            response = "Agradecemos la idea Javier. Se ha enviado un recordatorio formal a través del canal de avisos generales.",
            responseDate = "25 Sep, 12:00 PM",
            responderName = "Carlos Ruiz (Educador)"
        ),
        ParentSuggestion(
            id = "sug_5",
            parentName = "Patricia Vega",
            childName = "Santiago Gómez",
            roomName = "Sala 1A",
            date = "22 Sep",
            time = "08:45 AM",
            category = SuggestionCategory.HORARIOS,
            subject = "Apertura anticipada de acceso con lluvia",
            content = "En días de lluvia intensa el acceso principal se congestiona a la hora pico de salida de carritos. ¿Podría habilitarse la puerta lateral 10 minutos antes?",
            status = SuggestionStatus.PENDIENTE
        )
    )
}

enum class SuggestionCategory(val label: String) {
    TODAS("Todas"),
    ALIMENTACION("Alimentación"),
    INSTALACIONES("Instalaciones"),
    ACTIVIDADES("Actividades"),
    HORARIOS("Horarios"),
    GENERAL("General")
}

enum class SuggestionStatus(val label: String, val color: Long, val bgColor: Long) {
    PENDIENTE("Pendiente", 0xFFE65100, 0xFFFFF3E0),
    EN_REVISION("En proceso", 0xFF0288D1, 0xFFE1F5FE),
    ATENDIDA("Atendida", 0xFF2E7D32, 0xFFE8F5E9)
}

data class ParentSuggestion(
    val id: String,
    val parentName: String,
    val childName: String,
    val roomName: String,
    val date: String,
    val time: String,
    val category: SuggestionCategory,
    val subject: String,
    val content: String,
    val status: SuggestionStatus = SuggestionStatus.PENDIENTE,
    val response: String? = null,
    val responseDate: String? = null,
    val responderName: String? = null
)

data class WorkerMember(
    val id: String,
    val name: String,
    val role: String,
    val avatarInitials: String,
    val avatarBgColor: Long,
    val isOnline: Boolean = true
)

data class WorkerChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val time: String,
    val isOutgoing: Boolean = false,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null
)

data class WorkerChat(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isGlobal: Boolean = false,
    val isGroup: Boolean = false,
    val membersCount: Int = 2,
    val avatarInitials: String = "SC",
    val avatarBgColor: Long = 0xFF1976D2,
    val messages: List<WorkerChatMessage> = emptyList()
)

data class ParentChatSummary(
    val child: Child,
    val parentName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0
)

data class Announcement(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val fullContent: String,
    val isUnread: Boolean = true,
    val author: String = "Dirección General"
)

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
    val pediatricianPhone: String = "+34 555 111 222",
    val emergencyPhone: String = "+34 555 333 444",
    val motherName: String = "María López Fernández",
    val motherPhone: String = "+34 555 123 456",
    val fatherName: String = "Carlos García Ramos",
    val fatherPhone: String = "+34 555 987 654",
    val authorizedPickups: List<String> = listOf("Rosa Fernández (Abuela)", "Elena García (Tía)"),
    val medicalNotes: String = "Vacunación al día. No presenta intolerancias alimentarias adicionales.",
    val generalNotes: String = "Se adapta con facilidad a las rutinas de la sala. Duerme con su mantita en la siesta.",
    val medicalCertificateUri: String? = null,
    val medicalCertificateName: String? = null,
    val medicalCertificateDate: String? = null
)

data class WeeklySummary(
    val weekLabel: String = "Semana actual",
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



