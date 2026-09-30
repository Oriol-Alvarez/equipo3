package com.example.casosdeusopadre.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.casosdeusopadre.data.models.Child
import com.example.casosdeusopadre.data.repository.ChildcareRepository
import com.example.casosdeusopadre.data.repository.FakeChildcareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReportTab {
    WEEKLY, MONTHLY_AI
}

data class WeeklyDayData(
    val dayName: String,
    val present: Boolean,
    val mealPattern: String // e.g., "Completa", "Media", etc.
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val children: List<Child> = emptyList(),
    val selectedChild: Child? = null,
    val selectedTab: ReportTab = ReportTab.WEEKLY,
    
    // Weekly Data
    val weeklyData: List<WeeklyDayData> = emptyList(),
    val exportSummaryText: String = "",
    
    // Monthly Data
    val monthlyAiReport: String = "",
    val isMonthFinished: Boolean = false,
    
    val error: String? = null
)

class ReportsViewModel(
    private val repository: ChildcareRepository = FakeChildcareRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = repository.getCurrentUser()
            if (user != null) {
                val children = repository.getChildrenForParent(user.id)
                val selected = children.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    children = children,
                    selectedChild = selected,
                    isLoading = false
                )
                selected?.let { loadReportsForChild(it.id) }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no autenticado")
            }
        }
    }

    fun selectChild(child: Child) {
        if (_uiState.value.selectedChild?.id != child.id) {
            _uiState.value = _uiState.value.copy(selectedChild = child)
            loadReportsForChild(child.id)
        }
    }

    fun selectTab(tab: ReportTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    private fun loadReportsForChild(childId: String) {
        // In a real app, we'd fetch this from the repository.
        // For the fake implementation, we generate it here to simplify the UI state logic.
        
        val fakeWeeklyData = listOf(
            WeeklyDayData("Lunes", true, "Completa"),
            WeeklyDayData("Martes", true, "Completa"),
            WeeklyDayData("Miércoles", true, "Media"),
            WeeklyDayData("Jueves", true, "Completa"),
            WeeklyDayData("Viernes", false, "-") // Ausente
        )
        
        val summaryText = buildString {
            append("Resumen Semanal de ${_uiState.value.selectedChild?.name ?: "tu hijo"}\n\n")
            fakeWeeklyData.forEach { day ->
                val status = if (day.present) "Asistió (Comida: ${day.mealPattern})" else "Ausente"
                append("- ${day.dayName}: $status\n")
            }
        }

        // Toggle this boolean to test the "Pending" vs "Available" state for the AI report.
        val monthFinished = true 
        val aiReport = if (monthFinished) {
            "🤖 Reporte Inteligente: ${_uiState.value.selectedChild?.name ?: "El niño"} ha mejorado su patrón de sueño un 20% este mes y muestra excelente integración social. Participó activamente en las actividades grupales y su apetito se ha mantenido estable."
        } else {
            "El reporte inteligente de este mes estará disponible al finalizar el mes en curso."
        }

        _uiState.value = _uiState.value.copy(
            weeklyData = fakeWeeklyData,
            exportSummaryText = summaryText,
            monthlyAiReport = aiReport,
            isMonthFinished = monthFinished
        )
    }
}
