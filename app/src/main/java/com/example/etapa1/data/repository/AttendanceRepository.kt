package com.example.etapa1.data.repository

import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.Child
import com.example.etapa1.model.Horas
import com.example.etapa1.model.RegistroAsistencia
import kotlinx.coroutines.flow.StateFlow

interface AttendanceRepository {
    val attendanceRecords: StateFlow<List<AttendanceRecord>>
    fun registerAttendance(
        child: Child,
        isIngreso: Boolean,
        time: String,
        observations: String = ""
    ): Result<Unit>

    /** Registro completo del formulario (RF-03): hora y quién entrega o recoge. */
    fun registrarAsistencia(child: Child, registro: RegistroAsistencia): Result<Unit> =
        registerAttendance(child, registro.isIngreso, Horas.a12h(registro.hora24), registro.observaciones)

    /** Vuelve a pedir los datos al servidor. */
    fun refresh() {}

    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
