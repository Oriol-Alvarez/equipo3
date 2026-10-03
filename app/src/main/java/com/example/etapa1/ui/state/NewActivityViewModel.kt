package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository

class NewActivityViewModel(
    private val bitacoraRepository: BitacoraRepository
) : ViewModel() {

    fun saveActivity(time: String, category: String, portionLabel: String, description: String) {
        bitacoraRepository.addActivityCard(time, category, portionLabel, description)
    }
}
