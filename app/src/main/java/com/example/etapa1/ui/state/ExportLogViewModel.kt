package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.export.ExportedFile
import com.example.etapa1.data.export.LogSheetExporter
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.domain.LogSheetFormatter
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.Child
import com.example.etapa1.model.DailyLogSampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportLogUiState(
    val child: Child? = null,
    val weeks: List<ExportWeekOption> = emptyList(),
    val selectedWeek: Int = 0,
    val days: List<ExportDayStatus> = emptyList(),
    val isGenerating: Boolean = false,
    val exported: ExportedFile? = null,
    val canSaveToDownloads: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
) {
    val daysWithRecords: Int get() = days.count { it.hasAnyRecord }
}

private data class ExportProgress(
    val isGenerating: Boolean = false,
    val exported: ExportedFile? = null,
    val message: String? = null,
    val error: String? = null
)

class ExportLogViewModel(
    private val dailyLogRepository: DailyLogRepository,
    private val roomRepository: RoomRepository,
    private val exporter: LogSheetExporter,
    private val today: () -> SimpleDate = { SimpleDate.today() },
    private val now: () -> String = { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date()) }
) : ViewModel() {

    private val child = MutableStateFlow<Child?>(null)
    private val selectedWeek = MutableStateFlow(0)
    private val progress = MutableStateFlow(ExportProgress())
    private val weeks = ExportLogPresenter.weekOptions(today())

    init {
        // Con backend trae los datos más recientes del servidor; en modo demo no hace nada.
        dailyLogRepository.refresh()
    }

    val uiState: StateFlow<ExportLogUiState> = combine(
        dailyLogRepository.logs,
        child,
        selectedWeek,
        progress
    ) { _, currentChild, weekIndex, state ->
        val week = weeks[weekIndex]
        ExportLogUiState(
            child = currentChild,
            weeks = weeks,
            selectedWeek = weekIndex,
            days = currentChild?.let {
                ExportLogPresenter.dayStatuses(week.days, dailyLogRepository.logsFor(it.id, week.days), today())
            } ?: emptyList(),
            isGenerating = state.isGenerating,
            exported = state.exported,
            canSaveToDownloads = exporter.canSaveToDownloads,
            message = state.message,
            errorMessage = state.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExportLogUiState(weeks = weeks, canSaveToDownloads = exporter.canSaveToDownloads)
    )

    fun setChild(value: Child) {
        if (child.value?.id != value.id) progress.value = ExportProgress()
        child.value = value
    }

    fun selectWeek(index: Int) {
        if (index !in weeks.indices || progress.value.isGenerating) return
        selectedWeek.value = index
        // El PDF anterior era de otra semana.
        progress.value = ExportProgress()
    }

    fun generate() {
        val currentChild = child.value ?: return
        if (progress.value.isGenerating) return
        val week = weeks[selectedWeek.value]
        val logs = dailyLogRepository.logsFor(currentChild.id, week.days)
        if (logs.all { it == null }) {
            progress.value = ExportProgress(error = "No hay registros de ${currentChild.shortName} en esa semana.")
            return
        }
        val level = roomRepository.rooms.value.find { it.name == currentChild.roomText }?.level ?: currentChild.groupText
        val sheet = LogSheetFormatter.build(
            child = currentChild,
            levelOrGroup = level,
            estancia = DailyLogSampleData.ESTANCIA_NAME,
            week = week.days,
            logs = logs
        )
        progress.value = ExportProgress(isGenerating = true)
        viewModelScope.launch {
            exporter.exportPdf(sheet, ExportLogPresenter.fileName(currentChild, week.days.first()), now())
                .onSuccess { file ->
                    progress.value = ExportProgress(exported = file, message = "PDF listo: ${file.fileName}")
                }
                .onFailure {
                    progress.value = ExportProgress(error = "No se pudo generar el PDF: ${it.message ?: "error desconocido"}")
                }
        }
    }

    fun saveToDownloads() {
        val file = progress.value.exported ?: return
        viewModelScope.launch {
            exporter.saveToDownloads(file)
                .onSuccess { progress.value = progress.value.copy(message = it, error = null) }
                .onFailure { progress.value = progress.value.copy(error = it.message, message = null) }
        }
    }

    /** Para avisos que vienen de la pantalla (p. ej. no hay app para abrir PDF). */
    fun showError(message: String) {
        progress.value = progress.value.copy(error = message, message = null)
    }
}
