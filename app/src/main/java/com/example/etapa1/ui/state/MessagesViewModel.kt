package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.domain.EstadoConexion
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
    val isSearchOpen: Boolean = false,
    val conexionEstado: EstadoConexion = EstadoConexion.Desconectado,
    val totalWorkersUnreadCount: Int = 0,
    val totalParentsUnreadCount: Int = 0
)

private data class MessagesFilterState(
    val selectedTab: Int = 1,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository,
    private val chatConexion: ChatConexion? = null
) : ViewModel() {

    private val _filterState = MutableStateFlow(MessagesFilterState())
    private val defaultEstado = MutableStateFlow<EstadoConexion>(EstadoConexion.Conectado)
    private val estadoConexionFlow = chatConexion?.estado ?: defaultEstado

    val uiState: StateFlow<MessagesUiState> = combine(
        combine(
            messagesRepository.workerChats,
            messagesRepository.parentChats,
            messagesRepository.staffMembers,
            messagesRepository.globalChat
        ) { workerChats, parentChats, staff, global ->
            Quadruple(workerChats, parentChats, staff, global)
        },
        _filterState,
        estadoConexionFlow
    ) { (workerChats, parentChats, staff, global), filters, conexion ->
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

        val workersUnread = (global.unreadCount) + workerChats.sumOf { it.unreadCount }
        val parentsUnread = parentChats.sumOf { it.unreadCount }

        MessagesUiState(
            selectedTab = filters.selectedTab,
            workerChats = filteredWorkers,
            parentChats = filteredParents,
            staffMembers = staff,
            globalChat = if (showGlobal) global else null,
            searchQuery = filters.searchQuery,
            isSearchOpen = filters.isSearchOpen,
            conexionEstado = conexion,
            totalWorkersUnreadCount = workersUnread,
            totalParentsUnreadCount = parentsUnread
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

    fun markWorkerChatAsRead(chatId: String) {
        messagesRepository.markWorkerChatAsRead(chatId)
    }

    fun markParentChatAsRead(childId: String) {
        messagesRepository.markParentChatAsRead(childId)
    }
}
