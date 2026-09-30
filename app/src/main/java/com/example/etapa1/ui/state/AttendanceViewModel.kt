package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AttendanceUiState(
    val selectedChild: Child? = null,
    val isIngreso: Boolean = true,
    val selectedTime: String = "08:15 AM",
    val observations: String = "",
    val isSavedSuccess: Boolean = false
)

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val childRepository: ChildRepository,
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    val children: StateFlow<List<Child>> = childRepository.childrenSala1A

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun initSelectedChild(child: Child?) {
        if (child != null && _uiState.value.selectedChild == null) {
            _uiState.update { it.copy(selectedChild = child) }
        }
    }

    fun onChildSelected(child: Child) {
        _uiState.update { it.copy(selectedChild = child) }
    }

    fun onModeChanged(isIngreso: Boolean) {
        _uiState.update { it.copy(isIngreso = isIngreso) }
    }

    fun onTimeChanged(time: String) {
        _uiState.update { it.copy(selectedTime = time) }
    }

    fun onObservationsChanged(notes: String) {
        _uiState.update { it.copy(observations = notes) }
    }

    fun saveAttendance(child: Child, isIngreso: Boolean, time: String, notes: String) {
        val statusText = if (isIngreso) "$time • Presente" else "$time • Egresado(a)"
        childRepository.updateChildAttendance(child.id, isPresent = isIngreso, time = time, statusText = statusText)
        attendanceRepository.registerAttendance(child, isIngreso, time, notes)

        val actionLabel = if (isIngreso) "Ingreso: $time" else "Egreso: $time"
        bitacoraRepository.addEventChip(childId = child.id, time = time, text = actionLabel, iconType = "entry")

        _uiState.update { it.copy(isSavedSuccess = true) }
    }
}
