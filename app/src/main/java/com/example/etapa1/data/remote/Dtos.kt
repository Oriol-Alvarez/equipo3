package com.example.etapa1.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val role: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val displayName: String,
    val phone: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: UserDto
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String
)

@Serializable
data class RefreshResponseDto(
    val token: String,
    val expiresIn: Int
)

@Serializable
data class RoomDto(
    val id: String,
    val name: String,
    val level: String,
    val presentCount: Int,
    val totalCount: Int
)

@Serializable
data class ChildDto(
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
    val avatarBgColor: Long = 4294948685L
)

@Serializable
data class IngresoDto(
    val childId: String,
    val date: String,
    val dayOfWeek: String = "L",
    val entryTime: String,
    val assistantName: String? = null,
    val isChildSick: Boolean = false,
    val medicationOption: String = "NO_APLICA",
    val hasSymptoms: Boolean = false,
    val symptomsDetail: String = "",
    val isClean: Boolean = true,
    val backpackComplete: Boolean = true,
    val goodPhysicalCondition: Boolean = true,
    val injuryDetail: String = "",
    val observations: String = "",
    val delivererName: String? = null,
    val delivererSignatureData: String? = null,
    val receiverStaffName: String? = null,
    val receiverStaffSignatureData: String? = null
)

@Serializable
data class EgresoDto(
    val childId: String,
    val date: String,
    val exitTime: String,
    val goodPhysicalCondition: Boolean = true,
    val injuryDetail: String = "",
    val isClean: Boolean = true,
    val backpackComplete: Boolean = true,
    val observations: String = "",
    val receiverTutorName: String? = null,
    val receiverTutorSignatureData: String? = null,
    val delivererStaffName: String? = null,
    val delivererStaffSignatureData: String? = null
)

@Serializable
data class AttendanceResponseDto(
    val id: String,
    val childId: String,
    val isIngreso: Boolean,
    val status: String = "REGISTRADO",
    val recordedAt: String
)

@Serializable
data class AttendanceRecordDto(
    val id: String,
    val childId: String,
    val isIngreso: Boolean,
    val date: String,
    val dayOfWeek: String,
    val time: String,
    val observations: String = ""
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val fullContent: String,
    val isUnread: Boolean,
    val author: String
)

@Serializable
data class UnreadCountDto(
    val unreadCount: Int
)

@Serializable
data class SuggestionDto(
    val id: String,
    val parentName: String,
    val childName: String,
    val roomName: String,
    val date: String,
    val time: String,
    val category: String,
    val subject: String,
    val content: String,
    val status: String,
    val response: String? = null,
    val responseDate: String? = null,
    val responderName: String? = null
)

@Serializable
data class CreateSuggestionDto(
    val childId: String,
    val roomName: String,
    val category: String,
    val subject: String,
    val content: String
)

@Serializable
data class LostItemDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String? = null,
    val imageUri: String? = null,
    val room: String = "Sala 1A",
    val isClaimed: Boolean = false
)
