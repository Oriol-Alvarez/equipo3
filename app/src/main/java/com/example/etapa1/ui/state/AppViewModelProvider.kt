package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.etapa1.SonrisasApplication


object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { SesionViewModel(sonrisasApplication().container.authRepository) }
        initializer { LoginViewModel(sonrisasApplication().container.authRepository) }
        initializer { RoomSelectionViewModel(sonrisasApplication().container.roomRepository) }
        initializer { RoomDashboardViewModel(sonrisasApplication().container.childRepository) }
        initializer { ChildDetailViewModel(sonrisasApplication().container.childRepository) }
        initializer {
            AttendanceViewModel(
                sonrisasApplication().container.attendanceRepository,
                sonrisasApplication().container.childRepository,
                sonrisasApplication().container.bitacoraRepository,
                sonrisasApplication().container.dailyLogRepository
            )
        }
        initializer {
            ChatBitacoraViewModel(
                sonrisasApplication().container.bitacoraRepository,
                sonrisasApplication().container.chatConexion
            )
        }
        initializer {
            NewActivityViewModel(
                sonrisasApplication().container.bitacoraRepository,
                sonrisasApplication().container.familyGroupsRepository
            )
        }
        initializer {
            LostObjectsViewModel(
                sonrisasApplication().container.lostObjectsRepository,
                sonrisasApplication().container.authRepository
            )
        }
        initializer { PublishLostObjectViewModel(sonrisasApplication().container.lostObjectsRepository) }
        initializer { SuggestionsViewModel(sonrisasApplication().container.suggestionsRepository) }
        initializer {
            MessagesViewModel(
                sonrisasApplication().container.messagesRepository,
                sonrisasApplication().container.chatConexion
            )
        }
        initializer { ParentChildSelectionViewModel(sonrisasApplication().container.childRepository) }
        initializer { ParentHomeViewModel(sonrisasApplication().container.childRepository) }
        initializer { ParentSuggestionsViewModel(sonrisasApplication().container.suggestionsRepository) }
        initializer { AnnouncementsViewModel(sonrisasApplication().container.announcementRepository) }
        initializer {
            FamilyGroupsViewModel(
                sonrisasApplication().container.familyGroupsRepository,
                sonrisasApplication().container.messagesRepository,
                sonrisasApplication().container.childRepository
            )
        }
        initializer { WeeklyObservationsViewModel(sonrisasApplication().container.weeklyObservationsRepository) }
        initializer { BirthdaysViewModel(sonrisasApplication().container.childRepository) }
        initializer { DailyPlanViewModel(sonrisasApplication().container.dailyPlanRepository) }
        initializer {
            ExportLogViewModel(
                sonrisasApplication().container.dailyLogRepository,
                sonrisasApplication().container.roomRepository,
                sonrisasApplication().container.logSheetExporter
            )
        }
    }
}

private fun CreationExtras.sonrisasApplication(): SonrisasApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SonrisasApplication
