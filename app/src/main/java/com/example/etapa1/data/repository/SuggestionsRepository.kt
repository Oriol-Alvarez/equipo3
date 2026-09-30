package com.example.etapa1.data.repository

import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.flow.StateFlow

interface SuggestionsRepository {
    val suggestions: StateFlow<List<ParentSuggestion>>
    fun addSuggestion(suggestion: ParentSuggestion)
    fun updateSuggestionStatus(id: String, status: SuggestionStatus, response: String? = null, responderName: String? = null)
}
