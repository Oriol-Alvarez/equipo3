package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockChildRepositoryImpl : ChildRepository {
    private val _childrenSala1A = MutableStateFlow<List<Child>>(MockDataRepository.childrenSala1A)
    override val childrenSala1A: StateFlow<List<Child>> = _childrenSala1A.asStateFlow()

    override fun getChildById(childId: String): Child? {
        return _childrenSala1A.value.find { it.id == childId }
    }

    override fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String) {
        _childrenSala1A.update { currentList ->
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
    }

    override fun getChildFullProfile(child: Child): ChildFullProfile {
        return MockDataRepository.getChildFullProfile(child)
    }

    override fun getWeeklySummaryForChild(child: Child): WeeklySummary {
        return MockDataRepository.getWeeklySummaryForChild(child)
    }
}
