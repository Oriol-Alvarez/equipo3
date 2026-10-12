package com.example.etapa1.ui.state

import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.Child
import com.example.etapa1.model.DailyLog
import java.text.Normalizer

/** Semana que se puede exportar (lunes a viernes). */
data class ExportWeekOption(val label: String, val rangeText: String, val days: List<SimpleDate>)

/** Qué se registró cada día, para mostrarlo antes de generar el PDF. */
data class ExportDayStatus(
    val date: SimpleDate,
    val label: String,
    val hasIngreso: Boolean,
    val hasEstadia: Boolean,
    val hasEgreso: Boolean,
    val isFuture: Boolean
) {
    val hasAnyRecord: Boolean get() = hasIngreso || hasEstadia || hasEgreso
}

/** Reglas de la pantalla de exportación, sin Android para poder probarlas. */
object ExportLogPresenter {

    const val WEEKS_AVAILABLE = 4

    private val WEEK_NAMES = listOf("Esta semana", "Semana pasada", "Hace 2 semanas", "Hace 3 semanas")

    fun weekOptions(today: SimpleDate): List<ExportWeekOption> =
        (0 until WEEKS_AVAILABLE).map { index ->
            val days = SchoolWeek.weekOf(SchoolWeek.plusDays(today, -7 * index))
            ExportWeekOption(
                label = WEEK_NAMES[index],
                rangeText = "${SchoolWeek.numericLabel(days.first()).dropLast(5)} – ${SchoolWeek.numericLabel(days.last())}",
                days = days
            )
        }

    fun dayStatuses(days: List<SimpleDate>, logs: List<DailyLog?>, today: SimpleDate): List<ExportDayStatus> =
        days.zip(logs).map { (date, log) ->
            ExportDayStatus(
                date = date,
                label = SchoolWeek.shortLabel(date),
                hasIngreso = log?.ingreso != null,
                hasEstadia = log?.estadia != null,
                hasEgreso = log?.egreso != null,
                isFuture = SchoolWeek.isAfter(date, today)
            )
        }

    /** Ej. "Bitacora_Mateo_Garcia_2026-10-05.pdf" (sin acentos ni espacios para que abra en cualquier app). */
    fun fileName(child: Child, monday: SimpleDate): String {
        val plain = Normalizer.normalize(child.fullName, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
        val date = String.format(java.util.Locale.US, "%04d-%02d-%02d", monday.year, monday.month, monday.day)
        return "Bitacora_${plain}_$date.pdf"
    }
}
