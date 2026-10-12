package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.domain.DailyLogValidator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.StateFlow

class AttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val childRepository: ChildRepository,
    private val bitacoraRepository: BitacoraRepository,
    /** Opcional: si está, la hora también queda en la bitácora diaria que se exporta a PDF. */
    private val dailyLogRepository: DailyLogRepository? = null
) : ViewModel() {

    val children: StateFlow<List<Child>> = childRepository.childrenSala1A

    fun saveAttendance(child: Child, isIngreso: Boolean, time: String, notes: String) {
        val statusText = if (isIngreso) "$time • Presente" else "$time • Egresado(a)"
        childRepository.updateChildAttendance(child.id, isPresent = isIngreso, time = time, statusText = statusText)
        attendanceRepository.registerAttendance(child, isIngreso, time, notes)
        DailyLogValidator.attendanceFormTime(time)?.let { time24 ->
            dailyLogRepository?.recordAttendanceTime(child.id, SimpleDate.today(), isIngreso, time24, notes)
        }

        if (child.id == MockDataRepository.mateoGarcia.id) {
            val actionLabel = if (isIngreso) "Ingreso: $time" else "Egreso: $time"
            bitacoraRepository.addEventChip(time = time, text = actionLabel, iconType = "entry")
        }
    }
}
