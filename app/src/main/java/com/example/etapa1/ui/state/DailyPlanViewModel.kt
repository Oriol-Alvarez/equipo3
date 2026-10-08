package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.DailyPlanRepository
import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.FamilyGroupsSampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DailyPlanUiState(
    val today: SimpleDate,
    val weekDays: List<SimpleDate>,
    val selectedDate: SimpleDate,
    val dayTitle: String,
    val plan: DailyPlan? = null,
    val canEdit: Boolean = false,
    val isEditing: Boolean = false,
    val draft: DailyPlanDraft = DailyPlanDraft(),
    val errorMessage: String? = null,
    val savedMessage: String? = null
)

/** Resumen de "hoy" que se muestra en la sala (educadora) y en el inicio familiar. */
data class TodayPlanSummary(
    val date: SimpleDate,
    val title: String,
    val lunch: String?,
    val nextActivity: String?
)

private data class EditorState(
    val isEditing: Boolean = false,
    val draft: DailyPlanDraft = DailyPlanDraft(),
    val error: String? = null,
    val saved: String? = null
)

class DailyPlanViewModel(
    private val repository: DailyPlanRepository,
    private val today: () -> SimpleDate = { SimpleDate.today() }
) : ViewModel() {

    private val canEdit = MutableStateFlow(false)
    private val selectedDate = MutableStateFlow(SchoolWeek.nextSchoolDay(today()))
    private val editor = MutableStateFlow(EditorState())

    val uiState: StateFlow<DailyPlanUiState> = combine(
        repository.plans,
        selectedDate,
        canEdit,
        editor
    ) { plans, date, editable, edit ->
        val currentDay = today()
        DailyPlanUiState(
            today = currentDay,
            weekDays = SchoolWeek.schoolWeek(currentDay),
            selectedDate = date,
            dayTitle = DailyPlanPresenter.dayTitle(date, currentDay),
            plan = plans.find { it.date == date },
            canEdit = editable,
            isEditing = editable && edit.isEditing,
            draft = edit.draft,
            errorMessage = edit.error,
            savedMessage = edit.saved
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialState()
    )

    /** Planeación del próximo día de clases (hoy entre semana) para las tarjetas de acceso. */
    val todaySummary: StateFlow<TodayPlanSummary?> = repository.plans
        .map { summaryFor(SchoolWeek.nextSchoolDay(today())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = summaryFor(SchoolWeek.nextSchoolDay(today()))
        )

    /** La educadora puede editar; la familia solo consulta. */
    fun setCanEdit(value: Boolean) {
        canEdit.value = value
        if (!value) editor.value = EditorState()
    }

    fun selectDay(date: SimpleDate) {
        if (editor.value.isEditing) return
        selectedDate.value = date
        editor.value = EditorState()
    }

    fun startEditing() {
        if (!canEdit.value) return
        editor.value = EditorState(
            isEditing = true,
            draft = DailyPlanPresenter.draftFrom(repository.planFor(selectedDate.value))
        )
    }

    fun cancelEditing() {
        editor.value = EditorState()
    }

    fun updateDraft(transform: (DailyPlanDraft) -> DailyPlanDraft) {
        val current = editor.value
        if (!current.isEditing) return
        editor.value = current.copy(draft = transform(current.draft), error = null)
    }

    fun addActivity() = updateDraft { draft ->
        if (draft.activities.size >= DailyPlanPresenter.MAX_ACTIVITIES) draft
        else draft.copy(activities = draft.activities + ActivityDraft())
    }

    fun removeActivity(index: Int) = updateDraft { draft ->
        val remaining = draft.activities.filterIndexed { i, _ -> i != index }
        draft.copy(activities = remaining.ifEmpty { listOf(ActivityDraft()) })
    }

    fun updateActivity(index: Int, time: String, title: String) = updateDraft { draft ->
        draft.copy(
            activities = draft.activities.mapIndexed { i, row ->
                if (i == index) ActivityDraft(time.take(5), title.take(DailyPlanPresenter.MAX_TEXT_LENGTH)) else row
            }
        )
    }

    fun save(): Boolean {
        val current = editor.value
        if (!canEdit.value || !current.isEditing) return false
        return DailyPlanPresenter.toPlan(
            draft = current.draft,
            date = selectedDate.value,
            author = FamilyGroupsSampleData.EDUCADORA.displayName
        ).fold(
            onSuccess = { plan ->
                repository.savePlan(plan)
                editor.value = EditorState(saved = "Planeación publicada. Las familias ya pueden verla.")
                true
            },
            onFailure = {
                editor.value = current.copy(error = it.message)
                false
            }
        )
    }

    private fun summaryFor(date: SimpleDate): TodayPlanSummary {
        val plan = repository.planFor(date)
        val currentDay = today()
        return TodayPlanSummary(
            date = date,
            title = DailyPlanPresenter.dayTitle(date, currentDay),
            lunch = plan?.menu?.lunch,
            nextActivity = plan?.activities?.firstOrNull()?.let { "${it.time} · ${it.title}" }
        )
    }

    private fun initialState(): DailyPlanUiState {
        val currentDay = today()
        val date = selectedDate.value
        return DailyPlanUiState(
            today = currentDay,
            weekDays = SchoolWeek.schoolWeek(currentDay),
            selectedDate = date,
            dayTitle = DailyPlanPresenter.dayTitle(date, currentDay),
            plan = repository.planFor(date)
        )
    }
}
