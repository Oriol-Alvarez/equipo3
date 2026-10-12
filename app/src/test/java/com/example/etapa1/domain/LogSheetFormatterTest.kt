package com.example.etapa1.domain

import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.EstadiaLog
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MedicationPrescriptionOption
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.MoodState
import com.example.etapa1.model.SphincterControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSheetFormatterTest {

    private val week = SchoolWeek.weekOf(SimpleDate(2026, 10, 7))
    private val child = MockDataRepository.mateoGarcia

    private val monday = DailyLog(
        childId = child.id,
        date = week[0],
        ingreso = IngresoLog(
            time = "08:10", assistantName = "Laura Gómez", deliveredBy = "María López", receivedBy = "Laura Gómez",
            isSick = true, medicationWithPrescription = MedicationPrescriptionOption.SI,
            hasSymptoms = true, symptoms = "Tos leve", arrivedClean = true, backpackComplete = false,
            goodPhysicalCondition = true
        ),
        estadia = EstadiaLog(
            breakfast = FoodConsumptionOption.TODO, snack = FoodConsumptionOption.POCO,
            lunch = FoodConsumptionOption.PIDIO_MAS, slept = true, napMinutes = 70,
            sphincterControl = SphincterControl.AVISO_SI, peeCount = 3, poopCount = 1,
            mood = MoodState.FELIZ, cried = false, fought = false, participated = true,
            hadAccident = false, hadHealthIssue = true, healthIssue = "Tos leve", careProvided = "Medicamento 12:00"
        ),
        egreso = EgresoLog(
            time = "15:05", goodPhysicalCondition = true, collectedBy = "Rosa Fernández",
            deliveredClean = true, belongingsComplete = true, deliveredByStaff = "Lucía Martín"
        )
    )
    private val wednesday = DailyLog(
        childId = child.id,
        date = week[2],
        ingreso = IngresoLog(time = "08:30")
    )

    private val sheet = LogSheetFormatter.build(
        child = child,
        levelOrGroup = "Lactantes",
        estancia = "Estancia Infantil Sonrisas de Cristal",
        week = week,
        logs = listOf(monday, null, wednesday, null, null)
    )

    private fun rowOf(label: String, pageIndex: Int? = null): LogSheetRow {
        val pages = if (pageIndex == null) sheet.pages else listOf(sheet.pages[pageIndex])
        return pages.flatMap { it.sections }.flatMap { it.rows }.first { it.label == label }
    }

    private fun valuesOf(label: String, pageIndex: Int? = null): List<String> = rowOf(label, pageIndex).values

    private fun choiceOf(label: String, day: Int, pageIndex: Int? = null): LogSheetCell.Choice =
        rowOf(label, pageIndex).cells[day] as LogSheetCell.Choice

    private val injuryLabel = "En caso que la niña o el niño sea entregada(o) con algún rasguño, mordida, quemadura, " +
        "golpe, rozadura, etc. Indicar la parte del cuerpo donde se presenta y especificar la lesión:"

    @Test
    fun header_hasOfficialTitlesChildAndWeek() {
        assertEquals(LogSheetFormatter.TITLE_LINES, sheet.titleLines)
        assertEquals("Mateo García", sheet.childName)
        assertEquals("L", sheet.group)
        assertEquals("Semana del 05/10/2026 al 09/10/2026", sheet.weekLabel)
        assertEquals("Fecha: 05/10/2026", sheet.dayHeaders.first())
        assertEquals("Fecha: 09/10/2026", sheet.dayHeaders.last())
        assertEquals(2, sheet.daysWithRecords)
    }

    @Test
    fun sectionsFollowTheOfficialOrder() {
        assertEquals(2, sheet.pages.size)
        assertEquals("II. Registro de Ingreso de la niña o niño a la Estancia Infantil", sheet.pages[0].sections[0].title)
        assertEquals("IV. Registro de egreso de la niña o niño de la Estancia Infantil", sheet.pages[0].sections[1].title)
        assertEquals("III. Registro de estadía de la niña o niño en la Estancia Infantil", sheet.pages[1].sections[0].title)
        assertEquals(
            listOf("Alimentación", "Descanso", "Funciones Excretoras", "Estado de ánimo", "Accidentes", "Salud"),
            sheet.pages[1].sections.drop(1).map { it.title }
        )
        sheet.pages.flatMap { it.sections }.flatMap { it.rows }.filterNot { it.isCaption }
            .forEach { assertEquals(5, it.cells.size) }
    }

    @Test
    fun choicesShowEveryOptionOfTheOfficialSheet() {
        assertEquals(listOf("Sí", "No"), choiceOf("La niña o el niño está enferma (o)", 0).options)
        assertEquals(
            listOf("Sí", "No", "No aplica"),
            choiceOf("En el caso de que la niña o el niño ingrese enfermo se dejó medicamento con receta médica y especificaciones", 0).options
        )
        assertEquals(listOf("Nada", "Poco", "Todo", "Pidió más"), choiceOf("Desayuno", 0).options)
        assertEquals(listOf("Feliz", "Triste", "Enojado"), choiceOf("¿Durante su estadía, la niña o el niño estuvo?", 0).options)
        assertEquals(listOf("No aplica", "Avisó: Sí", "Avisó: No"), choiceOf("Control de esfínteres", 0).options)
        // Los textos de las opciones coinciden con los de la app.
        assertEquals(FoodConsumptionOption.entries.map { it.label }, choiceOf("Comida", 0).options)
        assertEquals(MoodState.entries.map { it.label }, choiceOf("¿Durante su estadía, la niña o el niño estuvo?", 0).options)
        assertEquals(SphincterControl.entries.map { it.label }, choiceOf("Control de esfínteres", 0).options)
    }

    @Test
    fun ingresoValues() {
        assertEquals(listOf("08:10 hrs.", "", "08:30 hrs.", "", ""), valuesOf("Hora de entrada:"))
        assertEquals("Sí", valuesOf("La niña o el niño está enferma (o)")[0])
        assertEquals(
            "Sí",
            valuesOf("En el caso de que la niña o el niño ingrese enfermo se dejó medicamento con receta médica y especificaciones")[0]
        )
        assertEquals("Sí", valuesOf("La niña o el niño ingresa con síntomas de enfermedad")[0])
        assertEquals("Tos leve", valuesOf("¿Cuál(es)?")[0])
        assertEquals("No", valuesOf("La niña o el niño trajo su mochila completa según sus necesidades:")[0])
        // Sin lesiones se escribe "NA", igual que en la hoja.
        assertEquals("NA", valuesOf(injuryLabel, 0)[0])
    }

    @Test
    fun partialDay_leavesUnansweredQuestionsWithEmptyBoxes() {
        assertEquals(null, choiceOf("La niña o el niño está enferma (o)", 2).selected)
        assertEquals("", valuesOf("Nombre completo de la Asistente al cuidado de la (el) niña (o)")[2])
        // Día sin registro: las opciones se imprimen vacías para llenarlas a mano.
        assertEquals(null, choiceOf("La niña o el niño está enferma (o)", 1).selected)
        assertEquals(listOf("Sí", "No"), choiceOf("La niña o el niño está enferma (o)", 1).options)
    }

    @Test
    fun estadiaAndEgresoValues() {
        assertEquals("Todo", valuesOf("Desayuno")[0])
        assertEquals("Pidió más", valuesOf("Comida")[0])
        assertEquals("70 minutos", valuesOf("Tiempo de siesta (minutos)")[0])
        assertEquals("Avisó: Sí", valuesOf("Control de esfínteres")[0])
        assertEquals("pipí: 3   popó: 1", valuesOf("Pipí y/o Popó · Número de veces (pipí / popó)")[0])
        assertEquals("Feliz", valuesOf("¿Durante su estadía, la niña o el niño estuvo?")[0])
        assertEquals("No", valuesOf("¿La niña o el niño tuvo algún accidente?")[0])
        assertEquals("Sí", valuesOf("¿La niña o el niño presentó algún problema de salud?")[0])
        assertEquals("Tos leve", valuesOf("¿Cuál?")[0])
        assertEquals("15:05 hrs.", valuesOf("Hora de salida:")[0])
        assertEquals("Rosa Fernández", valuesOf("Nombre y firma de quien recibe a la niña o el niño:")[0])
        // Miércoles sin estadía ni egreso registrados: en blanco.
        assertEquals("", valuesOf("Desayuno")[2])
        assertEquals("", valuesOf("Hora de salida:")[2])
    }

    @Test
    fun signatureRows_printNameAndLeaveSpaceToSign() {
        val signatures = sheet.pages.flatMap { it.sections }.flatMap { it.rows }.filter { it.isSignature }

        assertEquals(4, signatures.size)
        assertEquals("María López", valuesOf("Nombre y firma de quien entrega a la niña o el niño:")[0])
        assertEquals("", valuesOf("Nombre y firma de quien entrega a la niña o el niño:")[1])
    }

    @Test
    fun groupLetter_fromRoomLevelOrGroup() {
        assertEquals("L", LogSheetFormatter.groupLetter("Lactantes A"))
        assertEquals("L", LogSheetFormatter.groupLetter("Sala Bebés"))
        assertEquals("M", LogSheetFormatter.groupLetter("Maternales B"))
        assertEquals("P", LogSheetFormatter.groupLetter("Preescolar"))
        assertEquals("", LogSheetFormatter.groupLetter("Grupo Sala 1A"))
    }

    @Test
    fun weekOf_weekendGivesTheWeekThatJustEnded() {
        assertEquals(week, SchoolWeek.weekOf(SimpleDate(2026, 10, 10)))
        assertEquals(week, SchoolWeek.weekOf(SimpleDate(2026, 10, 11)))
        assertEquals(SimpleDate(2026, 10, 12), SchoolWeek.weekOf(SimpleDate(2026, 10, 12)).first())
    }
}
