package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.WeeklySummary

class ChildDetailViewModel(
    private val childRepository: ChildRepository
) : ViewModel() {
    fun getFullProfile(child: Child): ChildFullProfile {
        return childRepository.getChildFullProfile(child)
    }

    fun getWeeklySummary(child: Child): WeeklySummary {
        return childRepository.getWeeklySummaryForChild(child)
    }
}
