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
}
