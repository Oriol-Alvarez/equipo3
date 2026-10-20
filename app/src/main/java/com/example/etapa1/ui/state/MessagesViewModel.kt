package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MessagesUiState(
    val selectedTab: Int = 1,
    val isWorkerRole: Boolean = true,
    val workerChats: List<WorkerChat> = emptyList(),
    val parentChats: List<ParentChatSummary> = emptyList(),
    val familyChats: List<WorkerChat> = emptyList(),
    val staffMembers: List<WorkerMember> = emptyList(),
    val globalChat: WorkerChat? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val conexionEstado: EstadoConexion = EstadoConexion.Desconectado,
    val totalWorkersUnreadCount: Int = 0,
    val totalParentsUnreadCount: Int = 0,
    val availableStaffToChat: List<WorkerMember> = emptyList(),
    val availableChildrenToChat: List<Child> = emptyList()
)

private data class MessagesFilterState(
    val selectedTab: Int = 1,
    val isWorkerRole: Boolean = true,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false
)

private data class DataBundle(
    val workerChats: List<WorkerChat>,
    val parentChats: List<ParentChatSummary>,
    val familyChats: List<WorkerChat>,
    val staff: List<WorkerMember>,
    val global: WorkerChat
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository,
    private val chatConexion: ChatConexion? = null,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _filterState = MutableStateFlow(MessagesFilterState())
    private val defaultEstado = MutableStateFlow<EstadoConexion>(EstadoConexion.Conectado)
    private val estadoConexionFlow = chatConexion?.estado ?: defaultEstado

    val uiState: StateFlow<MessagesUiState> = combine(
        combine(
            messagesRepository.workerChats,
            messagesRepository.parentChats,
            messagesRepository.familyChats,
            messagesRepository.staffMembers,
            messagesRepository.globalChat
        ) { workerChats, parentChats, familyChats, staff, global ->
            DataBundle(workerChats, parentChats, familyChats, staff, global)
        },
        _filterState,
        estadoConexionFlow
    ) { bundle, filters, conexion ->
        val query = filters.searchQuery.trim()
        val isWorker = filters.isWorkerRole

        val currentStaffChats = if (isWorker) bundle.workerChats else bundle.familyChats
        val filteredStaffChats = if (query.isBlank()) currentStaffChats else currentStaffChats.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.subtitle.contains(query, ignoreCase = true) ||
            it.lastMessage.contains(query, ignoreCase = true)
        }

        val filteredParents = if (query.isBlank()) bundle.parentChats else bundle.parentChats.filter {
            it.child.fullName.contains(query, ignoreCase = true) ||
            it.parentName.contains(query, ignoreCase = true) ||
            it.lastMessage.contains(query, ignoreCase = true)
        }

        val showGlobal = isWorker && (query.isBlank() ||
            bundle.global.title.contains(query, ignoreCase = true) ||
            bundle.global.lastMessage.contains(query, ignoreCase = true))

        // Búsqueda de contactos nuevos (con los que nunca antes se ha hablado y no están en la lista activa)
        val existingStaffNames = currentStaffChats.map { it.title.trim().lowercase() }.toSet()
        val availableStaff = if (query.isBlank()) {
            emptyList()
        } else {
            bundle.staff.filter { member ->
                (member.name.contains(query, ignoreCase = true) || member.role.contains(query, ignoreCase = true)) &&
                    !existingStaffNames.contains(member.name.trim().lowercase())
            }
        }

        val allRegisteredChildren = (MockDataRepository.childrenSala1A + MockDataRepository.parentChildren).distinctBy { it.id }
        val existingChildIds = bundle.parentChats.map { it.child.id }.toSet()
        val availableChildren = if (query.isBlank()) {
            emptyList()
        } else {
            allRegisteredChildren.filter { child ->
                (child.fullName.contains(query, ignoreCase = true) || child.shortName.contains(query, ignoreCase = true) || child.roomText.contains(query, ignoreCase = true)) &&
                    !existingChildIds.contains(child.id)
            }
        }

        val workersUnread = if (isWorker) (bundle.global.unreadCount + bundle.workerChats.sumOf { it.unreadCount }) else bundle.familyChats.sumOf { it.unreadCount }
        val parentsUnread = if (isWorker) bundle.parentChats.sumOf { it.unreadCount } else 0

        MessagesUiState(
            selectedTab = filters.selectedTab,
            isWorkerRole = isWorker,
            workerChats = filteredStaffChats,
            parentChats = filteredParents,
            familyChats = filteredStaffChats,
            staffMembers = bundle.staff,
            globalChat = if (showGlobal) bundle.global else null,
            searchQuery = filters.searchQuery,
            isSearchOpen = filters.isSearchOpen,
            conexionEstado = conexion,
            totalWorkersUnreadCount = workersUnread,
            totalParentsUnreadCount = parentsUnread,
            availableStaffToChat = availableStaff,
            availableChildrenToChat = availableChildren
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
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

    fun setWorkerRole(isWorker: Boolean) {
        _filterState.update { it.copy(isWorkerRole = isWorker) }
    }

    fun startNewWorkerChat(staffMember: WorkerMember): WorkerChat {
        return messagesRepository.startNewWorkerChat(staffMember)
    }

    fun startNewParentChat(child: Child): ParentChatSummary {
        val parentName = if (child.id == MockDataRepository.luciaGarcia.id || child.id == MockDataRepository.mateoGarcia.id) {
            "María López (Mamá)"
        } else {
            "Familiar Tutor"
        }
        return messagesRepository.startNewParentChat(child, parentName)
    }

    fun recargar() {
        messagesRepository.recargar()
    }

    fun startNewFamilyEducatorChat(staffMember: WorkerMember): WorkerChat {
        return messagesRepository.startNewFamilyEducatorChat(staffMember)
    }
}
