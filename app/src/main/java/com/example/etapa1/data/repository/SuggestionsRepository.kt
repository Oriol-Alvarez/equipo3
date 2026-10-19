package com.example.etapa1.data.repository

import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.flow.StateFlow

interface SuggestionsRepository {
    val suggestions: StateFlow<List<ParentSuggestion>>

    /** [childId]: niño al que se refiere la sugerencia (la familia solo puede usar a sus hijos). */
    fun addSuggestion(suggestion: ParentSuggestion, childId: String = "")
    fun updateSuggestionStatus(id: String, status: SuggestionStatus, response: String? = null, responderName: String? = null)

    /** Vuelve a pedir la lista al servidor. */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
