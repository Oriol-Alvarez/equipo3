package com.example.casosdeusopadre.data.repository

import com.example.casosdeusopadre.data.models.*
import kotlinx.coroutines.flow.Flow

interface ChildcareRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun getChildrenForParent(parentId: String): List<Child>
    suspend fun getActivityLogs(childId: String, date: String): List<ActivityLog>
    suspend fun getPhotos(childId: String): List<Photo>
    suspend fun requestPhotoDeletion(photoId: String): Result<Unit>
    suspend fun uploadMedicalCertificate(childId: String, fileUri: String, isOnline: Boolean): Result<MedicalCertificate>
    suspend fun getMedicalCertificate(childId: String): MedicalCertificate?
    suspend fun getWeeklySummary(childId: String): String // Simple summary for now
    fun getNotices(): Flow<List<Notice>>
    fun getCurrentUser(): User?
}
