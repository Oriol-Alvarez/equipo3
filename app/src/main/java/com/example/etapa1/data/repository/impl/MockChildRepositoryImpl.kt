package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Datos en memoria para el modo sin servidor (AppContainer.USE_MOCKS) y las pruebas. */
class MockChildRepositoryImpl : ChildRepository {
    private val _salaActual = MutableStateFlow<String?>(null)
    override val salaActual: StateFlow<String?> = _salaActual.asStateFlow()

    // Los datos de ejemplo solo tienen niños de la Sala 1A
    private val _childrenOfRoom = MutableStateFlow<List<Child>>(MockDataRepository.childrenSala1A)
    override val childrenOfRoom: StateFlow<List<Child>> = _childrenOfRoom.asStateFlow()

    private val _cambios = MutableStateFlow(0)
    override val cambios: StateFlow<Int> = _cambios.asStateFlow()

    override fun abrirSala(roomId: String) {
        _salaActual.value = roomId
    }

    override fun cargarDetalle(childId: String) {
        // En memoria todo está disponible de inmediato
    }

    private val _parentChildren = MutableStateFlow<List<Child>>(MockDataRepository.parentChildren)
    override val parentChildren: StateFlow<List<Child>> = _parentChildren.asStateFlow()

    override fun getChildById(childId: String): Child? {
        return _childrenOfRoom.value.find { it.id == childId } ?: _parentChildren.value.find { it.id == childId }
    }

    override fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String) {
        val updater: (List<Child>) -> List<Child> = { currentList ->
            currentList.map { child ->
                if (child.id == childId) {
                    child.copy(
                        isPresent = isPresent,
                        arrivalTime = if (isPresent) time else child.arrivalTime,
                        statusText = statusText
                    )
                } else {
                    child
                }
            }
        }
        _childrenOfRoom.update(updater)
        _parentChildren.update(updater)
    }

    override fun refresh() {
        // En modo mock no requiere recarga remota
    }

    private val _childProfiles = mutableMapOf<String, ChildFullProfile>()

    override fun getChildFullProfile(child: Child): ChildFullProfile {
        return _childProfiles.getOrPut(child.id) {
            MockDataRepository.getChildFullProfile(child)
        }
    }

    override fun updateChildFullProfile(childId: String, profile: ChildFullProfile) {
        _childProfiles[childId] = profile
        MockDataRepository.updateChildFullProfile(childId, profile)
    }

    private val _weeklySummaries = mutableMapOf<String, List<WeeklySummary>>()

    override fun getWeeklySummaryForChild(child: Child): WeeklySummary {
        return getWeeklySummariesForChild(child).firstOrNull()
            ?: MockDataRepository.getWeeklySummaryForChild(child)
    }

    override fun getWeeklySummariesForChild(child: Child): List<WeeklySummary> {
        return _weeklySummaries.getOrPut(child.id) {
            MockDataRepository.getWeeklySummariesForChild(child)
        }
    }

    override fun markWeeklySummaryReadStatus(
        childId: String,
        weekRangeText: String,
        isRead: Boolean,
        timestampText: String?
    ) {
        val currentList = _weeklySummaries[childId] ?: return
        val updatedList = currentList.map { summary ->
            if (summary.weekRangeText == weekRangeText) {
                summary.copy(
                    isReadByParent = isRead,
                    readTimestampText = if (isRead) (timestampText ?: "Hoy, 18:30") else null
                )
            } else {
                summary
            }
        }
        _weeklySummaries[childId] = updatedList
        _cambios.update { it + 1 }
    }

    override fun setWeeklySummaryReaction(
        childId: String,
        weekRangeText: String,
        reaction: String?
    ) {
        val currentList = _weeklySummaries[childId] ?: return
        val updatedList = currentList.map { summary ->
            if (summary.weekRangeText == weekRangeText) {
                summary.copy(
                    parentReaction = if (reaction.isNullOrBlank()) null else reaction
                )
            } else {
                summary
            }
        }
        _weeklySummaries[childId] = updatedList
        _cambios.update { it + 1 }
    }

    override fun getDailyBitacorasForChild(child: Child): List<DailyBitacora> {
        return MockDataRepository.getDailyBitacorasForChild(child)
    }
}
