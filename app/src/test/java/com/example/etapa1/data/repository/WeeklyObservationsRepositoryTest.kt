package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockWeeklyObservationsRepositoryImpl
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WEEKLY_OBSERVATION_MAX_LENGTH
import com.example.etapa1.model.WeeklyObservationsSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WeeklyObservationsRepositoryTest {

    private lateinit var repository: MockWeeklyObservationsRepositoryImpl
    private val week = "Semana del 22 al 26 de Septiembre, 2026"

    @Before
    fun setUp() {
        repository = MockWeeklyObservationsRepositoryImpl(
            initialObservations = emptyList(),
            currentTime = { "26/09 08:00 PM" }
        )
    }

    @Test
    fun addObservation_savesTrimmedTextWithAuthorAndWeek() {
        val result = repository.addObservation(
            childId = "mateo_garcia",
            weekKey = week,
            authorRole = ObservationAuthorRole.FAMILIA,
            authorName = "María López (Mamá de Mateo)",
            text = "   Durmió mejor en casa.   "
        )

        assertTrue(result.isSuccess)
        val saved = repository.observations.value.single()
        assertEquals("Durmió mejor en casa.", saved.text)
        assertEquals("mateo_garcia", saved.childId)
        assertEquals(week, saved.weekKey)
        assertEquals(ObservationAuthorRole.FAMILIA, saved.authorRole)
        assertEquals("26/09 08:00 PM", saved.time)
    }

    @Test
    fun addObservation_blankText_fails() {
        val result = repository.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Mamá", "   ")

        assertTrue(result.isFailure)
        assertEquals("Escribe una observación antes de guardar.", result.exceptionOrNull()?.message)
        assertTrue(repository.observations.value.isEmpty())
    }

    @Test
    fun addObservation_tooLong_fails() {
        val longText = "a".repeat(WEEKLY_OBSERVATION_MAX_LENGTH + 1)

        val result = repository.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Mamá", longText)

        assertTrue(result.isFailure)
        assertTrue(repository.observations.value.isEmpty())
    }

    @Test
    fun addObservation_exactlyMaxLength_isAllowed() {
        val text = "a".repeat(WEEKLY_OBSERVATION_MAX_LENGTH)

        assertTrue(repository.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Mamá", text).isSuccess)
    }

    @Test
    fun addObservation_keepsOrderOfArrival() {
        repository.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Mamá", "Primera")
        repository.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Mamá", "Segunda")

        assertEquals(listOf("Primera", "Segunda"), repository.observations.value.map { it.text })
    }

    @Test
    fun sampleData_hasFamilyObservationForMateoInPreviousWeek() {
        val sample = MockWeeklyObservationsRepositoryImpl().observations.value.single()

        assertEquals("mateo_garcia", sample.childId)
        assertEquals(ObservationAuthorRole.FAMILIA, sample.authorRole)
        assertEquals(WeeklyObservationsSampleData.FAMILIA_GARCIA_NAME, sample.authorName)
        assertEquals("Semana del 15 al 19 de Septiembre, 2026", sample.weekKey)
    }
}
