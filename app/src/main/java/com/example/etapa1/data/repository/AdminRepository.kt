package com.example.etapa1.data.repository

import com.example.etapa1.model.AdminLog
import com.example.etapa1.model.AdminStats
import com.example.etapa1.model.AdminUser
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.ParentSuggestion
import kotlinx.coroutines.flow.StateFlow

interface AdminRepository {
    val users: StateFlow<List<AdminUser>>
    val logs: StateFlow<List<AdminLog>>
    val stats: StateFlow<AdminStats>
    val announcements: StateFlow<List<Announcement>>
    val suggestions: StateFlow<List<ParentSuggestion>>
    val lostObjects: StateFlow<List<LostItem>>

    suspend fun refreshAll()
    suspend fun loadUsers(role: String? = null, search: String? = null)
    suspend fun createUser(email: String, password: String, role: String, displayName: String, phone: String?): Result<AdminUser>
    suspend fun updateUser(userId: String, displayName: String?, email: String?, phone: String?, role: String?, password: String?): Result<AdminUser>
    suspend fun deleteUser(userId: String): Result<Unit>

    suspend fun createAnnouncement(title: String, summary: String, fullContent: String, author: String): Result<Announcement>
    suspend fun updateAnnouncement(id: String, title: String?, summary: String?, fullContent: String?, author: String?): Result<Announcement>
    suspend fun deleteAnnouncement(id: String): Result<Unit>

    suspend fun updateSuggestionStatus(id: String, status: String, response: String?, responderName: String?): Result<ParentSuggestion>
    suspend fun deleteSuggestion(id: String): Result<Unit>

    suspend fun createLostObject(title: String, description: String, category: String, location: String?, room: String): Result<LostItem>
    suspend fun updateLostObject(id: String, title: String?, description: String?, category: String?, location: String?, room: String?, isClaimed: Boolean?): Result<LostItem>
    suspend fun deleteLostObject(id: String): Result<Unit>

    suspend fun loadLogs(category: String? = null, search: String? = null)
    suspend fun loadStats()

    suspend fun getAllChildren(): Result<List<com.example.etapa1.model.AdminChildSummary>>
    suspend fun createAndLinkChild(
        userId: String,
        fullName: String,
        room: String,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?
    ): Result<com.example.etapa1.model.AdminChildSummary>
    suspend fun linkChildToParent(userId: String, childId: String): Result<com.example.etapa1.model.AdminChildSummary>
    suspend fun unlinkChildFromParent(userId: String, childId: String): Result<Unit>
    suspend fun updateChild(
        childId: String,
        fullName: String?,
        room: String?,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?,
        allergyAlert: String?,
        birthDate: String?
    ): Result<com.example.etapa1.model.AdminChildSummary>
}
