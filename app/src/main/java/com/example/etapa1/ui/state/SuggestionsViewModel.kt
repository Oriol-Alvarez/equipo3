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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SuggestionsUiState(
    val suggestions: List<ParentSuggestion> = emptyList(),
    val selectedStatusFilter: SuggestionStatus? = null,
    val selectedCategoryFilter: SuggestionCategory = SuggestionCategory.TODAS,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val inReviewCount: Int = 0,
    val attendedCount: Int = 0,
    val filteredSuggestions: List<ParentSuggestion> = emptyList()
)

private data class SuggestionFilterState(
    val status: SuggestionStatus? = null,
    val category: SuggestionCategory = SuggestionCategory.TODAS,
    val query: String = "",
    val isSearchOpen: Boolean = false
)

class SuggestionsViewModel(
    private val suggestionsRepository: SuggestionsRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(SuggestionFilterState())

    val uiState: StateFlow<SuggestionsUiState> = combine(
        suggestionsRepository.suggestions,
        _filterState
    ) { allSuggestions, filters ->
        val total = allSuggestions.size
        val pending = allSuggestions.count { it.status == SuggestionStatus.PENDIENTE }
        val inReview = allSuggestions.count { it.status == SuggestionStatus.EN_REVISION }
        val attended = allSuggestions.count { it.status == SuggestionStatus.ATENDIDA }

        val trimmedQuery = filters.query.trim()
        val filtered = allSuggestions.filter { item ->
            val matchesStatus = filters.status == null || item.status == filters.status
            val matchesCategory = filters.category == SuggestionCategory.TODAS || item.category == filters.category
            val matchesSearch = trimmedQuery.isBlank() ||
                item.subject.contains(trimmedQuery, ignoreCase = true) ||
                item.content.contains(trimmedQuery, ignoreCase = true) ||
                item.parentName.contains(trimmedQuery, ignoreCase = true) ||
                item.childName.contains(trimmedQuery, ignoreCase = true) ||
                item.roomName.contains(trimmedQuery, ignoreCase = true)
            matchesStatus && matchesCategory && matchesSearch
        }

        SuggestionsUiState(
            suggestions = allSuggestions,
            selectedStatusFilter = filters.status,
            selectedCategoryFilter = filters.category,
            searchQuery = filters.query,
            isSearchOpen = filters.isSearchOpen,
            totalCount = total,
            pendingCount = pending,
            inReviewCount = inReview,
            attendedCount = attended,
            filteredSuggestions = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SuggestionsUiState()
    )

    fun onStatusFilterSelected(status: SuggestionStatus?) {
        val current = _filterState.value
        _filterState.value = current.copy(status = if (current.status == status) null else status)
    }

    fun onCategoryFilterSelected(category: SuggestionCategory) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun onSearchQueryChanged(query: String) {
        _filterState.value = _filterState.value.copy(query = query)
    }

    fun toggleSearch() {
        val current = _filterState.value
        val next = !current.isSearchOpen
        _filterState.value = current.copy(
            isSearchOpen = next,
            query = if (!next) "" else current.query
        )
    }

    fun respondSuggestion(suggestionId: String, responseText: String, newStatus: SuggestionStatus) {
        suggestionsRepository.updateSuggestionStatus(
            id = suggestionId,
            status = newStatus,
            response = responseText,
            responderName = "Laura Méndez (Dirección)"
        )
    }
}
