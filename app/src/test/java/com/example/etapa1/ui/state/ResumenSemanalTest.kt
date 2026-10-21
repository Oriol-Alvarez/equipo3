package com.example.etapa1.ui.state

import com.example.etapa1.model.AttendanceRecord
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.WeeklySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ResumenSemanalTest {

    // Miércoles 21 de octubre de 2026 → semana del 19 al 23
    private val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("21/10/2026")!!
    private val semanas = CalculadoraResumenSemanal.semanas(hoy)

    private fun ingreso(fecha: String, hora: String, nino: String = "n1") =
        AttendanceRecord(id = "a$fecha$hora", childId = nino, isIngreso = true, date = fecha, dayOfWeek = "L", time = hora)

    private fun actividad(fecha: String, cat: String, badge: String) =
        TimelineItem.ActivityCard(id = "$fecha$cat$badge", time = "10:00 AM", category = cat, portionLabel = badge, description = "", date = fecha)

    @Test
    fun semanasVanDeLunesAViernesConEtiquetasReales() {
        assertEquals(4, semanas.size)
        assertEquals("Semana actual", semanas[0].etiqueta)
        assertEquals("Semana del 19 al 23 de octubre, 2026", semanas[0].rango)
        assertEquals("12 - 16 oct", semanas[1].etiqueta)
        assertEquals("28 sep - 2 oct", semanas[3].etiqueta)
    }

    @Test
    fun domingoPerteneceALaSemanaQueTermina() {
        val domingo = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("25/10/2026")!!
        assertEquals("Semana del 19 al 23 de octubre, 2026", CalculadoraResumenSemanal.semanas(domingo)[0].rango)
    }

    @Test
    fun sinRegistrosNoInventaDatos() {
        val r = CalculadoraResumenSemanal.calcular("Mateo", "n1", semanas[0], emptyList(), emptyList())
        assertEquals("0/5 días", r.asistenciaTexto)
        assertEquals("Sin datos", r.alimentacionTexto)
        assertEquals("Sin datos", r.descansoTexto)
        assertEquals("Sin datos", r.animoTexto)
        assertTrue(r.texto.startsWith("Todavía no hay registros de Mateo"))
    }

    @Test
    fun calculaConLosRegistrosDeLaSemanaElegida() {
        val asistencias = listOf(
            ingreso("2026-10-19", "08:20 AM"),
            ingreso("2026-10-20", "07:55 AM"),
            ingreso("2026-10-20", "07:55 AM"),        // duplicado del mismo día
            ingreso("2026-10-21", "07:40 AM", "otro"), // otro niño
            ingreso("2026-10-12", "07:00 AM")          // semana anterior
        )
        val timeline = listOf(
            actividad("19/10/2026", "Alimentación", "Comida: Todo"),
            actividad("20/10/2026", "Alimentación", "Comida: Poco"),
            actividad("21/10/2026", "Alimentación", "Comida: Pidió más"),
            actividad("19/10/2026", "Descanso", "60 min siesta"),
            actividad("20/10/2026", "Descanso", "90 min siesta"),
            actividad("21/10/2026", "Descanso", "No durmió"),
            actividad("19/10/2026", "Estado de Ánimo", "🙂 Feliz"),
            actividad("20/10/2026", "Estado de Ánimo", "🙂 Feliz"),
            actividad("21/10/2026", "Estado de Ánimo", "😢 Triste"),
            actividad("13/10/2026", "Alimentación", "Comida: Nada") // semana anterior
        )
        val r = CalculadoraResumenSemanal.calcular("Mateo", "n1", semanas[0], timeline, asistencias)

        assertEquals("2/5 días", r.asistenciaTexto)
        assertEquals("07:55 AM", r.entradaMasTemprana)
        assertEquals("66%", r.alimentacionTexto)
        assertEquals("2 de 3 comidas completas", r.alimentacionDetalle)
        assertEquals("2h 30m", r.descansoTexto)
        assertEquals("Prom. 1h 15m/día", r.descansoDetalle)
        assertEquals("Feliz", r.animoTexto)
        assertTrue(r.texto.contains("asistió 2 días"))
        assertTrue(r.texto.contains("terminó 2 de 3 comidas"))
    }

    @Test
    fun semanaAnteriorUsaSoloSusRegistros() {
        val timeline = listOf(actividad("13/10/2026", "Alimentación", "Comida: Nada"))
        val r = CalculadoraResumenSemanal.calcular("Mateo", "n1", semanas[1], timeline, emptyList())
        assertEquals("0%", r.alimentacionTexto)
        assertNull(r.animoFrecuente)
    }

    @Test
    fun entendeAmbosFormatosDeFechaYHora() {
        assertEquals(
            CalculadoraResumenSemanal.parseFecha("2026-10-19"),
            CalculadoraResumenSemanal.parseFecha("19/10/2026")
        )
        assertNull(CalculadoraResumenSemanal.parseFecha("Hoy"))
        assertEquals(8 * 60 + 5, CalculadoraResumenSemanal.minutosDelDia("08:05 AM"))
        assertEquals(14 * 60, CalculadoraResumenSemanal.minutosDelDia("14:00"))
    }

    private fun delServidor(id: String) = WeeklySummary(
        id = id,
        attendanceDaysCount = 1,
        totalSchoolDays = 2,
        averageArrivalTime = "08:20 AM",
        foodIntakePercentage = "78%",
        totalNapHours = "0h 45m",
        averageNapDaily = "-",
        moodSummary = "Mayormente feliz",
        executiveSummary = "Mateo asistió 1 de 2 días.",
        foodHighlights = listOf("Comida: comió completo 1 de 1 día"),
        napHighlights = "",
        pedagogicalHighlights = emptyList(),
        healthAndSafetySummary = ""
    )

    @Test
    fun usaElResumenDelServidorCuandoExisteParaEsaSemana() {
        val local = CalculadoraResumenSemanal.calcular("Mateo", "n1", semanas[0], emptyList(), emptyList())
        val v = vistaResumen(local, listOf(delServidor("2026-10-19")))
        assertTrue(v.delServidor)
        assertEquals("1/2 días", v.asistenciaTexto)
        assertEquals("Llegada promedio 08:20 AM", v.asistenciaDetalle)
        assertEquals("78%", v.alimentacionTexto)
        assertEquals("Sin siestas registradas", v.descansoDetalle)
        assertEquals("Mateo asistió 1 de 2 días.", v.texto)
    }

    @Test
    fun sinResumenDelServidorUsaElCalculoLocal() {
        val local = CalculadoraResumenSemanal.calcular("Mateo", "n1", semanas[0], emptyList(), emptyList())
        val v = vistaResumen(local, listOf(delServidor("2026-10-12")))  // otra semana
        assertTrue(!v.delServidor)
        assertEquals(local.asistenciaTexto, v.asistenciaTexto)
        assertEquals(local.texto, v.texto)
        assertEquals("2026-10-19", semanas[0].idServidor())
    }
}
