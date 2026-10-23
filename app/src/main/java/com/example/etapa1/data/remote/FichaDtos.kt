package com.example.etapa1.data.remote

import kotlinx.serialization.Serializable

// DTOs de la ficha del niño: expediente, resúmenes semanales e historial diario.
// Los nombres de los campos coinciden con backend/app/schemas.py.

@Serializable
data class ChildFullProfileDto(
    val childId: String,
    val birthDate: String = "",
    val enrollmentDate: String = "",
    val studentId: String = "",
    val bloodType: String = "",
    val pediatrician: String = "",
    val pediatricianPhone: String = "",
    val emergencyPhone: String = "",
    val emergencyPhoneRelation: String = "",
    val motherName: String = "",
    val motherPhone: String = "",
    val fatherName: String = "",
    val fatherPhone: String = "",
    val authorizedPickups: List<String> = emptyList(),
    val medicalNotes: String = "",
    val generalNotes: String = "",
    val medicalCertificateUri: String? = null,
    val medicalCertificateName: String? = null,
    val medicalCertificateDate: String? = null
)

@Serializable
data class ChildProfileUpdateDto(
    val bloodType: String? = null,
    val pediatrician: String? = null,
    val pediatricianPhone: String? = null,
    val medicalNotes: String? = null,
    val habitsAndPedagogicalNotes: String? = null,
    val emergencyPhone: String? = null,
    val emergencyPhoneRelation: String? = null,
    val authorizedPickups: List<String>? = null,
    val medicalCertificateUri: String? = null,
    val medicalCertificateName: String? = null,
    val medicalCertificateDate: String? = null
)

@Serializable
data class WeeklySummaryDto(
    val id: String,
    val weekLabel: String,
    val weekRangeText: String,
    val attendanceDaysCount: Int = 0,
    val totalSchoolDays: Int = 0,
    val attendancePercentageText: String = "—",
    val averageArrivalTime: String = "—",
    val foodIntakePercentage: String = "—",
    val totalNapHours: String = "—",
    val averageNapDaily: String = "—",
    val moodSummary: String = "",
    val executiveSummary: String = "",
    val foodHighlights: List<String> = emptyList(),
    val napHighlights: String = "",
    val pedagogicalHighlights: List<String> = emptyList(),
    val healthAndSafetySummary: String = "",
    val isReadByParent: Boolean = false,
    val readTimestampText: String? = null,
    val parentReaction: String? = null
)

/** RF-19: reporte mensual del niño (GET /children/{id}/monthly-reports/{YYYY-MM}). */
@Serializable
data class ReporteMensualDto(
    val id: String,
    val monthLabel: String,
    val periodText: String = "",
    val isCurrentMonth: Boolean = false,
    val schoolDays: Int = 0,
    val attendanceDays: Int = 0,
    val attendancePercentageText: String = "—",
    val averageArrivalTime: String = "—",
    val foodIntakePercentage: String = "—",
    val napDays: Int = 0,
    val averageNapDaily: String = "—",
    val moodSummary: String = "",
    val activitiesRecorded: Int = 0,
    val behaviorHighlights: List<String> = emptyList(),
    val healthEvents: List<String> = emptyList(),
    val pedagogicalHighlights: List<String> = emptyList(),
    val observations: List<String> = emptyList(),
    val pointsForFamily: List<String> = emptyList(),
    val reportText: String = "",
    val generatedWithAi: Boolean = false,
    val generatedAt: String = ""
)

@Serializable
data class WeeklySummaryFeedbackDto(
    val isRead: Boolean? = null,
    /** Cadena vacía = quitar la reacción. */
    val reaction: String? = null
)

@Serializable
data class BitacoraIngresoDto(
    val time: String = "",
    val delivererName: String = "",
    val receiverName: String = "",
    val physicalCondition: String = "",
    val hasSymptoms: Boolean = false,
    val symptomsDetail: String = "",
    val observations: String = ""
)

@Serializable
data class BitacoraEgresoDto(
    val time: String = "",
    val collectorName: String = "",
    val delivererName: String = "",
    val physicalCondition: String = "",
    val observations: String = ""
)

@Serializable
data class BitacoraActividadDto(
    val id: String,
    val time: String = "",
    val title: String = "",
    val category: String = "OTROS",
    val portion: String? = null,
    val description: String = ""
)

@Serializable
data class BitacoraDiaDto(
    val date: String,
    val dayLabel: String,
    val isToday: Boolean = false,
    val ingreso: BitacoraIngresoDto? = null,
    val egreso: BitacoraEgresoDto? = null,
    val activities: List<BitacoraActividadDto> = emptyList()
)
