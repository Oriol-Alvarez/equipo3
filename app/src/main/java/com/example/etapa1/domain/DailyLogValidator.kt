package com.example.etapa1.domain

import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.SphincterControl

/**
 * Reglas para que un registro diario quede completo y coherente con la hoja oficial.
 * Sin dependencias de Android para poder probarlas con pruebas unitarias.
 */
object DailyLogValidator {

    const val MAX_TEXT_LENGTH = 300
    const val MAX_NAP_MINUTES = 300
    const val MAX_COUNT = 20

    private val TIME_24H = Regex("^(\\d{1,2}):(\\d{2})$")
    private val TIME_12H = Regex("^(\\d{1,2}):(\\d{2})\\s*([AaPp])\\.?\\s*[Mm]\\.?$")

    /**
     * Convierte la hora a "HH:mm" (24 h). Acepta "8:05", "08:05", "08:05 AM" o "02:10 PM".
     * Si la hora ya viene en formato de 24 h (p. ej. "14:10 AM") se ignora el AM/PM.
     * Devuelve null si no es una hora válida.
     */
    fun normalizeTime(raw: String): String? {
        val text = raw.trim()
        TIME_24H.matchEntire(text)?.let { match ->
            val (h, m) = match.destructured
            return format(h.toInt(), m.toInt())
        }
        TIME_12H.matchEntire(text)?.let { match ->
            val (h, m, suffix) = match.destructured
            var hour = h.toInt()
            if (hour in 1..12) {
                val isPm = suffix.equals("p", ignoreCase = true)
                hour = when {
                    isPm && hour != 12 -> hour + 12
                    !isPm && hour == 12 -> 0
                    else -> hour
                }
            }
            return format(hour, m.toInt())
        }
        return null
    }

    /**
     * Hora que manda el formulario de asistencia: ya viene en 24 h ("14:10 hrs.") pero se le
     * agrega " AM" o " PM" al final. Ese sufijo se ignora para no convertir 11:30 en 23:30.
     */
    fun attendanceFormTime(raw: String): String? =
        normalizeTime(raw.trim().replace(Regex("\\s*[AaPp]\\.?\\s*[Mm]\\.?$"), ""))

    fun validateIngreso(ingreso: IngresoLog): Result<IngresoLog> {
        val time = normalizeTime(ingreso.time) ?: return fail("Escribe la hora de entrada como 08:15.")
        return when {
            ingreso.assistantName.isBlank() -> fail("Escribe el nombre de la asistente al cuidado.")
            ingreso.deliveredBy.isBlank() -> fail("Escribe quién entrega a la niña o el niño.")
            ingreso.receivedBy.isBlank() -> fail("Escribe quién recibe a la niña o el niño en la estancia.")
            ingreso.isSick == null -> fail("Indica si la niña o el niño está enferma(o).")
            ingreso.isSick && ingreso.medicationWithPrescription == null ->
                fail("Indica si dejaron medicamento con receta médica.")
            ingreso.hasSymptoms == null -> fail("Indica si ingresa con síntomas de enfermedad.")
            ingreso.hasSymptoms && ingreso.symptoms.isBlank() -> fail("Describe los síntomas.")
            ingreso.arrivedClean == null -> fail("Indica si se presentó limpia(o).")
            ingreso.backpackComplete == null -> fail("Indica si trajo su mochila completa.")
            ingreso.goodPhysicalCondition == null -> fail("Indica si se presentó en buen estado físico.")
            !ingreso.goodPhysicalCondition && ingreso.injuries.isBlank() ->
                fail("Describe la lesión y la parte del cuerpo.")
            tooLong(
                ingreso.assistantName, ingreso.deliveredBy, ingreso.receivedBy,
                ingreso.symptoms, ingreso.injuries, ingreso.observations
            ) -> fail(tooLongMessage())
            else -> Result.success(
                ingreso.copy(
                    time = time,
                    assistantName = ingreso.assistantName.trim(),
                    deliveredBy = ingreso.deliveredBy.trim(),
                    receivedBy = ingreso.receivedBy.trim(),
                    // Sin enfermedad no aplica la pregunta de medicamento.
                    medicationWithPrescription = if (ingreso.isSick) ingreso.medicationWithPrescription else null,
                    symptoms = if (ingreso.hasSymptoms) ingreso.symptoms.trim() else "",
                    injuries = ingreso.injuries.trim(),
                    observations = ingreso.observations.trim()
                )
            )
        }
    }

