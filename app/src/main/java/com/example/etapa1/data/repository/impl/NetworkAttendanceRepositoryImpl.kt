package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.EgresoDto
import com.example.etapa1.data.remote.IngresoDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.Child
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkAttendanceRepositoryImpl(
    private val api: SonrisasApi
) : AttendanceRepository {

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    override val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        RemoteSync.launch("Cargar registros de asistencia") {
            val list = api.getAttendanceRecords().bodyOrLog("GET /attendance/records")?.map { dto ->
                AttendanceRecord(
                    id = dto.id,
                    childId = dto.childId,
                    isIngreso = dto.isIngreso,
                    date = dto.date,
                    dayOfWeek = dto.dayOfWeek,
                    time = dto.time,
                    observations = dto.observations
                )
            } ?: return@launch
            _attendanceRecords.value = list
        }
    }

    override fun limpiar() {
        _attendanceRecords.value = emptyList()
    }

    override fun registerAttendance(
        child: Child,
        isIngreso: Boolean,
        time: String,
        observations: String
    ): Result<Unit> {
        val record = AttendanceRecord(
            id = "att_${System.currentTimeMillis()}",
            childId = child.id,
            isIngreso = isIngreso,
            date = "Hoy",
            dayOfWeek = "L",
            time = time,
            observations = observations
        )
        _attendanceRecords.update { listOf(record) + it }

        val todayIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isIngreso) {
                    api.registrarIngreso(
                        IngresoDto(
                            childId = child.id,
                            date = todayIso,
                            entryTime = time,
                            observations = observations
                        )
                    )
                } else {
                    api.registrarEgreso(
                        EgresoDto(
                            childId = child.id,
                            date = todayIso,
                            exitTime = time,
                            observations = observations
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        return Result.success(Unit)
    }
}
