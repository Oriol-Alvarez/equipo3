package com.example.etapa1.domain

import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MedicationPrescriptionOption
import com.example.etapa1.model.SphincterControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLogValidatorTest {

    private val validIngreso = IngresoLog(
        time = "8:05",
        assistantName = " Laura Gómez ",
        deliveredBy = "María López",
        receivedBy = "Laura Gómez",
        isSick = false,
        hasSymptoms = false,
        arrivedClean = true,
        backpackComplete = true,
        goodPhysicalCondition = true
    )

    private val validEgreso = EgresoLog(
        time = "15:30",
        goodPhysicalCondition = true,
        collectedBy = "María López",
        deliveredClean = true,
        belongingsComplete = true,
        deliveredByStaff = "Lucía Martín"
    )

    private fun ingresoError(ingreso: IngresoLog) = DailyLogValidator.validateIngreso(ingreso).exceptionOrNull()?.message
    private fun egresoError(egreso: EgresoLog, entry: String? = null) =
        DailyLogValidator.validateEgreso(egreso, entry).exceptionOrNull()?.message
    private fun estadiaError(estadia: EstadiaLog) = DailyLogValidator.validateEstadia(estadia).exceptionOrNull()?.message

    @Test
    fun normalizeTime_acceptsFormatsUsedInTheApp() {
        assertEquals("08:05", DailyLogValidator.normalizeTime("8:05"))
        assertEquals("08:15", DailyLogValidator.normalizeTime("08:15 AM"))
        assertEquals("14:10", DailyLogValidator.normalizeTime("02:10 PM"))
        assertEquals("00:30", DailyLogValidator.normalizeTime("12:30 AM"))
        assertEquals("12:30", DailyLogValidator.normalizeTime("12:30 PM"))
        // El formulario de asistencia agrega "AM" aunque la hora ya sea de 24 h.
        assertEquals("14:10", DailyLogValidator.normalizeTime("14:10 AM"))
        assertEquals(null, DailyLogValidator.normalizeTime("25:00"))
        assertEquals(null, DailyLogValidator.normalizeTime("8 am"))
        assertEquals(null, DailyLogValidator.normalizeTime(""))
    }

    @Test
    fun validIngreso_isNormalizedAndTrimmed() {
        val result = DailyLogValidator.validateIngreso(validIngreso).getOrThrow()

        assertEquals("08:05", result.time)
        assertEquals("Laura Gómez", result.assistantName)
        assertEquals(null, result.medicationWithPrescription)
    }

    @Test
    fun ingreso_requiredAnswers() {
        assertEquals("Escribe la hora de entrada como 08:15.", ingresoError(validIngreso.copy(time = "ocho")))
        assertEquals("Escribe quién entrega a la niña o el niño.", ingresoError(validIngreso.copy(deliveredBy = " ")))
        assertEquals("Indica si la niña o el niño está enferma(o).", ingresoError(validIngreso.copy(isSick = null)))
        assertEquals("Indica si trajo su mochila completa.", ingresoError(validIngreso.copy(backpackComplete = null)))
    }

    @Test
    fun ingreso_sickNeedsMedicationAnswer_andSymptomsNeedDetail() {
        assertEquals(
            "Indica si dejaron medicamento con receta médica.",
            ingresoError(validIngreso.copy(isSick = true))
        )
        assertTrue(
            DailyLogValidator.validateIngreso(
                validIngreso.copy(isSick = true, medicationWithPrescription = MedicationPrescriptionOption.NO)
            ).isSuccess
        )
        assertEquals("Describe los síntomas.", ingresoError(validIngreso.copy(hasSymptoms = true)))
    }

    @Test
    fun ingreso_badPhysicalConditionNeedsInjuryDescription() {
        assertEquals(
            "Describe la lesión y la parte del cuerpo.",
            ingresoError(validIngreso.copy(goodPhysicalCondition = false))
        )
        assertTrue(
            DailyLogValidator.validateIngreso(
                validIngreso.copy(goodPhysicalCondition = false, injuries = "Rasguño en brazo izquierdo")
            ).isSuccess
        )
    }

    @Test
    fun ingreso_longTextIsRejected() {
        val long = "a".repeat(DailyLogValidator.MAX_TEXT_LENGTH + 1)

        assertEquals(
            "Cada respuesta puede tener hasta 300 caracteres.",
            ingresoError(validIngreso.copy(observations = long))
        )
    }

    @Test
    fun egreso_cannotBeBeforeEntry() {
        assertEquals(
            "La hora de salida no puede ser antes de la entrada (08:05).",
            egresoError(validEgreso.copy(time = "07:59"), entry = "8:05")
        )
        assertTrue(DailyLogValidator.validateEgreso(validEgreso, "08:05").isSuccess)
        assertTrue(DailyLogValidator.validateEgreso(validEgreso, null).isSuccess)
    }

    @Test
    fun egreso_requiredAnswers() {
        assertEquals("Escribe quién recibe a la niña o el niño.", egresoError(validEgreso.copy(collectedBy = "")))
        assertEquals(
            "Indica si se entregó con sus pertenencias completas.",
            egresoError(validEgreso.copy(belongingsComplete = null))
        )
        assertEquals(
            "Escribe quién del personal entrega a la niña o el niño.",
            egresoError(validEgreso.copy(deliveredByStaff = ""))
        )
    }

    @Test
    fun estadia_rules() {
        assertTrue(DailyLogValidator.validateEstadia(EstadiaLog()).isSuccess)
        assertEquals(
            "Escribe los minutos de siesta (entre 1 y 300).",
            estadiaError(EstadiaLog(slept = true))
        )
        assertEquals(
            "El número de veces debe estar entre 0 y 20.",
            estadiaError(EstadiaLog(peeCount = 21))
        )
        assertEquals("Describe brevemente el accidente.", estadiaError(EstadiaLog(hadAccident = true)))
        assertEquals(
            "Indica la atención proporcionada.",
            estadiaError(EstadiaLog(hadHealthIssue = true, healthIssue = "Fiebre"))
        )
    }

    @Test
    fun estadia_clearsDetailsThatDoNotApply() {
        val result = DailyLogValidator.validateEstadia(
            EstadiaLog(slept = false, napMinutes = 40, hadAccident = false, accidentDescription = "x")
        ).getOrThrow()

        assertEquals(null, result.napMinutes)
        assertEquals("", result.accidentDescription)
    }

    @Test
    fun estadia_sphincterCountsOnlyWhenItApplies() {
        val noAplica = DailyLogValidator.validateEstadia(
            EstadiaLog(sphincterControl = SphincterControl.NO_APLICA, sphincterPeeCount = 2, peeCount = 4)
        ).getOrThrow()
        assertEquals(null, noAplica.sphincterPeeCount)
        assertEquals(4, noAplica.peeCount)

        val avisa = DailyLogValidator.validateEstadia(
            EstadiaLog(sphincterControl = SphincterControl.AVISO_SI, sphincterPeeCount = 2)
        ).getOrThrow()
        assertEquals(2, avisa.sphincterPeeCount)
        assertTrue(DailyLogValidator.validateEstadia(EstadiaLog(sphincterPoopCount = 25)).isFailure)
    }

    @Test
    fun attendanceFormTime_ignoraElSufijoPorqueElFormularioYaUsa24h() {
        assertEquals("11:30", DailyLogValidator.attendanceFormTime("11:30 PM"))
        assertEquals("12:30", DailyLogValidator.attendanceFormTime("12:30 AM"))
        assertEquals("15:05", DailyLogValidator.attendanceFormTime("15:05 PM"))
        assertEquals("08:10", DailyLogValidator.attendanceFormTime("08:10 AM"))
        assertEquals("08:10", DailyLogValidator.attendanceFormTime("08:10"))
        assertEquals(null, DailyLogValidator.attendanceFormTime(" AM"))
    }
}
