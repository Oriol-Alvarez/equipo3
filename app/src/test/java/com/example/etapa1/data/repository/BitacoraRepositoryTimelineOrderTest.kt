package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitacoraRepositoryTimelineOrderTest {

    @Test
    fun itemsAreSortedChronologically_regardlessOfInsertionOrder() {
        val repo = MockBitacoraRepositoryImpl()
        repo.abrir("lucas_martinez")

        // Se insertan fuera de orden, todos el mismo día
        repo.addActivityCard(
            childId = "lucas_martinez",
            time = "02:00 PM",
            category = "Juego",
            portionLabel = "Patio",
            description = "Jugando afuera",
            date = "12/10/2026"
        )
        repo.addEventChip("lucas_martinez", "08:00 AM", "Ingreso temprano", "entry", "12/10/2026")
        repo.addEventChip("lucas_martinez", "04:30 PM", "Salida", "exit", "12/10/2026")
        repo.addActivityCard(
            childId = "lucas_martinez",
            time = "10:15 AM",
            category = "Alimentación",
            portionLabel = "Fruta",
            description = "Comió fruta",
            date = "12/10/2026"
        )

        val items = repo.timeline.value

        val idx8am = items.indexOfFirst { it.time == "08:00 AM" }
        val idx1015am = items.indexOfFirst { it.time == "10:15 AM" }
        val idx2pm = items.indexOfFirst { it.time == "02:00 PM" }
        val idx430pm = items.indexOfFirst { it.time == "04:30 PM" }

        assertTrue(idx8am >= 0)
        assertTrue(idx1015am >= 0)
        assertTrue(idx2pm >= 0)
        assertTrue(idx430pm >= 0)

        assertTrue("08:00 AM debe ir antes de 10:15 AM", idx8am < idx1015am)
        assertTrue("10:15 AM debe ir antes de 02:00 PM", idx1015am < idx2pm)
        assertTrue("02:00 PM debe ir antes de 04:30 PM", idx2pm < idx430pm)
    }

    @Test
    fun itemsFromDifferentDays_areSortedChronologicallyByDayAndTime() {
        val repo = MockBitacoraRepositoryImpl()
        repo.abrir("mateo_garcia")

        // Actividad previa a mediodía (12:20 PM el 12/10/2026)
        repo.addActivityCard(
            childId = "mateo_garcia",
            time = "12:20 PM",
            category = "Alimentación",
            portionLabel = "Almuerzo",
            description = "Comió sopa",
            date = "12/10/2026"
        )

        // Mensaje y foto enviados días después a primera hora de la mañana (08:30 AM el 19/10/2026)
        repo.addChatMessage(
            childId = "mateo_garcia",
            text = "Foto y mensaje de hoy por la mañana",
            isOutgoing = true,
            fileUri = "content://media/foto.jpg",
            fileName = "foto.jpg",
            fileMimeType = "image/jpeg",
            date = "19/10/2026"
        )

        val items = repo.timeline.value
        val idxActividad = items.indexOfLast { it.time == "12:20 PM" && it.date == "12/10/2026" }
        val idxMensajeFoto = items.indexOfFirst { it.date == "19/10/2026" }

        assertTrue("Debe existir la actividad", idxActividad >= 0)
        assertTrue("Debe existir el mensaje posterior", idxMensajeFoto >= 0)
        assertTrue(
            "El mensaje de hoy (19/10/2026) debe aparecer DESPUÉS de la actividad del día previo (12/10/2026), sin importar que su hora sea más temprana",
            idxActividad < idxMensajeFoto
        )
    }

    @Test
    fun cadaNinoTieneSuPropiaBitacora() {
        val repo = MockBitacoraRepositoryImpl()

        repo.abrir("sofia_lopez")
        repo.addChatMessage(childId = "sofia_lopez", text = "Mensaje para la familia de Sofía")

        // Abrir a otro niño no muestra lo de Sofía
        repo.abrir("lucas_martinez")
        assertTrue(repo.timeline.value.none { it is com.example.etapa1.model.TimelineItem.ChatMessage })

        // Lo que se agrega para Sofía con Lucas abierto no aparece en la pantalla de Lucas
        repo.addChatMessage(childId = "sofia_lopez", text = "Segundo mensaje")
        assertTrue(repo.timeline.value.isEmpty())

        // Al volver a Sofía están sus dos mensajes
        repo.abrir("sofia_lopez")
        assertEquals(2, repo.timeline.value.size)
        assertEquals("sofia_lopez", repo.childIdActual.value)
    }
}
