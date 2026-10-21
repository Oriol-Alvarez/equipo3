package com.example.etapa1.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val role: String? = null
)

@Serializable
data class AdminChildSummaryDto(
    val id: String,
    val fullName: String,
    val shortName: String = "",
    val ageText: String = "",
    val roomText: String = "",
    val groupText: String = "",
    val isPresent: Boolean = false,
    val statusText: String = "",
    val arrivalTime: String? = null,
    val allergyAlert: String? = null,
    val avatarInitials: String = "MG",
    val avatarBgColor: Long = 4294948685L,
    val birthDate: String? = null,
    val enrollmentDate: String? = null,
    val studentId: String? = null,
    val bloodType: String? = null,
    val pediatrician: String? = null,
    val pediatricianPhone: String? = null,
    val emergencyPhone: String? = null,
    val emergencyPhoneRelation: String? = null,
    val motherName: String? = null,
    val motherPhone: String? = null,
    val fatherName: String? = null,
    val fatherPhone: String? = null,
    val authorizedPickups: List<String> = emptyList(),
    val medicalNotes: String? = null,
    val generalNotes: String? = null,
    val habitsNotes: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val displayName: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val children: List<AdminChildSummaryDto> = emptyList()
)

@Serializable
data class DeviceTokenRequestDto(
    val deviceToken: String
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
    val avatarBgColor: Long = 4294948685L,
    val birthDate: String = "",
    val roomId: String = "",
    val educatorName: String? = null,
    val educatorRole: String? = null,
    val educatorAvatarInitials: String? = null,
    val educatorAvatarBgColor: Long? = null
)

