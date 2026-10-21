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

    /** Todas las observaciones del niño en esa semana, de la más antigua a la más reciente. */
    fun observationsFor(
        all: List<WeeklyObservation>,
        childId: String,
        weekKey: String
    ): List<WeeklyObservation> = all.filter { it.childId == childId && it.weekKey == weekKey }

    /** Observaciones redactadas por el personal / educadoras. */
    fun educatorObservations(
        all: List<WeeklyObservation>,
        childId: String,
        weekKey: String
    ): List<WeeklyObservation> = all.filter {
        it.childId == childId && it.weekKey == weekKey && it.authorRole == ObservationAuthorRole.EDUCADORA
    }

    /** Observaciones redactadas por la familia. */
    fun familyObservations(
        all: List<WeeklyObservation>,
        childId: String,
        weekKey: String
    ): List<WeeklyObservation> = all.filter {
        it.childId == childId && it.weekKey == weekKey && it.authorRole == ObservationAuthorRole.FAMILIA
    }

    /**
     * Tanto la familia como los trabajadores pueden escribir su observación en la semana actual.
     * En semanas pasadas, todo queda cerrado como histórico de solo lectura.
     */
    fun canWrite(selection: ObservationsSelection): Boolean = selection.isCurrentWeek

    /** ¿El usuario actual puede escribir en el apartado de trabajadores? */
    fun canWriteAsEducator(selection: ObservationsSelection): Boolean =
        selection.viewerRole == ObservationAuthorRole.EDUCADORA && selection.isCurrentWeek

    /** ¿El usuario actual puede escribir en el apartado de familia? */
    fun canWriteAsFamily(selection: ObservationsSelection): Boolean =
        selection.viewerRole == ObservationAuthorRole.FAMILIA && selection.isCurrentWeek

    fun emptyMessageForStaff(selection: ObservationsSelection): String = when {
        selection.viewerRole == ObservationAuthorRole.EDUCADORA && selection.isCurrentWeek ->
            "Todavía no has agregado observaciones esta semana. Puedes dejar notas sobre el progreso o conducta del menor."
        selection.viewerRole == ObservationAuthorRole.EDUCADORA ->
            "No se registraron observaciones del personal en esta semana."
        else ->
            "El equipo de educadoras aún no ha registrado observaciones en esta semana."
    }

    fun emptyMessageForFamily(selection: ObservationsSelection): String = when {
        selection.viewerRole == ObservationAuthorRole.FAMILIA && selection.isCurrentWeek ->
            "Todavía no has escrito observaciones esta semana. Cuéntale a la estancia cómo ves a tu hijo o hija en casa."
        selection.viewerRole == ObservationAuthorRole.FAMILIA ->
            "No escribiste observaciones en esta semana."
        else ->
            "La familia no ha escrito observaciones en esta semana."
    }

    fun emptyMessage(selection: ObservationsSelection): String =
        if (selection.viewerRole == ObservationAuthorRole.EDUCADORA) {
            emptyMessageForStaff(selection)
        } else {
            emptyMessageForFamily(selection)
        }

    fun readOnlyHint(selection: ObservationsSelection): String? = when {
        canWrite(selection) -> null
        else -> "Solo puedes agregar observaciones en la semana actual."
    }
}
