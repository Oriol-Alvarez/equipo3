package com.example.etapa1.data.repository

import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WeeklyObservation
import kotlinx.coroutines.flow.StateFlow

interface WeeklyObservationsRepository {
    /** Todas las observaciones de todos los niños y semanas. */
    val observations: StateFlow<List<WeeklyObservation>>

    /**
     * Agrega una observación al resumen semanal de un niño.
     * Falla con un mensaje en español si el texto está vacío o es demasiado largo.
     */
    fun addObservation(
        childId: String,
        weekKey: String,
        authorRole: ObservationAuthorRole,
        authorName: String,
        text: String
    ): Result<WeeklyObservation>
}
