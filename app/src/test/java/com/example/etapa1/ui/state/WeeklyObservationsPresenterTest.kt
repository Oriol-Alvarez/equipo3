package com.example.etapa1.ui.state

import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WeeklyObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyObservationsPresenterTest {

    private val currentWeek = "Semana del 22 al 26 de Septiembre, 2026"
    private val pastWeek = "Semana del 15 al 19 de Septiembre, 2026"

    private fun selection(role: ObservationAuthorRole, isCurrentWeek: Boolean) = ObservationsSelection(
        childId = "mateo_garcia",
        weekKey = if (isCurrentWeek) currentWeek else pastWeek,
        isCurrentWeek = isCurrentWeek,
        viewerRole = role,
        viewerName = "Quien ve"
    )

    private fun observation(
        id: String,
        childId: String,
        week: String,
        role: ObservationAuthorRole = ObservationAuthorRole.FAMILIA
    ) = WeeklyObservation(
        id = id,
        childId = childId,
        weekKey = week,
        authorRole = role,
        authorName = if (role == ObservationAuthorRole.FAMILIA) "Mamá" else "Laura Gómez",
        text = "Texto $id",
        time = "10:00 AM"
    )

    @Test
    fun observationsFor_returnsOnlyThatChildAndWeek_inOrder() {
        val all = listOf(
            observation("1", "mateo_garcia", currentWeek),
            observation("2", "sofia_lopez", currentWeek),
            observation("3", "mateo_garcia", pastWeek),
            observation("4", "mateo_garcia", currentWeek)
        )

        val result = WeeklyObservationsPresenter.observationsFor(all, "mateo_garcia", currentWeek)

        assertEquals(listOf("1", "4"), result.map { it.id })
    }

    @Test
    fun separates_educator_and_family_observations() {
        val obs1 = observation("1", "mateo_garcia", currentWeek, ObservationAuthorRole.EDUCADORA)
        val obs2 = observation("2", "mateo_garcia", currentWeek, ObservationAuthorRole.FAMILIA)
        val obs3 = observation("3", "mateo_garcia", currentWeek, ObservationAuthorRole.EDUCADORA)
        val all = listOf(obs1, obs2, obs3)

        val educator = WeeklyObservationsPresenter.educatorObservations(all, "mateo_garcia", currentWeek)
        val family = WeeklyObservationsPresenter.familyObservations(all, "mateo_garcia", currentWeek)

        assertEquals(listOf("1", "3"), educator.map { it.id })
        assertEquals(listOf("2"), family.map { it.id })
    }

    @Test
    fun family_canWriteOnlyInCurrentWeek() {
        val current = selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = true)
        val past = selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = false)

        assertTrue(WeeklyObservationsPresenter.canWrite(current))
        assertTrue(WeeklyObservationsPresenter.canWriteAsFamily(current))
        assertFalse(WeeklyObservationsPresenter.canWriteAsEducator(current))

        assertFalse(WeeklyObservationsPresenter.canWrite(past))
        assertFalse(WeeklyObservationsPresenter.canWriteAsFamily(past))
    }

    @Test
    fun educadora_canWriteOnlyInCurrentWeek() {
        val current = selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = true)
        val past = selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = false)

        assertTrue(WeeklyObservationsPresenter.canWrite(current))
        assertTrue(WeeklyObservationsPresenter.canWriteAsEducator(current))
        assertFalse(WeeklyObservationsPresenter.canWriteAsFamily(current))

        assertFalse(WeeklyObservationsPresenter.canWrite(past))
        assertFalse(WeeklyObservationsPresenter.canWriteAsEducator(past))
    }

    @Test
    fun pastWeek_showsWhyItCannotWrite() {
        assertEquals(
            "Solo puedes agregar observaciones en la semana actual.",
            WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = false))
        )
        assertEquals(
            "Solo puedes agregar observaciones en la semana actual.",
            WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = false))
        )
        assertEquals(null, WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = true)))
        assertEquals(null, WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = true)))
    }

    @Test
    fun weekKeys_matchTheWeeklySummariesShownInTheApp() {
        val weekKeys = MockDataRepository.getWeeklySummariesForChild(MockDataRepository.mateoGarcia).map { it.weekRangeText }

        assertEquals(currentWeek, weekKeys[0])
        assertEquals(pastWeek, weekKeys[1])
        assertEquals("Todas las semanas deben tener una clave distinta", weekKeys.size, weekKeys.toSet().size)
    }
}
