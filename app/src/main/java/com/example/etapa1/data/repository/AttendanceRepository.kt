package com.example.etapa1.data.repository

import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.StateFlow

interface AttendanceRepository {
    val attendanceRecords: StateFlow<List<AttendanceRecord>>
    fun registerAttendance(
        child: Child,
        isIngreso: Boolean,
        time: String,
        observations: String = "",
        personName: String? = null,
        staffName: String? = null,
        goodPhysicalCondition: Boolean = true,
        injuryDetail: String = "",
        hasSymptoms: Boolean = false,
        symptomsDetail: String = "",
        isClean: Boolean = true,
        backpackComplete: Boolean = true
    ): Result<Unit>

    /** Vuelve a pedir los datos al servidor. */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
