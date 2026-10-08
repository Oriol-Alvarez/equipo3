package com.example.etapa1

import android.app.Application
import android.content.Context
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyPlanRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.data.repository.WeeklyObservationsRepository
import com.example.etapa1.data.repository.impl.MockAnnouncementRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAttendanceRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockDailyPlanRepositoryImpl
import com.example.etapa1.data.repository.impl.MockFamilyGroupsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockLostObjectsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.data.repository.impl.MockRoomRepositoryImpl
import com.example.etapa1.data.repository.impl.MockSuggestionsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockWeeklyObservationsRepositoryImpl


open class AppContainer(val context: Context) {
    val authRepository: AuthRepository by lazy { MockAuthRepositoryImpl() }
    val roomRepository: RoomRepository by lazy { MockRoomRepositoryImpl() }
    val childRepository: ChildRepository by lazy { MockChildRepositoryImpl() }
    val attendanceRepository: AttendanceRepository by lazy { MockAttendanceRepositoryImpl() }
    val bitacoraRepository: BitacoraRepository by lazy { MockBitacoraRepositoryImpl() }
    val lostObjectsRepository: LostObjectsRepository by lazy { MockLostObjectsRepositoryImpl() }
    val suggestionsRepository: SuggestionsRepository by lazy { MockSuggestionsRepositoryImpl() }
    val messagesRepository: MessagesRepository by lazy { MockMessagesRepositoryImpl() }
    val announcementRepository: AnnouncementRepository by lazy { MockAnnouncementRepositoryImpl() }
    val familyGroupsRepository: FamilyGroupsRepository by lazy { MockFamilyGroupsRepositoryImpl() }
    val weeklyObservationsRepository: WeeklyObservationsRepository by lazy { MockWeeklyObservationsRepositoryImpl() }
    val dailyPlanRepository: DailyPlanRepository by lazy { MockDailyPlanRepositoryImpl() }
}

class SonrisasApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
