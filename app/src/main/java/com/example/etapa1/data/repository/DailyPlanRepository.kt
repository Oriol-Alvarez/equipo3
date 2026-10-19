package com.example.etapa1.data.repository

import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyPlan
import kotlinx.coroutines.flow.StateFlow

interface DailyPlanRepository {
    /** Planeación publicada por día (una por fecha). */
    val plans: StateFlow<List<DailyPlan>>

    fun planFor(date: SimpleDate): DailyPlan?

    /** Publica o reemplaza la planeación de ese día. Debe venir ya validada. */
    fun savePlan(plan: DailyPlan)

    /** Sala cuya planeación se muestra (la abierta por la educadora o la del hijo de la familia). */
    fun abrirSala(roomId: String) {}

    /** Vuelve a cargar los datos del servidor. En la versión en memoria no hace nada. */
    fun refresh() {}
    
    /** Borra lo cargado al cerrar sesión. */
    fun limpiar() {}
}
