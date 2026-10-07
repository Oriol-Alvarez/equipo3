package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.WeeklyObservationsRepository
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WEEKLY_OBSERVATION_MAX_LENGTH
import com.example.etapa1.model.WeeklyObservation
import com.example.etapa1.model.WeeklyObservationsSampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementación en memoria: las observaciones se pierden al cerrar el proceso de la app.
 * [currentTime] se puede reemplazar en pruebas para obtener horas fijas.
 */
class MockWeeklyObservationsRepositoryImpl(
    initialObservations: List<WeeklyObservation> = WeeklyObservationsSampleData.initialObservations(),
    private val currentTime: () -> String = {
        SimpleDateFormat("dd/MM hh:mm a", Locale.US).format(Date())
    }
) : WeeklyObservationsRepository {

    private val _observations = MutableStateFlow(initialObservations)
    override val observations: StateFlow<List<WeeklyObservation>> = _observations.asStateFlow()

    private var idCounter = 0

    override fun addObservation(
        childId: String,
        weekKey: String,
        authorRole: ObservationAuthorRole,
        authorName: String,
        text: String
    ): Result<WeeklyObservation> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe una observación antes de guardar."))
        }
        if (cleanText.length > MAX_LENGTH) {
            return Result.failure(
                IllegalArgumentException("La observación puede tener hasta $MAX_LENGTH caracteres.")
            )
        }

        idCounter += 1
        val observation = WeeklyObservation(
            id = "wobs_${System.currentTimeMillis()}_$idCounter",
            childId = childId,
            weekKey = weekKey,
            authorRole = authorRole,
            authorName = authorName,
            text = cleanText,
            time = currentTime()
        )
        _observations.update { it + observation }
        return Result.success(observation)
    }

    companion object {
        const val MAX_LENGTH = WEEKLY_OBSERVATION_MAX_LENGTH
    }
}
