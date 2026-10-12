package com.example.etapa1.domain

import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.DailyLogSampleData
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSheetLayoutTest {

    /** Superficie falsa: mide el texto con un ancho fijo por letra y anota lo que se dibuja. */
    private class FakeSurface : SheetSurface {
        var begun = 0
        var ended = 0
        var open = false
        val rectBottoms = mutableListOf<Float>()
        val texts = mutableListOf<Pair<String, Float>>()
        val drawnOutsidePage = mutableListOf<String>()

        override fun measure(text: String, size: Float, bold: Boolean): Float = text.length * size * 0.55f
        override fun beginPage() { begun++; open = true }
        override fun endPage() { ended++; open = false }
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Int?, strokeColor: Int?) {
            if (!open) drawnOutsidePage += "rect"
            rectBottoms += bottom
            assertTrue("El rectángulo se sale a lo ancho", left >= 0f && right <= LogSheetLayout.PAGE_WIDTH)
        }
        override fun drawText(text: String, x: Float, baseline: Float, size: Float, bold: Boolean, color: Int) {
            if (!open) drawnOutsidePage += text
            texts += text to baseline
        }
    }

    private val week = SchoolWeek.weekOf(SimpleDate(2026, 10, 7))
    private val child = MockDataRepository.mateoGarcia

    private fun sheet(logs: List<DailyLog?>) =
        LogSheetFormatter.build(child, "Lactantes", DailyLogSampleData.ESTANCIA_NAME, week, logs)

    private fun draw(sheet: LogSheet): Pair<Int, FakeSurface> {
        val surface = FakeSurface()
        val pages = LogSheetLayout.draw(sheet, surface) { page, total -> "Página $page de $total" }
        return pages to surface
    }

    @Test
    fun hojaNormal_tieneDosPaginasConPieCorrecto() {
        val logs = DailyLogSampleData.generate(listOf(child), SimpleDate(2026, 10, 9))
            .filter { it.childId == child.id }
        val (pages, surface) = draw(sheet(week.map { day -> logs.find { it.date == day } }))
        assertEquals(2, pages)
        assertEquals(2, surface.begun)
        assertEquals(2, surface.ended)
        assertTrue(surface.texts.any { it.first == "Página 1 de 2" })
        assertTrue(surface.texts.any { it.first == "Página 2 de 2" })
    }

    @Test
    fun textosLargos_pasanAOtraPaginaSinSalirseDelMargen() {
        val long = "El niño llegó con una pequeña raspadura en la rodilla derecha y se avisó a la mamá. ".repeat(4)
        val logs = week.map { day ->
            DailyLog(
                childId = child.id,
                date = day,
                ingreso = IngresoLog(time = "08:00", hasSymptoms = true, symptoms = long, observations = long),
                estadia = EstadiaLog(foodNotes = long, napComments = long, moodNotes = long, generalNotes = long)
            )
        }
        val (pages, surface) = draw(sheet(logs))
        assertTrue("Con textos largos debe haber más de 2 páginas", pages > 2)
        assertEquals(pages, surface.begun)
        assertEquals(pages, surface.ended)
        val limit = LogSheetLayout.PAGE_HEIGHT - 34f + 0.5f
        assertTrue("Nada de la tabla debe quedar en la zona del pie", surface.rectBottoms.all { it <= limit })
        assertTrue(surface.texts.all { it.second <= LogSheetLayout.PAGE_HEIGHT })
        assertTrue(surface.texts.any { it.first.contains("(continuación)") })
        assertTrue(surface.texts.any { it.first == "Página $pages de $pages" })
    }

    @Test
    fun nadaSeDibujaFueraDeUnaPagina() {
        val (_, surface) = draw(sheet(week.map { null }))
        assertTrue(surface.drawnOutsidePage.isEmpty())
    }

    @Test
    fun wrap_parteRenglonesYPalabrasMuyLargas() {
        val surface = FakeSurface()
        val lines = LogSheetLayout.wrap("uno dos tres cuatro", 20f, 10f, false, surface)
        assertTrue(lines.size > 1)
        assertTrue(lines.all { surface.measure(it, 10f, false) <= 20f })
        val giant = LogSheetLayout.wrap("supercalifragilistico", 20f, 10f, false, surface)
        assertEquals("supercalifragilistico", giant.joinToString(""))
        assertTrue(giant.all { surface.measure(it, 10f, false) <= 20f })
        assertEquals(listOf(""), LogSheetLayout.wrap("", 20f, 10f, false, surface))
    }
}
