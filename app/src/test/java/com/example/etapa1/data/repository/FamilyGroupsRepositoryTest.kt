package com.example.etapa1.data.repository

import com.example.etapa1.data.repository.impl.MockFamilyGroupsRepositoryImpl
import com.example.etapa1.model.FamilyGroupsSampleData
import com.example.etapa1.model.MessageAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FamilyGroupsRepositoryTest {

    private lateinit var repository: MockFamilyGroupsRepositoryImpl
    private val educadora = FamilyGroupsSampleData.EDUCADORA
    private val familiaGarcia = FamilyGroupsSampleData.FAMILIA_GARCIA

    @Before
    fun setUp() {
        repository = MockFamilyGroupsRepositoryImpl(
            initialGroups = emptyList(),
            currentTime = { "10:00 AM" }
        )
    }

    @Test
    fun createGroup_withChosenFamilies_addsGroupAtTopWithOnlyThoseFamilies() {
        val result = repository.createGroup(
            title = "  Sofía y Mateo  ",
            memberChildIds = listOf("sofia_lopez", "mateo_garcia", "sofia_lopez"),
            creator = educadora
        )

        assertTrue(result.isSuccess)
        val group = repository.groups.value.first()
        assertEquals("Sofía y Mateo", group.title)
        assertEquals(listOf("sofia_lopez", "mateo_garcia"), group.memberChildIds)
        assertEquals(educadora.id, group.createdById)
        assertEquals(1, group.messages.size)
        assertEquals("10:00 AM", group.messages.first().time)
    }

    @Test
    fun createGroup_withoutName_failsAndDoesNotAddGroup() {
        val result = repository.createGroup("   ", listOf("sofia_lopez"), educadora)

        assertTrue(result.isFailure)
        assertEquals("Escribe un nombre para el grupo.", result.exceptionOrNull()?.message)
        assertTrue(repository.groups.value.isEmpty())
    }

    @Test
    fun createGroup_withoutFamilies_fails() {
        val result = repository.createGroup("Paseo", emptyList(), educadora)

        assertTrue(result.isFailure)
        assertEquals("Elige al menos una familia para el grupo.", result.exceptionOrNull()?.message)
        assertTrue(repository.groups.value.isEmpty())
    }

    @Test
    fun createGroup_withTooLongName_fails() {
        val longName = "a".repeat(MockFamilyGroupsRepositoryImpl.MAX_TITLE_LENGTH + 1)

        val result = repository.createGroup(longName, listOf("sofia_lopez"), educadora)

        assertTrue(result.isFailure)
        assertTrue(repository.groups.value.isEmpty())
    }

    @Test
    fun sendMessage_fromFamily_isStoredWithSenderAndAttachment() {
        val group = repository.createGroup("Paseo", listOf("mateo_garcia"), educadora).getOrThrow()
        val photo = MessageAttachment("content://foto/1", "foto.jpg", "image/jpeg")

        val sent = repository.sendMessage(group.id, familiaGarcia, "  Aquí la autorización  ", photo)

        assertTrue(sent)
        val last = repository.groups.value.first().messages.last()
        assertEquals(familiaGarcia.id, last.senderId)
        assertEquals(familiaGarcia.displayName, last.senderName)
        assertEquals("Aquí la autorización", last.text)
        assertEquals(photo, last.attachment)
    }

    @Test
    fun sendMessage_onlyAttachment_isAllowed() {
        val group = repository.createGroup("Paseo", listOf("mateo_garcia"), educadora).getOrThrow()
        val file = MessageAttachment("content://doc/1", "permiso.pdf", "application/pdf")

        assertTrue(repository.sendMessage(group.id, educadora, "", file))
        assertEquals(2, repository.groups.value.first().messages.size)
    }

    @Test
    fun sendMessage_emptyOrUnknownGroup_isRejected() {
        val group = repository.createGroup("Paseo", listOf("mateo_garcia"), educadora).getOrThrow()

        assertFalse(repository.sendMessage(group.id, educadora, "   ", null))
        assertFalse(repository.sendMessage("no_existe", educadora, "Hola", null))
        assertEquals(1, repository.groups.value.first().messages.size)
    }

    @Test
    fun sendMessage_movesGroupToTop() {
        val first = repository.createGroup("Primero", listOf("mateo_garcia"), educadora).getOrThrow()
        repository.createGroup("Segundo", listOf("sofia_lopez"), educadora)
        assertEquals("Segundo", repository.groups.value.first().title)

        repository.sendMessage(first.id, educadora, "Hola", null)

        assertEquals(listOf("Primero", "Segundo"), repository.groups.value.map { it.title })
    }

    @Test
    fun sampleData_includesExampleGroupWithMateo() {
        val sampleRepository = MockFamilyGroupsRepositoryImpl()

        val sample = sampleRepository.groups.value.single()
        assertTrue("mateo_garcia" in sample.memberChildIds)
        assertTrue(sample.messages.isNotEmpty())
    }
}
