package com.example.casosdeusopadre.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.casosdeusopadre.data.models.ActivityLog
import com.example.casosdeusopadre.data.models.Child
import com.example.casosdeusopadre.data.models.Notice
import com.example.casosdeusopadre.data.repository.ChildcareRepository
import com.example.casosdeusopadre.data.repository.FakeChildcareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val notice: Notice? = null,
    val children: List<Child> = emptyList(),
    val selectedChild: Child? = null,
    val activityLogs: List<ActivityLog> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val repository: ChildcareRepository = FakeChildcareRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val user = repository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no autenticado")
                return@launch
            }

            try {
                // Collect notices
                launch {
                    repository.getNotices().collect { notices ->
                        _uiState.value = _uiState.value.copy(notice = notices.firstOrNull())
                    }
                }

                // Load children
                val children = repository.getChildrenForParent(user.id)
                val selectedChild = children.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    children = children,
                    selectedChild = selectedChild,
                    isLoading = false
                )

                // Load activities for selected child
                selectedChild?.let { loadActivitiesForChild(it.id) }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar datos"
                )
            }
        }
    }

    fun selectChild(child: Child) {
        if (_uiState.value.selectedChild?.id != child.id) {
            _uiState.value = _uiState.value.copy(selectedChild = child)
            loadActivitiesForChild(child.id)
        }
    }

    private fun loadActivitiesForChild(childId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Using hardcoded date for fake data matching
                val logs = repository.getActivityLogs(childId, "2023-10-25")
                _uiState.value = _uiState.value.copy(
                    activityLogs = logs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar actividades"
                )
            }
        }
    }

    fun toggleNetworkMode() {
        _uiState.value = _uiState.value.copy(isOnline = !_uiState.value.isOnline)
    }
}
