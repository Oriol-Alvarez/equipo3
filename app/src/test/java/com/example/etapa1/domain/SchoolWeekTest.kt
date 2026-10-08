package com.example.etapa1.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolWeekTest {

    private val wednesday = SimpleDate(2026, 10, 7)
    private val saturday = SimpleDate(2026, 10, 10)

    @Test
    fun schoolWeek_isMondayToFridayOfCurrentWeek() {
        val week = SchoolWeek.schoolWeek(wednesday)

        assertEquals(SimpleDate(2026, 10, 5), week.first())
        assertEquals(SimpleDate(2026, 10, 9), week.last())
        assertEquals(5, week.size)
    }

    @Test
    fun onWeekend_usesNextWeek() {
        assertEquals(SimpleDate(2026, 10, 12), SchoolWeek.nextSchoolDay(saturday))
        assertEquals(SimpleDate(2026, 10, 12), SchoolWeek.schoolWeek(saturday).first())
    }

    @Test
    fun weekCrossingMonth_isCorrect() {
        val week = SchoolWeek.schoolWeek(SimpleDate(2026, 10, 29))

        assertEquals(SimpleDate(2026, 10, 26), week.first())
        assertEquals(SimpleDate(2026, 10, 30), week.last())
        assertEquals(SimpleDate(2026, 11, 2), SchoolWeek.plusDays(SimpleDate(2026, 10, 30), 3))
    }

    @Test
    fun schoolDay_andLabels() {
        assertTrue(SchoolWeek.isSchoolDay(wednesday))
        assertFalse(SchoolWeek.isSchoolDay(saturday))
        assertEquals(wednesday, SchoolWeek.nextSchoolDay(wednesday))
        assertEquals("Mié 7", SchoolWeek.shortLabel(wednesday))
        assertEquals("Lun 5", SchoolWeek.shortLabel(SimpleDate(2026, 10, 5)))
    }
}
