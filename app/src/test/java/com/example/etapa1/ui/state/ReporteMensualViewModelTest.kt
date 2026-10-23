package com.example.etapa1.ui.state

import com.example.etapa1.data.remote.ReporteMensualDto
import com.example.etapa1.data.repository.ReporteMensualRepository
import com.example.etapa1.data.repository.ResultadoReporte
import com.example.etapa1.data.repository.mesesRecientes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** RF-19: la pantalla del reporte mensual carga el mes elegido y muestra el error si no hay red. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReporteMensualViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class RepoFalso(var fallar: Boolean = false) : ReporteMensualRepository {
        val pedidos = mutableListOf<Pair<String, String>>()
        override fun mesesDisponibles() = listOf("2026-10", "2026-09", "2026-08")
        override suspend fun obtener(childId: String, mes: String): ResultadoReporte {
            pedidos += childId to mes
            return if (fallar) ResultadoReporte.Error("Sin conexión")
            else ResultadoReporte.Listo(ReporteMensualDto(id = mes, monthLabel = mes, reportText = "Texto $mes"))
        }
    }

    @Test
    fun alAbrirCargaElMesActual() {
        val repo = RepoFalso()
        val vm = ReporteMensualViewModel(repo)
        vm.abrir("mateo_garcia")
        val estado = vm.estado.value
        assertEquals("2026-10", estado.mes)
        assertFalse(estado.cargando)
        assertEquals("Texto 2026-10", (estado.resultado as ResultadoReporte.Listo).reporte.reportText)
        assertEquals(listOf("mateo_garcia" to "2026-10"), repo.pedidos)
    }

    @Test
    fun elegirOtroMesLoPide() {
        val repo = RepoFalso()
        val vm = ReporteMensualViewModel(repo)
        vm.abrir("mateo_garcia")
        vm.elegirMes("2026-09")
        assertEquals("2026-09", (vm.estado.value.resultado as ResultadoReporte.Listo).reporte.id)
        // Volver a tocar el mismo mes no repite la petición
        vm.elegirMes("2026-09")
        assertEquals(2, repo.pedidos.size)
    }

    @Test
    fun errorYReintento() {
        val repo = RepoFalso(fallar = true)
        val vm = ReporteMensualViewModel(repo)
        vm.abrir("lucia_garcia")
        assertTrue(vm.estado.value.resultado is ResultadoReporte.Error)
        repo.fallar = false
        vm.reintentar()
        assertTrue(vm.estado.value.resultado is ResultadoReporte.Listo)
    }

    @Test
    fun mesesRecientesSonTresDelMasNuevoAlMasViejo() {
        val meses = mesesRecientes()
        assertEquals(3, meses.size)
        assertTrue(meses.all { Regex("""\d{4}-\d{2}""").matches(it) })
        assertTrue(meses[0] > meses[1] && meses[1] > meses[2])
    }
}
