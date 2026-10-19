package com.example.etapa1.data.remote

import kotlinx.serialization.Serializable

// DTOs de grupos con familias, observaciones semanales, planeación/menú y registro diario.
// Los nombres de los campos coinciden con backend/app/schemas.py.

@Serializable
data class FamilyGroupDto(
    val id: String,
    val title: String,
    val memberChildIds: List<String> = emptyList(),
    val createdById: String,
    val avatarBgColor: Long,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null
)

@Serializable
data class CreateFamilyGroupDto(
    val title: String,
    val memberChildIds: List<String>
)

@Serializable
data class AttachmentDto(
    val uri: String,
    val name: String,
    val mimeType: String
)

@Serializable
data class FamilyGroupMessageDto(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val time: String,
    val attachment: AttachmentDto? = null
)

@Serializable
data class SendFamilyGroupMessageDto(
    val text: String,
    val attachment: AttachmentDto? = null,
    val time: String? = null
)

@Serializable
data class FamilyGroupCandidateDto(
    val childId: String,
    val childName: String,
    val parentName: String,
    val avatarInitials: String,
    val avatarBgColor: Long
)

@Serializable
data class MediaUploadDto(
    val fileId: String,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

@Serializable
data class WeeklyObservationDto(
    val id: String,
    val childId: String,
    val weekKey: String,
    val authorRole: String,
    val authorName: String,
    val text: String,
    val time: String
)

@Serializable
data class CreateWeeklyObservationDto(
    val weekKey: String,
    val text: String
)

@Serializable
data class DailyMenuDto(
    val breakfast: String,
    val snack: String,
    val lunch: String,
    val notes: String = ""
)

@Serializable
data class PlannedActivityDto(
    val time: String,
    val title: String
)

@Serializable
data class DailyPlanDto(
    val date: String,
    val roomId: String = "sala_1a",
    val menu: DailyMenuDto,
    val activities: List<PlannedActivityDto>,
    val updatedBy: String = ""
)

@Serializable
data class SaveDailyPlanDto(
    val menu: DailyMenuDto,
    val activities: List<PlannedActivityDto>,
    val roomId: String = "sala_1a"
)

@Serializable
data class IngresoLogDto(
    val time: String,
    val assistantName: String = "",
    val deliveredBy: String = "",
    val receivedBy: String = "",
    val isSick: Boolean? = null,
    val medicationWithPrescription: String? = null,
    val hasSymptoms: Boolean? = null,
    val symptoms: String = "",
    val arrivedClean: Boolean? = null,
    val backpackComplete: Boolean? = null,
    val goodPhysicalCondition: Boolean? = null,
    val injuries: String = "",
    val observations: String = ""
)

@Serializable
data class EstadiaLogDto(
    val breakfast: String? = null,
    val snack: String? = null,
    val lunch: String? = null,
    val foodNotes: String = "",
    val slept: Boolean? = null,
    val napMinutes: Int? = null,
    val napComments: String = "",
    val sphincterControl: String? = null,
    val sphincterPeeCount: Int? = null,
    val sphincterPoopCount: Int? = null,
    val peeCount: Int? = null,
    val poopCount: Int? = null,
    val excretionNotes: String = "",
    val mood: String? = null,
    val cried: Boolean? = null,
    val fought: Boolean? = null,
    val participated: Boolean? = null,
    val moodNotes: String = "",
    val hadAccident: Boolean? = null,
    val accidentDescription: String = "",
    val accidentFolio: String = "",
    val hadHealthIssue: Boolean? = null,
    val healthIssue: String = "",
    val careProvided: String = "",
    val generalNotes: String = ""
)

@Serializable
data class EgresoLogDto(
    val time: String,
    val goodPhysicalCondition: Boolean? = null,
    val injuries: String = "",
    val collectedBy: String = "",
    val deliveredClean: Boolean? = null,
    val belongingsComplete: Boolean? = null,
    val observations: String = "",
    val deliveredByStaff: String = ""
)

@Serializable
data class DailyLogDto(
    val childId: String,
    val date: String,
    val ingreso: IngresoLogDto? = null,
    val estadia: EstadiaLogDto? = null,
    val egreso: EgresoLogDto? = null
)
