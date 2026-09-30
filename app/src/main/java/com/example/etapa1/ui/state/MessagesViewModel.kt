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

data class MessagesUiState(
    val selectedTab: Int = 1,
    val workerChats: List<WorkerChat> = emptyList(),
    val parentChats: List<ParentChatSummary> = emptyList(),
    val staffMembers: List<WorkerMember> = emptyList(),
    val globalChat: WorkerChat? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(1)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchOpen = MutableStateFlow(false)

    val uiState: StateFlow<MessagesUiState> = combine(
        messagesRepository.workerChats,
        messagesRepository.parentChats,
        messagesRepository.staffMembers,
        messagesRepository.globalChat,
        _selectedTab
    ) { workerChats, parentChats, staff, global, tab ->
        MessagesUiState(
            selectedTab = tab,
            workerChats = workerChats,
            parentChats = parentChats,
            staffMembers = staff,
            globalChat = global,
            searchQuery = _searchQuery.value,
            isSearchOpen = _isSearchOpen.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessagesUiState()
    )

    fun onTabSelected(tab: Int) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        val next = !_isSearchOpen.value
        _isSearchOpen.value = next
        if (!next) _searchQuery.value = ""
    }

    fun sendMessage(chatId: String, text: String) {
        messagesRepository.sendWorkerMessage(chatId, text)
    }
}
