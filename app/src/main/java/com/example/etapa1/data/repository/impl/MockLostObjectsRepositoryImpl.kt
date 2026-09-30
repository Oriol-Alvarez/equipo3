package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockLostObjectsRepositoryImpl : LostObjectsRepository {
    private val _lostItems = MutableStateFlow<List<LostItem>>(MockDataRepository.getInitialLostItems())
    override val lostItems: StateFlow<List<LostItem>> = _lostItems.asStateFlow()

    private val _claimedItems = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val claimedItems: StateFlow<Map<String, Boolean>> = _claimedItems.asStateFlow()

    override fun publishLostItem(newItem: LostItem) {
        _lostItems.update { listOf(newItem) + it }
    }

    override fun claimLostItem(itemId: String) {
        _claimedItems.update { currentMap ->
            currentMap + (itemId to true)
        }
    }
}
