package com.example.etapa1.ui.state

import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WeeklyObservation

/** Qué semana de qué niño se está viendo y quién la está viendo. */
data class ObservationsSelection(
    val childId: String,
    val weekKey: String,
    val isCurrentWeek: Boolean,
    val viewerRole: ObservationAuthorRole,
    val viewerName: String
)

/**
 * Reglas de las observaciones semanales, sin dependencias de Android
 * para poder probarlas con pruebas unitarias.
 */
object WeeklyObservationsPresenter {

    /** Observaciones del niño en esa semana, de la más antigua a la más reciente. */
    fun observationsFor(
        all: List<WeeklyObservation>,
        childId: String,
        weekKey: String
    ): List<WeeklyObservation> = all.filter { it.childId == childId && it.weekKey == weekKey }

    /**
     * Por ahora solo la familia escribe aquí, y solo en la semana actual:
     * las semanas pasadas ya están cerradas y se muestran como consulta.
     */
    fun canWrite(selection: ObservationsSelection): Boolean =
        selection.viewerRole == ObservationAuthorRole.FAMILIA && selection.isCurrentWeek

    fun emptyMessage(selection: ObservationsSelection): String = when {
        selection.viewerRole == ObservationAuthorRole.FAMILIA && selection.isCurrentWeek ->
            "Todavía no has escrito observaciones esta semana. Cuéntale a la estancia cómo ves a tu hijo o hija en casa."
        selection.viewerRole == ObservationAuthorRole.FAMILIA ->
            "No escribiste observaciones en esta semana."
        else -> "La familia no ha escrito observaciones en esta semana."
    }

    fun readOnlyHint(selection: ObservationsSelection): String? = when {
        canWrite(selection) -> null
        selection.viewerRole == ObservationAuthorRole.FAMILIA ->
            "Solo puedes agregar observaciones en la semana actual."
        else -> null
    }
}
