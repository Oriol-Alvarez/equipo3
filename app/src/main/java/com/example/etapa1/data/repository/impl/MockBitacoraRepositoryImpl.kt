package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bitácora en memoria para el modo sin servidor (AppContainer.USE_MOCKS) y las pruebas.
 * Guarda una línea de tiempo por niño; Mateo empieza con los datos de ejemplo.
 */
class MockBitacoraRepositoryImpl : BitacoraRepository {
    private val porNino = mutableMapOf<String, List<TimelineItem>>(
        MockDataRepository.mateoGarcia.id to MockDataRepository.getInitialMateoTimeline()
    )

    private val _childIdActual = MutableStateFlow<String?>(null)
    override val childIdActual: StateFlow<String?> = _childIdActual.asStateFlow()

    private val _timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    override val timeline: StateFlow<List<TimelineItem>> = _timeline.asStateFlow()

    override fun abrir(childId: String) {
        _childIdActual.value = childId
        _timeline.value = porNino[childId].orEmpty()
    }

    private fun agregar(childId: String, item: TimelineItem) {
        val lista = OrdenLineaDeTiempo.ordenar(porNino[childId].orEmpty() + item)
        porNino[childId] = lista
        if (_childIdActual.value == childId) _timeline.value = lista
    }

    override fun addChatMessage(
        childId: String,
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?,
        senderRole: UserRole?,
        date: String?
    ) {
        agregar(
            childId,
            TimelineItem.ChatMessage(
                id = "msg_${System.nanoTime()}",
                time = horaActual(),
                date = date ?: fechaActual(),
                message = text,
                isOutgoing = isOutgoing,
                fileUri = fileUri,
                fileName = fileName,
                fileMimeType = fileMimeType,
                senderRole = senderRole
            )
        )
    }

    override fun addEventChip(childId: String, time: String, text: String, iconType: String, date: String?) {
        agregar(
            childId,
            TimelineItem.EventChip(
                id = "chip_${System.nanoTime()}",
                time = time,
                date = date ?: fechaActual(),
                text = text,
                iconType = iconType
            )
        )
    }

    override fun addActivityCard(
        childId: String,
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        date: String?
    ) {
        agregar(
            childId,
            TimelineItem.ActivityCard(
                id = "act_${System.nanoTime()}",
                time = time,
                date = date ?: fechaActual(),
                category = category,
                portionLabel = portionLabel,
                description = description
            )
        )
    }

    private fun horaActual(): String = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    private fun fechaActual(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
}
