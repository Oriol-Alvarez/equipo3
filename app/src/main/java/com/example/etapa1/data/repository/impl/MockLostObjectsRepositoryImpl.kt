package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockLostObjectsRepositoryImpl(
    initialItems: List<LostItem> = MockDataRepository.getInitialLostItems()
) : LostObjectsRepository {
    private val _lostItems = MutableStateFlow<List<LostItem>>(initialItems)
    override val lostItems: StateFlow<List<LostItem>> = _lostItems.asStateFlow()

    private val _claimedItems = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val claimedItems: StateFlow<Map<String, Boolean>> = _claimedItems.asStateFlow()

    private val _claimedByNames = MutableStateFlow<Map<String, String>>(emptyMap())
    override val claimedByNames: StateFlow<Map<String, String>> = _claimedByNames.asStateFlow()

    override fun publishLostItem(newItem: LostItem) {
        _lostItems.update { listOf(newItem) + it }
    }

    override fun claimLostItem(itemId: String, childName: String?) {
        _claimedItems.update { currentMap ->
            currentMap + (itemId to true)
        }
        if (!childName.isNullOrBlank()) {
            _claimedByNames.update { currentMap ->
                currentMap + (itemId to childName)
            }
        }
        _lostItems.update { items ->
            items.map {
                if (it.id == itemId) it.copy(isClaimed = true, claimedByName = childName ?: it.claimedByName)
                else it
            }
        }
    }

    override fun deleteLostItem(itemId: String) {
        _lostItems.update { currentList ->
            currentList.filterNot { it.id == itemId }
        }
        _claimedItems.update { currentMap ->
            currentMap - itemId
        }
        _claimedByNames.update { currentMap ->
            currentMap - itemId
        }
    }
}
