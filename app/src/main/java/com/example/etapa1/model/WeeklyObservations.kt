package com.example.etapa1.model

/** Largo máximo de una observación semanal. */
const val WEEKLY_OBSERVATION_MAX_LENGTH = 500

/**
 * Observaciones manuales que se agregan al resumen semanal de un niño.
 *
 * Cada observación guarda quién la escribió ([ObservationAuthorRole]) para que la misma
 * estructura sirva tanto para las observaciones de la familia como para las de la educadora.
 * Los datos son simulados y viven solo mientras el proceso de la app está activo.
 */
enum class ObservationAuthorRole(val label: String) {
    FAMILIA("Familia"),
    EDUCADORA("Educadora")
}

data class WeeklyObservation(
    val id: String,
    val childId: String,
    /** Identifica la semana; se usa [WeeklySummary.weekRangeText]. */
    val weekKey: String,
    val authorRole: ObservationAuthorRole,
    val authorName: String,
    val text: String,
    val time: String
)

object WeeklyObservationsSampleData {

    /** Misma familia con la que se inicia sesión en el perfil Familiar. */
    val FAMILIA_GARCIA_NAME: String = FamilyGroupsSampleData.FAMILIA_GARCIA.displayName

    fun initialObservations(): List<WeeklyObservation> = listOf(
        WeeklyObservation(
            id = "wobs_1",
            childId = "mateo_garcia",
            weekKey = "Semana del 15 al 19 de Septiembre, 2026",
            authorRole = ObservationAuthorRole.FAMILIA,
            authorName = FAMILIA_GARCIA_NAME,
            text = "En casa también lo notamos más tranquilo para dormir. El fin de semana comió brócoli en puré sin problema.",
            time = "19/09 07:40 PM"
        )
    )
}