@Serializable
data class EducatorDto(
    val id: String,
    val name: String,
    val role: String,
    val avatarInitials: String = "LG",
    val avatarBgColor: Long = 4278364369L,
    val roomId: String = "",
    val roomName: String = ""
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
data class UpdateSuggestionStatusDto(
    val status: String,
    val response: String? = null,
    val responderName: String? = null
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

@Serializable
data class CreateLostObjectDto(
    val title: String,
    val description: String,
    val category: String,
    val location: String? = null,
    val room: String = "General",
    val imageUri: String? = null
)

// ---------------------------------------------------------------- Mensajería y Chat en Tiempo Real
@Serializable
data class StaffMemberDto(
    val id: String,
    val name: String,
    val role: String,
    val avatarInitials: String,
    val avatarBgColor: Long,
    val isOnline: Boolean = true
)

@Serializable
data class WorkerChatMessageDto(
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

@Serializable
data class CreateWorkerGroupDto(
    val title: String,
    val memberIds: List<String> = emptyList()
)

@Serializable
data class WorkerChatDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val isGlobal: Boolean = false,
    val isGroup: Boolean = false,
    val avatarInitials: String = "SC",
    val avatarBgColor: Long = 4280391378L
)

@Serializable
data class StaffGlobalChatDto(
    val id: String,
    val title: String,
    val membersCount: Int = 18,
    val messages: List<WorkerChatMessageDto> = emptyList()
)

@Serializable
data class SendWorkerMessageDto(
    val message: String,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val time: String? = null
)

@Serializable
data class ParentChatSummaryDto(
    val childId: String,
    val childName: String,
    val parentName: String,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0
)

@Serializable
data class BitacoraMessageDto(
    val id: String,
    val senderId: String,
    val senderName: String,
    val time: String,
    val date: String? = null,
    val message: String,
    val isOutgoing: Boolean = false,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null
)

@Serializable
data class SendBitacoraMessageDto(
    val message: String,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val time: String? = null,
    val date: String? = null
)

@Serializable
data class TimelineItemDto(
    val type: String,
    val id: String,
    val time: String,
    val date: String? = null,
    val text: String? = null,
    val iconType: String? = null,
    val message: String? = null,
    val isOutgoing: Boolean? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val category: String? = null,
    val portionLabel: String? = null,
    val description: String? = null
)

@Serializable
data class CreateActivityDto(
    val time: String,
    val category: String,
    val title: String,
    val description: String,
    val portionOption: String? = null,
    val mealType: String? = null,
    val consumptionOption: String? = null,
    val napMinutes: Int? = null,
    val moodState: String? = null,
    val excretoryFunction: String? = null,
    val date: String? = null
)

@Serializable
data class NewActivityResponseDto(
    val id: String,
    val status: String = "CREADA",
    val timelineItem: TimelineItemDto
)

@Serializable
data class QuickEventDto(
    val time: String,
    val text: String,
    val iconType: String,
    val date: String? = null
)

@Serializable
data class DeviceTokenRequestDto(
    val deviceToken: String,
    val platform: String = "android"
)

@Serializable
data class CreateAnnouncementDto(
    val title: String,
    val summary: String,
    val fullContent: String,
    val author: String
)

@Serializable
data class UpdateAnnouncementDto(
    val title: String? = null,
    val summary: String? = null,
    val fullContent: String? = null,
    val author: String? = null
)

@Serializable
data class UpdateLostObjectDto(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val location: String? = null,
    val room: String? = null,
    val isClaimed: Boolean? = null
)

@Serializable
data class AdminUserCreateDto(
    val email: String,
    val password: String,
    val role: String = "EDUCADORA",
    val displayName: String,
    val phone: String? = null
)

@Serializable
data class AdminUserUpdateDto(
    val displayName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val password: String? = null
)

@Serializable
data class AdminLogDto(
    val id: String,
    val timestamp: String,
    val category: String,
    val title: String,
    val description: String,
    val actorName: String,
    val actorRole: String? = null,
    val childName: String? = null,
    val roomName: String? = null
)

@Serializable
data class AdminStatsDto(
    val totalChildren: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val attendanceRate: Float = 0f,
    val totalEducators: Int = 0,
    val totalFamilies: Int = 0,
    val childrenFedCount: Int = 0,
    val breakfastCount: Int = 0,
    val lunchCount: Int = 0,
    val snackCount: Int = 0,
    val childrenNappedCount: Int = 0,
    val totalNapMinutes: Int = 0,
    val averageNapMinutes: Int = 0,
    val ingresosCount: Int = 0,
    val egresosCount: Int = 0,
    val pendingPickupsCount: Int = 0,
    val diaperChangesCount: Int = 0,
    val incidentsCount: Int = 0,
    val totalSuggestions: Int = 0,
    val pendingSuggestions: Int = 0,
    val attendedSuggestions: Int = 0,
    val totalLostObjects: Int = 0,
    val claimedLostObjects: Int = 0,
    val unclaimedLostObjects: Int = 0,
    val totalAnnouncements: Int = 0
)

@Serializable
data class AdminCreateChildDto(
    val fullName: String,
    val shortName: String? = null,
    val ageText: String? = "2 años",
    val roomText: String? = "Sala 1A",
    val groupText: String? = "Maternal A",
    val allergyAlert: String? = null,
    val birthDate: String? = null,
    val studentId: String? = null,
    val bloodType: String? = null,
    val pediatrician: String? = null,
    val pediatricianPhone: String? = null,
    val emergencyPhone: String? = null,
    val emergencyPhoneRelation: String? = null,
    val motherName: String? = null,
    val motherPhone: String? = null,
    val fatherName: String? = null,
    val fatherPhone: String? = null,
    val medicalNotes: String? = null,
    val generalNotes: String? = null,
    val habitsNotes: String? = null,
    val authorizedPickups: List<String> = emptyList()
)

@Serializable
data class AdminUpdateChildDto(
    val fullName: String? = null,
    val shortName: String? = null,
    val ageText: String? = null,
    val roomText: String? = null,
    val groupText: String? = null,
    val statusText: String? = null,
    val isPresent: Boolean? = null,
    val allergyAlert: String? = null,
    val birthDate: String? = null,
    val enrollmentDate: String? = null,
    val studentId: String? = null,
    val bloodType: String? = null,
    val pediatrician: String? = null,
    val pediatricianPhone: String? = null,
    val emergencyPhone: String? = null,
    val emergencyPhoneRelation: String? = null,
    val motherName: String? = null,
    val motherPhone: String? = null,
    val fatherName: String? = null,
    val fatherPhone: String? = null,
    val authorizedPickups: List<String>? = null,
    val medicalNotes: String? = null,
    val generalNotes: String? = null,
    val habitsNotes: String? = null
)

@Serializable
data class QuickEventDto(
    val time: String,
    val text: String,
    val iconType: String,
    val date: String? = null
)

@Serializable
data class CreateWorkerGroupDto(
    val title: String,
    val membersIds: List<String>
)



