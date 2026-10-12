package com.example.etapa1.ui.state

import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportLogPresenterTest {

    // Miércoles 7 de octubre de 2026
    private val today = SimpleDate(2026, 10, 7)

    @Test
    fun weekOptions_daCuatroSemanasDeLunesAViernes() {
        val weeks = ExportLogPresenter.weekOptions(today)
        assertEquals(4, weeks.size)
        assertEquals("Esta semana", weeks[0].label)
        assertEquals(SimpleDate(2026, 10, 5), weeks[0].days.first())
        assertEquals(SimpleDate(2026, 10, 9), weeks[0].days.last())
        assertEquals(SimpleDate(2026, 9, 28), weeks[1].days.first())
        assertEquals(SimpleDate(2026, 9, 14), weeks[3].days.first())
        assertTrue(weeks.all { it.days.size == 5 })
        assertEquals("05/10 – 09/10/2026", weeks[0].rangeText)
    }

    @Test
    fun weekOptions_enFinDeSemanaEmpiezaConLaSemanaQueTermino() {
        val weeks = ExportLogPresenter.weekOptions(SimpleDate(2026, 10, 11)) // domingo
        assertEquals(SimpleDate(2026, 10, 5), weeks[0].days.first())
    }

    @Test
    fun dayStatuses_marcaLoRegistradoYLosDiasFuturos() {
        val days = ExportLogPresenter.weekOptions(today)[0].days
        val child = MockDataRepository.mateoGarcia
        val logs = listOf(
            DailyLog(child.id, days[0], ingreso = IngresoLog(time = "08:00"), egreso = EgresoLog(time = "15:00")),
            null,
            DailyLog(child.id, days[2], ingreso = IngresoLog(time = "08:10")),
            null,
            null
        )
        val statuses = ExportLogPresenter.dayStatuses(days, logs, today)
        assertTrue(statuses[0].hasIngreso && statuses[0].hasEgreso && !statuses[0].hasEstadia)
        assertFalse(statuses[1].hasAnyRecord)
        assertFalse(statuses[1].isFuture)
        assertFalse(statuses[2].isFuture)
        assertTrue(statuses[3].isFuture && statuses[4].isFuture)
        assertEquals(2, statuses.count { it.hasAnyRecord })
    }

    @Test
    fun fileName_sinAcentosNiEspacios() {
        val child = MockDataRepository.mateoGarcia.copy(fullName = "José Ñúñez  Pérez-López")
        assertEquals(
            "Bitacora_Jose_Nunez_Perez_Lopez_2026-10-05.pdf",
            ExportLogPresenter.fileName(child, SimpleDate(2026, 10, 5))
        )
    }
}
