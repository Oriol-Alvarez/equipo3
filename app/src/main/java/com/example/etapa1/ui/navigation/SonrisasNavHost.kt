package com.example.etapa1.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.etapa1.SonrisasApplication
import com.example.etapa1.domain.EstadoConexion
import com.example.etapa1.domain.Sesion
import com.example.etapa1.model.Child
import com.example.etapa1.model.SinDatos
import com.example.etapa1.ui.components.AppBottomBar
import com.example.etapa1.ui.components.ChipConexion
import com.example.etapa1.ui.screens.*
import com.example.etapa1.ui.state.*
import com.example.etapa1.ui.theme.AppBackground

@Composable
fun SonrisasNavHost(
    sesion: Sesion,
    onSalir: () -> Unit
) {
    val nav = rememberNavController()
    val isFamiliar = sesion.isFamiliar
    val isEducadora = sesion.isEducadora
    val isAdmin = sesion.isAdmin
    val startRoute = when {
        isAdmin -> Route.ADMIN_PANEL
        isFamiliar -> Route.PARENT_CHILD_SELECTION
        else -> Route.ROOM_SELECTION
    }

    val announcementsViewModel: AnnouncementsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val unreadAnnouncementsCount by announcementsViewModel.unreadCount.collectAsState()

    val messagesViewModel: MessagesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    LaunchedEffect(isFamiliar) {
        messagesViewModel.setWorkerRole(!isFamiliar)
    }
    val messagesUiState by messagesViewModel.uiState.collectAsState()
    val totalUnreadMessages = if (isFamiliar) {
        messagesUiState.familyChats.sumOf { it.unreadCount }
    } else {
        messagesUiState.totalWorkersUnreadCount + messagesUiState.totalParentsUnreadCount
    }

    // Sin selección hasta que la persona elige una sala o un niño (nunca datos de ejemplo)
    var selectedRoom by remember { mutableStateOf(SinDatos.sala) }
    var selectedChild by remember { mutableStateOf(SinDatos.nino) }
    var selectedParentChild by remember { mutableStateOf<Child?>(null) }
    var selectedWorkerChat by remember { mutableStateOf<com.example.etapa1.model.WorkerChat?>(null) }
    var attendanceChild by remember { mutableStateOf<Child?>(null) }

    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startRoute

    val currentTabs = when {
        isAdmin -> emptyList()
        isFamiliar -> listOf(Route.PARENT_HOME, Route.MESSAGES, Route.PARENT_SUGGESTIONS, Route.LOST_OBJECTS, Route.MORE_MENU)
        else -> listOf(Route.ROOM_SELECTION, Route.MESSAGES, Route.SUGGESTIONS, Route.LOST_OBJECTS, Route.MORE_MENU)
    }

    val showBottomBar = !isAdmin && (
        currentRoute in currentTabs ||
            (isEducadora && currentRoute == Route.ROOM_DASHBOARD)
        )

    val selectedBottomTab = when {
        isEducadora && currentRoute == Route.ROOM_DASHBOARD -> 0
        else -> currentTabs.indexOf(currentRoute).coerceAtLeast(0)
    }

    fun navigateToTab(targetRoute: String) {
        nav.navigate(targetRoute) {
            popUpTo(startRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToHome: () -> Unit = {
        nav.navigate(if (isFamiliar) Route.PARENT_HOME else Route.ROOM_SELECTION) {
            popUpTo(startRoute)
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val app = context.applicationContext as? SonrisasApplication
    val estadoConexion = app?.container?.chatConexion?.estado?.collectAsState()?.value ?: EstadoConexion.Conectado

    // Familia: el hijo elegido o, si aún no elige, el primero de sus hijos
    val hijosFamilia = app?.container?.childRepository?.parentChildren?.collectAsState()?.value.orEmpty()
    val hijoFamilia: Child? = selectedParentChild ?: hijosFamilia.firstOrNull()

    val familyGroupsVm: FamilyGroupsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    LaunchedEffect(isFamiliar, hijoFamilia?.id) {
        if (isFamiliar) {
            familyGroupsVm.useFamilyViewer(hijoFamilia?.let { listOf(it.id) })
        } else {
            familyGroupsVm.useEducatorViewer()
        }
    }
    val familyGroupsState by familyGroupsVm.uiState.collectAsState()

    /** Abre el chat de bitácora de la familia, o la lista de hijos si todavía no hay uno. */
    val abrirChatFamilia: () -> Unit = {
        val hijo = hijoFamilia
        selectedWorkerChat = null
        if (hijo != null) {
            selectedChild = hijo
            nav.navigate(Route.CHAT_BITACORA)
        } else {
            nav.navigate(Route.PARENT_CHILD_SELECTION)
        }
    }

    val cambiarHijo: () -> Unit = { nav.navigate(Route.PARENT_CHILD_SELECTION) }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selectedTab = selectedBottomTab,
                    showMessagesTab = true,
                    hasUnreadMessages = totalUnreadMessages > 0,
                    unreadMessagesCount = totalUnreadMessages,
                    onTabSelected = { tab -> navigateToTab(currentTabs[tab]) }
                )
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = startRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
            composable(Route.ROOM_SELECTION) {
                val vm: RoomSelectionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                RoomSelectionScreen(
                    viewModel = vm,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onRoomSelected = { room ->
                        selectedRoom = room
                        nav.navigate(Route.ROOM_DASHBOARD)
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) },
                    onNavigateToMessages = { nav.navigate(Route.MESSAGES) },
                    onNavigateToMore = { nav.navigate(Route.MORE_MENU) }
                )
            }

            composable(Route.ROOM_DASHBOARD) {
                val vm: RoomDashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
                if (selectedRoom.id.isEmpty()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                LaunchedEffect(selectedRoom.id) { vm.abrirSala(selectedRoom.id) }
                RoomDashboardScreen(
                    viewModel = vm,
                    room = selectedRoom,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onBack = { nav.popBackStack() },
                    onChildSelected = { child ->
                        selectedWorkerChat = null
                        selectedChild = child
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onChildInfo = { child ->
                        selectedChild = child
                        nav.navigate(Route.CHILD_DETAIL)
                    },
                    onPassAttendance = { child ->
                        attendanceChild = child
                        nav.navigate(Route.ATTENDANCE)
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) },
                    onNavigateToMessages = { nav.navigate(Route.MESSAGES) },
                    onNavigateToMore = { nav.navigate(Route.MORE_MENU) },
                    onOpenDailyPlan = { nav.navigate(Route.DAILY_PLAN) }
                )
            }

            composable(Route.CHILD_DETAIL) {
                val vm: ChildDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
                if (selectedChild.id.isEmpty()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                ChildDetailScreen(
                    viewModel = vm,
                    child = selectedChild,
                    onBack = { nav.popBackStack() },
                    onNavigateToChat = {
                        selectedWorkerChat = null
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onNavigateToAttendance = {
                        attendanceChild = selectedChild
                        nav.navigate(Route.ATTENDANCE)
                    },
                    isFamilyViewer = isFamiliar,
                    onExportLog = if (isEducadora) {
                        { nav.navigate(Route.EXPORT_LOG) }
                    } else {
                        null
                    }
                )
            }

            composable(Route.EXPORT_LOG) {
                val vm: ExportLogViewModel = viewModel(factory = AppViewModelProvider.Factory)
                if (selectedChild.id.isEmpty()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                ExportLogScreen(
                    viewModel = vm,
                    child = selectedChild,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.CHAT_BITACORA) {
                androidx.compose.runtime.DisposableEffect(Unit) {
                    onDispose {
                        selectedWorkerChat = null
                    }
                }

                val chatElegido = selectedWorkerChat
                if (chatElegido != null) {
                    // Chat del personal, grupo de familias o chat global
                    val currentWorkerChat = messagesUiState.workerChats.find { it.id == chatElegido.id }
                        ?: messagesUiState.familyChats.find { it.id == chatElegido.id }
                        ?: messagesUiState.globalChat?.takeIf { it.id == chatElegido.id }
                        ?: familyGroupsState.groups.find { it.id == chatElegido.id }
                        ?: chatElegido

                    LaunchedEffect(currentWorkerChat.id) {
                        messagesViewModel.markWorkerChatAsRead(currentWorkerChat.id)
                    }

                    val isGroup = currentWorkerChat.isGroup || currentWorkerChat.isGlobal
                    val isFamilyGroup = familyGroupsState.groups.any { it.id == currentWorkerChat.id }

                    qChatBitacoraScreen(
                        workerChat = currentWorkerChat,
                        child = null,
                        isWorkerRole = isEducadora,
                        canShareActivity = isEducadora && isGroup,
                        onBack = {
                            selectedWorkerChat = null
                            nav.popBackStack()
                        },
                        onSendMessage = { text, uri, name, mime ->
                            if (isFamilyGroup) {
                                familyGroupsVm.sendMessage(
                                    groupId = currentWorkerChat.id,
                                    text = text,
                                    fileUri = uri?.toString(),
                                    fileName = name,
                                    fileMimeType = mime
                                )
                            } else {
                                messagesViewModel.sendMessage(currentWorkerChat.id, text)
                            }
                        },
                        onSendActivity = if (isEducadora && isFamilyGroup) {
                            { cat, badge, desc ->
                                familyGroupsVm.sendGroupActivity(currentWorkerChat.id, cat, badge, desc)
                            }
                        } else null
                    )
                } else {
                    val vm: ChatBitacoraViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    if (selectedChild.id.isEmpty()) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                        return@composable
                    }
                    // Abre la bitácora de este niño y marca su chat como leído
                    LaunchedEffect(selectedChild.id) { vm.abrir(selectedChild.id) }
                    ChatBitacoraScreen(
                        viewModel = vm,
                        child = selectedChild,
                        isWorkerRole = isEducadora,
                        onBack = { nav.popBackStack() },
                        onChildInfoClick = { nav.navigate(Route.CHILD_DETAIL) },
                        onNewActivityClick = { nav.navigate(Route.NEW_ACTIVITY) },
                        onAttendanceClick = {
                            attendanceChild = selectedChild
                            nav.navigate(Route.ATTENDANCE)
                        }
                    )
                }
            }

            composable(Route.NEW_ACTIVITY) {
                val vm: NewActivityViewModel = viewModel(factory = AppViewModelProvider.Factory)
                // Solo el personal registra actividades
                if (isFamiliar || selectedChild.id.isEmpty()) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                NewActivityScreen(
                    viewModel = vm,
                    child = selectedChild,
                    onBack = { nav.popBackStack() },
                    onSubmitActivity = { _, _, _, _ -> nav.popBackStack() }
                )
            }

            composable(Route.MESSAGES) {
                LaunchedEffect(Unit) {
                    messagesViewModel.recargar()
                }
                MessagesScreen(
                    viewModel = messagesViewModel,
                    familyGroupsViewModel = familyGroupsVm,
                    isFamiliar = isFamiliar,
                    activeChild = if (isFamiliar) hijoFamilia else null,
                    onSwitchChild = cambiarHijo,
                    onNavigateToHome = navigateToHome,
                    onNavigateToChildChat = { child ->
                        selectedWorkerChat = null
                        selectedChild = child
                        if (isFamiliar) selectedParentChild = child
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onNavigateToWorkerChat = { chat ->
                        selectedWorkerChat = chat
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onNavigateToMore = { nav.navigate(Route.MORE_MENU) }
                )
            }

            composable(Route.SUGGESTIONS) {
                val vm: SuggestionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                SuggestionsScreen(
                    viewModel = vm,
                    onNavigateToHome = navigateToHome
                )
            }

            composable(Route.MORE_MENU) {
                MoreMenuScreen(
                    activeChild = if (isFamiliar) hijoFamilia else null,
                    onSwitchChild = if (isFamiliar) cambiarHijo else null,
                    onNavigateToLostObjects = { nav.navigate(Route.LOST_OBJECTS) },
                    onNavigateToHome = navigateToHome,
                    onNavigateToMessages = {
                        if (isFamiliar) abrirChatFamilia() else nav.navigate(Route.MESSAGES)
                    },
                    onLogout = onSalir
                )
            }

            composable(Route.LOST_OBJECTS) {
                val vm: LostObjectsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LostObjectsCatalogScreen(
                    viewModel = vm,
                    activeChild = if (isFamiliar) hijoFamilia else null,
                    onSwitchChild = if (isFamiliar) cambiarHijo else null,
                    onBack = { nav.popBackStack() },
                    onNavigateToPublish = { nav.navigate(Route.PUBLISH_LOST_OBJECT) },
                    onNavigateToHome = navigateToHome,
                    onNavigateToMessages = {
                        if (isFamiliar) abrirChatFamilia() else nav.navigate(Route.MESSAGES)
                    }
                )
            }

            composable(Route.PUBLISH_LOST_OBJECT) {
                val vm: PublishLostObjectViewModel = viewModel(factory = AppViewModelProvider.Factory)
                PublishLostObjectScreen(
                    viewModel = vm,
                    roomName = selectedRoom.name.ifEmpty { "General" },
                    onBack = { nav.popBackStack() },
                    onPublishSuccess = { nav.popBackStack() }
                )
            }

            composable(Route.ATTENDANCE) {
                val vm: AttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AttendanceScreen(
                    viewModel = vm,
                    room = selectedRoom,
                    initialChild = attendanceChild,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.ANNOUNCEMENTS) {
                AnnouncementsScreen(
                    viewModel = announcementsViewModel,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.PARENT_CHILD_SELECTION) {
                val vm: ParentChildSelectionViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val canGoBack = nav.previousBackStackEntry != null
                ParentChildSelectionScreen(
                    viewModel = vm,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onChildSelected = { child ->
                        selectedParentChild = child
                        selectedChild = child
                        nav.navigate(Route.PARENT_HOME) {
                            popUpTo(Route.PARENT_CHILD_SELECTION) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) },
                    onBack = if (canGoBack) { { nav.popBackStack() } } else null
                )
            }

            composable(Route.PARENT_HOME) {
                val vm: ParentHomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LaunchedEffect(hijoFamilia?.id) {
                    hijoFamilia?.let(vm::selectChild)
                }
                // Mensajes sin leer del chat con la educadora de este hijo
                val childUnreadChat = if (isFamiliar) {
                    messagesUiState.familyChats.find { it.id == "fchat_${hijoFamilia?.id}" }?.unreadCount ?: 0
                } else {
                    messagesUiState.parentChats.find { it.child.id == hijoFamilia?.id }?.unreadCount ?: 0
                }
                ParentHomeScreen(
                    viewModel = vm,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    unreadMessagesCount = childUnreadChat,
                    onNavigateToChat = { child ->
                        selectedWorkerChat = null
                        selectedChild = child
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onNavigateToChildDetail = { child ->
                        selectedChild = child
                        nav.navigate(Route.CHILD_DETAIL)
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) },
                    onNavigateToGroups = { nav.navigate(Route.PARENT_GROUPS) },
                    onOpenDailyPlan = { nav.navigate(Route.DAILY_PLAN) },
                    onSwitchChild = cambiarHijo
                )
            }

            composable(Route.DAILY_PLAN) {
                val vm: DailyPlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val salaPlan = if (isFamiliar) hijoFamilia?.roomId.orEmpty() else selectedRoom.id
                LaunchedEffect(salaPlan) { vm.abrirSala(salaPlan) }
                DailyPlanScreen(
                    viewModel = vm,
                    canEdit = isEducadora,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.PARENT_GROUPS) {
                ParentGroupsScreen(
                    viewModel = familyGroupsVm,
                    activeChildId = hijoFamilia?.id,
                    onBack = { nav.popBackStack() },
                    onNavigateToWorkerChat = { group ->
                        selectedWorkerChat = group
                        nav.navigate(Route.CHAT_BITACORA)
                    }
                )
            }

            composable(Route.PARENT_SUGGESTIONS) {
                val vm: ParentSuggestionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ParentSuggestionsScreen(
                    viewModel = vm,
                    activeChild = hijoFamilia,
                    onSwitchChild = cambiarHijo,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.ADMIN_PANEL) {
                val vm: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
                AdminPanelScreen(
                    viewModel = vm,
                    onLogout = onSalir
                )
            }
        }

        if (app != null && !app.container.useMocks && estadoConexion != EstadoConexion.Conectado) {
            ChipConexion(
                estado = estadoConexion,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp, end = 12.dp)
            )
        }
    }
}
}
