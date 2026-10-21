package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.AdminRepository
import com.example.etapa1.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MockAdminRepositoryImpl : AdminRepository {

    private val centerChildrenPool = mutableListOf(
        AdminChildSummary(
            id = "mateo_garcia",
            fullName = "Mateo García López",
            shortName = "Mateo",
            ageText = "2 años",
            roomText = "Sala 1A",
            groupText = "Maternal A",
            isPresent = true,
            statusText = "Presente",
            allergyAlert = null,
            studentId = "CURP-GARM240414HDFRLA01",
            bloodType = "O+",
            pediatrician = "Dra. Sofía Morales (Hospital Infantil)",
            pediatricianPhone = "+52 55 1234 5678",
            emergencyPhone = "+52 55 9876 5432",
            emergencyPhoneRelation = "Abuela materna (Rosa)",
            motherName = "María López Fernández",
            motherPhone = "+52 55 1234 5678",
            fatherName = "Carlos García Ramos",
            fatherPhone = "+52 55 3333 4444",
            authorizedPickups = listOf("Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555", "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444"),
            medicalNotes = "Esquema de vacunación completo conforme a Cartilla Nacional de Salud al día. Sin alergias registradas.",
            generalNotes = "Muy sociable y participativo. Se adapta muy bien a las rutinas.",
            habitsNotes = "Duerme con su mantita de apego."
        ),
        AdminChildSummary(
            id = "lucia_garcia",
            fullName = "Lucía García López",
            shortName = "Lucía",
            ageText = "1 año y 2 meses",
            roomText = "Sala 1A",
            groupText = "Lactantes A",
            isPresent = true,
            statusText = "Presente",
            studentId = "CURP-GARL250810MDFRLA02",
            bloodType = "O+",
            pediatrician = "Dra. Sofía Morales (Hospital Infantil)",
            pediatricianPhone = "+52 55 1234 5678",
            emergencyPhone = "+52 55 9876 5432",
            emergencyPhoneRelation = "Abuela materna (Rosa)",
            motherName = "María López Fernández",
            motherPhone = "+52 55 1234 5678",
            fatherName = "Carlos García Ramos",
            fatherPhone = "+52 55 3333 4444",
            authorizedPickups = listOf("Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555", "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444"),
            medicalNotes = "Sin alergias conocidas. Calendario de vacunación al día.",
            generalNotes = "Comenzando a dar sus primeros pasos de forma alegre.",
            habitsNotes = "Toma biberón de 180ml a media mañana."
        ),
        AdminChildSummary(
            id = "lucas_martinez",
            fullName = "Lucas Martínez Sánchez",
            shortName = "Lucas",
            ageText = "2 años",
            roomText = "Sala 2B",
            groupText = "Maternal B",
            isPresent = false,
            statusText = "No ha ingresado",
            allergyAlert = null,
            studentId = "CURP-MASL240222HDFRSN03",
            bloodType = "A+",
            pediatrician = "Dr. Manuel Benítez (Clínica Pedregal)",
            pediatricianPhone = "+52 55 2222 3333",
            emergencyPhone = "+52 55 8888 9999",
            emergencyPhoneRelation = "Tío (Javier)",
            motherName = "Elena Sánchez Gil",
            motherPhone = "+52 55 7777 8888",
            fatherName = "Roberto Martínez Ruiz",
            fatherPhone = "+52 55 6666 7777",
            authorizedPickups = listOf("Elena Sánchez Gil (Madre) - CURP: SAGE850918MDFLNN03 • Tel: +52 55 7777 8888", "Roberto Martínez Ruiz (Padre) - CURP: MARR830422HDFRRB04 • Tel: +52 55 6666 7777"),
            medicalNotes = "Intolerancia leve a la lactosa (sustituir lácteos enteros por leche vegetal o deslactosada). Cartilla Nacional al día.",
            generalNotes = "Le gusta la música y construir torres de bloques.",
            habitsNotes = "Usa chupete únicamente para conciliar el sueño."
        ),
        AdminChildSummary(
            id = "sofia_lopez",
            fullName = "Sofía López Gómez",
            shortName = "Sofía",
            ageText = "1 año y 8 meses",
            roomText = "Sala 1A",
            groupText = "Maternal A",
            isPresent = true,
            statusText = "Presente",
            studentId = "CURP-LOGS240515MDFPZ04",
            bloodType = "B+",
            pediatrician = "Dra. Elena Castro (Hospital Ángeles)",
            pediatricianPhone = "+52 55 3333 4444",
            emergencyPhone = "+52 55 1212 3434",
            emergencyPhoneRelation = "Abuelo (Fernando)",
            motherName = "Ana López Navarro",
            motherPhone = "+52 55 5656 7878",
            fatherName = "David Gómez Prieto",
            fatherPhone = "+52 55 4343 2121",
            authorizedPickups = listOf("Ana López Navarro (Madre) - CURP: LONA861105MDFPRD05 • Tel: +52 55 5656 7878", "Fernando López (Abuelo) - CURP: LOPF580214HDFRRN06 • Tel: +52 55 1212 3434"),
            medicalNotes = "Salud general óptima. Esquema de vacunación nacional al día. Sin alergias registradas.",
            generalNotes = "Muy sociable y curiosa, disfruta de las actividades de dibujo.",
            habitsNotes = "Duerme siesta regular de 13:30 a 15:00."
        )
    )

    private val initialUsers = mutableListOf(
        AdminUser("usr_dir_mendez", "direccion@sonrisas.edu", "Laura Méndez (Dirección)", "DIRECCION", "+52 55 1234 0001"),
        AdminUser("usr_educadora_laura", "educadora@sonrisas.edu", "Laura Gómez (Educadora)", "EDUCADORA", "+52 55 1234 0002"),
        AdminUser("usr_educador_carlos", "carlos@sonrisas.edu", "Carlos Ruiz (Educador)", "EDUCADORA", "+52 55 1234 0003"),
        AdminUser(
            id = "usr_familia_garcia",
            email = "familiar@sonrisas.edu",
            displayName = "María López Fernández",
            role = "FAMILIAR",
            phone = "+52 55 1234 5678",
            children = listOf(centerChildrenPool[0], centerChildrenPool[1])
        ),
        AdminUser(
            id = "usr_familia_martinez",
            email = "martinez@sonrisas.edu",
            displayName = "Roberto Martínez Ruiz",
            role = "FAMILIAR",
            phone = "+52 55 8765 4321",
            children = listOf(centerChildrenPool[2])
        ),
        AdminUser(
            id = "usr_familia_lopez",
            email = "lopez@sonrisas.edu",
            displayName = "Ana López Navarro",
            role = "FAMILIAR",
            phone = "+52 55 5678 1234",
            children = listOf(centerChildrenPool[3])
        )
    )

    private val _users = MutableStateFlow<List<AdminUser>>(initialUsers)
    override val users: StateFlow<List<AdminUser>> = _users.asStateFlow()

    private val _announcements = MockDataRepository.sharedAnnouncements
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _suggestions = MutableStateFlow<List<ParentSuggestion>>(MockDataRepository.getInitialSuggestions())
    override val suggestions: StateFlow<List<ParentSuggestion>> = _suggestions.asStateFlow()

    private val _lostObjects = MutableStateFlow<List<LostItem>>(MockDataRepository.getInitialLostItems())
    override val lostObjects: StateFlow<List<LostItem>> = _lostObjects.asStateFlow()

    private val initialLogs = mutableListOf(
        AdminLog("l_1", "Hoy, 08:15 AM", "ASISTENCIA", "Ingreso al Centro", "Alumno Mateo García ingresó en buen estado. Entregó: María López. Recibió: Laura Gómez.", "Laura Gómez", "EDUCADORA", "Mateo García", "Sala 1A"),
        AdminLog("l_2", "Hoy, 08:22 AM", "ASISTENCIA", "Ingreso al Centro", "Alumno Sofía Hernández ingresó puntual. Recibió: Laura Gómez.", "Laura Gómez", "EDUCADORA", "Sofía Hernández", "Sala 1A"),
        AdminLog("l_3", "Hoy, 09:30 AM", "ACTIVIDAD", "Desayuno: Fruta y Papilla", "Mateo consumió la totalidad de su porción de avena y manzana al vapor.", "Laura Gómez", "EDUCADORA", "Mateo García", "Sala 1A"),
        AdminLog("l_4", "Hoy, 10:15 AM", "MENSAJE", "Mensaje en Bitácora (Mateo García)", "¿Podrían recordarle tomar su agua tibia a las 11:00 AM? Muchas gracias.", "María López", "FAMILIAR", "Mateo García", "Sala 1A"),
        AdminLog("l_5", "Hoy, 11:30 AM", "ACTIVIDAD", "Control de Esfínteres", "Cambio de pañal limpio realizado sin irritaciones observadas.", "Laura Gómez", "EDUCADORA", "Mateo García", "Sala 1A"),
        AdminLog("l_6", "Hoy, 12:45 PM", "ACTIVIDAD", "Descanso / Siesta", "Durmió 1 hora y 15 minutos de forma tranquila y continua.", "Laura Gómez", "EDUCADORA", "Mateo García", "Sala 1A"),
        AdminLog("l_7", "Hoy, 01:10 PM", "AVISO", "Aviso Escolar: Festival de Primavera", "Se envió circular a todas las familias sobre la vestimenta para el viernes.", "Dirección General", "DIRECCION", null, "Todo el Centro"),
        AdminLog("l_8", "Hoy, 01:45 PM", "SUGERENCIA", "Sugerencia en Revisión: Menú Vegetariano", "Solicitud de opciones de fruta fresca en sustitución de lácteos.", "María López", "FAMILIAR", "Mateo García", "Sala 1A"),
        AdminLog("l_9", "Hoy, 02:30 PM", "OBJETO", "Objeto Perdido Reportado: Suéter Azul", "Encontrado en patio central. Etiqueta con nombre borroso.", "Personal de Apoyo", "EDUCADORA", null, "Patio Central"),
        AdminLog("l_10", "Hoy, 03:05 PM", "ASISTENCIA", "Egreso / Retiro del Alumno", "Alumno Mateo García retirado por María López (Madre). Pertenencias completas.", "Laura Gómez", "EDUCADORA", "Mateo García", "Sala 1A")
    )

    private val _logs = MutableStateFlow<List<AdminLog>>(initialLogs)
    override val logs: StateFlow<List<AdminLog>> = _logs.asStateFlow()

    private val _stats = MutableStateFlow(
        AdminStats(
            totalChildren = 21,
            presentCount = 18,
            absentCount = 3,
            attendanceRate = 85.7f,
            totalEducators = 4,
            totalFamilies = 19,
            childrenFedCount = 1,
            breakfastCount = 0,
            lunchCount = 1,
            snackCount = 0,
            childrenNappedCount = 0,
            totalNapMinutes = 0,
            averageNapMinutes = 0,
            ingresosCount = 1,
            egresosCount = 0,
            pendingPickupsCount = 1,
            diaperChangesCount = 0,
            incidentsCount = 0,
            totalSuggestions = _suggestions.value.size,
            pendingSuggestions = _suggestions.value.count { it.status == SuggestionStatus.PENDIENTE },
            attendedSuggestions = _suggestions.value.count { it.status == SuggestionStatus.ATENDIDA },
            totalLostObjects = _lostObjects.value.size,
            claimedLostObjects = _lostObjects.value.count { it.isClaimed },
            unclaimedLostObjects = _lostObjects.value.count { !it.isClaimed },
            totalAnnouncements = _announcements.value.size
        )
    )
    override val stats: StateFlow<AdminStats> = _stats.asStateFlow()

    override suspend fun refreshAll() {
        loadStats()
        loadUsers()
    }

    override suspend fun loadUsers(role: String?, search: String?) {
        var list = initialUsers.toList()
        if (!role.isNullOrBlank() && role.uppercase() != "TODOS") {
            list = list.filter { it.role.equals(role, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.trim().lowercase()
            list = list.filter { it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q) }
        }
        _users.value = list
    }

    override suspend fun createUser(
        email: String,
        password: String,
        role: String,
        displayName: String,
        phone: String?
    ): Result<AdminUser> {
        val newUser = AdminUser(
            id = "usr_${UUID.randomUUID().toString().take(6)}",
            email = email.trim(),
            displayName = displayName.trim(),
            role = role.uppercase(),
            phone = phone?.trim()
        )
        initialUsers.add(0, newUser)
        _users.value = initialUsers.toList()
        return Result.success(newUser)
    }

    override suspend fun updateUser(
        userId: String,
        displayName: String?,
        email: String?,
        phone: String?,
        role: String?,
        password: String?
    ): Result<AdminUser> {
        val idx = initialUsers.indexOfFirst { it.id == userId }
        if (idx == -1) return Result.failure(Exception("Usuario no encontrado"))
        val curr = initialUsers[idx]
        val updated = curr.copy(
            displayName = displayName ?: curr.displayName,
            email = email ?: curr.email,
            phone = phone ?: curr.phone,
            role = role ?: curr.role
        )
        initialUsers[idx] = updated
        _users.value = initialUsers.toList()
        return Result.success(updated)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        initialUsers.removeAll { it.id == userId }
        _users.value = initialUsers.toList()
        return Result.success(Unit)
    }

    override suspend fun createAnnouncement(
        title: String,
        summary: String,
        fullContent: String,
        author: String
    ): Result<Announcement> {
        val newAnn = Announcement(
            id = "ann_${UUID.randomUUID().toString().take(6)}",
            title = title.trim(),
            date = "Hoy, 10:00 AM",
            summary = summary.trim(),
            fullContent = fullContent.trim(),
            isUnread = true,
            author = author
        )
        val updated = listOf(newAnn) + _announcements.value
        _announcements.value = updated
        return Result.success(newAnn)
    }

    override suspend fun updateAnnouncement(
        id: String,
        title: String?,
        summary: String?,
        fullContent: String?,
        author: String?
    ): Result<Announcement> {
        val current = _announcements.value
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Aviso no encontrado"))
        val item = current[idx]
        val updated = item.copy(
            title = title ?: item.title,
            summary = summary ?: item.summary,
            fullContent = fullContent ?: item.fullContent,
            author = author ?: item.author
        )
        val newList = current.toMutableList()
        newList[idx] = updated
        _announcements.value = newList
        return Result.success(updated)
    }

    override suspend fun deleteAnnouncement(id: String): Result<Unit> {
        _announcements.value = _announcements.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun updateSuggestionStatus(
        id: String,
        status: String,
        response: String?,
        responderName: String?
    ): Result<ParentSuggestion> {
        val current = _suggestions.value
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Sugerencia no encontrada"))
        val item = current[idx]
        val statusEnum = when (status.uppercase()) {
            "ATENDIDA" -> SuggestionStatus.ATENDIDA
            "EN_REVISION" -> SuggestionStatus.EN_REVISION
            else -> SuggestionStatus.PENDIENTE
        }
        val updated = item.copy(
            status = statusEnum,
            response = response ?: item.response,
            responseDate = if (response != null) "Hoy, 12:00 PM" else item.responseDate,
            responderName = responderName ?: item.responderName
        )
        val newList = current.toMutableList()
        newList[idx] = updated
        _suggestions.value = newList
        return Result.success(updated)
    }

    override suspend fun deleteSuggestion(id: String): Result<Unit> {
        _suggestions.value = _suggestions.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun createLostObject(
        title: String,
        description: String,
        category: String,
        location: String?,
        room: String
    ): Result<LostItem> {
        val newObj = LostItem(
            id = "lost_${UUID.randomUUID().toString().take(6)}",
            title = title.trim(),
            description = description.trim(),
            category = category,
            location = location,
            room = room,
            isClaimed = false
        )
        val updated = listOf(newObj) + _lostObjects.value
        _lostObjects.value = updated
        return Result.success(newObj)
    }

    override suspend fun updateLostObject(
        id: String,
        title: String?,
        description: String?,
        category: String?,
        location: String?,
        room: String?,
        isClaimed: Boolean?
    ): Result<LostItem> {
        val current = _lostObjects.value
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return Result.failure(Exception("Objeto no encontrado"))
        val item = current[idx]
        val updated = item.copy(
            title = title ?: item.title,
            description = description ?: item.description,
            category = category ?: item.category,
            location = location ?: item.location,
            room = room ?: item.room,
            isClaimed = isClaimed ?: item.isClaimed
        )
        val newList = current.toMutableList()
        newList[idx] = updated
        _lostObjects.value = newList
        return Result.success(updated)
    }

    override suspend fun deleteLostObject(id: String): Result<Unit> {
        _lostObjects.value = _lostObjects.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun loadLogs(category: String?, search: String?) {
        var list = initialLogs.toList()
        if (!category.isNullOrBlank() && category.uppercase() != "TODOS") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.actorName.lowercase().contains(q) ||
                (it.childName?.lowercase()?.contains(q) == true)
            }
        }
        _logs.value = list
    }

    override suspend fun loadStats() {
        val totObj = _lostObjects.value.size
        val claimedObj = _lostObjects.value.count { it.isClaimed }
        val totSug = _suggestions.value.size
        val attSug = _suggestions.value.count { it.status == SuggestionStatus.ATENDIDA }
        val pendSug = _suggestions.value.count { it.status == SuggestionStatus.PENDIENTE }
        val totAnn = _announcements.value.size

        _stats.value = _stats.value.copy(
            totalLostObjects = totObj,
            claimedLostObjects = claimedObj,
            unclaimedLostObjects = totObj - claimedObj,
            totalSuggestions = totSug,
            attendedSuggestions = attSug,
            pendingSuggestions = pendSug,
            totalAnnouncements = totAnn
        )
    }

    override suspend fun getAllChildren(): Result<List<AdminChildSummary>> {
        return Result.success(centerChildrenPool.toList())
    }

    override suspend fun createAndLinkChild(
        userId: String,
        fullName: String,
        room: String,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?
    ): Result<AdminChildSummary> {
        val idx = initialUsers.indexOfFirst { it.id == userId }
        if (idx == -1) return Result.failure(Exception("Usuario no encontrado"))
        val newChild = AdminChildSummary(
            id = "child_${UUID.randomUUID().toString().take(6)}",
            fullName = fullName.trim(),
            shortName = fullName.split(" ").firstOrNull() ?: fullName,
            ageText = "2 años",
            roomText = room.trim(),
            groupText = "Maternal A",
            isPresent = true,
            statusText = "Presente",
            bloodType = bloodType?.trim() ?: "O+",
            pediatrician = pediatrician?.trim(),
            emergencyPhone = emergencyPhone?.trim(),
            medicalNotes = medicalNotes?.trim()
        )
        centerChildrenPool.add(newChild)
        val user = initialUsers[idx]
        val updatedUser = user.copy(children = user.children + newChild)
        initialUsers[idx] = updatedUser
        _users.value = initialUsers.toList()
        return Result.success(newChild)
    }

    override suspend fun linkChildToParent(userId: String, childId: String): Result<AdminChildSummary> {
        val userIdx = initialUsers.indexOfFirst { it.id == userId }
        if (userIdx == -1) return Result.failure(Exception("Usuario no encontrado"))
        val targetChild = centerChildrenPool.find { it.id == childId }
            ?: return Result.failure(Exception("Alumno no encontrado"))

        val user = initialUsers[userIdx]
        if (user.children.any { it.id == childId }) return Result.success(targetChild)

        val updatedUser = user.copy(children = user.children + targetChild)
        initialUsers[userIdx] = updatedUser
        _users.value = initialUsers.toList()
        return Result.success(targetChild)
    }

    override suspend fun unlinkChildFromParent(userId: String, childId: String): Result<Unit> {
        val userIdx = initialUsers.indexOfFirst { it.id == userId }
        if (userIdx == -1) return Result.failure(Exception("Usuario no encontrado"))
        val user = initialUsers[userIdx]
        val updatedUser = user.copy(children = user.children.filterNot { it.id == childId })
        initialUsers[userIdx] = updatedUser
        _users.value = initialUsers.toList()
        return Result.success(Unit)
    }

    override suspend fun updateChild(
        childId: String,
        fullName: String?,
        room: String?,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?,
        allergyAlert: String?,
        birthDate: String?
    ): Result<AdminChildSummary> {
        val poolIdx = centerChildrenPool.indexOfFirst { it.id == childId }
        val currentChild = if (poolIdx != -1) centerChildrenPool[poolIdx] else null

        val updated = (currentChild ?: AdminChildSummary(id = childId, fullName = fullName ?: "Alumno")).copy(
            fullName = fullName?.trim() ?: currentChild?.fullName ?: "Alumno",
            roomText = room?.trim() ?: currentChild?.roomText ?: "",
            bloodType = bloodType?.trim() ?: currentChild?.bloodType,
            pediatrician = pediatrician?.trim() ?: currentChild?.pediatrician,
            emergencyPhone = emergencyPhone?.trim() ?: currentChild?.emergencyPhone,
            medicalNotes = medicalNotes?.trim() ?: currentChild?.medicalNotes,
            allergyAlert = allergyAlert?.trim() ?: currentChild?.allergyAlert,
            birthDate = birthDate?.trim() ?: currentChild?.birthDate
        )

        if (poolIdx != -1) {
            centerChildrenPool[poolIdx] = updated
        } else {
            centerChildrenPool.add(updated)
        }

        for (i in initialUsers.indices) {
            val user = initialUsers[i]
            val childIdx = user.children.indexOfFirst { it.id == childId }
            if (childIdx != -1) {
                val newChildren = user.children.toMutableList()
                newChildren[childIdx] = updated
                initialUsers[i] = user.copy(children = newChildren)
            }
        }
        _users.value = initialUsers.toList()
        return Result.success(updated)
    }
}
