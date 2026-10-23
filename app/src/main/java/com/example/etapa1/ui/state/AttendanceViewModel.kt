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

    /** Obtiene el expediente completo (con autorizados cargados desde el backend/caché). */
    fun getChildFullProfile(child: Child) = childRepository.getChildFullProfile(child)

    fun cargarDetalle(childId: String) = childRepository.cargarDetalle(childId)

    val cambios: StateFlow<Int> = childRepository.cambios

    fun saveAttendance(
        child: Child,
        isIngreso: Boolean,
        time: String,
        notes: String,
        personName: String? = null,
        staffName: String? = null,
        goodPhysicalCondition: Boolean = true,
        injuryDetail: String = "",
        hasSymptoms: Boolean = false,
        symptomsDetail: String = "",
        isClean: Boolean = true,
        backpackComplete: Boolean = true
    ) {
        val cleanTime = DailyLogValidator.attendanceFormTime(time) ?: time.trim()
        val statusText = if (isIngreso) "$cleanTime • Presente" else "$cleanTime • Retirado(a)"
        childRepository.updateChildAttendance(child.id, isPresent = isIngreso, time = cleanTime, statusText = statusText)
        attendanceRepository.registerAttendance(
            child = child,
            isIngreso = isIngreso,
            time = cleanTime,
            observations = notes,
            personName = personName,
            staffName = staffName,
            goodPhysicalCondition = goodPhysicalCondition,
            injuryDetail = injuryDetail,
            hasSymptoms = hasSymptoms,
            symptomsDetail = symptomsDetail,
            isClean = isClean,
            backpackComplete = backpackComplete
        )
        dailyLogRepository?.recordAttendanceTime(
            childId = child.id,
            date = SimpleDate.today(),
            isIngreso = isIngreso,
            time = cleanTime,
            observations = notes,
            personName = personName,
            staffName = staffName,
            goodPhysicalCondition = goodPhysicalCondition,
            injuryDetail = injuryDetail,
            hasSymptoms = hasSymptoms,
            symptomsDetail = symptomsDetail,
            isClean = isClean,
            backpackComplete = backpackComplete
        )
    }
}
