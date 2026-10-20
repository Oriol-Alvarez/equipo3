package com.example.etapa1.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.etapa1.ui.screens.LoginScreen
import com.example.etapa1.ui.state.AppViewModelProvider
import com.example.etapa1.ui.state.LoginViewModel
import com.example.etapa1.ui.state.SesionEstado
import com.example.etapa1.ui.state.SesionViewModel
import com.example.etapa1.ui.screens.LostObjectsCatalogScreen
import com.example.etapa1.ui.screens.MoreMenuScreen
import com.example.etapa1.ui.screens.NewActivityScreen
import com.example.etapa1.ui.screens.PublishLostObjectScreen
import com.example.etapa1.ui.screens.RoomDashboardScreen
import com.example.etapa1.ui.screens.RoomSelectionScreen
import com.example.etapa1.ui.screens.WeeklySummaryScreen
import com.example.etapa1.ui.screens.AttendanceScreen
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.model.PortionOption

sealed interface Screen {
    data object Login : Screen
    data object RoomSelection : Screen
    data class RoomDashboard(val room: Room) : Screen
    data class ChatBitacora(val child: Child) : Screen
    data class NewActivity(val child: Child) : Screen
    data object MoreMenu : Screen
    data object LostObjectsCatalog : Screen
    data class PublishLostObject(val roomName: String = "Sala 1A") : Screen
    data class Attendance(val room: Room, val initialChild: Child? = null) : Screen
    data class WeeklySummary(val child: Child) : Screen
}

@Composable
fun SonrisasApp() {
    val sesionViewModel: SesionViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val estado by sesionViewModel.estado.collectAsState()

    when (val actual = estado) {
        SesionEstado.Cargando -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        SesionEstado.Anonimo -> {
            val loginViewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { role ->
                    sesionViewModel.iniciarSesion(role)
                }
            )
        }

        is SesionEstado.Activa -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as? com.example.etapa1.SonrisasApplication

            LaunchedEffect(actual.sesion) {
                app?.container?.asegurarConexionChat()
                app?.container?.recargarTodo()
                app?.container?.registrarTelefonoParaPush()
            }

            LaunchedEffect(Unit) {
                app?.container?.tokenManager?.sessionExpired?.collect {
                    app.container.desconectarChat()
                    app.container.limpiarTodo()
                    sesionViewModel.salir()
                }
            }

            SonrisasActiveContent()
        }
    }
}

@Composable
fun SonrisasActiveContent() {
    val backStack = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<Screen>(Screen.RoomSelection) }
    val currentScreen = backStack.lastOrNull() ?: Screen.RoomSelection

    val mateoTimeline = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateListOf<TimelineItem>().apply {
            addAll(MockDataRepository.getInitialMateoTimeline())
        }
    }

    val lostItems = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateListOf<com.example.etapa1.model.LostItem>().apply {
            addAll(MockDataRepository.getInitialLostItems())
        }
    }

    val childrenSala1AState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateListOf<Child>().apply {
            addAll(MockDataRepository.childrenSala1A)
        }
    }

    androidx.activity.compose.BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (androidx.compose.animation.slideInHorizontally { width -> width } + androidx.compose.animation.fadeIn())
                    .togetherWith(androidx.compose.animation.slideOutHorizontally { width -> -width } + androidx.compose.animation.fadeOut())
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.Login -> {
                    // Manejado arriba
                }

                is Screen.RoomSelection -> {
                    RoomSelectionScreen(
                        onRoomSelected = { selectedRoom ->
                            backStack.add(Screen.RoomDashboard(selectedRoom))
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
                        onPassAttendance = { targetChild ->
                            backStack.add(Screen.Attendance(screen.room, targetChild))
                        },
                        onNavigateToMore = {
                            backStack.add(Screen.MoreMenu)
                        }
                    )
                }

                is Screen.ChatBitacora -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val app = context.applicationContext as? com.example.etapa1.SonrisasApplication
                    val chatViewModel: com.example.etapa1.ui.state.ChatBitacoraViewModel = viewModel(
                        factory = app?.container?.let {
                            com.example.etapa1.ui.state.ChatBitacoraViewModel.provideFactory(it, screen.child.id)
                        } ?: androidx.lifecycle.viewmodel.CreationExtras.Empty
                    )
                    ChatBitacoraScreen(
                        viewModel = chatViewModel,
                        child = screen.child,
                        isWorkerRole = true,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onNewActivityClick = {
                            backStack.add(Screen.NewActivity(screen.child))
                        },
                        onAttendanceClick = {
                            val currentRoom = MockDataRepository.rooms.first()
                            backStack.add(Screen.Attendance(currentRoom, screen.child))
                        },
                        onWeeklySummaryClick = {
                            backStack.add(Screen.WeeklySummary(screen.child))
                        }
                    )
                }

                is Screen.WeeklySummary -> {
                    WeeklySummaryScreen(
                        child = screen.child,
                        timelineItems = mateoTimeline,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onChatClick = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onSaveManualObservations = { manualNotes ->
                            if (manualNotes.isNotBlank()) {
                                mateoTimeline.add(
                                    TimelineItem.ChatMessage(
                                        id = "msg_${System.currentTimeMillis()}",
                                        time = "02:00 PM",
                                        message = "📝 [Observaciones Manuales]: $manualNotes",
                                        isOutgoing = true
                                    )
                                )
                            }
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
                        onSubmitActivity = { category, portion, comments ->
                            val portionSuffix = when (portion) {
                                PortionOption.POCO -> "Poco plato"
                                PortionOption.MEDIO -> "Medio plato"
                                PortionOption.TODO -> "Todo el plato"
                            }
                            mateoTimeline.add(
                                TimelineItem.ActivityCard(
                                    id = "act_${System.currentTimeMillis()}",
                                    time = "01:45 PM",
                                    category = category.label,
                                    portionLabel = portionSuffix,
                                    description = comments.ifBlank {
                                        "Mateo disfrutó mucho su comida de hoy y pidió un poco más de fruta."
                                    }
                                )
                            )
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
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
                        onLogout = {
                            backStack.clear()
                            backStack.add(Screen.RoomSelection)
                        }
                    )
                }

                is Screen.LostObjectsCatalog -> {
                    LostObjectsCatalogScreen(
                        items = lostItems,
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
                                if (notes.isNotBlank()) {
                                    mateoTimeline.add(
                                        TimelineItem.ChatMessage(
                                            id = "msg_${System.currentTimeMillis()}",
                                            time = time,
                                            message = "📋 [Observación de Asistencia]: $notes",
                                            isOutgoing = true
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
