package com.example.etapa1.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.PortionOption
import com.example.etapa1.model.Room
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.ui.screens.AttendanceScreen
import com.example.etapa1.ui.screens.ChatBitacoraScreen
import com.example.etapa1.ui.screens.LoginScreen
import com.example.etapa1.ui.screens.LostObjectsCatalogScreen
import com.example.etapa1.ui.screens.MoreMenuScreen
import com.example.etapa1.ui.screens.NewActivityScreen
import com.example.etapa1.ui.screens.PublishLostObjectScreen
import com.example.etapa1.ui.screens.RoomDashboardScreen
import com.example.etapa1.ui.screens.RoomSelectionScreen
import com.example.etapa1.ui.theme.AppBackground

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
}

@Composable
fun SonrisasApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Login) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Login

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

    // Estado en memoria de los niños para actualizar asistencias y métricas en tiempo real
    val childrenSala1AState = remember {
        mutableStateListOf<Child>().apply {
            addAll(MockDataRepository.childrenSala1A)
        }
    }

    // Manejo de retroceso con botón físico o gesto de Android
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
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
                        onLoginSuccess = {
                            backStack.add(Screen.RoomSelection)
                        }
                    )
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
                    ChatBitacoraScreen(
                        child = screen.child,
                        timelineItems = mateoTimeline,
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
                        onSendMessage = { text ->
                            mateoTimeline.add(
                                TimelineItem.ChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    time = "01:50 PM",
                                    message = text,
                                    isOutgoing = true
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
                        onSubmitActivity = { category, portion, comments ->
                            val portionSuffix = when (portion) {
                                PortionOption.POCO -> "Poco plato"
                                PortionOption.MEDIO -> "Medio plato"
                                PortionOption.TODO -> "Todo el plato"
                            }
                            // Agregar nueva actividad maquetada exactamente como en la imagen
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
                            // Regresar al chat de bitácora
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
                            backStack.add(Screen.Login)
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
            }
        }
    }
}
