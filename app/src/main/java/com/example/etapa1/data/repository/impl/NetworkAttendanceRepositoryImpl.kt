package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ColaSinConexion
import com.example.etapa1.data.remote.EgresoDto
import com.example.etapa1.data.remote.IngresoDto
import com.example.etapa1.data.remote.NetworkClient
import com.example.etapa1.data.remote.Pendiente
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.ResultadoEnvio
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TipoPendiente
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.Child
import com.example.etapa1.model.Horas
import com.example.etapa1.model.RegistroAsistencia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Asistencia (RF-03) con operación sin conexión (RF-11).
 *
 * Cada ingreso o egreso se guarda primero en la cola del teléfono y se manda al servidor cuando
 * hay red. Antes, si no había internet, el error se ignoraba y el registro se perdía sin aviso.
 */
class NetworkAttendanceRepositoryImpl(
    private val api: SonrisasApi,
    private val cola: ColaSinConexion? = null,
    private val tokenManager: TokenManager? = null
) : AttendanceRepository {

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    override val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    /** Se avisa cuando un ingreso o egreso ya quedó en el servidor (para recargar el registro diario). */
    var onAsistenciaEnviada: ((childId: String) -> Unit)? = null

    init {
        refresh()
        cola?.let { c ->
            RemoteSync.scope.launch {
                c.resultados.collect { r ->
                    val tipo = r.pendiente.tipo
                    if (tipo != TipoPendiente.INGRESO && tipo != TipoPendiente.EGRESO) return@collect
                    if (r is ResultadoEnvio.Enviado) {
                        refresh()
                        onAsistenciaEnviada?.invoke(r.pendiente.childId)
                    }
                }
            }
        }
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
            // Lo que sigue en la cola todavía no está en el servidor: se conserva en pantalla
            val pendientes = _attendanceRecords.value.filter { it.id.startsWith(PREFIJO_LOCAL) }
                .filter { local -> cola?.pendientes?.value?.any { PREFIJO_LOCAL + it.id == local.id } == true }
            _attendanceRecords.value = pendientes + list
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
    ): Result<Unit> = registrarAsistencia(
        child,
        RegistroAsistencia(isIngreso = isIngreso, hora24 = time, observaciones = observations)
    )

    override fun registrarAsistencia(child: Child, registro: RegistroAsistencia): Result<Unit> {
        val hoy = Date()
        val fechaIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(hoy)
        val cuerpo = if (registro.isIngreso) {
            NetworkClient.json.encodeToString(
                IngresoDto(
                    childId = child.id,
                    date = fechaIso,
                    dayOfWeek = diaDeLaSemana(),
                    entryTime = registro.hora24,
                    assistantName = registro.asistente.ifBlank { null },
                    isChildSick = registro.enfermo,
                    medicationOption = registro.medicamento.name,
                    hasSymptoms = registro.conSintomas,
                    symptomsDetail = registro.sintomas,
                    isClean = registro.limpio,
                    backpackComplete = registro.mochilaCompleta,
                    goodPhysicalCondition = registro.buenEstadoFisico,
                    injuryDetail = registro.lesiones,
                    observations = registro.observaciones,
                    delivererName = registro.entrega.ifBlank { null },
                    receiverStaffName = registro.recibePersonal.ifBlank { null }
                )
            )
        } else {
            NetworkClient.json.encodeToString(
                EgresoDto(
                    childId = child.id,
                    date = fechaIso,
                    exitTime = registro.hora24,
                    goodPhysicalCondition = registro.buenEstadoFisico,
                    injuryDetail = registro.lesiones,
                    isClean = registro.limpio,
                    backpackComplete = registro.mochilaCompleta,
                    observations = registro.observaciones,
                    receiverTutorName = registro.recoge.ifBlank { null },
                    delivererStaffName = registro.entregaPersonal.ifBlank { null }
                )
            )
        }

        val pendiente = Pendiente(
            tipo = if (registro.isIngreso) TipoPendiente.INGRESO else TipoPendiente.EGRESO,
            userId = tokenManager?.userId.orEmpty(),
            childId = child.id,
            cuerpo = cuerpo
        )

        val local = AttendanceRecord(
            id = PREFIJO_LOCAL + pendiente.id,
            childId = child.id,
            isIngreso = registro.isIngreso,
            date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(hoy),
            dayOfWeek = diaDeLaSemana(),
            time = Horas.a12h(registro.hora24),
            observations = registro.observaciones
        )
        _attendanceRecords.update { listOf(local) + it }

        val c = cola
        if (c != null) {
            c.agregar(pendiente)
        } else {
            // Sin cola (pruebas): envío directo
            RemoteSync.launch("Registrar asistencia de ${child.id}") {
                if (registro.isIngreso) {
                    api.registrarIngresoCola(pendiente.id, NetworkClient.json.decodeFromString(cuerpo))
                } else {
                    api.registrarEgresoCola(pendiente.id, NetworkClient.json.decodeFromString(cuerpo))
                }
            }
        }
        return Result.success(Unit)
    }

    private fun diaDeLaSemana(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "M"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        Calendar.SATURDAY -> "S"
        else -> "D"
    }

    private companion object {
        const val PREFIJO_LOCAL = "local_"
    }
}
