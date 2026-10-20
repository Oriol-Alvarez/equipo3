package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyActivityInfo
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.DailyEgresoInfo
import com.example.etapa1.model.DailyIngresoInfo
import com.example.etapa1.model.PortionOption
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChildDetailViewModel(
    private val childRepository: ChildRepository,
    private val bitacoraRepository: BitacoraRepository? = null,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    fun getFullProfile(child: Child): ChildFullProfile {
        return childRepository.getChildFullProfile(child)
    }

    fun getWeeklySummaries(child: Child): List<WeeklySummary> {
        return childRepository.getWeeklySummariesForChild(child)
    }

    fun getDailyBitacoras(child: Child): List<DailyBitacora> {
        return getDailyBitacorasFlow(child).value
    }

    fun getDailyBitacorasFlow(child: Child): StateFlow<List<DailyBitacora>> {
        val timelineFlow = bitacoraRepository?.getTimelineForChild(child.id)
            ?: MutableStateFlow(emptyList())

        return timelineFlow.map { timelineItems ->
            val now = Calendar.getInstance()
            val todayDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)
            val todayDayLabel = SimpleDateFormat("'Hoy, 'd MMM", Locale("es", "ES")).format(now.time).replaceFirstChar { it.uppercase() }

            val baseBitacoras = childRepository.getDailyBitacorasForChild(child)
            val pastBitacoras = baseBitacoras.filter { !it.isToday && it.date != todayDateStr }
            val existingToday = baseBitacoras.find { it.isToday || it.date == todayDateStr }

            var todayIngreso = existingToday?.ingreso ?: if (child.isPresent) {
                DailyIngresoInfo(
                    time = child.arrivalTime ?: "08:15 AM",
                    delivererName = "Familiar",
                    receiverName = "Educadora de Sala",
                    physicalCondition = "Buen estado general, alegre y sin lesiones.",
                    hasSymptoms = false,
                    observations = "Ingreso normal a la sala."
                )
            } else null

            var todayEgreso: DailyEgresoInfo? = existingToday?.egreso

            val timelineActivities = mutableListOf<DailyActivityInfo>()

            for (item in timelineItems) {
                when (item) {
                    is TimelineItem.ActivityCard -> {
                        val cat = parseActivityCategory(item.category)
                        val portion = parsePortionOption(item.portionLabel)
                        timelineActivities.add(
                            DailyActivityInfo(
                                id = item.id,
                                time = item.time,
                                title = "${item.category}${if (item.portionLabel.isNotBlank()) " • ${item.portionLabel}" else ""}",
                                category = cat,
                                portion = portion,
                                description = item.description
                            )
                        )
                    }
                    is TimelineItem.ChatMessage -> {
                        if (item.message.startsWith("[ACTIVIDAD]")) {
                            val parsed = parseStructuredActivityMessage(item.id, item.time, item.message)
                            if (parsed != null) timelineActivities.add(parsed)
                        } else if (item.fileUri != null) {
                            timelineActivities.add(
                                DailyActivityInfo(
                                    id = item.id,
                                    time = item.time,
                                    title = "Foto compartida en chat",
                                    category = ActivityCategory.ESTADO_ANIMO,
                                    description = item.message.ifBlank { "Fotografía enviada durante la jornada." }
                                )
                            )
                        } else if (item.message.isNotBlank()) {
                            val senderLabel = if (item.senderRole == UserRole.EDUCADORA) "Educadora" else "Familia"
                            timelineActivities.add(
                                DailyActivityInfo(
                                    id = item.id,
                                    time = item.time,
                                    title = "Nota de chat • $senderLabel",
                                    category = ActivityCategory.OTROS,
                                    description = item.message
                                )
                            )
                        }
                    }
                    is TimelineItem.EventChip -> {
                        if (item.text.contains("ingreso", ignoreCase = true) || item.text.contains("llegada", ignoreCase = true)) {
                            todayIngreso = DailyIngresoInfo(
                                time = item.time,
                                delivererName = "Familiar",
                                receiverName = "Educadora de Sala",
                                physicalCondition = "Buen estado general",
                                hasSymptoms = false,
                                observations = item.text
                            )
                        } else if (item.text.contains("egreso", ignoreCase = true) || item.text.contains("salida", ignoreCase = true)) {
                            todayEgreso = DailyEgresoInfo(
                                time = item.time,
                                collectorName = "Familiar Tutor",
                                delivererName = "Educadora de Sala",
                                physicalCondition = "Buen estado general",
                                observations = item.text
                            )
                        } else {
                            timelineActivities.add(
                                DailyActivityInfo(
                                    id = item.id,
                                    time = item.time,
                                    title = "Registro de Evento",
                                    category = ActivityCategory.OTROS,
                                    description = item.text
                                )
                            )
                        }
                    }
                }
            }

            val baseActivities = existingToday?.activities ?: emptyList()
            val existingIds = baseActivities.map { it.id }.toSet()
            val combinedActivities = baseActivities + timelineActivities.filter { it.id !in existingIds }

            val todayBitacora = DailyBitacora(
                date = todayDateStr,
                dayLabel = todayDayLabel,
                isToday = true,
                ingreso = todayIngreso,
                egreso = todayEgreso,
                activities = combinedActivities
            )

            listOf(todayBitacora) + pastBitacoras
        }.stateIn(
            scope = coroutineScope ?: viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = run {
                val now = Calendar.getInstance()
                val todayDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)
                val todayDayLabel = SimpleDateFormat("'Hoy, 'd MMM", Locale("es", "ES")).format(now.time).replaceFirstChar { it.uppercase() }
                val base = childRepository.getDailyBitacorasForChild(child)
                val existing = base.find { it.isToday || it.date == todayDateStr }
                val past = base.filter { !it.isToday && it.date != todayDateStr }
                val today = existing?.copy(date = todayDateStr, dayLabel = todayDayLabel, isToday = true)
                    ?: DailyBitacora(
                        date = todayDateStr,
                        dayLabel = todayDayLabel,
                        isToday = true,
                        ingreso = if (child.isPresent) DailyIngresoInfo(child.arrivalTime ?: "08:15 AM", "Familiar", "Educadora", "Buen estado", false) else null,
                        egreso = null,
                        activities = emptyList()
                    )
                listOf(today) + past
            }
        )
    }

    private fun parseActivityCategory(raw: String): ActivityCategory {
        val clean = raw.trim().lowercase()
        return when {
            clean.contains("alimen") || clean.contains("comida") -> ActivityCategory.ALIMENTACION
            clean.contains("descan") || clean.contains("siesta") || clean.contains("sueño") -> ActivityCategory.DESCANSO
            clean.contains("excret") || clean.contains("esfínter") || clean.contains("esfinter") || clean.contains("pañal") -> ActivityCategory.FUNCIONES_EXCRETORAS
            clean.contains("ánimo") || clean.contains("animo") || clean.contains("emoci") -> ActivityCategory.ESTADO_ANIMO
            clean.contains("acciden") || clean.contains("golpe") -> ActivityCategory.ACCIDENTES
            clean.contains("salud") || clean.contains("médic") || clean.contains("medic") -> ActivityCategory.SALUD
            else -> ActivityCategory.OTROS
        }
    }

    private fun parsePortionOption(raw: String): PortionOption? {
        val clean = raw.trim().uppercase()
        return when {
            clean.contains("TODO") -> PortionOption.TODO
            clean.contains("MITAD") || clean.contains("MEDIO") -> PortionOption.MEDIO
            clean.contains("POCO") -> PortionOption.POCO
            else -> null
        }
    }

    private fun parseStructuredActivityMessage(id: String, time: String, raw: String): DailyActivityInfo? {
        val lines = raw.lines()
        val catLine = lines.find { it.startsWith("Categoría:", ignoreCase = true) }
            ?.substringAfter(":")?.trim() ?: "Actividad"
        val detLine = lines.find { it.startsWith("Detalle:", ignoreCase = true) }
            ?.substringAfter(":")?.trim() ?: ""
        val obsLine = lines.find { it.startsWith("Observaciones:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()
            ?: lines.filter {
                !it.startsWith("[ACTIVIDAD]") && !it.startsWith("Categoría:", ignoreCase = true) && !it.startsWith("Detalle:", ignoreCase = true)
            }.joinToString(" ").trim()

        return DailyActivityInfo(
            id = id,
            time = time,
            title = "$catLine${if (detLine.isNotBlank()) " • $detLine" else ""}",
            category = parseActivityCategory(catLine),
            portion = parsePortionOption(detLine),
            description = obsLine.ifBlank { "Actividad compartida en grupo." }
        )
    }
}
