package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockLostObjectsRepositoryImpl
import com.example.etapa1.model.LostItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LostObjectsRepositoryTest {

    private lateinit var repository: MockLostObjectsRepositoryImpl

    private val item1 = LostItem(
        id = "lost_test_1",
        title = "Chaqueta Azul",
        description = "Chaqueta de invierno",
        category = "Ropa",
        location = "Patio"
    )

    private val item2 = LostItem(
        id = "lost_test_2",
        title = "Peluche Oso",
        description = "Oso pequeño marrón",
        category = "Juguetes",
        location = "Sala 1A"
    )

    @Before
    fun setUp() {
        repository = MockLostObjectsRepositoryImpl(
            initialItems = listOf(item1, item2)
        )
    }

    @Test
    fun deleteLostItem_removesItemFromLostItems() {
        assertEquals(2, repository.lostItems.value.size)

        repository.deleteLostItem("lost_test_1")

        val currentItems = repository.lostItems.value
        assertEquals(1, currentItems.size)
        assertEquals("lost_test_2", currentItems.first().id)
        assertFalse(currentItems.any { it.id == "lost_test_1" })
    }

    @Test
    fun deleteLostItem_alsoRemovesClaimedStatusIfPresent() {
        repository.claimLostItem("lost_test_1")
        assertTrue(repository.claimedItems.value["lost_test_1"] == true)

        repository.deleteLostItem("lost_test_1")

        assertFalse(repository.claimedItems.value.containsKey("lost_test_1"))
        assertFalse(repository.lostItems.value.any { it.id == "lost_test_1" })
    }

    @Test
    fun deleteLostItem_nonExistentIdDoesNotAffectList() {
        repository.deleteLostItem("non_existent_id")

        assertEquals(2, repository.lostItems.value.size)
    }

    @Test
    fun publishAndThenDeleteLostItem() {
        val newItem = LostItem(
            id = "lost_new_99",
            title = "Gorra Verde",
            description = "Gorra de sol",
            category = "Ropa"
        )
        repository.publishLostItem(newItem)
        assertEquals(3, repository.lostItems.value.size)
        assertEquals("lost_new_99", repository.lostItems.value.first().id)

        repository.deleteLostItem("lost_new_99")
        assertEquals(2, repository.lostItems.value.size)
        assertFalse(repository.lostItems.value.any { it.id == "lost_new_99" })
    }
}
