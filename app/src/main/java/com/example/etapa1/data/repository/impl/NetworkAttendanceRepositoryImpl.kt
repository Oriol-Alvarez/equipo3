package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.EgresoDto
import com.example.etapa1.data.remote.IngresoDto
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.getAttendanceRecords()
                if (res.isSuccessful && res.body() != null) {
                    val list = res.body()!!.map { dto ->
                        AttendanceRecord(
                            id = dto.id,
                            childId = dto.childId,
                            isIngreso = dto.isIngreso,
                            date = dto.date,
                            dayOfWeek = dto.dayOfWeek,
                            time = dto.time,
                            observations = dto.observations
                        )
                    }
                    _attendanceRecords.value = list
                }
            } catch (_: Exception) {
            }
        }
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isIngreso) {
                    api.registrarIngreso(
                        IngresoDto(
                            childId = child.id,
                            date = "2026-10-12",
                            entryTime = time,
                            observations = observations
                        )
                    )
                } else {
                    api.registrarEgreso(
                        EgresoDto(
                            childId = child.id,
                            date = "2026-10-12",
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
