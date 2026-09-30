package com.example.etapa1.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.etapa1.ui.components.AppBottomBar
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.Room
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.etapa1.SonrisasApplication
import com.example.etapa1.di.AppContainer
import com.example.etapa1.di.DefaultAppContainer
import com.example.etapa1.ui.state.AppViewModelFactory
import com.example.etapa1.ui.state.AttendanceViewModel
import com.example.etapa1.ui.state.ChatBitacoraViewModel
import com.example.etapa1.ui.state.ChildDetailViewModel
import com.example.etapa1.ui.state.LoginViewModel
import com.example.etapa1.ui.state.LostObjectsViewModel
import com.example.etapa1.ui.state.MessagesViewModel
import com.example.etapa1.ui.state.NewActivityViewModel
import com.example.etapa1.ui.state.PublishLostObjectViewModel
import com.example.etapa1.ui.state.RoomDashboardViewModel
import com.example.etapa1.ui.state.RoomSelectionViewModel
import com.example.etapa1.ui.state.SuggestionsViewModel
import com.example.etapa1.ui.screens.AttendanceScreen
import com.example.etapa1.ui.screens.ChatBitacoraScreen
import com.example.etapa1.ui.screens.ChildDetailScreen
import com.example.etapa1.ui.screens.LoginScreen
import com.example.etapa1.ui.screens.LostObjectsCatalogScreen
import com.example.etapa1.ui.screens.MessagesScreen
import com.example.etapa1.ui.screens.MoreMenuScreen
import com.example.etapa1.ui.screens.NewActivityScreen
import com.example.etapa1.ui.screens.PublishLostObjectScreen
import com.example.etapa1.ui.screens.RoomDashboardScreen
import com.example.etapa1.ui.screens.RoomSelectionScreen
import com.example.etapa1.ui.screens.SuggestionsScreen
import com.example.etapa1.ui.theme.AppBackground

sealed interface Screen {
    data object Login : Screen
    data object RoomSelection : Screen
    data class RoomDashboard(val room: Room) : Screen
    data class ChatBitacora(val child: Child) : Screen
    data class ChildDetail(val child: Child) : Screen
    data class NewActivity(val child: Child) : Screen
    data object Messages : Screen
    data object Suggestions : Screen
    data object MoreMenu : Screen
    data object LostObjectsCatalog : Screen
    data class PublishLostObject(val roomName: String = "Sala 1A") : Screen
    data class Attendance(val room: Room, val initialChild: Child? = null) : Screen
}

