package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    initialChild: Child? = null,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val defaultChild = (initialChild?.let { childRepository.getChildById(it.id) ?: it })
        ?: childRepository.parentChildren.value.firstOrNull()
        ?: MockDataRepository.mateoGarcia
    private val defaultWeeklySummaries = childRepository.getWeeklySummariesForChild(defaultChild)

    private val _uiState = MutableStateFlow(
        ParentHomeUiState(
            selectedChild = defaultChild,
            allChildren = childRepository.parentChildren.value,
            fullProfile = childRepository.getChildFullProfile(defaultChild),
            dailyBitacoras = childRepository.getDailyBitacorasForChild(defaultChild),
            weeklySummaries = defaultWeeklySummaries,
            weeklySummary = defaultWeeklySummaries.firstOrNull() ?: childRepository.getWeeklySummaryForChild(defaultChild)
        )
    )
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    init {
        childRepository.refresh()
        val launchScope = coroutineScope ?: try {
            viewModelScope
        } catch (_: Throwable) {
            null
        }
        try {
            launchScope?.launch {
                childRepository.parentChildren.collect { childrenList ->
                    if (childrenList.isNotEmpty()) {
                        _uiState.update { current ->
                            val updatedSelected = childrenList.find { it.id == current.selectedChild.id }
                                ?: childrenList.first()
                            current.copy(
                                selectedChild = updatedSelected,
                                allChildren = childrenList,
                                fullProfile = childRepository.getChildFullProfile(updatedSelected),
                                dailyBitacoras = childRepository.getDailyBitacorasForChild(updatedSelected),
                                weeklySummaries = childRepository.getWeeklySummariesForChild(updatedSelected),
                                weeklySummary = childRepository.getWeeklySummariesForChild(updatedSelected).firstOrNull()
                                    ?: childRepository.getWeeklySummaryForChild(updatedSelected)
                            )
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignorar en tests unitarios sin Dispatchers.Main
        }
    }

    fun selectChild(child: Child) {
        val resolvedChild = childRepository.getChildById(child.id) ?: child
        val summaries = childRepository.getWeeklySummariesForChild(resolvedChild)
        _uiState.update {
            it.copy(
                selectedChild = resolvedChild,
                fullProfile = childRepository.getChildFullProfile(resolvedChild),
                dailyBitacoras = childRepository.getDailyBitacorasForChild(resolvedChild),
                weeklySummaries = summaries,
                weeklySummary = summaries.firstOrNull() ?: childRepository.getWeeklySummaryForChild(resolvedChild)
            )
        }
    }

    fun updateChildProfile(
        pediatrician: String,
        pediatricianPhone: String,
        medicalNotes: String,
        habitsAndPedagogicalNotes: String,
        emergencyPhone: String,
        emergencyPhoneRelation: String = "Tutor legal",
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
            emergencyPhoneRelation = emergencyPhoneRelation.trim(),
            authorizedPickups = authorizedPickups,
            medicalCertificateUri = medicalCertificateUri,
            medicalCertificateName = medicalCertificateName,
            medicalCertificateDate = medicalCertificateDate
        )
        childRepository.updateChildFullProfile(currentChild.id, updated)
        _uiState.update { it.copy(fullProfile = updated) }
    }
}
