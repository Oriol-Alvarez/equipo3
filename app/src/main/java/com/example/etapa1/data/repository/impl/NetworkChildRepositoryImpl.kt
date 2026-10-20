package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.ChildDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkChildRepositoryImpl(
    private val api: SonrisasApi
) : ChildRepository {

    private val _childrenSala1A = MutableStateFlow<List<Child>>(MockDataRepository.childrenSala1A)
    override val childrenSala1A: StateFlow<List<Child>> = _childrenSala1A.asStateFlow()

    private val _parentChildren = MutableStateFlow<List<Child>>(MockDataRepository.parentChildren)
    override val parentChildren: StateFlow<List<Child>> = _parentChildren.asStateFlow()

    private val _childProfiles = mutableMapOf<String, ChildFullProfile>()

    init {
        refresh()
    }

    override fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val salaRes = api.getChildrenByRoom("sala_1a")
                if (salaRes.isSuccessful && !salaRes.body().isNullOrEmpty()) {
                    _childrenSala1A.value = salaRes.body()!!.map { it.toDomain() }
                }
            } catch (_: Exception) {
            }

            try {
                val parentRes = api.getMyChildren()
                if (parentRes.isSuccessful && !parentRes.body().isNullOrEmpty()) {
                    _parentChildren.value = parentRes.body()!!.map { it.toDomain() }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun getChildById(childId: String): Child? {
        return _childrenSala1A.value.find { it.id == childId }
            ?: _parentChildren.value.find { it.id == childId }
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
        _childrenSala1A.update(updater)
        _parentChildren.update(updater)
    }

    override fun getChildFullProfile(child: Child): ChildFullProfile {
        return _childProfiles.getOrPut(child.id) {
            MockDataRepository.getChildFullProfile(child)
        }
    }

    override fun updateChildFullProfile(childId: String, profile: ChildFullProfile) {
        _childProfiles[childId] = profile
        MockDataRepository.updateChildFullProfile(childId, profile)
    }

    override fun getWeeklySummaryForChild(child: Child): WeeklySummary {
        return MockDataRepository.getWeeklySummaryForChild(child)
    }

    override fun getWeeklySummariesForChild(child: Child): List<WeeklySummary> {
        return MockDataRepository.getWeeklySummariesForChild(child)
    }

    override fun getDailyBitacorasForChild(child: Child): List<DailyBitacora> {
        return MockDataRepository.getDailyBitacorasForChild(child)
    }

    private fun ChildDto.toDomain(): Child {
        return Child(
            id = id,
            fullName = fullName,
            shortName = shortName,
            ageText = ageText,
            roomText = roomText,
            groupText = groupText,
            statusText = statusText,
            isPresent = isPresent,
            arrivalTime = arrivalTime,
            allergyAlert = allergyAlert,
            avatarInitials = avatarInitials,
            avatarBgColor = avatarBgColor,
            educatorName = educatorName,
            educatorRole = educatorRole,
            educatorAvatarInitials = educatorAvatarInitials,
            educatorAvatarBgColor = educatorAvatarBgColor
        )
    }
}
