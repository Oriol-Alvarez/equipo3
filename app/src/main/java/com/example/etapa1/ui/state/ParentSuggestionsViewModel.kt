package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import kotlinx.coroutines.CoroutineScope

data class ParentSuggestionsUiState(
    val mySuggestions: List<ParentSuggestion> = emptyList(),
    val isSendingSuccess: Boolean = false
)

class ParentSuggestionsViewModel(
    private val suggestionsRepository: SuggestionsRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _isSendingSuccess = MutableStateFlow(false)
    val isSendingSuccess: StateFlow<Boolean> = _isSendingSuccess.asStateFlow()

    val uiState: StateFlow<ParentSuggestionsUiState> = combine(
        suggestionsRepository.suggestions,
        _isSendingSuccess
    ) { allSuggestions, success ->
        ParentSuggestionsUiState(
            mySuggestions = allSuggestions,
            isSendingSuccess = success
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ParentSuggestionsUiState(mySuggestions = suggestionsRepository.suggestions.value)
    )

    fun sendSuggestion(
        category: SuggestionCategory,
        subject: String,
        content: String,
        childName: String = "Mateo García",
        roomName: String = "Sala 1A"
    ): Boolean {
        if (subject.isBlank() || content.isBlank()) return false

        val nowDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val nowTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val newSuggestion = ParentSuggestion(
            id = "sug_${System.currentTimeMillis()}",
            parentName = "María López (Mamá de Mateo)",
            childName = childName,
            roomName = roomName,
            date = "Hoy, $nowDate",
            time = nowTime,
            category = category,
            subject = subject.trim(),
            content = content.trim(),
            status = SuggestionStatus.PENDIENTE,
            response = null,
            responseDate = null,
            responderName = null
        )

        suggestionsRepository.addSuggestion(newSuggestion)
        _isSendingSuccess.value = true
        return true
    }
}
