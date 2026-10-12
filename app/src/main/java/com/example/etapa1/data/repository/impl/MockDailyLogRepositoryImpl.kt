package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.domain.DailyLogValidator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.DailyLogSampleData
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Implementación en memoria; trae cuatro semanas de ejemplo para la demo. */
class MockDailyLogRepositoryImpl(
    initialLogs: List<DailyLog> = DailyLogSampleData.generate(
        children = MockDataRepository.childrenSala1A + MockDataRepository.parentChildren,
        today = SimpleDate.today()
    )
) : DailyLogRepository {

    private val _logs = MutableStateFlow(initialLogs)
    override val logs: StateFlow<List<DailyLog>> = _logs.asStateFlow()

    override fun logFor(childId: String, date: SimpleDate): DailyLog? =
        _logs.value.find { it.childId == childId && it.date == date }

    override fun logsFor(childId: String, days: List<SimpleDate>): List<DailyLog?> =
        days.map { logFor(childId, it) }

    override fun saveIngreso(childId: String, date: SimpleDate, ingreso: IngresoLog): Result<DailyLog> =
        DailyLogValidator.validateIngreso(ingreso).map { valid ->
            upsert(childId, date) { it.copy(ingreso = valid) }
        }

    override fun saveEstadia(childId: String, date: SimpleDate, estadia: EstadiaLog): Result<DailyLog> =
        DailyLogValidator.validateEstadia(estadia).map { valid ->
            upsert(childId, date) { it.copy(estadia = valid) }
        }

    override fun updateEstadia(
        childId: String,
        date: SimpleDate,
        change: (EstadiaLog) -> EstadiaLog
    ): Result<DailyLog> {
        val current = logFor(childId, date)?.estadia ?: EstadiaLog()
        return saveEstadia(childId, date, change(current))
    }

    override fun saveEgreso(childId: String, date: SimpleDate, egreso: EgresoLog): Result<DailyLog> {
        val entryTime = logFor(childId, date)?.ingreso?.time
        return DailyLogValidator.validateEgreso(egreso, entryTime).map { valid ->
            upsert(childId, date) { it.copy(egreso = valid) }
        }
    }

    override fun recordAttendanceTime(
        childId: String,
        date: SimpleDate,
        isIngreso: Boolean,
        time: String,
        observations: String
    ): Result<DailyLog> {
        val normalized = DailyLogValidator.normalizeTime(time)
            ?: return Result.failure(IllegalArgumentException("Hora no válida: $time"))
        val notes = observations.trim()
        return Result.success(
            upsert(childId, date) { log ->
                if (isIngreso) {
                    val base = log.ingreso ?: IngresoLog(time = normalized)
                    log.copy(ingreso = base.copy(time = normalized, observations = notes.ifEmpty { base.observations }))
                } else {
                    val base = log.egreso ?: EgresoLog(time = normalized)
                    log.copy(egreso = base.copy(time = normalized, observations = notes.ifEmpty { base.observations }))
                }
            }
        )
    }

    private fun upsert(childId: String, date: SimpleDate, change: (DailyLog) -> DailyLog): DailyLog {
        val updated = change(logFor(childId, date) ?: DailyLog(childId = childId, date = date))
        _logs.update { list -> list.filterNot { it.childId == childId && it.date == date } + updated }
        return updated
    }
}
