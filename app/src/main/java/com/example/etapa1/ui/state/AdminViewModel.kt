package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.AdminRepository
import com.example.etapa1.model.AdminChildSummary
import com.example.etapa1.model.AdminLog
import com.example.etapa1.model.AdminStats
import com.example.etapa1.model.AdminUser
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.ParentSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.CoroutineScope

data class AdminUiState(
    val selectedTab: Int = 0, // 0: Resumen, 1: Perfiles, 2: Avisos, 3: Objetos/Sugerencias, 4: Logs, 5: Estadísticas
    val selectedUserFilter: String = "TODOS", // TODOS, EDUCADORA, FAMILIAR
    val userSearchQuery: String = "",
    val selectedLogFilter: String = "TODOS", // TODOS, ASISTENCIA, ACTIVIDAD, MENSAJE, AVISO, OBJETO, SUGERENCIA
    val logSearchQuery: String = "",
    val selectedLostOrSugTab: Int = 0, // 0: Objetos Perdidos, 1: Buzón Sugerencias
    val lostObjectsFilter: String = "TODOS", // TODOS, EN_CUSTODIA, ENTREGADOS
    val suggestionsFilter: String = "TODAS", // TODAS, PENDIENTES, ATENDIDAS
    val isRefreshing: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    val users: StateFlow<List<AdminUser>> = adminRepository.users
    val logs: StateFlow<List<AdminLog>> = adminRepository.logs
    val stats: StateFlow<AdminStats> = adminRepository.stats
    val announcements: StateFlow<List<Announcement>> = adminRepository.announcements
    val suggestions: StateFlow<List<ParentSuggestion>> = adminRepository.suggestions
    val lostObjects: StateFlow<List<LostItem>> = adminRepository.lostObjects

    private val _selectedUserForChildren = MutableStateFlow<AdminUser?>(null)
    val selectedUserForChildren: StateFlow<AdminUser?> = _selectedUserForChildren.asStateFlow()

    private val _availableCenterChildren = MutableStateFlow<List<AdminChildSummary>>(emptyList())
    val availableCenterChildren: StateFlow<List<AdminChildSummary>> = _availableCenterChildren.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectUserFilter(filter: String) {
        _uiState.update { it.copy(selectedUserFilter = filter) }
        scope.launch {
            adminRepository.loadUsers(filter, _uiState.value.userSearchQuery)
        }
    }

    fun onUserSearchChange(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        scope.launch {
            adminRepository.loadUsers(_uiState.value.selectedUserFilter, query)
        }
    }

    fun selectLogFilter(filter: String) {
        _uiState.update { it.copy(selectedLogFilter = filter) }
        scope.launch {
            adminRepository.loadLogs(filter, _uiState.value.logSearchQuery)
        }
    }

    fun onLogSearchChange(query: String) {
        _uiState.update { it.copy(logSearchQuery = query) }
        scope.launch {
            adminRepository.loadLogs(_uiState.value.selectedLogFilter, query)
        }
    }

    fun selectLostOrSugTab(index: Int) {
        _uiState.update { it.copy(selectedLostOrSugTab = index) }
    }

    fun selectLostObjectsFilter(filter: String) {
        _uiState.update { it.copy(lostObjectsFilter = filter) }
    }

    fun selectSuggestionsFilter(filter: String) {
        _uiState.update { it.copy(suggestionsFilter = filter) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                adminRepository.refreshAll()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE PERFILES
    // -------------------------------------------------------------
    fun createUser(
        displayName: String,
        email: String,
        role: String,
        phone: String?,
        password: String,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.createUser(email, password, role, displayName, phone)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Perfil de ${result.getOrNull()?.displayName} creado exitosamente.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                adminRepository.loadStats()
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al crear el perfil.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun updateUser(
        userId: String,
        displayName: String?,
        email: String?,
        phone: String?,
        role: String?,
        password: String?,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.updateUser(userId, displayName, email, phone, role, password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Perfil actualizado correctamente.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al actualizar el perfil.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun deleteUser(userId: String) {
        scope.launch {
            val result = adminRepository.deleteUser(userId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Perfil eliminado.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                adminRepository.loadStats()
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al eliminar el usuario.",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE HIJOS ASOCIADOS A FAMILIARES
    // -------------------------------------------------------------
    fun openChildrenDialog(user: AdminUser) {
        _selectedUserForChildren.value = user
        loadAvailableCenterChildren()
    }

    fun closeChildrenDialog() {
        _selectedUserForChildren.value = null
    }

    fun loadAvailableCenterChildren() {
        scope.launch {
            val res = adminRepository.getAllChildren()
            if (res.isSuccess) {
                _availableCenterChildren.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun linkChildToParent(userId: String, childId: String) {
        scope.launch {
            val res = adminRepository.linkChildToParent(userId, childId)
            if (res.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Alumno vinculado a la cuenta familiar.", isErrorFeedback = false) }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                val updatedList = adminRepository.users.value
                _selectedUserForChildren.value = updatedList.find { it.id == userId }
            } else {
                _uiState.update { it.copy(feedbackMessage = res.exceptionOrNull()?.message ?: "Error al vincular el alumno.", isErrorFeedback = true) }
            }
        }
    }

    fun unlinkChildFromParent(userId: String, childId: String) {
        scope.launch {
            val res = adminRepository.unlinkChildFromParent(userId, childId)
            if (res.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Alumno desvinculado de la cuenta familiar.", isErrorFeedback = false) }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                val updatedList = adminRepository.users.value
                _selectedUserForChildren.value = updatedList.find { it.id == userId }
            } else {
                _uiState.update { it.copy(feedbackMessage = res.exceptionOrNull()?.message ?: "Error al desvincular.", isErrorFeedback = true) }
            }
        }
    }

    fun createAndLinkChild(
        userId: String,
        fullName: String,
        room: String,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?,
        onComplete: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            val res = adminRepository.createAndLinkChild(
                userId = userId,
                fullName = fullName,
                room = room,
                bloodType = bloodType,
                pediatrician = pediatrician,
                emergencyPhone = emergencyPhone,
                medicalNotes = medicalNotes
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Nuevo alumno registrado y vinculado.", isErrorFeedback = false) }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                val updatedList = adminRepository.users.value
                _selectedUserForChildren.value = updatedList.find { it.id == userId }
                loadAvailableCenterChildren()
                onComplete(true)
            } else {
                _uiState.update { it.copy(feedbackMessage = res.exceptionOrNull()?.message ?: "Error al registrar alumno.", isErrorFeedback = true) }
                onComplete(false)
            }
        }
    }

    fun updateChild(
        childId: String,
        fullName: String?,
        room: String?,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?,
        allergyAlert: String?,
        birthDate: String?,
        onComplete: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            val res = adminRepository.updateChild(
                childId = childId,
                fullName = fullName,
                room = room,
                bloodType = bloodType,
                pediatrician = pediatrician,
                emergencyPhone = emergencyPhone,
                medicalNotes = medicalNotes,
                allergyAlert = allergyAlert,
                birthDate = birthDate
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Expediente del menor actualizado correctamente.", isErrorFeedback = false) }
                adminRepository.loadUsers(_uiState.value.selectedUserFilter, _uiState.value.userSearchQuery)
                val currUser = _selectedUserForChildren.value
                if (currUser != null) {
                    val updatedList = adminRepository.users.value
                    _selectedUserForChildren.value = updatedList.find { it.id == currUser.id }
                }
                loadAvailableCenterChildren()
                onComplete(true)
            } else {
                _uiState.update { it.copy(feedbackMessage = res.exceptionOrNull()?.message ?: "Error al actualizar expediente.", isErrorFeedback = true) }
                onComplete(false)
            }
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE AVISOS DEL CENTRO
    // -------------------------------------------------------------
    fun createAnnouncement(
        title: String,
        summary: String,
        fullContent: String,
        author: String = "Dirección del Centro",
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.createAnnouncement(title, summary, fullContent, author)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Aviso difundido a todo el centro escolar.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al publicar aviso.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun updateAnnouncement(
        id: String,
        title: String?,
        summary: String?,
        fullContent: String?,
        author: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.updateAnnouncement(id, title, summary, fullContent, author)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Aviso modificado correctamente.",
                        isErrorFeedback = false
                    )
                }
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al modificar aviso.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun deleteAnnouncement(id: String) {
        scope.launch {
            val result = adminRepository.deleteAnnouncement(id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Aviso escolar eliminado.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al eliminar aviso.",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE OBJETOS PERDIDOS
    // -------------------------------------------------------------
    fun createLostObject(
        title: String,
        description: String,
        category: String,
        location: String?,
        room: String,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.createLostObject(title, description, category, location, room)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Objeto perdido registrado en custodia.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al registrar objeto.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun toggleLostObjectClaim(item: LostItem) {
        scope.launch {
            val newStatus = !item.isClaimed
            val result = adminRepository.updateLostObject(
                id = item.id,
                title = null,
                description = null,
                category = null,
                location = null,
                room = null,
                isClaimed = newStatus
            )
            if (result.isSuccess) {
                val msg = if (newStatus) "Objeto marcado como Entregado a la familia." else "Objeto marcado como En Custodia."
                _uiState.update { it.copy(feedbackMessage = msg, isErrorFeedback = false) }
                adminRepository.loadStats()
            }
        }
    }

    fun deleteLostObject(id: String) {
        scope.launch {
            val result = adminRepository.deleteLostObject(id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Registro de objeto eliminado.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
            }
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE SUGERENCIAS Y QUEJAS
    // -------------------------------------------------------------
    fun answerSuggestion(
        id: String,
        status: String,
        response: String,
        responderName: String = "Dirección General",
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val result = adminRepository.updateSuggestionStatus(id, status, response, responderName)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Respuesta oficial guardada y estado actualizado.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = result.exceptionOrNull()?.message ?: "Error al responder sugerencia.",
                        isErrorFeedback = true
                    )
                }
                onComplete(false)
            }
        }
    }

    fun deleteSuggestion(id: String) {
        scope.launch {
            val result = adminRepository.deleteSuggestion(id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Sugerencia eliminada.",
                        isErrorFeedback = false
                    )
                }
                adminRepository.loadStats()
            }
        }
    }
}
