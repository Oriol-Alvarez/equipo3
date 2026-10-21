package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChildDetailUiState(
    val childId: String,
    val fullProfile: ChildFullProfile,
    val weeklySummaries: List<WeeklySummary>,
    val dailyBitacoras: List<DailyBitacora>
)

class ChildDetailViewModel(
    private val childRepository: ChildRepository
) : ViewModel() {

    private val child = MutableStateFlow<Child?>(null)

    /** Ficha del niño abierto; se actualiza sola cuando llegan los datos del servidor. */
    val uiState: StateFlow<ChildDetailUiState?> =
        combine(child, childRepository.cambios) { actual, _ -> actual?.let(::estadoPara) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Abre la ficha y pide al servidor los datos más recientes. */
    fun abrir(nino: Child) {
        child.value = nino
        childRepository.cargarDetalle(nino.id)
    }

    fun estadoPara(nino: Child) = ChildDetailUiState(
        childId = nino.id,
        fullProfile = childRepository.getChildFullProfile(nino),
        weeklySummaries = childRepository.getWeeklySummariesForChild(nino),
        dailyBitacoras = childRepository.getDailyBitacorasForChild(nino)
    )

    fun getFullProfile(child: Child): ChildFullProfile = childRepository.getChildFullProfile(child)

    fun updateChildProfile(childId: String, updatedProfile: ChildFullProfile) {
        childRepository.updateChildFullProfile(childId, updatedProfile)
    }

    fun getWeeklySummaries(child: Child): List<WeeklySummary> = childRepository.getWeeklySummariesForChild(child)

    fun getDailyBitacoras(child: Child): List<DailyBitacora> = childRepository.getDailyBitacorasForChild(child)
}
