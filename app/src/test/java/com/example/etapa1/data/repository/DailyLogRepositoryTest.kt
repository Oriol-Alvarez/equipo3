package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockDailyLogRepositoryImpl
import com.example.etapa1.domain.DailyLogValidator
import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyLogSampleData
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLogRepositoryTest {

    private val today = SimpleDate(2026, 10, 7) // miércoles
    private val child = "mateo_garcia"

    private fun emptyRepository() = MockDailyLogRepositoryImpl(initialLogs = emptyList())

    private val ingreso = IngresoLog(
        time = "08:10",
        assistantName = "Laura Gómez",
        deliveredBy = "María López",
        receivedBy = "Laura Gómez",
        isSick = false,
        hasSymptoms = false,
        arrivedClean = true,
        backpackComplete = true,
        goodPhysicalCondition = true
    )

    @Test
    fun saveIngreso_thenEstadiaAndEgreso_buildOneDailyLog() {
        val repository = emptyRepository()

        assertTrue(repository.saveIngreso(child, today, ingreso).isSuccess)
        assertTrue(repository.updateEstadia(child, today) { it.copy(breakfast = FoodConsumptionOption.TODO) }.isSuccess)
        assertTrue(repository.updateEstadia(child, today) { it.copy(lunch = FoodConsumptionOption.POCO) }.isSuccess)
        val egreso = EgresoLog(
            time = "15:00", goodPhysicalCondition = true, collectedBy = "María López",
            deliveredClean = true, belongingsComplete = true, deliveredByStaff = "Lucía Martín"
        )
        assertTrue(repository.saveEgreso(child, today, egreso).isSuccess)

        val log = repository.logFor(child, today)!!
        assertEquals("08:10", log.ingreso?.time)
        assertEquals(FoodConsumptionOption.TODO, log.estadia?.breakfast)
        assertEquals(FoodConsumptionOption.POCO, log.estadia?.lunch)
        assertEquals("15:00", log.egreso?.time)
        assertEquals(1, repository.logs.value.size)
    }

    @Test
    fun invalidData_isNotSaved() {
        val repository = emptyRepository()

        assertTrue(repository.saveIngreso(child, today, ingreso.copy(deliveredBy = "")).isFailure)
        assertTrue(repository.logs.value.isEmpty())
    }

    @Test
    fun egresoBeforeEntry_isRejected() {
        val repository = emptyRepository()
        repository.saveIngreso(child, today, ingreso)

        val result = repository.saveEgreso(
            child, today,
            EgresoLog(
                time = "07:00", goodPhysicalCondition = true, collectedBy = "Mamá",
                deliveredClean = true, belongingsComplete = true, deliveredByStaff = "Lucía"
            )
        )

        assertTrue(result.isFailure)
        assertEquals(null, repository.logFor(child, today)?.egreso)
    }

    @Test
    fun recordAttendanceTime_keepsOtherAnswers() {
        val repository = emptyRepository()
        repository.saveIngreso(child, today, ingreso)

        repository.recordAttendanceTime(child, today, isIngreso = true, time = "08:25 AM", observations = "")

        val saved = repository.logFor(child, today)!!.ingreso!!
        assertEquals("08:25", saved.time)
        assertEquals("María López", saved.deliveredBy)
        assertTrue(repository.recordAttendanceTime(child, today, false, "hora mala").isFailure)
    }

    @Test
    fun logsFor_returnsNullOnDaysWithoutRecords() {
        val repository = emptyRepository()
        repository.recordAttendanceTime(child, today, isIngreso = true, time = "08:00")

        val week = SchoolWeek.weekOf(today)
        val result = repository.logsFor(child, week)

        assertEquals(5, result.size)
        assertEquals(listOf(false, false, true, false, false), result.map { it != null })
    }

    @Test
    fun sampleData_isValid_hasNoFutureDays_andTodayIsStillOpen() {
        val children = MockDataRepository.childrenSala1A + MockDataRepository.parentChildren
        val logs = DailyLogSampleData.generate(children, today)

        assertTrue(logs.isNotEmpty())
        assertFalse(logs.any { SchoolWeek.isAfter(it.date, today) })
        assertTrue(logs.all { SchoolWeek.isSchoolDay(it.date) })
        logs.forEach { log ->
            log.ingreso?.let { assertTrue("Ingreso inválido ${log.childId} ${log.date}", DailyLogValidator.validateIngreso(it).isSuccess) }
            log.estadia?.let { assertTrue("Estadía inválida ${log.childId} ${log.date}", DailyLogValidator.validateEstadia(it).isSuccess) }
            log.egreso?.let {
                assertTrue(
                    "Egreso inválido ${log.childId} ${log.date}",
                    DailyLogValidator.validateEgreso(it, log.ingreso?.time).isSuccess
                )
            }
        }
        val todayLogs = logs.filter { it.date == today }
        assertTrue(todayLogs.isNotEmpty())
        assertTrue(todayLogs.all { it.egreso == null })
        // Mismo resultado cada vez (sin azar).
        assertEquals(logs, DailyLogSampleData.generate(children, today))
    }
}
