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
                sonrisasApplication().container.bitacoraRepository
            )
        }
        initializer { ChatBitacoraViewModel(sonrisasApplication().container.bitacoraRepository) }
        initializer { NewActivityViewModel(sonrisasApplication().container.bitacoraRepository) }
        initializer {
            LostObjectsViewModel(
                sonrisasApplication().container.lostObjectsRepository,
                sonrisasApplication().container.authRepository
            )
        }
        initializer { PublishLostObjectViewModel(sonrisasApplication().container.lostObjectsRepository) }
        initializer { SuggestionsViewModel(sonrisasApplication().container.suggestionsRepository) }
        initializer { MessagesViewModel(sonrisasApplication().container.messagesRepository) }
        initializer { ParentChildSelectionViewModel(sonrisasApplication().container.childRepository) }
        initializer { ParentHomeViewModel(sonrisasApplication().container.childRepository) }
        initializer { ParentSuggestionsViewModel(sonrisasApplication().container.suggestionsRepository) }
        initializer { AnnouncementsViewModel(sonrisasApplication().container.announcementRepository) }
    }
}

private fun CreationExtras.sonrisasApplication(): SonrisasApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SonrisasApplication
