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
        observations: String,
        personName: String?,
        staffName: String?,
        goodPhysicalCondition: Boolean,
        injuryDetail: String,
        hasSymptoms: Boolean,
        symptomsDetail: String,
        isClean: Boolean,
        backpackComplete: Boolean
    ): Result<Unit> {
        val record = AttendanceRecord(
            id = "att_${System.currentTimeMillis()}",
            childId = child.id,
            isIngreso = isIngreso,
            date = "Hoy",
            dayOfWeek = "M",
            time = time,
            observations = observations,
            delivererName = if (isIngreso) personName else null,
            receiverStaffName = if (isIngreso) staffName else null,
            receiverTutorName = if (!isIngreso) personName else null,
            delivererStaffName = if (!isIngreso) staffName else null,
            goodPhysicalCondition = goodPhysicalCondition,
            injuryDetail = injuryDetail,
            hasSymptoms = hasSymptoms,
            symptomsDetail = symptomsDetail,
            isClean = isClean,
            backpackComplete = backpackComplete
        )
        _attendanceRecords.update { listOf(record) + it }
        return Result.success(Unit)
    }
}
