package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkLostObjectsRepositoryImpl(
    private val api: SonrisasApi,
    initialItems: List<LostItem> = MockDataRepository.getInitialLostItems()
) : LostObjectsRepository {

    private val _lostItems = MutableStateFlow<List<LostItem>>(initialItems)
    override val lostItems: StateFlow<List<LostItem>> = _lostItems.asStateFlow()

    private val _claimedItems = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val claimedItems: StateFlow<Map<String, Boolean>> = _claimedItems.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = api.getLostObjects()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val list = res.body()!!.map { dto ->
                        LostItem(
                            id = dto.id,
                            title = dto.title,
                            description = dto.description,
                            category = dto.category,
                            location = dto.location,
                            imageUri = dto.imageUri,
                            room = dto.room,
                            isClaimed = dto.isClaimed
                        )
                    }
                    _lostItems.value = list
                    _claimedItems.value = list.filter { it.isClaimed }.associate { it.id to true }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun publishLostItem(newItem: LostItem) {
        _lostItems.update { listOf(newItem) + it }
    }

    override fun claimLostItem(itemId: String) {
        _claimedItems.update { currentMap ->
            currentMap + (itemId to true)
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.claimLostObject(itemId)
            } catch (_: Exception) {
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
    }
}
