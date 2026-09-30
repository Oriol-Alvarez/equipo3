package com.example.casosdeusopadre.data.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "Familiar"
)

data class Child(
    val id: String,
    val name: String,
    val age: Int,
    val parentId: String,
    val photoUrl: String = ""
)

data class ActivityLog(
    val id: String,
    val childId: String,
    val date: String,
    val mealStatus: String, // completa, media, poca
    val napDurationMinutes: Int,
    val mood: String, // feliz, tranquilo, inquieto
    val pottyOrBath: String,
    val educatorObservations: String
)

data class Photo(
    val id: String,
    val childIds: List<String>,
    val imageUrl: String,
    val date: String
)

data class Notice(
    val id: String,
    val message: String,
    val date: String
)

data class MedicalCertificate(
    val id: String,
    val childId: String,
    val fileUri: String,
    val uploadDate: String,
    val isSynced: Boolean
)
