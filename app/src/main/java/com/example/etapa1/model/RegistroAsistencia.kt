package com.example.etapa1.model

import java.util.Locale

/**
 * Todo lo que captura el formulario de asistencia (RF-03): hora y quién entrega o recoge.
 * Antes la pantalla solo mandaba la hora y las observaciones; los nombres se perdían.
 */
data class RegistroAsistencia(
    val isIngreso: Boolean,
    /** Hora de 24 h tal como la muestra el formulario, p. ej. "14:30". */
    val hora24: String,
    val observaciones: String = "",
    /** Ingreso: asistente al cuidado del niño. */
    val asistente: String = "",
    /** Ingreso: familiar que entrega al niño. */
    val entrega: String = "",
    /** Ingreso: personal que recibe. */
    val recibePersonal: String = "",
    /** Egreso: familiar o persona autorizada que recoge al niño. */
    val recoge: String = "",
    /** Egreso: personal que entrega al niño. */
    val entregaPersonal: String = "",
    val enfermo: Boolean = false,
    val medicamento: MedicationPrescriptionOption = MedicationPrescriptionOption.NO_APLICA,
    val conSintomas: Boolean = false,
    val sintomas: String = "",
    val limpio: Boolean = true,
    val mochilaCompleta: Boolean = true,
    val buenEstadoFisico: Boolean = true,
    val lesiones: String = ""
)

/** Conversión de horas para que la app muestre siempre "02:30 PM" (antes salía "14:30 AM"). */
object Horas {
    private val HORA_24 = Regex("^(\\d{1,2}):(\\d{2})$")

    /** "14:30" -> "02:30 PM"; "08:05" -> "08:05 AM". Si no se entiende, la regresa igual. */
    fun a12h(hora24: String): String {
        val m = HORA_24.matchEntire(hora24.trim()) ?: return hora24
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h !in 0..23 || min !in 0..59) return hora24
        val sufijo = if (h < 12) "AM" else "PM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", h12, min, sufijo)
    }
}
