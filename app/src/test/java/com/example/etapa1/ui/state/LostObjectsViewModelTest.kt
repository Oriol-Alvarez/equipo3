package com.example.etapa1.ui.state

import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.data.repository.impl.MockLostObjectsRepositoryImpl
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LostObjectsViewModelTest {

    private lateinit var authRepository: MockAuthRepositoryImpl
    private lateinit var lostObjectsRepository: MockLostObjectsRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val sampleItem1 = LostItem(
        id = "item_1",
        title = "Chaqueta Amarilla",
        description = "Chaqueta impermeable",
        category = "Ropa",
        location = "Patio"
    )

    private val sampleItem2 = LostItem(
        id = "item_2",
        title = "Peluche Conejo",
        description = "Conejo blanco",
        category = "Juguetes",
        location = "Sala 1A"
    )

    @Before
    fun setUp() {
        authRepository = MockAuthRepositoryImpl()
        lostObjectsRepository = MockLostObjectsRepositoryImpl(
            initialItems = listOf(sampleItem1, sampleItem2)
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun workerRole_hasWorkerFlagTrue() {
        authRepository.setRole(UserRole.EDUCADORA)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        assertTrue(latestState?.isWorkerRole == true)
        assertEquals(2, latestState?.items?.size)

        job.cancel()
    }

    @Test
    fun familiarRole_hasWorkerFlagFalse() {
        authRepository.setRole(UserRole.FAMILIAR)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        assertFalse(latestState?.isWorkerRole == true)

        job.cancel()
    }

    @Test
    fun deleteItem_removesFromUiStateAndFilteredItems() {
        authRepository.setRole(UserRole.EDUCADORA)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        assertEquals(2, latestState?.filteredItems?.size)

        // Eliminar un objeto (como trabajador cuando es reclamado)
        viewModel.deleteItem("item_1")

        assertEquals(1, latestState?.items?.size)
        assertEquals(1, latestState?.filteredItems?.size)
        assertEquals("item_2", latestState?.filteredItems?.first()?.id)
        assertFalse(latestState?.items?.any { it.id == "item_1" } == true)

        job.cancel()
    }

    @Test
    fun deleteClaimedItem_removesClaimedAndItemState() {
        authRepository.setRole(UserRole.EDUCADORA)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        viewModel.claimItem("item_1")
        assertTrue(latestState?.claimedItemsMap?.get("item_1") == true)

        // Se entrega en persona en la estancia y el trabajador lo elimina
        viewModel.deleteItem("item_1")

        assertFalse(latestState?.claimedItemsMap?.containsKey("item_1") == true)
        assertFalse(latestState?.items?.any { it.id == "item_1" } == true)

        job.cancel()
    }

    @Test
    fun claimItemWithChildName_updatesClaimedByNamesMap() {
        authRepository.setRole(UserRole.FAMILIAR)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        viewModel.claimItem("item_2", "Mateo García")
        assertTrue(latestState?.claimedItemsMap?.get("item_2") == true)
        assertEquals("Mateo García", latestState?.claimedByNamesMap?.get("item_2"))
        assertEquals("Mateo García", latestState?.items?.find { it.id == "item_2" }?.claimedByName)

        job.cancel()
    }

    @Test
    fun filterByCategoryAndSearch_worksCorrectlyWithDeletion() {
        authRepository.setRole(UserRole.EDUCADORA)
        val viewModel = LostObjectsViewModel(lostObjectsRepository, authRepository, testScope)

        var latestState: LostObjectsUiState? = null
        val job = viewModel.uiState.onEach { latestState = it }.launchIn(testScope)

        viewModel.selectCategory("Ropa")
        assertEquals(1, latestState?.filteredItems?.size)
        assertEquals("item_1", latestState?.filteredItems?.first()?.id)

        // Al eliminar item_1 que pertenecía a Ropa, filteredItems queda vacío
        viewModel.deleteItem("item_1")
        assertTrue(latestState?.filteredItems?.isEmpty() == true)

        // Cambiar a Juguetes muestra item_2
        viewModel.selectCategory("Juguetes")
        assertEquals(1, latestState?.filteredItems?.size)
        assertEquals("item_2", latestState?.filteredItems?.first()?.id)

        job.cancel()
    }
}
