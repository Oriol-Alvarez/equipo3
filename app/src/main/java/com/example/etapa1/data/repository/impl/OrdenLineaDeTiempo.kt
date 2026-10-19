package com.example.etapa1.data.repository.impl

import com.example.etapa1.model.TimelineItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Orden cronológico (fecha y hora) de la línea de tiempo de la bitácora. */
object OrdenLineaDeTiempo {

    private val formatosFecha = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
    private val formatosHora = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")

    fun ordenar(items: List<TimelineItem>): List<TimelineItem> =
        items.sortedBy { marcaDeTiempo(it.date, it.time) }

    fun minutosDelDia(hora: String): Int {
        val limpia = limpiarHora(hora)
        for (formato in formatosHora) {
            try {
                val parsed = SimpleDateFormat(formato, Locale.US).apply { isLenient = false }.parse(limpia)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply { time = parsed }
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }
            } catch (_: Exception) {
            }
        }
        return 9999
    }

    fun marcaDeTiempo(fecha: String, hora: String): Long {
        val fechaLimpia = fecha.trim().ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()) }
        for (formato in formatosFecha) {
            try {
                val dia = SimpleDateFormat(formato, Locale.US).apply { isLenient = false }.parse(fechaLimpia)
                if (dia != null) {
                    val minutos = minutosDelDia(hora)
                    return dia.time + (if (minutos == 9999) 0 else minutos) * 60_000L
                }
            } catch (_: Exception) {
            }
        }
        return minutosDelDia(hora).toLong() * 60_000L
    }

    private fun limpiarHora(hora: String): String =
        hora.trim().uppercase(Locale.US)
            .replace(".", "")
            .replace("A M", "AM")
            .replace("P M", "PM")
}
