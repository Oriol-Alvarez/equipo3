package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.SinDatos
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        ?: SinDatos.nino

    private val _uiState = MutableStateFlow(estadoPara(defaultChild, childRepository.parentChildren.value))
    val uiState: StateFlow<ParentHomeUiState> = _uiState.asStateFlow()

    /** Arma el estado del niño con lo que ya está cargado (el repositorio pide lo que falte). */
    private fun estadoPara(child: Child, children: List<Child>): ParentHomeUiState {
        val summaries = if (child.id.isEmpty()) emptyList() else childRepository.getWeeklySummariesForChild(child)
        return ParentHomeUiState(
            selectedChild = child,
            allChildren = children,
            fullProfile = childRepository.getChildFullProfile(child),
            dailyBitacoras = if (child.id.isEmpty()) emptyList() else childRepository.getDailyBitacorasForChild(child),
            weeklySummaries = summaries,
            weeklySummary = summaries.firstOrNull() ?: childRepository.getWeeklySummaryForChild(child)
        )
    }

    init {
        childRepository.refresh()
        val launchScope = coroutineScope ?: try {
            viewModelScope
        } catch (_: Throwable) {
            null
        }
        try {
            launchScope?.launch {
                // Cuando llegan los hijos o datos nuevos del servidor se vuelve a leer todo
                combine(childRepository.parentChildren, childRepository.cambios) { hijos, _ -> hijos }
                    .collect { childrenList ->
                        _uiState.update { current ->
                            val seleccionado = childrenList.find { it.id == current.selectedChild.id }
                                ?: childrenList.firstOrNull()
                                ?: current.selectedChild
                            estadoPara(seleccionado, childrenList.ifEmpty { current.allChildren })
                        }
                    }
            }
        } catch (_: Throwable) {
            // Ignorar en tests unitarios sin Dispatchers.Main
        }
    }

    fun selectChild(child: Child) {
        val resolvedChild = childRepository.getChildById(child.id) ?: child
        if (resolvedChild.id.isNotEmpty()) childRepository.cargarDetalle(resolvedChild.id)
        _uiState.update { estadoPara(resolvedChild, it.allChildren) }
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
        medicalCertificateDate: String?,
        bloodType: String = _uiState.value.fullProfile.bloodType
    ) {
        val currentChild = _uiState.value.selectedChild
        val currentProfile = _uiState.value.fullProfile
        val updated = currentProfile.copy(
            bloodType = bloodType.trim(),
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

    fun toggleWeeklySummaryReadStatus(summary: WeeklySummary) {
        val currentChild = _uiState.value.selectedChild
        val newReadState = !summary.isReadByParent
        val timeNow = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
        childRepository.markWeeklySummaryReadStatus(
            childId = currentChild.id,
            weekRangeText = summary.weekRangeText,
            isRead = newReadState,
            timestampText = if (newReadState) "Hoy, $timeNow" else null
        )
        val updatedSummaries = childRepository.getWeeklySummariesForChild(currentChild)
        _uiState.update {
            it.copy(
                weeklySummaries = updatedSummaries,
                weeklySummary = updatedSummaries.firstOrNull() ?: it.weeklySummary
            )
        }
    }

    fun setWeeklySummaryReaction(summary: WeeklySummary, reaction: String) {
        val currentChild = _uiState.value.selectedChild
        childRepository.setWeeklySummaryReaction(
            childId = currentChild.id,
            weekRangeText = summary.weekRangeText,
            reaction = reaction
        )
        val updatedSummaries = childRepository.getWeeklySummariesForChild(currentChild)
        _uiState.update {
            it.copy(
                weeklySummaries = updatedSummaries,
                weeklySummary = updatedSummaries.firstOrNull() ?: it.weeklySummary
            )
        }
    }
}
