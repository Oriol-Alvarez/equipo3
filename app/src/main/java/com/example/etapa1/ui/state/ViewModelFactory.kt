package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.etapa1.di.AppContainer

class AppViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(appContainer.authRepository) as T
            }
            modelClass.isAssignableFrom(LostObjectsViewModel::class.java) -> {
                LostObjectsViewModel(appContainer.lostObjectsRepository, appContainer.authRepository) as T
            }
            modelClass.isAssignableFrom(PublishLostObjectViewModel::class.java) -> {
                PublishLostObjectViewModel(appContainer.lostObjectsRepository) as T
            }
            modelClass.isAssignableFrom(SuggestionsViewModel::class.java) -> {
                SuggestionsViewModel(appContainer.suggestionsRepository) as T
            }
            modelClass.isAssignableFrom(MessagesViewModel::class.java) -> {
                MessagesViewModel(appContainer.messagesRepository) as T
            }
            modelClass.isAssignableFrom(AttendanceViewModel::class.java) -> {
                AttendanceViewModel(
                    appContainer.attendanceRepository,
                    appContainer.childRepository,
                    appContainer.bitacoraRepository
                ) as T
            }
            modelClass.isAssignableFrom(ChatBitacoraViewModel::class.java) -> {
                ChatBitacoraViewModel(appContainer.bitacoraRepository) as T
            }
            modelClass.isAssignableFrom(NewActivityViewModel::class.java) -> {
                NewActivityViewModel(appContainer.bitacoraRepository) as T
            }
            modelClass.isAssignableFrom(RoomSelectionViewModel::class.java) -> {
                RoomSelectionViewModel(appContainer.roomRepository) as T
            }
            modelClass.isAssignableFrom(RoomDashboardViewModel::class.java) -> {
                RoomDashboardViewModel(appContainer.childRepository) as T
            }
            modelClass.isAssignableFrom(ChildDetailViewModel::class.java) -> {
                ChildDetailViewModel(appContainer.childRepository) as T
            }
            modelClass.isAssignableFrom(ParentChildSelectionViewModel::class.java) -> {
                ParentChildSelectionViewModel(appContainer.childRepository) as T
            }
            modelClass.isAssignableFrom(ParentHomeViewModel::class.java) -> {
                ParentHomeViewModel(appContainer.childRepository) as T
            }
            modelClass.isAssignableFrom(ParentSuggestionsViewModel::class.java) -> {
                ParentSuggestionsViewModel(appContainer.suggestionsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
