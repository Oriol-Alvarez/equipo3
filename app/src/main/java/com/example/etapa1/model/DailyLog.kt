package com.example.etapa1.model

import com.example.etapa1.domain.SimpleDate

/**
 * Registro diario de un niño con los mismos apartados de la hoja oficial
 * "Bitácora de filtro diario" (Apoyos para la inclusión en la primera infancia):
 * II. Ingreso, III. Estadía y IV. Egreso.
 *
 * Los campos `Boolean?` y los enums opcionales valen `null` cuando no se contestaron.
 */

/** Control de esfínteres tal como aparece en la hoja. */
enum class SphincterControl(val label: String) {
    NO_APLICA("No aplica"),
    AVISO_SI("Avisó: Sí"),
    AVISO_NO("Avisó: No")
}

/** II. Registro de ingreso de la niña o niño a la Estancia Infantil. */
data class IngresoLog(
    /** Hora de entrada en formato 24 h "HH:mm". */
    val time: String,
    val assistantName: String = "",
    val deliveredBy: String = "",
    val receivedBy: String = "",
    val isSick: Boolean? = null,
    val medicationWithPrescription: MedicationPrescriptionOption? = null,
    val hasSymptoms: Boolean? = null,
    val symptoms: String = "",
    val arrivedClean: Boolean? = null,
    val backpackComplete: Boolean? = null,
    val goodPhysicalCondition: Boolean? = null,
    /** Rasguño, mordida, golpe, etc. y parte del cuerpo. Vacío = NA. */
    val injuries: String = "",
    val observations: String = ""
)

/** III. Registro de estadía de la niña o niño en la Estancia Infantil. */
data class EstadiaLog(
    val breakfast: FoodConsumptionOption? = null,
    val snack: FoodConsumptionOption? = null,
    val lunch: FoodConsumptionOption? = null,
    val foodNotes: String = "",
    val slept: Boolean? = null,
    val napMinutes: Int? = null,
    val napComments: String = "",
    val sphincterControl: SphincterControl? = null,
    /** "Control de esfínteres → Número de veces" (cuántas veces avisó). Solo si no es "No aplica". */
    val sphincterPeeCount: Int? = null,
    val sphincterPoopCount: Int? = null,
    /** "Pipí y/o Popó → Número de veces" (total del día). */
    val peeCount: Int? = null,
    val poopCount: Int? = null,
    val excretionNotes: String = "",
    val mood: MoodState? = null,
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

/** IV. Registro de egreso de la niña o niño de la Estancia Infantil. */
data class EgresoLog(
    /** Hora de salida en formato 24 h "HH:mm". */
    val time: String,
    val goodPhysicalCondition: Boolean? = null,
    val injuries: String = "",
    val collectedBy: String = "",
    val deliveredClean: Boolean? = null,
    val belongingsComplete: Boolean? = null,
    val observations: String = "",
    val deliveredByStaff: String = ""
)

data class DailyLog(
    val childId: String,
    val date: SimpleDate,
    val ingreso: IngresoLog? = null,
    val estadia: EstadiaLog? = null,
    val egreso: EgresoLog? = null
)
