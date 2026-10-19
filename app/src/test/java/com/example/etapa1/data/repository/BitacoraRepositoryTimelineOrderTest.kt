package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import org.junit.Assert.assertTrue
import org.junit.Test

class BitacoraRepositoryTimelineOrderTest {

    @Test
    fun itemsAreSortedChronologically_regardlessOfInsertionOrder() {
        val repo = MockBitacoraRepositoryImpl()

        // Insert items out of chronological order
        repo.addActivityCard(
            time = "02:00 PM",
            category = "Juego",
            portionLabel = "Patio",
            description = "Jugando afuera",
            childId = "mateo_garcia"
        )

        repo.addEventChip("08:00 AM", "Ingreso temprano", "entry")
        repo.addEventChip("04:30 PM", "Salida", "exit")
        repo.addActivityCard(
            time = "10:15 AM",
            category = "Alimentación",
            portionLabel = "Fruta",
            description = "Comió fruta",
            childId = "mateo_garcia"
        )

        val items = repo.mateoTimeline.value

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
}
