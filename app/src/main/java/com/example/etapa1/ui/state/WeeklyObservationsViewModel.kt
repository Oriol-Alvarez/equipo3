package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.WeeklyObservationsRepository
import com.example.etapa1.model.WEEKLY_OBSERVATION_MAX_LENGTH
import com.example.etapa1.model.WeeklyObservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WeeklyObservationsUiState(
    /** null mientras la pantalla todavía no indica qué semana se está viendo. */
    val selection: ObservationsSelection? = null,
    val observations: List<WeeklyObservation> = emptyList(),
    val canWrite: Boolean = false,
    val draft: String = "",
    val maxLength: Int = WEEKLY_OBSERVATION_MAX_LENGTH,
    val emptyMessage: String = "",
    val readOnlyHint: String? = null,
    val errorMessage: String? = null,
    /** Se muestra un momento después de guardar para confirmar a la familia. */
    val savedMessage: String? = null
)

private data class Feedback(val error: String? = null, val saved: String? = null)

class WeeklyObservationsViewModel(
    private val repository: WeeklyObservationsRepository
) : ViewModel() {

    private val selection = MutableStateFlow<ObservationsSelection?>(null)
    private val draft = MutableStateFlow("")
    private val feedback = MutableStateFlow(Feedback())

    val uiState: StateFlow<WeeklyObservationsUiState> = combine(
        repository.observations,
        selection,
        draft,
        feedback
    ) { all, current, currentDraft, currentFeedback ->
        if (current == null) {
            WeeklyObservationsUiState()
        } else {
            WeeklyObservationsUiState(
                selection = current,
                observations = WeeklyObservationsPresenter.observationsFor(all, current.childId, current.weekKey),
                canWrite = WeeklyObservationsPresenter.canWrite(current),
                draft = currentDraft,
                emptyMessage = WeeklyObservationsPresenter.emptyMessage(current),
                readOnlyHint = WeeklyObservationsPresenter.readOnlyHint(current),
                errorMessage = currentFeedback.error,
                savedMessage = currentFeedback.saved
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklyObservationsUiState()
    )

    /** La pantalla avisa qué niño y qué semana se están viendo; al cambiar, se limpia el borrador. */
    fun select(newSelection: ObservationsSelection) {
        if (selection.value == newSelection) return
        val sameWeek = selection.value?.let {
            it.childId == newSelection.childId && it.weekKey == newSelection.weekKey
        } ?: false
        selection.value = newSelection
        if (!sameWeek) {
            draft.value = ""
            feedback.value = Feedback()
        }
    }

    fun onDraftChange(text: String) {
        draft.value = text.take(WEEKLY_OBSERVATION_MAX_LENGTH)
        feedback.value = Feedback()
    }

    fun submit(): Boolean {
        val current = selection.value ?: return false
        if (!WeeklyObservationsPresenter.canWrite(current)) {
            feedback.value = Feedback(error = "No puedes agregar observaciones en esta semana.")
            return false
        }
        return repository.addObservation(
            childId = current.childId,
            weekKey = current.weekKey,
            authorRole = current.viewerRole,
            authorName = current.viewerName,
            text = draft.value
        ).fold(
            onSuccess = {
                draft.value = ""
                feedback.value = Feedback(saved = "Observación guardada. La educadora ya puede verla.")
                true
            },
            onFailure = {
                feedback.value = Feedback(error = it.message)
                false
            }
        )
    }
}
