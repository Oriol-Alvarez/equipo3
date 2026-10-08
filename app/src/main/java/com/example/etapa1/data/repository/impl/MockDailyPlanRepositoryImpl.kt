package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.DailyPlanRepository
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.DailyPlanSampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Implementación en memoria con la semana actual de ejemplo. */
class MockDailyPlanRepositoryImpl(
    initialPlans: List<DailyPlan> = DailyPlanSampleData.weekFor(SimpleDate.today())
) : DailyPlanRepository {

    private val _plans = MutableStateFlow(initialPlans)
    override val plans: StateFlow<List<DailyPlan>> = _plans.asStateFlow()

    override fun planFor(date: SimpleDate): DailyPlan? = _plans.value.find { it.date == date }

    override fun savePlan(plan: DailyPlan) {
        _plans.update { list -> list.filter { it.date != plan.date } + plan }
    }
}
