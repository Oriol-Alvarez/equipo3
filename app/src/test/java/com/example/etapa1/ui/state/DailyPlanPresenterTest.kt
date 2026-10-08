package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockDailyPlanRepositoryImpl
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyPlanSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlanPresenterTest {

    private val date = SimpleDate(2026, 10, 7)

    private val validDraft = DailyPlanDraft(
        breakfast = " Avena ",
        snack = "Fruta",
        lunch = "Sopa y pollo",
        notes = "",
        activities = listOf(
            ActivityDraft("11:30", "Patio"),
            ActivityDraft("", ""),
            ActivityDraft("09:00", " Asamblea ")
        )
    )

    private fun errorOf(draft: DailyPlanDraft): String? =
        DailyPlanPresenter.toPlan(draft, date, "Educadora").exceptionOrNull()?.message

    @Test
    fun toPlan_trimsIgnoresBlankRowsAndSortsByTime() {
        val plan = DailyPlanPresenter.toPlan(validDraft, date, "Educadora Sala 1A").getOrThrow()

        assertEquals("Avena", plan.menu.breakfast)
        assertEquals(listOf("09:00", "11:30"), plan.activities.map { it.time })
        assertEquals("Asamblea", plan.activities.first().title)
        assertEquals(date, plan.date)
        assertEquals("Educadora Sala 1A", plan.updatedBy)
    }

    @Test
    fun missingMeal_fails() {
        assertEquals("Completa el desayuno, la colación y la comida.", errorOf(validDraft.copy(lunch = "  ")))
    }

    @Test
    fun noActivities_fails() {
        assertEquals(
            "Agrega al menos una actividad.",
            errorOf(validDraft.copy(activities = listOf(ActivityDraft(" ", ""))))
        )
    }

    @Test
    fun activityWithoutName_fails() {
        assertEquals(
            "Cada actividad necesita un nombre.",
            errorOf(validDraft.copy(activities = listOf(ActivityDraft("09:00", ""))))
        )
    }

    @Test
    fun invalidTime_fails() {
        assertEquals(
            "Escribe la hora como 09:30 (formato de 24 horas).",
            errorOf(validDraft.copy(activities = listOf(ActivityDraft("9:00", "Asamblea"))))
        )
        assertTrue(DailyPlanPresenter.isValidTime("23:59"))
        assertFalse(DailyPlanPresenter.isValidTime("24:00"))
        assertFalse(DailyPlanPresenter.isValidTime("10:60"))
    }

    @Test
    fun tooManyActivities_fails() {
        val many = (1..DailyPlanPresenter.MAX_ACTIVITIES + 1).map { ActivityDraft("09:00", "Actividad $it") }

        assertEquals("Puedes agregar hasta 10 actividades por día.", errorOf(validDraft.copy(activities = many)))
    }

    @Test
    fun draftFrom_existingPlan_roundTrips() {
        val plan = DailyPlanSampleData.weekFor(date).first()

        val draft = DailyPlanPresenter.draftFrom(plan)
        val again = DailyPlanPresenter.toPlan(draft, plan.date, plan.updatedBy).getOrThrow()

        assertEquals(plan, again)
        assertEquals(1, DailyPlanPresenter.draftFrom(null).activities.size)
    }

    @Test
    fun dayTitle_marksToday() {
        assertEquals("Hoy · miércoles 7 de octubre", DailyPlanPresenter.dayTitle(date, date))
        assertEquals("Viernes 9 de octubre", DailyPlanPresenter.dayTitle(SimpleDate(2026, 10, 9), date))
    }

    @Test
    fun repository_hasWholeWeek_andSaveReplacesThatDay() {
        val repository = MockDailyPlanRepositoryImpl(DailyPlanSampleData.weekFor(date))
        assertEquals(5, repository.plans.value.size)

        val edited = DailyPlanPresenter.toPlan(validDraft, date, "Educadora").getOrThrow()
        repository.savePlan(edited)

        assertEquals(5, repository.plans.value.size)
        assertEquals("Sopa y pollo", repository.planFor(date)?.menu?.lunch)
        assertEquals(null, repository.planFor(SimpleDate(2026, 10, 12)))
    }
}
