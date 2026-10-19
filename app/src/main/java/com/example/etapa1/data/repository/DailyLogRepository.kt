package com.example.etapa1.data.repository

import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.IngresoLog
import kotlinx.coroutines.flow.StateFlow

/**
 * Registro diario por niño (bitácora de filtro diario). Es la fuente de datos de la hoja
 * oficial que se exporta en PDF.
 *
 * Para conectar los formularios: el formulario de ingreso debe llamar a [saveIngreso],
 * el de egreso a [saveEgreso] y el de actividades a [saveEstadia] / [updateEstadia].
 */
interface DailyLogRepository {
    val logs: StateFlow<List<DailyLog>>

    fun logFor(childId: String, date: SimpleDate): DailyLog?

    /** Registros del niño para cada día indicado (null en los días sin registro). */
    fun logsFor(childId: String, days: List<SimpleDate>): List<DailyLog?>

    /** Guarda el ingreso completo. Valida con las reglas de la hoja y reemplaza el ingreso de ese día. */
    fun saveIngreso(childId: String, date: SimpleDate, ingreso: IngresoLog): Result<DailyLog>

    /** Guarda la estadía completa del día (reemplaza la anterior). */
    fun saveEstadia(childId: String, date: SimpleDate, estadia: EstadiaLog): Result<DailyLog>

    /**
     * Modifica solo una parte de la estadía (p. ej. al registrar la comida desde "Nueva actividad")
     * sin borrar lo que ya se había registrado ese día.
     */
    fun updateEstadia(childId: String, date: SimpleDate, change: (EstadiaLog) -> EstadiaLog): Result<DailyLog>

    /** Guarda el egreso completo. No permite una salida antes de la entrada del mismo día. */
    fun saveEgreso(childId: String, date: SimpleDate, egreso: EgresoLog): Result<DailyLog>

    /**
     * Registro rápido de la hora de entrada o salida (lo que guarda hoy el formulario de asistencia).
     * Conserva las demás respuestas del día si ya existían.
     */
    fun recordAttendanceTime(
        childId: String,
        date: SimpleDate,
        isIngreso: Boolean,
        time: String,
        observations: String = ""
    ): Result<DailyLog>

    /** Vuelve a cargar los datos del servidor. En la versión en memoria no hace nada. */
    fun refresh() {}
    
    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
