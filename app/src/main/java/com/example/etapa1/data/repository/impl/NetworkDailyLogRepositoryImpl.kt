package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.DailyLogDto
import com.example.etapa1.data.remote.EgresoLogDto
import com.example.etapa1.data.remote.EstadiaLogDto
import com.example.etapa1.data.remote.IngresoLogDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.RemoteSync.enumOrNull
import com.example.etapa1.data.remote.RemoteSync.plusDays
import com.example.etapa1.data.remote.RemoteSync.toIso
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.domain.DailyLogValidator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MedicationPrescriptionOption
import com.example.etapa1.model.MoodState
import com.example.etapa1.model.SphincterControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Registro diario (hoja oficial) guardado en el backend (/daily-logs).
 *
 * Se validan los datos igual que en la versión en memoria ([DailyLogValidator]), se actualiza la
 * pantalla al momento y se manda el apartado (ingreso, estadía o egreso) al servidor.
 * Carga las últimas 5 semanas, que es lo que se puede exportar en PDF.
 */
class NetworkDailyLogRepositoryImpl(
    private val api: SonrisasApi
) : DailyLogRepository {

    private val _logs = MutableStateFlow<List<DailyLog>>(emptyList())
    override val logs: StateFlow<List<DailyLog>> = _logs.asStateFlow()

    /** Días guardados desde la app mientras corre una recarga; no se pisan con datos viejos. */
    private val dirtyKeys = mutableSetOf<Pair<String, SimpleDate>>()

    init {
        refresh()
    }

    override fun refresh() {
        val today = SimpleDate.today()
        synchronized(dirtyKeys) { dirtyKeys.clear() }
        RemoteSync.launch("Cargar registro diario") {
            api.getDailyLogs(from = today.plusDays(-35).toIso(), to = today.toIso())
                .bodyOrLog("GET /daily-logs")
                ?.let { list ->
                    val loaded = list.mapNotNull { it.toDomain() }
                    _logs.update { current ->
                        val dirty = synchronized(dirtyKeys) { dirtyKeys.toSet() }
                        loaded.filter { (it.childId to it.date) !in dirty } +
                            current.filter { (it.childId to it.date) in dirty }
                    }
                }
        }
    }

    override fun logFor(childId: String, date: SimpleDate): DailyLog? =
        _logs.value.find { it.childId == childId && it.date == date }

    override fun logsFor(childId: String, days: List<SimpleDate>): List<DailyLog?> =
        days.map { logFor(childId, it) }

    override fun saveIngreso(childId: String, date: SimpleDate, ingreso: IngresoLog): Result<DailyLog> =
        DailyLogValidator.validateIngreso(ingreso).map { valid ->
            upsert(childId, date) { it.copy(ingreso = valid) }.also { pushIngreso(childId, date, valid) }
        }

    override fun saveEstadia(childId: String, date: SimpleDate, estadia: EstadiaLog): Result<DailyLog> =
        DailyLogValidator.validateEstadia(estadia).map { valid ->
            upsert(childId, date) { it.copy(estadia = valid) }.also { pushEstadia(childId, date, valid) }
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
            upsert(childId, date) { it.copy(egreso = valid) }.also { pushEgreso(childId, date, valid) }
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
        val updated = upsert(childId, date) { log ->
            if (isIngreso) {
                val base = log.ingreso ?: IngresoLog(time = normalized)
                log.copy(ingreso = base.copy(time = normalized, observations = notes.ifEmpty { base.observations }))
            } else {
                val base = log.egreso ?: EgresoLog(time = normalized)
                log.copy(egreso = base.copy(time = normalized, observations = notes.ifEmpty { base.observations }))
            }
        }
        if (isIngreso) updated.ingreso?.let { pushIngreso(childId, date, it) }
        else updated.egreso?.let { pushEgreso(childId, date, it) }
        return Result.success(updated)
    }

    // ------------------------------------------------------------------ envío al servidor

    private fun pushIngreso(childId: String, date: SimpleDate, ingreso: IngresoLog) =
        RemoteSync.launch("Guardar ingreso") {
            api.saveIngresoLog(childId, date.toIso(), ingreso.toDto()).bodyOrLog("PUT ingreso")?.let(::applyServer)
        }

    private fun pushEstadia(childId: String, date: SimpleDate, estadia: EstadiaLog) =
        RemoteSync.launch("Guardar estadía") {
            api.saveEstadiaLog(childId, date.toIso(), estadia.toDto()).bodyOrLog("PUT estadía")?.let(::applyServer)
        }

    private fun pushEgreso(childId: String, date: SimpleDate, egreso: EgresoLog) =
        RemoteSync.launch("Guardar egreso") {
            api.saveEgresoLog(childId, date.toIso(), egreso.toDto()).bodyOrLog("PUT egreso")?.let(::applyServer)
        }

    private fun applyServer(dto: DailyLogDto) {
        val log = dto.toDomain() ?: return
        _logs.update { list -> list.filterNot { it.childId == log.childId && it.date == log.date } + log }
    }

    private fun upsert(childId: String, date: SimpleDate, change: (DailyLog) -> DailyLog): DailyLog {
        synchronized(dirtyKeys) { dirtyKeys.add(childId to date) }
        val updated = change(logFor(childId, date) ?: DailyLog(childId = childId, date = date))
        _logs.update { list -> list.filterNot { it.childId == childId && it.date == date } + updated }
        return updated
    }

    // ------------------------------------------------------------------ conversiones

    private fun DailyLogDto.toDomain(): DailyLog? {
        val parsed = RemoteSync.parseIso(date) ?: return null
        return DailyLog(
            childId = childId,
            date = parsed,
            ingreso = ingreso?.toDomain(),
            estadia = estadia?.toDomain(),
            egreso = egreso?.toDomain()
        )
    }

    private fun IngresoLogDto.toDomain() = IngresoLog(
        time = time,
        assistantName = assistantName,
        deliveredBy = deliveredBy,
        receivedBy = receivedBy,
        isSick = isSick,
        medicationWithPrescription = enumOrNull<MedicationPrescriptionOption>(medicationWithPrescription),
        hasSymptoms = hasSymptoms,
        symptoms = symptoms,
        arrivedClean = arrivedClean,
        backpackComplete = backpackComplete,
        goodPhysicalCondition = goodPhysicalCondition,
        injuries = injuries,
        observations = observations
    )

    private fun IngresoLog.toDto() = IngresoLogDto(
        time = time,
        assistantName = assistantName,
        deliveredBy = deliveredBy,
        receivedBy = receivedBy,
        isSick = isSick,
        medicationWithPrescription = medicationWithPrescription?.name,
        hasSymptoms = hasSymptoms,
        symptoms = symptoms,
        arrivedClean = arrivedClean,
        backpackComplete = backpackComplete,
        goodPhysicalCondition = goodPhysicalCondition,
        injuries = injuries,
        observations = observations
    )

    private fun EstadiaLogDto.toDomain() = EstadiaLog(
        breakfast = enumOrNull<FoodConsumptionOption>(breakfast),
        snack = enumOrNull<FoodConsumptionOption>(snack),
        lunch = enumOrNull<FoodConsumptionOption>(lunch),
        foodNotes = foodNotes,
        slept = slept,
        napMinutes = napMinutes,
        napComments = napComments,
        sphincterControl = enumOrNull<SphincterControl>(sphincterControl),
        sphincterPeeCount = sphincterPeeCount,
        sphincterPoopCount = sphincterPoopCount,
        peeCount = peeCount,
        poopCount = poopCount,
        excretionNotes = excretionNotes,
        mood = enumOrNull<MoodState>(mood),
        cried = cried,
        fought = fought,
        participated = participated,
        moodNotes = moodNotes,
        hadAccident = hadAccident,
        accidentDescription = accidentDescription,
        accidentFolio = accidentFolio,
        hadHealthIssue = hadHealthIssue,
        healthIssue = healthIssue,
        careProvided = careProvided,
        generalNotes = generalNotes
    )

    private fun EstadiaLog.toDto() = EstadiaLogDto(
        breakfast = breakfast?.name,
        snack = snack?.name,
        lunch = lunch?.name,
        foodNotes = foodNotes,
        slept = slept,
        napMinutes = napMinutes,
        napComments = napComments,
        sphincterControl = sphincterControl?.name,
        sphincterPeeCount = sphincterPeeCount,
        sphincterPoopCount = sphincterPoopCount,
        peeCount = peeCount,
        poopCount = poopCount,
        excretionNotes = excretionNotes,
        mood = mood?.name,
        cried = cried,
        fought = fought,
        participated = participated,
        moodNotes = moodNotes,
        hadAccident = hadAccident,
        accidentDescription = accidentDescription,
        accidentFolio = accidentFolio,
        hadHealthIssue = hadHealthIssue,
        healthIssue = healthIssue,
        careProvided = careProvided,
        generalNotes = generalNotes
    )

    private fun EgresoLogDto.toDomain() = EgresoLog(
        time = time,
        goodPhysicalCondition = goodPhysicalCondition,
        injuries = injuries,
        collectedBy = collectedBy,
        deliveredClean = deliveredClean,
        belongingsComplete = belongingsComplete,
        observations = observations,
        deliveredByStaff = deliveredByStaff
    )

    private fun EgresoLog.toDto() = EgresoLogDto(
        time = time,
        goodPhysicalCondition = goodPhysicalCondition,
        injuries = injuries,
        collectedBy = collectedBy,
        deliveredClean = deliveredClean,
        belongingsComplete = belongingsComplete,
        observations = observations,
        deliveredByStaff = deliveredByStaff
    )
}