    fun validateEstadia(estadia: EstadiaLog): Result<EstadiaLog> = when {
        estadia.slept == true && (estadia.napMinutes == null || estadia.napMinutes !in 1..MAX_NAP_MINUTES) ->
            fail("Escribe los minutos de siesta (entre 1 y $MAX_NAP_MINUTES).")
        listOfNotNull(estadia.peeCount, estadia.poopCount, estadia.sphincterPeeCount, estadia.sphincterPoopCount).any { it !in 0..MAX_COUNT } ->
            fail("El número de veces debe estar entre 0 y $MAX_COUNT.")
        estadia.hadAccident == true && estadia.accidentDescription.isBlank() ->
            fail("Describe brevemente el accidente.")
        estadia.hadHealthIssue == true && estadia.healthIssue.isBlank() ->
            fail("Indica qué problema de salud presentó.")
        estadia.hadHealthIssue == true && estadia.careProvided.isBlank() ->
            fail("Indica la atención proporcionada.")
        tooLong(
            estadia.foodNotes, estadia.napComments, estadia.excretionNotes, estadia.moodNotes,
            estadia.accidentDescription, estadia.accidentFolio, estadia.healthIssue,
            estadia.careProvided, estadia.generalNotes
        ) -> fail(tooLongMessage())
        else -> Result.success(
            estadia.copy(
                napMinutes = if (estadia.slept == true) estadia.napMinutes else null,
                sphincterPeeCount = if (hasSphincterControl(estadia)) estadia.sphincterPeeCount else null,
                sphincterPoopCount = if (hasSphincterControl(estadia)) estadia.sphincterPoopCount else null,
                accidentDescription = if (estadia.hadAccident == true) estadia.accidentDescription.trim() else "",
                accidentFolio = if (estadia.hadAccident == true) estadia.accidentFolio.trim() else "",
                healthIssue = if (estadia.hadHealthIssue == true) estadia.healthIssue.trim() else "",
                careProvided = if (estadia.hadHealthIssue == true) estadia.careProvided.trim() else ""
            )
        )
    }

    private fun hasSphincterControl(estadia: EstadiaLog): Boolean =
        estadia.sphincterControl != null && estadia.sphincterControl != SphincterControl.NO_APLICA

    /** [ingresoTime] es la hora de entrada del mismo día, si existe, para evitar salidas antes de la entrada. */
    fun validateEgreso(egreso: EgresoLog, ingresoTime: String? = null): Result<EgresoLog> {
        val time = normalizeTime(egreso.time) ?: return fail("Escribe la hora de salida como 15:30.")
        val entry = ingresoTime?.let { normalizeTime(it) }
        return when {
            entry != null && time < entry -> fail("La hora de salida no puede ser antes de la entrada ($entry).")
            egreso.goodPhysicalCondition == null -> fail("Indica si se entrega en buen estado físico.")
            !egreso.goodPhysicalCondition && egreso.injuries.isBlank() ->
                fail("Describe la lesión y la parte del cuerpo.")
            egreso.collectedBy.isBlank() -> fail("Escribe quién recibe a la niña o el niño.")
            egreso.deliveredClean == null -> fail("Indica si se entregó limpia(o).")
            egreso.belongingsComplete == null -> fail("Indica si se entregó con sus pertenencias completas.")
            egreso.deliveredByStaff.isBlank() -> fail("Escribe quién del personal entrega a la niña o el niño.")
            tooLong(egreso.injuries, egreso.collectedBy, egreso.observations, egreso.deliveredByStaff) ->
                fail(tooLongMessage())
            else -> Result.success(
                egreso.copy(
                    time = time,
                    injuries = egreso.injuries.trim(),
                    collectedBy = egreso.collectedBy.trim(),
                    observations = egreso.observations.trim(),
                    deliveredByStaff = egreso.deliveredByStaff.trim()
                )
            )
        }
    }

    private fun format(hour: Int, minute: Int): String? =
        if (hour in 0..23 && minute in 0..59) "%02d:%02d".format(java.util.Locale.US, hour, minute) else null

    private fun tooLong(vararg texts: String) = texts.any { it.trim().length > MAX_TEXT_LENGTH }

    private fun tooLongMessage() = "Cada respuesta puede tener hasta $MAX_TEXT_LENGTH caracteres."

    private fun <T> fail(message: String): Result<T> = Result.failure(IllegalArgumentException(message))
}
