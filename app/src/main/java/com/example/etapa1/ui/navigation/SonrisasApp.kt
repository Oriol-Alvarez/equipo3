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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.etapa1.ui.components.AppBottomBar
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Announcement
import com.example.etapa1.model.Child
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.PortionOption
import com.example.etapa1.model.Room
import com.example.etapa1.model.SuggestionStatus
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.UserRole
import com.example.etapa1.ui.screens.AttendanceScreen
import com.example.etapa1.ui.screens.AnnouncementsScreen
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
    data object Announcements : Screen
}

@Composable
fun SonrisasApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Login) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Login
    var currentUserRole by remember { mutableStateOf(UserRole.EDUCADORA) }

    // Estado en memoria de la bitácora de Mateo
    val mateoTimeline = remember {
        mutableStateListOf<TimelineItem>().apply {
            addAll(MockDataRepository.getInitialMateoTimeline())
        }
    }

    // Estado en memoria del catálogo de objetos perdidos
    val lostItems = remember {
        mutableStateListOf<LostItem>().apply {
            addAll(MockDataRepository.getInitialLostItems())
        }
    }

    // Estado en memoria de las sugerencias de los padres
    val suggestions = remember {
        mutableStateListOf<ParentSuggestion>().apply {
            addAll(MockDataRepository.getInitialSuggestions())
        }
    }

    // Estado en memoria de los niños para actualizar asistencias y métricas en tiempo real
    val childrenSala1AState = remember {
        mutableStateListOf<Child>().apply {
            addAll(MockDataRepository.childrenSala1A)
        }
    }

    val announcementsState = remember {
        mutableStateListOf<Announcement>().apply {
            addAll(MockDataRepository.getInitialAnnouncements())
        }
    }

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
                        onLoginSuccess = { role ->
                            currentUserRole = role
                            backStack.add(Screen.RoomSelection)
                        }
                    )
                }

                is Screen.RoomSelection -> {
                    RoomSelectionScreen(
                        onRoomSelected = { selectedRoom ->
                            backStack.add(Screen.RoomDashboard(selectedRoom))
                        },
                        unreadAnnouncementsCount = announcementsState.count { it.isUnread },
                        onNavigateToAnnouncements = {
                            backStack.add(Screen.Announcements)
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
                        room = screen.room,
                        children = childrenSala1AState,
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
                        child = screen.child,
                        timelineItems = mateoTimeline,
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
                        },
                        onSendMessage = { text, uri, name, mime ->
                            mateoTimeline.add(
                                TimelineItem.ChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    time = "Ahora",
                                    message = text,
                                    isOutgoing = true,
                                    fileUri = uri?.toString(),
                                    fileName = name,
                                    fileMimeType = mime
                                )
                            )
                        }
                    )
                }

                is Screen.NewActivity -> {
                    NewActivityScreen(
                        child = screen.child,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onSubmitActivity = { category, summaryBadge, description ->
                            val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                            // Agregar nueva actividad maquetada exactamente como en la imagen
                            mateoTimeline.add(
                                TimelineItem.ActivityCard(
                                    id = "act_${System.currentTimeMillis()}",
                                    time = currentTime,
                                    category = category.label,
                                    portionLabel = summaryBadge,
                                    description = description
                                )
                            )
                            // Regresar al chat de bitácora
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                is Screen.Messages -> {
                    MessagesScreen(
                        children = childrenSala1AState,
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
                        suggestions = suggestions,
                        onRespondSuggestion = { suggestionId, responseText, newStatus ->
                            val index = suggestions.indexOfFirst { it.id == suggestionId }
                            if (index != -1) {
                                val current = suggestions[index]
                                suggestions[index] = current.copy(
                                    response = responseText,
                                    responseDate = "Hoy, ahora",
                                    responderName = "Laura Méndez (Dirección)",
                                    status = newStatus
                                )
                            }
                        },
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
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }
                    )
                }

                is Screen.LostObjectsCatalog -> {
                    LostObjectsCatalogScreen(
                        items = lostItems,
                        userRole = currentUserRole,
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
                        roomName = screen.roomName,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onPublishSuccess = { newItem ->
                            // El nuevo objeto se agrega en la primera posición del catálogo
                            lostItems.add(0, newItem)
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                is Screen.Attendance -> {
                    AttendanceScreen(
                        room = screen.room,
                        children = childrenSala1AState,
                        initialChild = screen.initialChild,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onSaveAttendance = { child, isIngreso, time, notes ->
                            val index = childrenSala1AState.indexOfFirst { it.id == child.id }
                            if (index != -1) {
                                childrenSala1AState[index] = childrenSala1AState[index].copy(
                                    isPresent = isIngreso,
                                    arrivalTime = if (isIngreso) time else childrenSala1AState[index].arrivalTime,
                                    statusText = if (isIngreso) "$time • Presente" else "$time • Egresado(a)"
                                )
                            }
                            // Si es Mateo, reflejar el registro de asistencia en el chat/bitácora
                            if (child.id == MockDataRepository.mateoGarcia.id) {
                                val actionLabel = if (isIngreso) "Ingreso: $time" else "Egreso: $time"
                                mateoTimeline.add(
                                    TimelineItem.EventChip(
                                        id = "chip_${System.currentTimeMillis()}",
                                        time = time,
                                        text = actionLabel,
                                        iconType = "entry"
                                    )
                                )
                            }
                        }
                    )
                }

                is Screen.Announcements -> {
                    AnnouncementsScreen(
                        announcements = announcementsState,
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
