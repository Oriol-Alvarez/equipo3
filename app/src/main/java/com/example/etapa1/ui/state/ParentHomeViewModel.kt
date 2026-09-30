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
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    val weeklySummary: WeeklySummary
)

class ParentHomeViewModel(
    private val childRepository: ChildRepository,
    initialChild: Child? = null
) : ViewModel() {

    private val defaultChild = initialChild ?: MockDataRepository.mateoGarcia
    private val defaultWeeklySummaries = childRepository.getWeeklySummariesForChild(defaultChild)

    private val _uiState = MutableStateFlow(
        ParentHomeUiState(
            selectedChild = defaultChild,
            allChildren = MockDataRepository.parentChildren,
            fullProfile = childRepository.getChildFullProfile(defaultChild),
            dailyBitacoras = MockDataRepository.getDailyBitacorasForChild(defaultChild),
            weeklySummaries = defaultWeeklySummaries,
            weeklySummary = defaultWeeklySummaries.firstOrNull() ?: childRepository.getWeeklySummaryForChild(defaultChild)
        )
    )
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    fun selectChild(child: Child) {
        val summaries = childRepository.getWeeklySummariesForChild(child)
        _uiState.update {
            it.copy(
                selectedChild = child,
                fullProfile = childRepository.getChildFullProfile(child),
                dailyBitacoras = MockDataRepository.getDailyBitacorasForChild(child),
                weeklySummaries = summaries,
                weeklySummary = summaries.firstOrNull() ?: childRepository.getWeeklySummaryForChild(child)
            )
        }
    }

    fun updateChildProfile(
        pediatrician: String,
        pediatricianPhone: String,
        medicalNotes: String,
        habitsAndPedagogicalNotes: String,
        emergencyPhone: String,
        authorizedPickups: List<String>,
        medicalCertificateUri: String?,
        medicalCertificateName: String?,
        medicalCertificateDate: String?
    ) {
        val currentChild = _uiState.value.selectedChild
        val currentProfile = _uiState.value.fullProfile
        val updated = currentProfile.copy(
            pediatrician = pediatrician.trim(),
            pediatricianPhone = pediatricianPhone.trim(),
            medicalNotes = medicalNotes.trim(),
            generalNotes = habitsAndPedagogicalNotes.trim(),
            emergencyPhone = emergencyPhone.trim(),
            authorizedPickups = authorizedPickups,
            medicalCertificateUri = medicalCertificateUri,
            medicalCertificateName = medicalCertificateName,
            medicalCertificateDate = medicalCertificateDate
        )
        childRepository.updateChildFullProfile(currentChild.id, updated)
        _uiState.update { it.copy(fullProfile = updated) }
    }
}
