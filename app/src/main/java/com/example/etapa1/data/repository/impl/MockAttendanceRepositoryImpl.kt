package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.Child
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockAttendanceRepositoryImpl : AttendanceRepository {
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    override val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

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
            dayOfWeek = "M",
            time = time,
            observations = observations
        )
        _attendanceRecords.update { listOf(record) + it }
        return Result.success(Unit)
    }
}
