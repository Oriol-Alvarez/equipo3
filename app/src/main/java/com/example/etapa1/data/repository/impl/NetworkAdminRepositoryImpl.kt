package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.*
import com.example.etapa1.data.repository.AdminRepository
import com.example.etapa1.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NetworkAdminRepositoryImpl(
    private val api: SonrisasApi
) : AdminRepository {

    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    override val users: StateFlow<List<AdminUser>> = _users.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    override val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _suggestions = MutableStateFlow<List<ParentSuggestion>>(emptyList())
    override val suggestions: StateFlow<List<ParentSuggestion>> = _suggestions.asStateFlow()

    private val _lostObjects = MutableStateFlow<List<LostItem>>(emptyList())
    override val lostObjects: StateFlow<List<LostItem>> = _lostObjects.asStateFlow()

    private val _logs = MutableStateFlow<List<AdminLog>>(emptyList())
    override val logs: StateFlow<List<AdminLog>> = _logs.asStateFlow()

    private val _stats = MutableStateFlow(AdminStats())
    override val stats: StateFlow<AdminStats> = _stats.asStateFlow()

    override suspend fun refreshAll() {
        withContext(Dispatchers.IO) {
            loadUsers()
            loadLogs()
            loadStats()
            loadAnnouncements()
            loadSuggestions()
            loadLostObjects()
        }
    }

    private suspend fun loadAnnouncements() {
        try {
            val res = api.getAnnouncements()
            if (res.isSuccessful && res.body() != null) {
                _announcements.value = res.body()!!.map {
                    Announcement(
                        id = it.id,
                        title = it.title,
                        date = it.date,
                        summary = it.summary,
                        fullContent = it.fullContent,
                        isUnread = it.isUnread,
                        author = it.author
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadSuggestions() {
        try {
            val res = api.getSuggestions()
            if (res.isSuccessful && res.body() != null) {
                _suggestions.value = res.body()!!.map {
                    val catEnum = when (it.category.uppercase()) {
                        "ALIMENTACION" -> SuggestionCategory.ALIMENTACION
                        "INSTALACIONES" -> SuggestionCategory.INSTALACIONES
                        "ACTIVIDADES" -> SuggestionCategory.ACTIVIDADES
                        "HORARIOS" -> SuggestionCategory.HORARIOS
                        else -> SuggestionCategory.GENERAL
                    }
                    val statusEnum = when (it.status.uppercase()) {
                        "ATENDIDA" -> SuggestionStatus.ATENDIDA
                        "EN_REVISION" -> SuggestionStatus.EN_REVISION
                        else -> SuggestionStatus.PENDIENTE
                    }
                    ParentSuggestion(
                        id = it.id,
                        parentName = it.parentName,
                        childName = it.childName,
                        roomName = it.roomName,
                        date = it.date,
                        time = it.time,
                        category = catEnum,
                        subject = it.subject,
                        content = it.content,
                        status = statusEnum,
                        response = it.response,
                        responseDate = it.responseDate,
                        responderName = it.responderName
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadLostObjects() {
        try {
            val res = api.getLostObjects()
            if (res.isSuccessful && res.body() != null) {
                _lostObjects.value = res.body()!!.map {
                    LostItem(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        category = it.category,
                        location = it.location,
                        imageUri = it.imageUri,
                        room = it.room,
                        isClaimed = it.isClaimed,
                        claimedByName = it.claimedByName
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun loadUsers(role: String?, search: String?) {
        withContext(Dispatchers.IO) {
            try {
                val filterRole = if (role.isNullOrBlank() || role.equals("TODOS", ignoreCase = true)) null else role.uppercase()
                val filterSearch = if (search.isNullOrBlank()) null else search.trim()
                val res = api.getAdminUsers(filterRole, filterSearch)
                if (res.isSuccessful && res.body() != null) {
                    _users.value = res.body()!!.map { u ->
                        AdminUser(
                            id = u.id,
                            email = u.email,
                            displayName = u.displayName,
                            role = u.role,
                            phone = u.phone,
                            avatarUrl = u.avatarUrl,
                            children = u.children.map { it.toDomain() }
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun createUser(
        email: String,
        password: String,
        role: String,
        displayName: String,
        phone: String?
    ): Result<AdminUser> = withContext(Dispatchers.IO) {
        try {
            val res = api.createAdminUser(
                AdminUserCreateDto(
                    email = email.trim(),
                    password = password,
                    role = role.uppercase(),
                    displayName = displayName.trim(),
                    phone = phone?.trim()
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val u = res.body()!!
                val created = AdminUser(u.id, u.email, u.displayName, u.role, u.phone, u.avatarUrl)
                _users.value = listOf(created) + _users.value
                Result.success(created)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun updateUser(
        userId: String,
        displayName: String?,
        email: String?,
        phone: String?,
        role: String?,
        password: String?
    ): Result<AdminUser> = withContext(Dispatchers.IO) {
        try {
            val res = api.updateAdminUser(
                userId,
                AdminUserUpdateDto(
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    role = role,
                    password = password
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val u = res.body()!!
                val updated = AdminUser(u.id, u.email, u.displayName, u.role, u.phone, u.avatarUrl)
                _users.value = _users.value.map { if (it.id == userId) updated else it }
                Result.success(updated)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteAdminUser(userId)
            if (res.isSuccessful) {
                _users.value = _users.value.filterNot { it.id == userId }
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun createAnnouncement(
        title: String,
        summary: String,
        fullContent: String,
        author: String
    ): Result<Announcement> = withContext(Dispatchers.IO) {
        try {
            val res = api.createAnnouncement(
                CreateAnnouncementDto(
                    title = title,
                    summary = summary,
                    fullContent = fullContent,
                    author = author
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val a = res.body()!!
                val created = Announcement(a.id, a.title, a.date, a.summary, a.fullContent, a.isUnread, a.author)
                _announcements.value = listOf(created) + _announcements.value
                Result.success(created)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun updateAnnouncement(
        id: String,
        title: String?,
        summary: String?,
        fullContent: String?,
        author: String?
    ): Result<Announcement> = withContext(Dispatchers.IO) {
        try {
            val res = api.updateAnnouncement(
                id,
                UpdateAnnouncementDto(
                    title = title,
                    summary = summary,
                    fullContent = fullContent,
                    author = author
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val a = res.body()!!
                val updated = Announcement(a.id, a.title, a.date, a.summary, a.fullContent, a.isUnread, a.author)
                _announcements.value = _announcements.value.map { if (it.id == id) updated else it }
                Result.success(updated)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun deleteAnnouncement(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteAnnouncement(id)
            if (res.isSuccessful) {
                _announcements.value = _announcements.value.filterNot { it.id == id }
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun updateSuggestionStatus(
        id: String,
        status: String,
        response: String?,
        responderName: String?
    ): Result<ParentSuggestion> = withContext(Dispatchers.IO) {
        try {
            val res = api.updateSuggestionStatus(
                id,
                UpdateSuggestionStatusDto(status = status, response = response, responderName = responderName)
            )
            if (res.isSuccessful && res.body() != null) {
                val it = res.body()!!
                val catEnum = when (it.category.uppercase()) {
                    "ALIMENTACION" -> SuggestionCategory.ALIMENTACION
                    "INSTALACIONES" -> SuggestionCategory.INSTALACIONES
                    "ACTIVIDADES" -> SuggestionCategory.ACTIVIDADES
                    "HORARIOS" -> SuggestionCategory.HORARIOS
                    else -> SuggestionCategory.GENERAL
                }
                val statusEnum = when (it.status.uppercase()) {
                    "ATENDIDA" -> SuggestionStatus.ATENDIDA
                    "EN_REVISION" -> SuggestionStatus.EN_REVISION
                    else -> SuggestionStatus.PENDIENTE
                }
                val updated = ParentSuggestion(
                    id = it.id,
                    parentName = it.parentName,
                    childName = it.childName,
                    roomName = it.roomName,
                    date = it.date,
                    time = it.time,
                    category = catEnum,
                    subject = it.subject,
                    content = it.content,
                    status = statusEnum,
                    response = it.response,
                    responseDate = it.responseDate,
                    responderName = it.responderName
                )
                _suggestions.value = _suggestions.value.map { if (it.id == id) updated else it }
                Result.success(updated)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun deleteSuggestion(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteSuggestion(id)
            if (res.isSuccessful) {
                _suggestions.value = _suggestions.value.filterNot { it.id == id }
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun createLostObject(
        title: String,
        description: String,
        category: String,
        location: String?,
        room: String
    ): Result<LostItem> = withContext(Dispatchers.IO) {
        try {
            val res = api.createLostObject(
                CreateLostObjectDto(
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    room = room
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val it = res.body()!!
                val created = LostItem(it.id, it.title, it.description, it.category, it.location, it.imageUri, it.room, it.isClaimed)
                _lostObjects.value = listOf(created) + _lostObjects.value
                Result.success(created)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun updateLostObject(
        id: String,
        title: String?,
        description: String?,
        category: String?,
        location: String?,
        room: String?,
        isClaimed: Boolean?
    ): Result<LostItem> = withContext(Dispatchers.IO) {
        try {
            val res = api.updateLostObject(
                id,
                UpdateLostObjectDto(
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    room = room,
                    isClaimed = isClaimed
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val it = res.body()!!
                val updated = LostItem(it.id, it.title, it.description, it.category, it.location, it.imageUri, it.room, it.isClaimed)
                _lostObjects.value = _lostObjects.value.map { if (it.id == id) updated else it }
                Result.success(updated)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun deleteLostObject(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteLostObject(id)
            if (res.isSuccessful) {
                _lostObjects.value = _lostObjects.value.filterNot { it.id == id }
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun loadLogs(category: String?, search: String?) {
        withContext(Dispatchers.IO) {
            try {
                val res = api.getAdminLogs(category, search)
                if (res.isSuccessful && res.body() != null) {
                    _logs.value = res.body()!!.map {
                        AdminLog(
                            id = it.id,
                            timestamp = it.timestamp,
                            category = it.category,
                            title = it.title,
                            description = it.description,
                            actorName = it.actorName,
                            actorRole = it.actorRole,
                            childName = it.childName,
                            roomName = it.roomName
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun loadStats() {
        withContext(Dispatchers.IO) {
            try {
                val res = api.getAdminStats()
                if (res.isSuccessful && res.body() != null) {
                    val d = res.body()!!
                    _stats.value = AdminStats(
                        totalChildren = d.totalChildren,
                        presentCount = d.presentCount,
                        absentCount = d.absentCount,
                        attendanceRate = d.attendanceRate,
                        totalEducators = d.totalEducators,
                        totalFamilies = d.totalFamilies,
                        childrenFedCount = d.childrenFedCount,
                        breakfastCount = d.breakfastCount,
                        lunchCount = d.lunchCount,
                        snackCount = d.snackCount,
                        childrenNappedCount = d.childrenNappedCount,
                        totalNapMinutes = d.totalNapMinutes,
                        averageNapMinutes = d.averageNapMinutes,
                        ingresosCount = d.ingresosCount,
                        egresosCount = d.egresosCount,
                        pendingPickupsCount = d.pendingPickupsCount,
                        diaperChangesCount = d.diaperChangesCount,
                        incidentsCount = d.incidentsCount,
                        totalSuggestions = d.totalSuggestions,
                        pendingSuggestions = d.pendingSuggestions,
                        attendedSuggestions = d.attendedSuggestions,
                        totalLostObjects = d.totalLostObjects,
                        claimedLostObjects = d.claimedLostObjects,
                        unclaimedLostObjects = d.unclaimedLostObjects,
                        totalAnnouncements = d.totalAnnouncements
                    )
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun getAllChildren(): Result<List<AdminChildSummary>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getAdminAllChildren()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun createAndLinkChild(
        userId: String,
        fullName: String,
        room: String,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?
    ): Result<AdminChildSummary> = withContext(Dispatchers.IO) {
        try {
            val req = AdminCreateChildDto(
                fullName = fullName.trim(),
                roomText = room.trim(),
                bloodType = bloodType?.trim(),
                pediatrician = pediatrician?.trim(),
                emergencyPhone = emergencyPhone?.trim(),
                medicalNotes = medicalNotes?.trim()
            )
            val res = api.createAndLinkChild(userId, req)
            if (res.isSuccessful && res.body() != null) {
                loadUsers()
                Result.success(res.body()!!.toDomain())
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun linkChildToParent(userId: String, childId: String): Result<AdminChildSummary> = withContext(Dispatchers.IO) {
        try {
            val res = api.linkChildToParent(userId, childId)
            if (res.isSuccessful && res.body() != null) {
                loadUsers()
                Result.success(res.body()!!.toDomain())
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }

    override suspend fun unlinkChildFromParent(userId: String, childId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.unlinkChildFromParent(userId, childId)
            if (res.isSuccessful) {
                loadUsers()
                Result.success(Unit)
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
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
    ): Result<AdminChildSummary> = withContext(Dispatchers.IO) {
        try {
            val req = AdminUpdateChildDto(
                fullName = fullName?.trim(),
                roomText = room?.trim(),
                bloodType = bloodType?.trim(),
                pediatrician = pediatrician?.trim(),
                emergencyPhone = emergencyPhone?.trim(),
                medicalNotes = medicalNotes?.trim(),
                allergyAlert = allergyAlert?.trim(),
                birthDate = birthDate?.trim()
            )
            val res = api.updateAdminChild(childId, req)
            if (res.isSuccessful && res.body() != null) {
                loadUsers()
                Result.success(res.body()!!.toDomain())
            } else {
                Result.failure(Exception(NetworkErrorHelper.parseResponse(res)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(NetworkErrorHelper.parseException(e), e))
        }
    }
}

private fun AdminChildSummaryDto.toDomain() = AdminChildSummary(
    id = id,
    fullName = fullName,
    shortName = shortName,
    ageText = ageText,
    roomText = roomText,
    groupText = groupText,
    isPresent = isPresent,
    statusText = statusText,
    arrivalTime = arrivalTime,
    allergyAlert = allergyAlert,
    avatarInitials = avatarInitials,
    avatarBgColor = avatarBgColor,
    birthDate = birthDate,
    enrollmentDate = enrollmentDate,
    studentId = studentId,
    bloodType = bloodType,
    pediatrician = pediatrician,
    pediatricianPhone = pediatricianPhone,
    emergencyPhone = emergencyPhone,
    emergencyPhoneRelation = emergencyPhoneRelation,
    motherName = motherName,
    motherPhone = motherPhone,
    fatherName = fatherName,
    fatherPhone = fatherPhone,
    authorizedPickups = authorizedPickups,
    medicalNotes = medicalNotes,
    generalNotes = generalNotes,
    habitsNotes = habitsNotes
)
