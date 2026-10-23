package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ReporteMensualRepository
import com.example.etapa1.data.repository.ResultadoReporte
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** RF-19: estado de la pantalla del reporte mensual. */
data class ReporteMensualEstado(
    val childId: String = "",
    val meses: List<String> = emptyList(),
    val mes: String = "",
    val cargando: Boolean = false,
    val resultado: ResultadoReporte? = null
)

class ReporteMensualViewModel(private val repo: ReporteMensualRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ReporteMensualEstado())
    val estado: StateFlow<ReporteMensualEstado> = _estado.asStateFlow()
    private var carga: Job? = null

    fun abrir(childId: String) {
        val meses = repo.mesesDisponibles()
        _estado.value = ReporteMensualEstado(childId = childId, meses = meses, mes = meses.first())
        cargar()
    }

    fun elegirMes(mes: String) {
        if (mes == _estado.value.mes && _estado.value.resultado is ResultadoReporte.Listo) return
        _estado.update { it.copy(mes = mes) }
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        val actual = _estado.value
        if (actual.childId.isBlank()) return
        carga?.cancel()
        _estado.update { it.copy(cargando = true, resultado = null) }
        carga = viewModelScope.launch {
            val r = repo.obtener(actual.childId, actual.mes)
            _estado.update { if (it.mes == actual.mes) it.copy(cargando = false, resultado = r) else it }
        }
    }
}
