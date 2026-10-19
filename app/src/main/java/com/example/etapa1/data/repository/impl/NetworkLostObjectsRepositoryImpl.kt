package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.CreateLostObjectDto
import com.example.etapa1.data.remote.LostItemDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.SubidaArchivos
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.model.LostItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Catálogo de objetos perdidos del servidor. Publicar, reclamar y eliminar se guardan allá. */
class NetworkLostObjectsRepositoryImpl(
    private val api: SonrisasApi,
    private val subidaArchivos: SubidaArchivos? = null
) : LostObjectsRepository {

    private val _lostItems = MutableStateFlow<List<LostItem>>(emptyList())
    override val lostItems: StateFlow<List<LostItem>> = _lostItems.asStateFlow()

    private val _claimedItems = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val claimedItems: StateFlow<Map<String, Boolean>> = _claimedItems.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        RemoteSync.launch("Cargar objetos perdidos") {
            val lista = api.getLostObjects().bodyOrLog("GET /lost-objects")?.map { it.toDomain() } ?: return@launch
            // Conserva lo que se está publicando (id temporal)
            _lostItems.update { actual -> actual.filter { it.id.startsWith("temp_") } + lista }
            _claimedItems.value = lista.filter { it.isClaimed }.associate { it.id to true }
        }
    }

    override fun limpiar() {
        _lostItems.value = emptyList()
        _claimedItems.value = emptyMap()
    }

    override fun publishLostItem(newItem: LostItem) {
        val tempId = "temp_${System.nanoTime()}"
        val pendiente = newItem.copy(id = tempId)
        _lostItems.update { listOf(pendiente) + it }

        RemoteSync.launch("Publicar objeto perdido") {
            // La foto del teléfono se sube primero para que todos la puedan ver
            val foto = newItem.imageUri?.let { uri ->
                subidaArchivos?.subir(uri, "objeto-perdido.jpg", "image/jpeg", "lost-objects")
            }
            val creado = api.createLostObject(
                CreateLostObjectDto(
                    title = newItem.title,
                    description = newItem.description,
                    category = newItem.category,
                    location = newItem.location,
                    room = newItem.room,
                    imageUri = foto
                )
            ).bodyOrLog("POST /lost-objects")
            _lostItems.update { lista ->
                if (creado == null) {
                    lista.filterNot { it.id == tempId }
                } else {
                    lista.map { if (it.id == tempId) creado.toDomain() else it }
                }
            }
        }
    }

    override fun claimLostItem(itemId: String) {
        _claimedItems.update { it + (itemId to true) }
        RemoteSync.launch("Reclamar objeto perdido $itemId") {
            if (api.claimLostObject(itemId).bodyOrLog("PATCH /lost-objects/$itemId/claim") == null) {
                _claimedItems.update { it - itemId }
            }
        }
    }

    override fun deleteLostItem(itemId: String) {
        val antes = _lostItems.value
        _lostItems.update { lista -> lista.filterNot { it.id == itemId } }
        _claimedItems.update { it - itemId }
        if (itemId.startsWith("temp_")) return
        RemoteSync.launch("Eliminar objeto perdido $itemId") {
            val respuesta = api.deleteLostObject(itemId)
            if (!respuesta.isSuccessful && respuesta.code() != 404) {
                respuesta.bodyOrLog("DELETE /lost-objects/$itemId")
                _lostItems.value = antes
            }
        }
    }

    private fun LostItemDto.toDomain() = LostItem(
        id = id,
        title = title,
        description = description,
        category = category,
        location = location,
        imageUri = imageUri?.let(RemoteSync::absoluteUrl),
        room = room,
        isClaimed = isClaimed
    )
}
