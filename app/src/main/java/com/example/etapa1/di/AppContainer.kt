package com.example.etapa1.di

import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.data.repository.impl.MockAttendanceRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockLostObjectsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.data.repository.impl.MockRoomRepositoryImpl
import com.example.etapa1.data.repository.impl.MockSuggestionsRepositoryImpl

interface AppContainer {
    val authRepository: AuthRepository
    val roomRepository: RoomRepository
    val childRepository: ChildRepository
    val attendanceRepository: AttendanceRepository
    val bitacoraRepository: BitacoraRepository
    val lostObjectsRepository: LostObjectsRepository
    val suggestionsRepository: SuggestionsRepository
    val messagesRepository: MessagesRepository
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy { MockAuthRepositoryImpl() }
    override val roomRepository: RoomRepository by lazy { MockRoomRepositoryImpl() }
    override val childRepository: ChildRepository by lazy { MockChildRepositoryImpl() }
    override val attendanceRepository: AttendanceRepository by lazy { MockAttendanceRepositoryImpl() }
    override val bitacoraRepository: BitacoraRepository by lazy { MockBitacoraRepositoryImpl() }
    override val lostObjectsRepository: LostObjectsRepository by lazy { MockLostObjectsRepositoryImpl() }
    override val suggestionsRepository: SuggestionsRepository by lazy { MockSuggestionsRepositoryImpl() }
    override val messagesRepository: MessagesRepository by lazy { MockMessagesRepositoryImpl() }
}
