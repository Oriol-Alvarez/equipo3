package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.CreateWeeklyObservationDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.WeeklyObservationDto
import com.example.etapa1.data.repository.WeeklyObservationsRepository
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WEEKLY_OBSERVATION_MAX_LENGTH
import com.example.etapa1.model.WeeklyObservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Observaciones semanales guardadas en el backend.
 * El servidor decide si el autor es familia o educadora según la sesión (no se puede falsear desde la app).
 */
class NetworkWeeklyObservationsRepositoryImpl(
    private val api: SonrisasApi
) : WeeklyObservationsRepository {

    /** Los cambios se mandan al servidor en orden, uno por uno. */
    private val writes = RemoteSync.SerialQueue()

    private val _observations = MutableStateFlow<List<WeeklyObservation>>(emptyList())
    override val observations: StateFlow<List<WeeklyObservation>> = _observations.asStateFlow()

    private var localCounter = 0

    init {
        refresh()
    }

    override fun refresh() {
        RemoteSync.launch("Cargar observaciones") {
            api.getWeeklyObservations().bodyOrLog("GET /weekly-observations")?.let { list ->
                val loaded = list.map { it.toDomain() }
                // Conserva las que se están guardando en este momento
                _observations.update { current ->
                    loaded + current.filter { it.id.startsWith(LOCAL_PREFIX) }
                }
            }
        }
    }

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
        if (cleanText.length > WEEKLY_OBSERVATION_MAX_LENGTH) {
            return Result.failure(
                IllegalArgumentException("La observación puede tener hasta $WEEKLY_OBSERVATION_MAX_LENGTH caracteres.")
            )
        }

        localCounter += 1
        val pending = WeeklyObservation(
            id = "$LOCAL_PREFIX${System.currentTimeMillis()}_$localCounter",
            childId = childId,
            weekKey = weekKey,
            authorRole = authorRole,
            authorName = authorName,
            text = cleanText,
            time = SimpleDateFormat("dd/MM hh:mm a", Locale.US).format(Date())
        )
        _observations.update { it + pending }

        writes.enqueue("Guardar observación") {
            val saved = api.addWeeklyObservation(childId, CreateWeeklyObservationDto(weekKey, cleanText))
                .bodyOrLog("POST observación")
            _observations.update { list ->
                val others = list.filter { it.id != pending.id && it.id != saved?.id }
                if (saved == null) others else others + saved.toDomain()
            }
        }
        return Result.success(pending)
    }

    private fun WeeklyObservationDto.toDomain() = WeeklyObservation(
        id = id,
        childId = childId,
        weekKey = weekKey,
        authorRole = RemoteSync.enumOrNull<ObservationAuthorRole>(authorRole) ?: ObservationAuthorRole.FAMILIA,
        authorName = authorName,
        text = text,
        time = time
    )

    private companion object {
        const val LOCAL_PREFIX = "wobs_local_"
    }
}
