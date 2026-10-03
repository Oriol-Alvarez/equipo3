package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem

class PublishLostObjectViewModel(
    private val lostObjectsRepository: LostObjectsRepository
) : ViewModel() {

    fun publishItem(item: LostItem) {
        lostObjectsRepository.publishLostItem(item)
    }
}
