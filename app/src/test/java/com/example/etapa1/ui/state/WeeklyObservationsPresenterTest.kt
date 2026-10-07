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

    private fun observation(id: String, childId: String, week: String) = WeeklyObservation(
        id = id,
        childId = childId,
        weekKey = week,
        authorRole = ObservationAuthorRole.FAMILIA,
        authorName = "Mamá",
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
    fun family_canWriteOnlyInCurrentWeek() {
        assertTrue(WeeklyObservationsPresenter.canWrite(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = true)))
        assertFalse(WeeklyObservationsPresenter.canWrite(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = false)))
    }

    @Test
    fun educadora_onlyReadsFamilyObservations() {
        assertFalse(WeeklyObservationsPresenter.canWrite(selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = true)))
        assertEquals(
            "La familia no ha escrito observaciones en esta semana.",
            WeeklyObservationsPresenter.emptyMessage(selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = true))
        )
        assertEquals(null, WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.EDUCADORA, isCurrentWeek = true)))
    }

    @Test
    fun family_pastWeek_showsWhyItCannotWrite() {
        assertEquals(
            "Solo puedes agregar observaciones en la semana actual.",
            WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = false))
        )
        assertEquals(null, WeeklyObservationsPresenter.readOnlyHint(selection(ObservationAuthorRole.FAMILIA, isCurrentWeek = true)))
    }

    @Test
    fun weekKeys_matchTheWeeklySummariesShownInTheApp() {
        val weekKeys = MockDataRepository.getWeeklySummariesForChild(MockDataRepository.mateoGarcia).map { it.weekRangeText }

        assertEquals(currentWeek, weekKeys[0])
        assertEquals(pastWeek, weekKeys[1])
        assertEquals("Todas las semanas deben tener una clave distinta", weekKeys.size, weekKeys.toSet().size)
    }
}
