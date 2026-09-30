package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockSuggestionsRepositoryImpl : SuggestionsRepository {
    private val _suggestions = MutableStateFlow<List<ParentSuggestion>>(MockDataRepository.getInitialSuggestions())
    override val suggestions: StateFlow<List<ParentSuggestion>> = _suggestions.asStateFlow()

    override fun addSuggestion(suggestion: ParentSuggestion) {
        _suggestions.update { listOf(suggestion) + it }
    }

    override fun updateSuggestionStatus(
        id: String,
        status: SuggestionStatus,
        response: String?,
        responderName: String?
    ) {
        _suggestions.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(
                        status = status,
                        response = response ?: item.response,
                        responseDate = if (response != null) "Hoy, ahora" else item.responseDate,
                        responderName = responderName ?: item.responderName
                    )
                } else item
            }
        }
    }
}
