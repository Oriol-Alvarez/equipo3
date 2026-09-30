package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LostObjectsUiState(
    val items: List<LostItem> = emptyList(),
    val categories: List<String> = listOf("Todos", "Ropa", "Juguetes", "Útiles", "Otros"),
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isWorkerRole: Boolean = true,
    val claimedItemsMap: Map<String, Boolean> = emptyMap(),
    val filteredItems: List<LostItem> = emptyList()
)

private data class LostFilterState(
    val category: String = "Todos",
    val query: String = "",
    val isSearchOpen: Boolean = false
)

class LostObjectsViewModel(
    private val lostObjectsRepository: LostObjectsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(LostFilterState())

    val uiState: StateFlow<LostObjectsUiState> = combine(
        lostObjectsRepository.lostItems,
        lostObjectsRepository.claimedItems,
        authRepository.currentUserRole,
        _filterState
    ) { items, claimedMap, role, filters ->
        val baseCategories = listOf("Todos", "Ropa", "Juguetes", "Útiles", "Otros")
        val allCategories = (baseCategories + items.map { it.category }).distinct()

        val isWorker = role != UserRole.FAMILIAR
        val trimmedQuery = filters.query.trim()

        val filtered = items.filter { item ->
            val matchesCategory = filters.category == "Todos" || item.category.equals(filters.category, ignoreCase = true)
            val matchesSearch = trimmedQuery.isBlank() ||
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                item.description.contains(trimmedQuery, ignoreCase = true) ||
                item.category.contains(trimmedQuery, ignoreCase = true) ||
                (!item.location.isNullOrBlank() && item.location.contains(trimmedQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }

        LostObjectsUiState(
            items = items,
            categories = allCategories,
            selectedCategory = filters.category,
            searchQuery = filters.query,
            isSearchOpen = filters.isSearchOpen,
            isWorkerRole = isWorker,
            claimedItemsMap = claimedMap,
            filteredItems = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LostObjectsUiState()
    )

    fun selectCategory(category: String) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun onSearchQueryChange(query: String) {
        _filterState.value = _filterState.value.copy(query = query)
    }

    fun toggleSearch() {
        val current = _filterState.value
        val willOpen = !current.isSearchOpen
        _filterState.value = current.copy(
            isSearchOpen = willOpen,
            query = if (!willOpen) "" else current.query
        )
    }

    fun claimItem(itemId: String) {
        lostObjectsRepository.claimLostItem(itemId)
    }
}
