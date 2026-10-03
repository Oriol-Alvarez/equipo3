package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MessagesUiState(
    val selectedTab: Int = 1,
    val workerChats: List<WorkerChat> = emptyList(),
    val parentChats: List<ParentChatSummary> = emptyList(),
    val staffMembers: List<WorkerMember> = emptyList(),
    val globalChat: WorkerChat? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
)

private data class MessagesFilterState(
    val selectedTab: Int = 1,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(MessagesFilterState())

    val uiState: StateFlow<MessagesUiState> = combine(
        messagesRepository.workerChats,
        messagesRepository.parentChats,
        messagesRepository.staffMembers,
        messagesRepository.globalChat,
        _filterState
    ) { workerChats, parentChats, staff, global, filters ->
        val query = filters.searchQuery.trim()
        val filteredWorkers = if (query.isBlank()) workerChats else workerChats.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.subtitle.contains(query, ignoreCase = true) ||
            it.lastMessage.contains(query, ignoreCase = true)
        }
        val filteredParents = if (query.isBlank()) parentChats else parentChats.filter {
            it.child.fullName.contains(query, ignoreCase = true) ||
            it.parentName.contains(query, ignoreCase = true) ||
            it.lastMessage.contains(query, ignoreCase = true)
        }
        val showGlobal = query.isBlank() ||
            global.title.contains(query, ignoreCase = true) ||
            global.lastMessage.contains(query, ignoreCase = true)

        MessagesUiState(
            selectedTab = filters.selectedTab,
            workerChats = filteredWorkers,
            parentChats = filteredParents,
            staffMembers = staff,
            globalChat = if (showGlobal) global else null,
            searchQuery = filters.searchQuery,
            isSearchOpen = filters.isSearchOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessagesUiState()
    )

    fun onTabSelected(tab: Int) {
        _filterState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _filterState.update {
            val next = !it.isSearchOpen
            it.copy(isSearchOpen = next, searchQuery = if (!next) "" else it.searchQuery)
        }
    }

    fun sendMessage(chatId: String, text: String) {
        messagesRepository.sendWorkerMessage(chatId, text)
    }
}
