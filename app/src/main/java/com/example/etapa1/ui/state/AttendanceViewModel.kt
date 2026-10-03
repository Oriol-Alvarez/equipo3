package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.StateFlow

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val childRepository: ChildRepository,
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    val children: StateFlow<List<Child>> = childRepository.childrenSala1A

    fun saveAttendance(child: Child, isIngreso: Boolean, time: String, notes: String) {
        val statusText = if (isIngreso) "$time • Presente" else "$time • Egresado(a)"
        childRepository.updateChildAttendance(child.id, isPresent = isIngreso, time = time, statusText = statusText)
        attendanceRepository.registerAttendance(child, isIngreso, time, notes)

        if (child.id == MockDataRepository.mateoGarcia.id) {
            val actionLabel = if (isIngreso) "Ingreso: $time" else "Egreso: $time"
            bitacoraRepository.addEventChip(time = time, text = actionLabel, iconType = "entry")
        }
    }
}
