package com.example.etapa1.ui.state

import com.example.etapa1.domain.BirthdayCalculator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyMenu
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.PlannedActivity

/** Fila editable de actividad (lo que escribe la educadora antes de guardar). */
data class ActivityDraft(val time: String = "", val title: String = "")

/** Formulario de planeación y menú mientras la educadora lo edita. */
data class DailyPlanDraft(
    val breakfast: String = "",
    val snack: String = "",
    val lunch: String = "",
    val notes: String = "",
    val activities: List<ActivityDraft> = listOf(ActivityDraft())
)

/** Reglas de la planeación diaria, sin dependencias de Android para poder probarlas. */
object DailyPlanPresenter {

    const val MAX_TEXT_LENGTH = 120
    const val MAX_ACTIVITIES = 10

    private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun isValidTime(time: String): Boolean = TIME_REGEX.matches(time.trim())

    fun draftFrom(plan: DailyPlan?): DailyPlanDraft = if (plan == null) {
        DailyPlanDraft()
    } else {
        DailyPlanDraft(
            breakfast = plan.menu.breakfast,
            snack = plan.menu.snack,
            lunch = plan.menu.lunch,
            notes = plan.menu.notes,
            activities = plan.activities.map { ActivityDraft(it.time, it.title) }.ifEmpty { listOf(ActivityDraft()) }
        )
    }

    /**
     * Convierte el formulario en una planeación lista para publicar.
     * Las filas de actividad totalmente vacías se ignoran; las actividades se ordenan por hora.
     */
    fun toPlan(draft: DailyPlanDraft, date: SimpleDate, author: String): Result<DailyPlan> {
        val breakfast = draft.breakfast.trim()
        val snack = draft.snack.trim()
        val lunch = draft.lunch.trim()
        val notes = draft.notes.trim()

        if (breakfast.isEmpty() || snack.isEmpty() || lunch.isEmpty()) {
            return failure("Completa el desayuno, la colación y la comida.")
        }
        if (listOf(breakfast, snack, lunch, notes).any { it.length > MAX_TEXT_LENGTH }) {
            return failure("Cada campo del menú puede tener hasta $MAX_TEXT_LENGTH caracteres.")
        }

        val rows = draft.activities
            .map { ActivityDraft(it.time.trim(), it.title.trim()) }
            .filter { it.time.isNotEmpty() || it.title.isNotEmpty() }

        if (rows.isEmpty()) return failure("Agrega al menos una actividad.")
        if (rows.size > MAX_ACTIVITIES) return failure("Puedes agregar hasta $MAX_ACTIVITIES actividades por día.")
        if (rows.any { it.title.isEmpty() }) return failure("Cada actividad necesita un nombre.")
        if (rows.any { it.title.length > MAX_TEXT_LENGTH }) {
            return failure("Cada actividad puede tener hasta $MAX_TEXT_LENGTH caracteres.")
        }
        if (rows.any { !isValidTime(it.time) }) return failure("Escribe la hora como 09:30 (formato de 24 horas).")

        return Result.success(
            DailyPlan(
                date = date,
                menu = DailyMenu(breakfast = breakfast, snack = snack, lunch = lunch, notes = notes),
                activities = rows.map { PlannedActivity(it.time, it.title) }.sortedBy { it.time },
                updatedBy = author
            )
        )
    }

    /** Ej. "Hoy · miércoles 7 de octubre" o "Viernes 9 de octubre". */
    fun dayTitle(date: SimpleDate, today: SimpleDate): String {
        val formatted = BirthdayCalculator.formatDate(date)
        return if (date == today) "Hoy · $formatted" else formatted.replaceFirstChar { it.uppercase() }
    }

    private fun failure(message: String): Result<DailyPlan> = Result.failure(IllegalArgumentException(message))
}
