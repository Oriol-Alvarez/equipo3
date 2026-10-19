package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.CreateSuggestionDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.SuggestionDto
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkSuggestionsRepositoryImpl(
    private val api: SonrisasApi
) : SuggestionsRepository {

    private val _suggestions = MutableStateFlow<List<ParentSuggestion>>(MockDataRepository.getInitialSuggestions())
    override val suggestions: StateFlow<List<ParentSuggestion>> = _suggestions.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.getSuggestions()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    _suggestions.value = res.body()!!.map { it.toDomain() }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun addSuggestion(suggestion: ParentSuggestion) {
        _suggestions.update { listOf(suggestion) + it }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.createSuggestion(
                    CreateSuggestionDto(
                        childId = "child_default",
                        roomName = suggestion.roomName,
                        category = suggestion.category.name,
                        subject = suggestion.subject,
                        content = suggestion.content
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun updateSuggestionStatus(id: String, status: SuggestionStatus, response: String?, responderName: String?) {
        _suggestions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = status,
                        response = response ?: it.response,
                        responseDate = if (response != null) "Hoy" else it.responseDate,
                        responderName = responderName ?: it.responderName
                    )
                } else it
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateSuggestionStatus(
                    id = id,
                    request = com.example.etapa1.data.remote.UpdateSuggestionStatusDto(
                        status = status.name,
                        response = response,
                        responderName = responderName
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun SuggestionDto.toDomain(): ParentSuggestion {
        val cat = try {
            SuggestionCategory.valueOf(category.uppercase())
        } catch (_: Exception) {
            SuggestionCategory.GENERAL
        }
        val stat = try {
            SuggestionStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            SuggestionStatus.PENDIENTE
        }
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
