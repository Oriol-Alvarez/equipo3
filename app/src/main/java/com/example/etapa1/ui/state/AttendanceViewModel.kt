package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.domain.DailyLogValidator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.StateFlow

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val childRepository: ChildRepository,
    /** Opcional: si está, la hora también queda en la bitácora diaria que se exporta a PDF. */
    private val dailyLogRepository: DailyLogRepository? = null
) : ViewModel() {

    /** Niños de la sala abierta (la misma del tablero). */
    val children: StateFlow<List<Child>> = childRepository.childrenOfRoom

    fun saveAttendance(child: Child, isIngreso: Boolean, time: String, notes: String) {
        val statusText = if (isIngreso) "$time • Presente" else "$time • Egresado(a)"
        childRepository.updateChildAttendance(child.id, isPresent = isIngreso, time = time, statusText = statusText)
        attendanceRepository.registerAttendance(child, isIngreso, time, notes)
        DailyLogValidator.attendanceFormTime(time)?.let { time24 ->
            dailyLogRepository?.recordAttendanceTime(child.id, SimpleDate.today(), isIngreso, time24, notes)
        }
        // El aviso "Ingreso/Egreso" de la bitácora lo crea el servidor con la asistencia
    }
}