@Composable
fun SonrisasApp() {
    val context = LocalContext.current
    val app = context.applicationContext as? SonrisasApplication
    val container: AppContainer = app?.container ?: remember { DefaultAppContainer() }
    val viewModelFactory = remember(container) { AppViewModelFactory(container) }

    val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
    val roomSelectionViewModel: RoomSelectionViewModel = viewModel(factory = viewModelFactory)
    val roomDashboardViewModel: RoomDashboardViewModel = viewModel(factory = viewModelFactory)
    val childDetailViewModel: ChildDetailViewModel = viewModel(factory = viewModelFactory)
    val attendanceViewModel: AttendanceViewModel = viewModel(factory = viewModelFactory)
    val chatBitacoraViewModel: ChatBitacoraViewModel = viewModel(factory = viewModelFactory)
    val newActivityViewModel: NewActivityViewModel = viewModel(factory = viewModelFactory)
    val lostObjectsViewModel: LostObjectsViewModel = viewModel(factory = viewModelFactory)
    val publishLostObjectViewModel: PublishLostObjectViewModel = viewModel(factory = viewModelFactory)
    val suggestionsViewModel: SuggestionsViewModel = viewModel(factory = viewModelFactory)
    val messagesViewModel: MessagesViewModel = viewModel(factory = viewModelFactory)

    val backStack = remember { mutableStateListOf<Screen>(Screen.Login) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Login

    // Manejo de retroceso con botón físico o gesto de Android
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    val showBottomBar = currentScreen is Screen.RoomSelection ||
            currentScreen is Screen.RoomDashboard ||
            currentScreen is Screen.Messages ||
            currentScreen is Screen.Suggestions ||
            currentScreen is Screen.LostObjectsCatalog ||
            currentScreen is Screen.MoreMenu

    val selectedBottomTab = when (currentScreen) {
        is Screen.RoomSelection, is Screen.RoomDashboard -> 0
        is Screen.Messages -> 1
        is Screen.Suggestions -> 2
        is Screen.LostObjectsCatalog -> 3
        is Screen.MoreMenu -> 4
        else -> 0
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            0 -> {
                                if (currentScreen !is Screen.RoomSelection && currentScreen !is Screen.RoomDashboard) {
                                    val lastHomeIndex = backStack.indexOfLast { it is Screen.RoomDashboard || it is Screen.RoomSelection }
                                    if (lastHomeIndex >= 0) {
                                        while (backStack.size > lastHomeIndex + 1) {
                                            backStack.removeAt(backStack.lastIndex)
                                        }
                                    } else {
                                        backStack.add(Screen.RoomSelection)
                                    }
                                }
                            }
                            1 -> {
                                if (currentScreen !is Screen.Messages) {
                                    backStack.add(Screen.Messages)
                                }
                            }
                            2 -> {
                                if (currentScreen !is Screen.Suggestions) {
                                    backStack.add(Screen.Suggestions)
                                }
                            }
                            3 -> {
                                if (currentScreen !is Screen.LostObjectsCatalog) {
                                    backStack.add(Screen.LostObjectsCatalog)
                                }
                            }
                            4 -> {
                                if (currentScreen !is Screen.MoreMenu) {
                                    backStack.add(Screen.MoreMenu)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "screen_transition"
            ) { screen ->
            when (screen) {
                is Screen.Login -> {
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { role ->
                            backStack.add(Screen.RoomSelection)
                        }
                    )
                }

                is Screen.RoomSelection -> {
                    RoomSelectionScreen(
                        viewModel = roomSelectionViewModel,
                        onRoomSelected = { selectedRoom ->
                            backStack.add(Screen.RoomDashboard(selectedRoom))
                        },
                        onNavigateToMessages = {
                            backStack.add(Screen.Messages)
                        },
                        onNavigateToMore = {
                            backStack.add(Screen.MoreMenu)
                        }
                    )
                }

                is Screen.RoomDashboard -> {
                    RoomDashboardScreen(
                        viewModel = roomDashboardViewModel,
                        room = screen.room,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onChildSelected = { selectedChild ->
                            backStack.add(Screen.ChatBitacora(selectedChild))
                        },
                        onChildInfo = { selectedChild ->
                            backStack.add(Screen.ChildDetail(selectedChild))
                        },
                        onPassAttendance = { targetChild ->
                            backStack.add(Screen.Attendance(screen.room, targetChild))
                        },
                        onNavigateToMessages = {
                            backStack.add(Screen.Messages)
                        },
                        onNavigateToMore = {
                            backStack.add(Screen.MoreMenu)
                        }
                    )
                }

                is Screen.ChildDetail -> {
                    ChildDetailScreen(
                        viewModel = childDetailViewModel,
                        child = screen.child,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onNavigateToChat = {
                            backStack.add(Screen.ChatBitacora(screen.child))
                        },
                        onNavigateToAttendance = {
                            val currentRoom = MockDataRepository.rooms.first()
                            backStack.add(Screen.Attendance(currentRoom, screen.child))
                        }
                    )
                }

                is Screen.ChatBitacora -> {
                    ChatBitacoraScreen(
                        viewModel = chatBitacoraViewModel,
                        child = screen.child,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onChildInfoClick = {
                            backStack.add(Screen.ChildDetail(screen.child))
                        },
                        onNewActivityClick = {
                            backStack.add(Screen.NewActivity(screen.child))
                        },
                        onAttendanceClick = {
                            val currentRoom = MockDataRepository.rooms.first()
                            backStack.add(Screen.Attendance(currentRoom, screen.child))
                        }
                    )
                }

                is Screen.NewActivity -> {
                    NewActivityScreen(
                        viewModel = newActivityViewModel,
                        child = screen.child,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onSubmitActivity = { _, _, _ ->
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                is Screen.Messages -> {
                    MessagesScreen(
                        viewModel = messagesViewModel,
                        onNavigateToHome = {
                            while (backStack.size > 1 && backStack.last() !is Screen.RoomSelection) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            if (backStack.none { it is Screen.RoomSelection }) {
                                backStack.add(Screen.RoomSelection)
                            }
                        },
                        onNavigateToChildChat = { selectedChild ->
                            backStack.add(Screen.ChatBitacora(selectedChild))
                        },
                        onNavigateToMore = {
                            backStack.add(Screen.MoreMenu)
                        }
                    )
                }

                is Screen.Suggestions -> {
                    SuggestionsScreen(
                        viewModel = suggestionsViewModel,
                        onNavigateToHome = {
                            while (backStack.size > 1 && backStack.last() !is Screen.RoomSelection) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            if (backStack.none { it is Screen.RoomSelection }) {
                                backStack.add(Screen.RoomSelection)
                            }
                        }
                    )
                }

                is Screen.MoreMenu -> {
                    MoreMenuScreen(
                        onNavigateToLostObjects = {
                            backStack.add(Screen.LostObjectsCatalog)
                        },
                        onNavigateToHome = {
                            while (backStack.size > 1 && backStack.last() !is Screen.RoomSelection) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            if (backStack.none { it is Screen.RoomSelection }) {
                                backStack.add(Screen.RoomSelection)
                            }
                        },
                        onNavigateToMessages = {
                            backStack.add(Screen.Messages)
                        },
                        onLogout = {
                            container.authRepository.logout()
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }
                    )
                }

                is Screen.LostObjectsCatalog -> {
                    LostObjectsCatalogScreen(
                        viewModel = lostObjectsViewModel,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onNavigateToPublish = {
                            backStack.add(Screen.PublishLostObject("Sala 1A"))
                        },
                        onNavigateToHome = {
                            while (backStack.size > 1 && backStack.last() !is Screen.RoomSelection) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            if (backStack.none { it is Screen.RoomSelection }) {
                                backStack.add(Screen.RoomSelection)
                            }
                        },
                        onNavigateToMessages = {
                            backStack.add(Screen.Messages)
                        }
                    )
                }

                is Screen.PublishLostObject -> {
                    PublishLostObjectScreen(
                        viewModel = publishLostObjectViewModel,
                        roomName = screen.roomName,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onPublishSuccess = { _ ->
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                is Screen.Attendance -> {
                    AttendanceScreen(
                        viewModel = attendanceViewModel,
                        room = screen.room,
                        initialChild = screen.initialChild,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}
}
