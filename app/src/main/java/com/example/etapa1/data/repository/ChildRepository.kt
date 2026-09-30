package com.example.etapa1.data.repository

import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.StateFlow

interface ChildRepository {
    val childrenSala1A: StateFlow<List<Child>>
    val parentChildren: StateFlow<List<Child>>
    fun getChildById(childId: String): Child?
    fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String)
    fun getChildFullProfile(child: Child): ChildFullProfile
    fun getWeeklySummaryForChild(child: Child): WeeklySummary
}
