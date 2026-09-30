package com.example.etapa1

import com.example.etapa1.data.repository.impl.MockAttendanceRepositoryImpl
import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.ui.state.AttendanceViewModel
import com.example.etapa1.ui.state.ChatBitacoraViewModel
import com.example.etapa1.ui.state.NewActivityViewModel
import org.junit.Assert.*
import org.junit.Test

class ChildTimelineRepositoryTest {
    private val mateoId = "mateo_garcia"
    private val sofiaId = "sofia_lopez"
    private val lucasId = "lucas_martinez"

    @Test fun onlyMateoStartsWithHisDemoHistory() {
        val repository = MockBitacoraRepositoryImpl()
        assertEquals(6, repository.timelineForChild(mateoId).value.size)
        assertTrue(repository.timelineForChild(sofiaId).value.isEmpty())
        assertTrue(repository.timelineForChild(lucasId).value.isEmpty())
    }

    @Test fun messagesAndAttachmentsStayWithTheAddressedChild() {
        val repository = MockBitacoraRepositoryImpl()
        val chat = ChatBitacoraViewModel(repository)
        val mateoBefore = repository.timelineForChild(mateoId).value
        val sofiaTimeline = chat.timelineForChild(sofiaId)
        chat.sendMessage(sofiaId, "  Hola Sofia  ")
        chat.sendMessage(lucasId, "", "content://demo/foto", "foto.jpg", "image/jpeg")

        val sofia = sofiaTimeline.value.single() as TimelineItem.ChatMessage
        assertEquals("Hola Sofia", sofia.message)
        assertNull(sofia.fileUri)
        val lucas = repository.timelineForChild(lucasId).value.single() as TimelineItem.ChatMessage
        assertEquals("content://demo/foto", lucas.fileUri)
        assertEquals("foto.jpg", lucas.fileName)
        assertEquals("image/jpeg", lucas.fileMimeType)
        assertEquals(mateoBefore, repository.timelineForChild(mateoId).value)
        assertEquals(sofiaTimeline.value, ChatBitacoraViewModel(repository).timelineForChild(sofiaId).value)
    }

    @Test fun blankMessagesDoNotCreateEntries() {
        val repository = MockBitacoraRepositoryImpl()
        ChatBitacoraViewModel(repository).sendMessage(sofiaId, "  \n  ")
        assertTrue(repository.timelineForChild(sofiaId).value.isEmpty())
    }

    @Test fun activitiesAreRecordedOnlyForTheSelectedChild() {
        val repository = MockBitacoraRepositoryImpl()
        val viewModel = NewActivityViewModel(repository)
        val mateoBefore = repository.timelineForChild(mateoId).value
        viewModel.saveActivity(sofiaId, "10:00 AM", "Descanso", "30 min", "Descanso de Sofia")
        viewModel.saveActivity(lucasId, "11:00 AM", "Alimentación", "Todo", "Comida de Lucas")
        val sofia = repository.timelineForChild(sofiaId).value.single() as TimelineItem.ActivityCard
        assertEquals("Descanso de Sofia", sofia.description)
        assertEquals("30 min", sofia.portionLabel)
        assertEquals("10:00 AM", sofia.time)
        val lucas = repository.timelineForChild(lucasId).value.single() as TimelineItem.ActivityCard
        assertEquals("Comida de Lucas", lucas.description)
        assertEquals(mateoBefore, repository.timelineForChild(mateoId).value)
    }

    @Test fun attendanceForAnotherChildUpdatesThatChildAndTheirTimeline() {
        val repository = MockBitacoraRepositoryImpl()
        val children = MockChildRepositoryImpl()
        val attendance = MockAttendanceRepositoryImpl()
        val viewModel = AttendanceViewModel(attendance, children, repository)
        val sofia = MockDataRepository.childrenSala1A.first { it.id == sofiaId }
        val mateoBefore = repository.timelineForChild(mateoId).value
        viewModel.saveAttendance(sofia, true, "08:30 AM", "Llega con mamá")
        viewModel.saveAttendance(sofia, false, "02:00 PM", "Sale con papá")

        val events = repository.timelineForChild(sofiaId).value.map { it as TimelineItem.EventChip }
        assertEquals(listOf("Ingreso: 08:30 AM", "Egreso: 02:00 PM"), events.map { it.text })
        assertEquals(mateoBefore, repository.timelineForChild(mateoId).value)
        assertTrue(repository.timelineForChild(lucasId).value.isEmpty())
        assertFalse(children.getChildById(sofiaId)!!.isPresent)
        assertEquals(2, attendance.attendanceRecords.value.size)
        assertTrue(attendance.attendanceRecords.value.all { it.childId == sofiaId })
    }

    @Test fun rapidWritesHaveDistinctKeysAndLoseNoRecords() {
        val repository = MockBitacoraRepositoryImpl()
        repeat(100) { repository.addChatMessage(sofiaId, "Mensaje $it") }
        val items = repository.timelineForChild(sofiaId).value
        assertEquals(100, items.size)
        assertEquals(100, items.map { it.id }.distinct().size)
    }

    @Test fun aNewDemoSessionStartsWithoutPreviousChildWrites() {
        val firstSession = MockBitacoraRepositoryImpl()
        firstSession.addChatMessage(sofiaId, "Solo esta sesión")
        assertTrue(MockBitacoraRepositoryImpl().timelineForChild(sofiaId).value.isEmpty())
    }
}
