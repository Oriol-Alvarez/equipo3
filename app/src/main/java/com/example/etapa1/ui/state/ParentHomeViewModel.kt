package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ParentHomeUiState(
    val selectedChild: Child,
    val allChildren: List<Child> = emptyList(),
    val fullProfile: ChildFullProfile,
    val dailyBitacoras: List<DailyBitacora> = emptyList(),
    val weeklySummary: WeeklySummary
)

class ParentHomeViewModel(
    private val childRepository: ChildRepository,
    initialChild: Child? = null
) : ViewModel() {

    private val defaultChild = initialChild ?: MockDataRepository.mateoGarcia

    private val _uiState = MutableStateFlow(
        ParentHomeUiState(
            selectedChild = defaultChild,
            allChildren = MockDataRepository.parentChildren,
            fullProfile = childRepository.getChildFullProfile(defaultChild),
            dailyBitacoras = MockDataRepository.getDailyBitacorasForChild(defaultChild),
            weeklySummary = childRepository.getWeeklySummaryForChild(defaultChild)
        )
    )
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    fun selectChild(child: Child) {
        _uiState.update {
            it.copy(
                selectedChild = child,
                fullProfile = childRepository.getChildFullProfile(child),
                dailyBitacoras = MockDataRepository.getDailyBitacorasForChild(child),
                weeklySummary = childRepository.getWeeklySummaryForChild(child)
            )
        }
    }
}
