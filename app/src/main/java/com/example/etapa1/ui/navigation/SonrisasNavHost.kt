package com.example.etapa1.ui.navigation

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.etapa1.domain.Sesion
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.ui.components.AppBottomBar
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
    val startRoute = if (isFamiliar) Route.PARENT_CHILD_SELECTION else Route.ROOM_SELECTION

    val announcementsViewModel: AnnouncementsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val unreadAnnouncementsCount by announcementsViewModel.unreadCount.collectAsState()

    var selectedRoom by remember { mutableStateOf(MockDataRepository.rooms.first()) }
    var selectedChild by remember { mutableStateOf(MockDataRepository.mateoGarcia) }
    var selectedParentChild by remember { mutableStateOf(MockDataRepository.mateoGarcia) }
    var attendanceChild by remember { mutableStateOf<Child?>(null) }

    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startRoute

    val currentTabs = if (isFamiliar) {
        listOf(Route.PARENT_HOME, Route.PARENT_SUGGESTIONS, Route.LOST_OBJECTS, Route.MORE_MENU)
    } else {
        listOf(Route.ROOM_SELECTION, Route.MESSAGES, Route.SUGGESTIONS, Route.LOST_OBJECTS, Route.MORE_MENU)
    }

    val showBottomBar = currentRoute in currentTabs ||
        (isFamiliar && currentRoute == Route.PARENT_CHILD_SELECTION) ||
        (isEducadora && currentRoute == Route.ROOM_DASHBOARD)

    val selectedBottomTab = when {
        isFamiliar && currentRoute == Route.PARENT_CHILD_SELECTION -> 0
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

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selectedTab = selectedBottomTab,
                    showMessagesTab = isEducadora,
                    onTabSelected = { tab -> navigateToTab(currentTabs[tab]) }
                )
            }
        }
    ) { paddingValues ->
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
                RoomDashboardScreen(
                    viewModel = vm,
                    room = selectedRoom,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onBack = { nav.popBackStack() },
                    onChildSelected = { child ->
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
                    onNavigateToMore = { nav.navigate(Route.MORE_MENU) }
                )
            }

            composable(Route.CHILD_DETAIL) {
                val vm: ChildDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ChildDetailScreen(
                    viewModel = vm,
                    child = selectedChild,
                    onBack = { nav.popBackStack() },
                    onNavigateToChat = { nav.navigate(Route.CHAT_BITACORA) },
                    onNavigateToAttendance = {
                        attendanceChild = selectedChild
                        nav.navigate(Route.ATTENDANCE)
                    }
                )
            }

            composable(Route.CHAT_BITACORA) {
                val vm: ChatBitacoraViewModel = viewModel(factory = AppViewModelProvider.Factory)
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

            composable(Route.NEW_ACTIVITY) {
                val vm: NewActivityViewModel = viewModel(factory = AppViewModelProvider.Factory)
                NewActivityScreen(
                    viewModel = vm,
                    child = selectedChild,
                    onBack = { nav.popBackStack() },
                    onSubmitActivity = { _, _, _ -> nav.popBackStack() }
                )
            }

            composable(Route.MESSAGES) {
                val vm: MessagesViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val familyGroupsVm: FamilyGroupsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                MessagesScreen(
                    viewModel = vm,
                    familyGroupsViewModel = familyGroupsVm,
                    onNavigateToHome = navigateToHome,
                    onNavigateToChildChat = { child ->
                        selectedChild = child
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
                    onNavigateToLostObjects = { nav.navigate(Route.LOST_OBJECTS) },
                    onNavigateToHome = navigateToHome,
                    onNavigateToMessages = {
                        if (isFamiliar) {
                            selectedChild = selectedParentChild
                            nav.navigate(Route.CHAT_BITACORA)
                        } else {
                            nav.navigate(Route.MESSAGES)
                        }
                    },
                    onLogout = onSalir
                )
            }

            composable(Route.LOST_OBJECTS) {
                val vm: LostObjectsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LostObjectsCatalogScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    onNavigateToPublish = { nav.navigate(Route.PUBLISH_LOST_OBJECT) },
                    onNavigateToHome = navigateToHome,
                    onNavigateToMessages = {
                        if (isFamiliar) {
                            selectedChild = selectedParentChild
                            nav.navigate(Route.CHAT_BITACORA)
                        } else {
                            nav.navigate(Route.MESSAGES)
                        }
                    }
                )
            }

            composable(Route.PUBLISH_LOST_OBJECT) {
                val vm: PublishLostObjectViewModel = viewModel(factory = AppViewModelProvider.Factory)
                PublishLostObjectScreen(
                    viewModel = vm,
                    roomName = selectedRoom.name,
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
                ParentChildSelectionScreen(
                    viewModel = vm,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onChildSelected = { child ->
                        selectedParentChild = child
                        selectedChild = child
                        nav.navigate(Route.PARENT_HOME)
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) }
                )
            }

            composable(Route.PARENT_HOME) {
                val vm: ParentHomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LaunchedEffect(selectedParentChild.id) {
                    vm.selectChild(selectedParentChild)
                }
                ParentHomeScreen(
                    viewModel = vm,
                    unreadAnnouncementsCount = unreadAnnouncementsCount,
                    onNavigateToChat = { child ->
                        selectedChild = child
                        nav.navigate(Route.CHAT_BITACORA)
                    },
                    onNavigateToChildDetail = { child ->
                        selectedChild = child
                        nav.navigate(Route.CHILD_DETAIL)
                    },
                    onNavigateToAnnouncements = { nav.navigate(Route.ANNOUNCEMENTS) },
                    onNavigateToGroups = { nav.navigate(Route.PARENT_GROUPS) },
                    onSwitchChild = { nav.navigate(Route.PARENT_CHILD_SELECTION) }
                )
            }

            composable(Route.PARENT_GROUPS) {
                val vm: FamilyGroupsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ParentGroupsScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.PARENT_SUGGESTIONS) {
                val vm: ParentSuggestionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                ParentSuggestionsScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
