package com.example.etapa1.data.repository

import com.example.etapa1.ZONA_ESTANCIA
import com.example.etapa1.data.remote.NetworkErrorHelper
import com.example.etapa1.data.remote.OfflineStore
import com.example.etapa1.data.remote.ReporteMensualDto
import com.example.etapa1.data.remote.SonrisasApi
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** RF-19: reporte mensual del niño. */
sealed interface ResultadoReporte {
    /** [sinConexion] = se muestra la última copia guardada en el teléfono. */
    data class Listo(val reporte: ReporteMensualDto, val sinConexion: Boolean = false) : ResultadoReporte
    data class Error(val mensaje: String) : ResultadoReporte
}

interface ReporteMensualRepository {
    /** Mes actual y los dos anteriores (YYYY-MM), del más reciente al más antiguo. */
    fun mesesDisponibles(): List<String> = mesesRecientes()

    suspend fun obtener(childId: String, mes: String): ResultadoReporte
}

fun mesesRecientes(cuantos: Int = 3): List<String> {
    val c = Calendar.getInstance(TimeZone.getTimeZone(ZONA_ESTANCIA))
    return (0 until cuantos).map { i ->
        val m = c.clone() as Calendar
        m.set(Calendar.DAY_OF_MONTH, 1)
        m.add(Calendar.MONTH, -i)
        String.format(Locale.US, "%04d-%02d", m.get(Calendar.YEAR), m.get(Calendar.MONTH) + 1)
    }
}

class NetworkReporteMensualRepository(
    private val api: SonrisasApi,
    private val cache: OfflineStore? = null
) : ReporteMensualRepository {

    override suspend fun obtener(childId: String, mes: String): ResultadoReporte {
        val clave = "reporte_${childId}_$mes"
        return try {
            val r = api.getReporteMensual(childId, mes)
            val cuerpo = r.body()
            if (r.isSuccessful && cuerpo != null) {
                cache?.save(clave, cuerpo)
                ResultadoReporte.Listo(cuerpo)
            } else {
                ResultadoReporte.Error(NetworkErrorHelper.parseResponse(r))
            }
        } catch (e: Exception) {
            val guardado = cache?.load<ReporteMensualDto>(clave)
            if (guardado != null) ResultadoReporte.Listo(guardado, sinConexion = true)
            else ResultadoReporte.Error(NetworkErrorHelper.parseException(e))
        }
    }
}

/** Datos de ejemplo cuando la app corre sin servidor (AppContainer.USE_MOCKS). */
class MockReporteMensualRepository : ReporteMensualRepository {
    override suspend fun obtener(childId: String, mes: String): ResultadoReporte = ResultadoReporte.Listo(
        ReporteMensualDto(
            id = mes,
            monthLabel = "Mes de ejemplo",
            periodText = "Datos de demostración",
            schoolDays = 16,
            attendanceDays = 15,
            attendancePercentageText = "94%",
            averageArrivalTime = "08:10 AM",
            foodIntakePercentage = "85%",
            napDays = 14,
            averageNapDaily = "1h 10m/día",
            moodSummary = "Mayormente feliz",
            activitiesRecorded = 40,
            behaviorHighlights = listOf("Feliz: 12 días", "Participó en las actividades 14 días"),
            pointsForFamily = listOf("Nada que requiera atención especial este mes."),
            reportText = "Reporte de ejemplo sin servidor.",
            generatedAt = ""
        )
    )
}
