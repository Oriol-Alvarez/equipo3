package com.example.etapa1.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias de la política de reintento exponencial (Práctica 12 - Charla).
 */
class ChatEsperaTest {

    @Test
    fun espera_duplica_el_tiempo_hasta_el_tope_de_30_segundos() {
        assertEquals(1_000L, esperaAntesDeReintentar(1))
        assertEquals(2_000L, esperaAntesDeReintentar(2))
        assertEquals(4_000L, esperaAntesDeReintentar(3))
        assertEquals(8_000L, esperaAntesDeReintentar(4))
        assertEquals(16_000L, esperaAntesDeReintentar(5))
        assertEquals(30_000L, esperaAntesDeReintentar(6))
        assertEquals(30_000L, esperaAntesDeReintentar(10))
    }

    @Test
    fun si_el_intento_es_cero_o_negativo_devuelve_al_menos_1_segundo() {
        assertEquals(1_000L, esperaAntesDeReintentar(0))
        assertEquals(1_000L, esperaAntesDeReintentar(-5))
    }
}
