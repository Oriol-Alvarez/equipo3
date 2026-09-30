package com.example.casosdeusopadre.data.repository

import com.example.casosdeusopadre.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeChildcareRepository : ChildcareRepository {

    // Dummy Data
    private val parent1 = User(id = "p1", name = "Mamá de Mateo", email = "mama@mateo.com")
    private val children = listOf(
        Child(id = "c1", name = "Mateo", age = 3, parentId = "p1"),
        Child(id = "c2", name = "Sofía", age = 2, parentId = "p1"),
        Child(id = "c3", name = "Lucas", age = 4, parentId = "p2") // Different parent
    )

    private val activityLogs = listOf(
        ActivityLog(
            id = "a1", childId = "c1", date = "2023-10-25", mealStatus = "completa",
            napDurationMinutes = 90, mood = "feliz", pottyOrBath = "2 cambios de pañal",
            educatorObservations = "Jugó muy bien con sus compañeros hoy."
        )
    )

    private val photos = mutableListOf(
        Photo(id = "ph1", childIds = listOf("c1"), imageUrl = "url1", date = "2023-10-25"),
        Photo(id = "ph2", childIds = listOf("c1", "c2"), imageUrl = "url2", date = "2023-10-24"),
        Photo(id = "ph3", childIds = listOf("c2"), imageUrl = "url3", date = "2023-10-23")
    )

    private val notices = listOf(
        Notice(id = "n1", message = "Junta de Padres este Viernes 5:00 PM", date = "2023-10-26")
    )

    private var currentUser: User? = null

    override suspend fun login(email: String, password: String): Result<User> {
        delay(1000) // Simulate network
        if (email == "mama@mateo.com" && password == "123456") {
            currentUser = parent1
            return Result.success(parent1)
        }
        return Result.failure(Exception("Credenciales incorrectas"))
    }

    override fun getCurrentUser(): User? = currentUser

    override suspend fun getChildrenForParent(parentId: String): List<Child> {
        delay(500)
        return children.filter { it.parentId == parentId }
    }

    override suspend fun getActivityLogs(childId: String, date: String): List<ActivityLog> {
        delay(500)
        return activityLogs.filter { it.childId == childId && it.date == date }
    }

    override suspend fun getPhotos(childId: String): List<Photo> {
        delay(500)
        return photos.filter { it.childIds.contains(childId) }
    }

    private val certificates = mutableMapOf<String, MedicalCertificate>()

    override suspend fun requestPhotoDeletion(photoId: String): Result<Unit> {
        delay(800)
        val photo = photos.find { it.id == photoId }
        if (photo != null) {
            photos.remove(photo)
            return Result.success(Unit)
        }
        return Result.failure(Exception("Foto no encontrada"))
    }

    override suspend fun uploadMedicalCertificate(childId: String, fileUri: String, isOnline: Boolean): Result<MedicalCertificate> {
        delay(1500)
        val newCert = MedicalCertificate(
            id = "cert_${System.currentTimeMillis()}",
            childId = childId,
            fileUri = fileUri,
            uploadDate = "Hoy",
            isSynced = isOnline
        )
        certificates[childId] = newCert
        return Result.success(newCert)
    }

    override suspend fun getMedicalCertificate(childId: String): MedicalCertificate? {
        delay(500)
        return certificates[childId]
    }

    override suspend fun getWeeklySummary(childId: String): String {
        delay(500)
        return "Resumen semanal para niño $childId"
    }

    override fun getNotices(): Flow<List<Notice>> = flow {
        emit(notices)
    }
}
