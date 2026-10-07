package com.example.etapa1.domain

import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayCalculatorTest {

    private val today = SimpleDate(2026, 10, 7)

    @Test
    fun parse_readsProfileFormat_andRejectsInvalidDates() {
        assertEquals(SimpleDate(2024, 4, 14), SimpleDate.parse("14/04/2024"))
        assertEquals(null, SimpleDate.parse("31/02/2024"))
        assertEquals(null, SimpleDate.parse("14-04-2024"))
        assertEquals(null, SimpleDate.parse(""))
    }

    @Test
    fun birthdayLaterThisYear_countsDaysAndAge() {
        val result = BirthdayCalculator.upcoming(SimpleDate(2024, 10, 9), "mia", "Mia Ramírez", today)

        assertEquals(SimpleDate(2026, 10, 9), result.date)
        assertEquals(2, result.daysUntil)
        assertEquals(2, result.turningAge)
        assertFalse(result.isToday)
    }

    @Test
    fun birthdayToday_isZeroDays() {
        val result = BirthdayCalculator.upcoming(SimpleDate(2024, 10, 7), "x", "Niño", today)

        assertEquals(0, result.daysUntil)
        assertTrue(result.isToday)
    }

    @Test
    fun birthdayAlreadyPassed_movesToNextYear() {
        val result = BirthdayCalculator.upcoming(SimpleDate(2024, 4, 14), "mateo", "Mateo García", today)

        assertEquals(SimpleDate(2027, 4, 14), result.date)
        assertEquals(3, result.turningAge)
        assertEquals(189, result.daysUntil)
    }

    @Test
    fun countsAcrossNewYear() {
        val result = BirthdayCalculator.upcoming(SimpleDate(2024, 1, 2), "x", "Niño", SimpleDate(2026, 12, 30))

        assertEquals(3, result.daysUntil)
        assertEquals(SimpleDate(2027, 1, 2), result.date)
    }

    @Test
    fun leapDay_isCelebratedOnFeb28InNonLeapYears() {
        assertEquals(
            SimpleDate(2027, 2, 28),
            BirthdayCalculator.nextBirthday(SimpleDate(2024, 2, 29), SimpleDate(2027, 1, 1))
        )
        assertEquals(
            SimpleDate(2028, 2, 29),
            BirthdayCalculator.nextBirthday(SimpleDate(2024, 2, 29), SimpleDate(2028, 1, 1))
        )
    }

    @Test
    fun upcomingWithin_keepsOnlyWindow_sortedByClosest() {
        val list = listOf(
            UpcomingBirthday("b", "Bruno", SimpleDate(2026, 10, 11), 4, 2),
            UpcomingBirthday("m", "Mia", SimpleDate(2026, 10, 9), 2, 2),
            UpcomingBirthday("l", "Lejano", SimpleDate(2026, 12, 1), 55, 2),
            UpcomingBirthday("h", "Hoy", SimpleDate(2026, 10, 7), 0, 2)
        )

        val result = BirthdayCalculator.upcomingWithin(list, days = 7)

        assertEquals(listOf("Hoy", "Mia", "Bruno"), result.map { it.childName })
    }

    @Test
    fun texts_areInSpanish() {
        assertEquals("viernes 9 de octubre", BirthdayCalculator.formatDate(SimpleDate(2026, 10, 9)))
        assertEquals("domingo 11 de octubre", BirthdayCalculator.formatDate(SimpleDate(2026, 10, 11)))
        assertEquals("Hoy", BirthdayCalculator.whenText(0))
        assertEquals("Mañana", BirthdayCalculator.whenText(1))
        assertEquals("En 4 días", BirthdayCalculator.whenText(4))
        assertEquals("1 año", BirthdayCalculator.ageText(1))
        assertEquals("2 años", BirthdayCalculator.ageText(2))
    }

    @Test
    fun roomData_everyChildHasValidBirthDate_andOctoberBirthdaysShowUp() {
        val children = MockDataRepository.childrenSala1A
        val birthdays = children.map { child ->
            val date = SimpleDate.parse(MockDataRepository.getChildFullProfile(child).birthDate)
            assertTrue("Fecha inválida para ${child.id}", date != null)
            BirthdayCalculator.upcoming(date!!, child.id, child.fullName, today)
        }

        val thisWeek = BirthdayCalculator.upcomingWithin(birthdays, days = 7)

        assertEquals(listOf("mia_ramirez", "bruno_benitez"), thisWeek.map { it.childId })
    }
}
