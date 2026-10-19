package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.CreateSuggestionDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.SuggestionDto
import com.example.etapa1.data.remote.UpdateSuggestionStatusDto
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Buzón de sugerencias del servidor. El personal ve todas; cada familia, solo las suyas
 * (lo filtra el servidor).
 */
class NetworkSuggestionsRepositoryImpl(
    private val api: SonrisasApi
) : SuggestionsRepository {

    private val _suggestions = MutableStateFlow<List<ParentSuggestion>>(emptyList())
    override val suggestions: StateFlow<List<ParentSuggestion>> = _suggestions.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        RemoteSync.launch("Cargar sugerencias") {
            val lista = api.getSuggestions().bodyOrLog("GET /suggestions")?.map { it.toDomain() } ?: return@launch
            _suggestions.update { actual -> actual.filter { it.id.startsWith("temp_") } + lista }
        }
    }

    override fun limpiar() {
        _suggestions.value = emptyList()
    }

    override fun addSuggestion(suggestion: ParentSuggestion, childId: String) {
        val tempId = "temp_${System.nanoTime()}"
        _suggestions.update { listOf(suggestion.copy(id = tempId)) + it }
        RemoteSync.launch("Enviar sugerencia") {
            val creada = if (childId.isBlank()) {
                null
            } else {
                api.createSuggestion(
                    CreateSuggestionDto(
                        childId = childId,
                        roomName = suggestion.roomName,
                        category = suggestion.category.name,
                        subject = suggestion.subject,
                        content = suggestion.content
                    )
                ).bodyOrLog("POST /suggestions")
            }
            _suggestions.update { lista ->
                if (creada == null) {
                    lista.filterNot { it.id == tempId }
                } else {
                    lista.map { if (it.id == tempId) creada.toDomain() else it }
                }
            }
        }
    }

    override fun updateSuggestionStatus(id: String, status: SuggestionStatus, response: String?, responderName: String?) {
        val antes = _suggestions.value
        _suggestions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = status,
                        response = response ?: it.response,
                        responderName = responderName ?: it.responderName
                    )
                } else {
                    it
                }
            }
        }
        RemoteSync.launch("Responder sugerencia $id") {
            val actualizada = api.updateSuggestionStatus(
                id = id,
                request = UpdateSuggestionStatusDto(status = status.name, response = response, responderName = responderName)
            ).bodyOrLog("PATCH /suggestions/$id/status")
            if (actualizada == null) {
                _suggestions.value = antes
            } else {
                // El servidor pone la fecha y el nombre de quien respondió
                _suggestions.update { lista -> lista.map { if (it.id == id) actualizada.toDomain() else it } }
            }
        }
    }

    private fun SuggestionDto.toDomain(): ParentSuggestion {
        val cat = SuggestionCategory.entries.firstOrNull { it.name == category.uppercase() } ?: SuggestionCategory.GENERAL
        val stat = SuggestionStatus.entries.firstOrNull { it.name == status.uppercase() } ?: SuggestionStatus.PENDIENTE
        return ParentSuggestion(
            id = id,
            parentName = parentName,
            childName = childName,
            roomName = roomName,
            date = date,
            time = time,
            category = cat,
            subject = subject,
            content = content,
            status = stat,
            response = response,
            responseDate = responseDate,
            responderName = responderName
        )
    }
}
