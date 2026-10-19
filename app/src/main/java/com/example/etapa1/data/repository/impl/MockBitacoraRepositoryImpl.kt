package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockBitacoraRepositoryImpl : BitacoraRepository {
    private val _mateoTimeline = MutableStateFlow<List<TimelineItem>>(MockDataRepository.getInitialMateoTimeline())
    override val mateoTimeline: StateFlow<List<TimelineItem>> = _mateoTimeline.asStateFlow()

    override fun recargar(childId: String) {
        // En modo mock conserva el estado acumulado en memoria
    }

    override fun addChatMessage(
        text: String,
        isOutgoing: Boolean,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?,
        senderRole: UserRole?
    ) {
        val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date())
        val message = TimelineItem.ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            time = currentTime,
            message = text,
            isOutgoing = isOutgoing,
            fileUri = fileUri,
            fileName = fileName,
            fileMimeType = fileMimeType,
            senderRole = senderRole
        )
        _mateoTimeline.update { (it + message).ordenadaCronologicamente() }
    }

    override fun addEventChip(time: String, text: String, iconType: String) {
        val chip = TimelineItem.EventChip(
            id = "chip_${System.currentTimeMillis()}",
            time = time,
            text = text,
            iconType = iconType
        )
        _mateoTimeline.update { (it + chip).ordenadaCronologicamente() }
    }

    override fun addActivityCard(time: String, category: String, portionLabel: String, description: String, childId: String) {
        val card = TimelineItem.ActivityCard(
            id = "act_${System.currentTimeMillis()}",
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )
        _mateoTimeline.update { (it + card).ordenadaCronologicamente() }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim().uppercase(java.util.Locale.US)
            .replace(".", "")
            .replace("A M", "AM")
            .replace("P M", "PM")
        val formats = listOf(
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US),
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.US),
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.US),
            java.text.SimpleDateFormat("H:mm", java.util.Locale.US)
        )
        for (sdf in formats) {
            try {
                val date = sdf.parse(clean)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = date }
                    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                }
            } catch (_: Exception) {
            }
        }
        return 9999
    }

    private fun List<TimelineItem>.ordenadaCronologicamente(): List<TimelineItem> =
        this.sortedBy { parseTimeToMinutes(it.time) }
}
