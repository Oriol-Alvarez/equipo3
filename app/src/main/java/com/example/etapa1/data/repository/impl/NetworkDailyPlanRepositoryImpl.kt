package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.DailyMenuDto
import com.example.etapa1.data.remote.DailyPlanDto
import com.example.etapa1.data.remote.PlannedActivityDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.RemoteSync.plusDays
import com.example.etapa1.data.remote.RemoteSync.toIso
import com.example.etapa1.data.remote.SaveDailyPlanDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.DailyPlanRepository
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyMenu
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.PlannedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Planeación y menú diario guardados en el backend (/daily-plans). */
class NetworkDailyPlanRepositoryImpl(
    private val api: SonrisasApi
) : DailyPlanRepository {

    private val _plans = MutableStateFlow<List<DailyPlan>>(emptyList())
    override val plans: StateFlow<List<DailyPlan>> = _plans.asStateFlow()

    /** Días guardados desde la app mientras corre una recarga; no se pisan con datos viejos. */
    private val dirtyDates = mutableSetOf<SimpleDate>()

    init {
        refresh()
    }

    override fun refresh() {
        val today = SimpleDate.today()
        synchronized(dirtyDates) { dirtyDates.clear() }
        RemoteSync.launch("Cargar planeación") {
            api.getDailyPlans(from = today.plusDays(-14).toIso(), to = today.plusDays(30).toIso())
                .bodyOrLog("GET /daily-plans")
                ?.let { list ->
                    val loaded = list.mapNotNull { it.toDomain() }
                    _plans.update { current ->
                        val dirty = synchronized(dirtyDates) { dirtyDates.toSet() }
                        loaded.filter { it.date !in dirty } + current.filter { it.date in dirty }
                    }
                }
        }
    }

    override fun planFor(date: SimpleDate): DailyPlan? = _plans.value.find { it.date == date }

    override fun savePlan(plan: DailyPlan) {
        val previous = planFor(plan.date)
        synchronized(dirtyDates) { dirtyDates.add(plan.date) }
        _plans.update { list -> list.filter { it.date != plan.date } + plan }

        RemoteSync.launch("Guardar planeación") {
            val body = SaveDailyPlanDto(
                menu = DailyMenuDto(plan.menu.breakfast, plan.menu.snack, plan.menu.lunch, plan.menu.notes),
                activities = plan.activities.map { PlannedActivityDto(it.time, it.title) }
            )
            val saved = api.saveDailyPlan(plan.date.toIso(), body).bodyOrLog("PUT /daily-plans")?.toDomain()
            _plans.update { list ->
                val others = list.filter { it.date != plan.date }
                when {
                    saved != null -> others + saved
                    previous != null -> others + previous // el servidor la rechazó: regresa la anterior
                    else -> others
                }
            }
        }
    }

    private fun DailyPlanDto.toDomain(): DailyPlan? {
        val parsed = RemoteSync.parseIso(date) ?: return null
        return DailyPlan(
            date = parsed,
            menu = DailyMenu(menu.breakfast, menu.snack, menu.lunch, menu.notes),
            activities = activities.map { PlannedActivity(it.time, it.title) },
            updatedBy = updatedBy
        )
    }
}
