package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.*
import com.example.etapa1.ui.components.DaycareLogo
import com.example.etapa1.ui.state.AdminViewModel
import com.example.etapa1.ui.theme.*

@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val lostObjects by viewModel.lostObjects.collectAsState()
    val selectedUserForChildren by viewModel.selectedUserForChildren.collectAsState()
    val availableCenterChildren by viewModel.availableCenterChildren.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Dialog state for Users
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<AdminUser?>(null) }
    var userToDelete by remember { mutableStateOf<AdminUser?>(null) }

    // Dialog state for Announcements
    var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
    var announcementToEdit by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    // Dialog state for Suggestions & Lost Objects
    var showCreateLostObjectDialog by remember { mutableStateOf(false) }
    var suggestionToAnswer by remember { mutableStateOf<ParentSuggestion?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AdminTopBar(
                onLogoutClick = { showLogoutDialog = true },
                onRefreshClick = { viewModel.refresh() },
                isRefreshing = uiState.isRefreshing
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Executive Tab Navigation
            AdminTabsRow(
                selectedTab = uiState.selectedTab,
                onTabSelect = { viewModel.selectTab(it) }
            )

            // Content per tab
            when (uiState.selectedTab) {
                0 -> AdminResumenTab(
                    stats = stats,
                    logs = logs,
                    announcementsCount = announcements.size,
                    usersCount = users.size,
                    onNavigateToTab = { viewModel.selectTab(it) },
                    onCreateUserClick = { showCreateUserDialog = true },
                    onCreateAnnouncementClick = { showCreateAnnouncementDialog = true }
                )
                1 -> AdminPerfilesTab(
                    users = users,
                    selectedFilter = uiState.selectedUserFilter,
                    searchQuery = uiState.userSearchQuery,
                    onFilterChange = { viewModel.selectUserFilter(it) },
                    onSearchChange = { viewModel.onUserSearchChange(it) },
                    onCreateUserClick = { showCreateUserDialog = true },
                    onEditUserClick = { userToEdit = it },
                    onDeleteUserClick = { userToDelete = it },
                    onManageChildrenClick = { viewModel.openChildrenDialog(it) }
                )
                2 -> AdminAvisosTab(
                    announcements = announcements,
                    onCreateAnnouncementClick = { showCreateAnnouncementDialog = true },
                    onEditAnnouncementClick = { announcementToEdit = it },
                    onDeleteAnnouncementClick = { announcementToDelete = it }
                )
                3 -> AdminObjetosYQuejasTab(
                    selectedSubTab = uiState.selectedLostOrSugTab,
                    onSubTabSelect = { viewModel.selectLostOrSugTab(it) },
                    lostObjects = lostObjects,
                    lostFilter = uiState.lostObjectsFilter,
                    onLostFilterChange = { viewModel.selectLostObjectsFilter(it) },
                    onCreateLostObjectClick = { showCreateLostObjectDialog = true },
                    onToggleLostClaim = { viewModel.toggleLostObjectClaim(it) },
                    onDeleteLostObject = { viewModel.deleteLostObject(it.id) },
                    suggestions = suggestions,
                    sugFilter = uiState.suggestionsFilter,
                    onSugFilterChange = { viewModel.selectSuggestionsFilter(it) },
                    onAnswerSuggestionClick = { suggestionToAnswer = it },
                    onDeleteSuggestion = { viewModel.deleteSuggestion(it.id) }
                )
                4 -> AdminLogsTab(
                    logs = logs,
                    selectedFilter = uiState.selectedLogFilter,
                    searchQuery = uiState.logSearchQuery,
                    onFilterChange = { viewModel.selectLogFilter(it) },
                    onSearchChange = { viewModel.onLogSearchChange(it) }
                )
                5 -> AdminEstadisticasTab(
                    stats = stats,
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }

    // --- DIÁLOGOS MODALES ---

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas salir del panel de administración?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { name, email, role, phone, pass ->
                viewModel.createUser(name, email, role, phone, pass) { success ->
                    if (success) showCreateUserDialog = false
                }
            }
        )
    }

    userToEdit?.let { u ->
        EditUserDialog(
            user = u,
            onDismiss = { userToEdit = null },
            onConfirm = { name, email, role, phone, pass ->
                viewModel.updateUser(u.id, name, email, phone, role, pass) { success ->
                    if (success) userToEdit = null
                }
            }
        )
    }

    userToDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Eliminar Perfil", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar el perfil de \"${u.displayName}\" (${u.role})? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(u.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    selectedUserForChildren?.let { parentUser ->
        AdminChildrenManagementDialog(
            parentUser = parentUser,
            availableCenterChildren = availableCenterChildren,
            onDismiss = { viewModel.closeChildrenDialog() },
            onLinkChild = { childId -> viewModel.linkChildToParent(parentUser.id, childId) },
            onUnlinkChild = { childId -> viewModel.unlinkChildFromParent(parentUser.id, childId) },
            onCreateAndLinkChild = { fullName, room, bloodType, ped, emer, notes ->
                viewModel.createAndLinkChild(parentUser.id, fullName, room, bloodType, ped, emer, notes)
            },
            onUpdateChild = { childId, fullName, room, bloodType, ped, emer, notes, allergy, birthDate ->
                viewModel.updateChild(childId, fullName, room, bloodType, ped, emer, notes, allergy, birthDate)
            }
        )
    }

    if (showCreateAnnouncementDialog) {
        CreateAnnouncementDialog(
            onDismiss = { showCreateAnnouncementDialog = false },
            onConfirm = { title, summary, content, author ->
                viewModel.createAnnouncement(title, summary, content, author) { success ->
                    if (success) showCreateAnnouncementDialog = false
                }
            }
        )
    }

    announcementToEdit?.let { a ->
        EditAnnouncementDialog(
            announcement = a,
            onDismiss = { announcementToEdit = null },
            onConfirm = { title, summary, content, author ->
                viewModel.updateAnnouncement(a.id, title, summary, content, author) { success ->
                    if (success) announcementToEdit = null
                }
            }
        )
    }

    announcementToDelete?.let { a ->
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            title = { Text("Eliminar Aviso", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar el comunicado \"${a.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAnnouncement(a.id)
                        announcementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCreateLostObjectDialog) {
        CreateLostObjectDialog(
            onDismiss = { showCreateLostObjectDialog = false },
            onConfirm = { title, desc, cat, loc, room ->
                viewModel.createLostObject(title, desc, cat, loc, room) { success ->
                    if (success) showCreateLostObjectDialog = false
                }
            }
        )
    }

    suggestionToAnswer?.let { s ->
        AnswerSuggestionDialog(
            suggestion = s,
            onDismiss = { suggestionToAnswer = null },
            onConfirm = { status, response ->
                viewModel.answerSuggestion(s.id, status, response) { success ->
                    if (success) suggestionToAnswer = null
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// TOP BAR & TABS
// ---------------------------------------------------------------------------

@Composable
private fun AdminTopBar(
    onLogoutClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DaycareLogo(size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Panel de Control",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandBlueContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }
                    Text(
                        text = "Dirección y Supervisión General",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = if (isRefreshing) BrandBlue else TextSecondary
                    )
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = AlertRed
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminTabsRow(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val tabs = listOf(
        "Resumen" to Icons.Default.Dashboard,
        "Perfiles" to Icons.Default.People,
        "Avisos" to Icons.Default.Campaign,
        "Obj. y Quejas" to Icons.Default.Inventory2,
        "Logs" to Icons.Default.ReceiptLong,
        "Estadísticas" to Icons.Default.BarChart
    )

    Surface(
        color = CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            divider = {},
            containerColor = CardBackground,
            contentColor = BrandBlue
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelect(index) },
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = BrandBlue,
                    unselectedContentColor = TextMuted
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 0: RESUMEN / DASHBOARD
// ---------------------------------------------------------------------------

@Composable
private fun AdminResumenTab(
    stats: AdminStats,
    logs: List<AdminLog>,
    announcementsCount: Int,
    usersCount: Int,
    onNavigateToTab: (Int) -> Unit,
    onCreateUserClick: () -> Unit,
    onCreateAnnouncementClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BrandBlue)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Bienvenido/a a la Dirección",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Control general de estancias infantiles, personal, bitácora en vivo y estadísticas consolidadas del centro.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateAnnouncementClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo Aviso", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCreateUserClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.7f))),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear Perfil", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Métricas Rápidas
        Text(
            text = "Estado del Centro Hoy",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                title = "Asistencia",
                value = "${stats.presentCount} / ${stats.totalChildren}",
                subtitle = "${stats.attendanceRate}% presentes",
                icon = Icons.Default.CheckCircle,
                accentColor = StatusPresentGreen,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(5) }
            )
            MetricTile(
                title = "Alimentación",
                value = "${stats.childrenFedCount} niños",
                subtitle = "${stats.breakfastCount} desayunos",
                icon = Icons.Default.Restaurant,
                accentColor = BrandBlue,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(5) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                title = "Siestas",
                value = "${stats.childrenNappedCount} niños",
                subtitle = "Prom: ${stats.averageNapMinutes} min",
                icon = Icons.Default.Bedtime,
                accentColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(5) }
            )
            MetricTile(
                title = "Egresos",
                value = "${stats.egresosCount} retirados",
                subtitle = "${stats.pendingPickupsCount} por recoger",
                icon = Icons.Default.DepartureBoard,
                accentColor = StatusAbsentOrange,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(5) }
            )
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun AdminQuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BrandBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 1: GESTIÓN DE PERFILES (EDUCADORES Y FAMILIARES)
// ---------------------------------------------------------------------------

@Composable
private fun AdminPerfilesTab(
    users: List<AdminUser>,
    selectedFilter: String,
    searchQuery: String,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onCreateUserClick: () -> Unit,
    onEditUserClick: (AdminUser) -> Unit,
    onDeleteUserClick: (AdminUser) -> Unit,
    onManageChildrenClick: (AdminUser) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Actions & Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfiles de Usuario",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Button(
                onClick = onCreateUserClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por nombre o correo...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = BorderSubtle
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TODOS" to "Todos", "EDUCADORA" to "Educadores", "FAMILIAR" to "Familiares").forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron perfiles con los filtros actuales.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    AdminUserCard(
                        user = user,
                        onEdit = { onEditUserClick(user) },
                        onDelete = { onDeleteUserClick(user) },
                        onManageChildren = { onManageChildrenClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageChildren: () -> Unit
) {
    val isEducator = user.role.equals("EDUCADORA", ignoreCase = true)
    val isDirector = user.role.equals("DIRECCION", ignoreCase = true)

    val roleBgColor = when {
        isDirector -> BrandBlueContainer
        isEducator -> Color(0xFFE0F2FE)
        else -> Color(0xFFFEF3C7)
    }
    val roleTextColor = when {
        isDirector -> BrandBlueDark
        isEducator -> Color(0xFF0369A1)
        else -> Color(0xFFB45309)
    }
    val roleLabel = when {
        isDirector -> "Dirección"
        isEducator -> "Educadora"
        else -> "Familiar"
    }

    val initials = user.displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "US" }

    Surface(
        onClick = {
            if (user.role.equals("FAMILIAR", ignoreCase = true)) {
                onManageChildren()
            } else {
                onEdit()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(roleBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    color = roleTextColor,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(roleBgColor)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleTextColor
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tel: ${user.phone}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = AlertRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 2: AVISOS DEL CENTRO (CIRCULARES A TODO EL MUNDO)
// ---------------------------------------------------------------------------

@Composable
private fun AdminAvisosTab(
    announcements: List<Announcement>,
    onCreateAnnouncementClick: () -> Unit,
    onEditAnnouncementClick: (Announcement) -> Unit,
    onDeleteAnnouncementClick: (Announcement) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Broadcast info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandBlueContainer)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Difusión a Toda la Comunidad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlueDark
                    )
                    Text(
                        text = "Los comunicados emitidos desde este panel se notifican a educadores y familias de forma global.",
                        fontSize = 11.sp,
                        color = BrandBlueDark.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comunicados Activos (${announcements.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Button(
                onClick = onCreateAnnouncementClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Emitir Aviso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No hay avisos registrados.", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(announcements, key = { it.id }) { ann ->
                    AdminAnnouncementCard(
                        announcement = ann,
                        onEdit = { onEditAnnouncementClick(ann) },
                        onDelete = { onDeleteAnnouncementClick(ann) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAnnouncementCard(
    announcement: Announcement,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = announcement.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = announcement.author,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                        Text(text = " • ${announcement.date}", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = BrandBlue, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AlertRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = announcement.summary,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = announcement.fullContent,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (expanded) "Mostrar menos" else "Leer aviso completo...",
                color = BrandBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 3: OBJETOS PERDIDOS Y SUGERENCIAS
// ---------------------------------------------------------------------------

@Composable
private fun AdminObjetosYQuejasTab(
    selectedSubTab: Int,
    onSubTabSelect: (Int) -> Unit,
    lostObjects: List<LostItem>,
    lostFilter: String,
    onLostFilterChange: (String) -> Unit,
    onCreateLostObjectClick: () -> Unit,
    onToggleLostClaim: (LostItem) -> Unit,
    onDeleteLostObject: (LostItem) -> Unit,
    suggestions: List<ParentSuggestion>,
    sugFilter: String,
    onSugFilterChange: (String) -> Unit,
    onAnswerSuggestionClick: (ParentSuggestion) -> Unit,
    onDeleteSuggestion: (ParentSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-tabs switcher
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = CardBackground,
            contentColor = BrandBlue,
            divider = {}
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { onSubTabSelect(0) },
                text = { Text("Objetos Perdidos (${lostObjects.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { onSubTabSelect(1) },
                text = { Text("Buzón de Sugerencias (${suggestions.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedSubTab == 0) {
            // OBJETOS PERDIDOS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TODOS" to "Todos", "EN_CUSTODIA" to "En Custodia", "ENTREGADOS" to "Entregados").forEach { (k, label) ->
                        FilterChip(
                            selected = lostFilter == k,
                            onClick = { onLostFilterChange(k) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                IconButton(onClick = onCreateLostObjectClick) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Registrar Objeto", tint = BrandBlue)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filteredLost = lostObjects.filter {
                when (lostFilter) {
                    "EN_CUSTODIA" -> !it.isClaimed
                    "ENTREGADOS" -> it.isClaimed
                    else -> true
                }
            }

            if (filteredLost.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay objetos con este filtro.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLost, key = { it.id }) { item ->
                        AdminLostItemCard(
                            item = item,
                            onToggleClaim = { onToggleLostClaim(item) },
                            onDelete = { onDeleteLostObject(item) }
                        )
                    }
                }
            }
        } else {
            // BUZÓN DE SUGERENCIAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODAS" to "Todas", "PENDIENTES" to "Pendientes", "ATENDIDAS" to "Atendidas").forEach { (k, label) ->
                    FilterChip(
                        selected = sugFilter == k,
                        onClick = { onSugFilterChange(k) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filteredSug = suggestions.filter {
                when (sugFilter) {
                    "PENDIENTES" -> it.status != SuggestionStatus.ATENDIDA
                    "ATENDIDAS" -> it.status == SuggestionStatus.ATENDIDA
                    else -> true
                }
            }

            if (filteredSug.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay sugerencias en esta categoría.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSug, key = { it.id }) { sug ->
                        AdminSuggestionCard(
                            suggestion = sug,
                            onAnswer = { onAnswerSuggestionClick(sug) },
                            onDelete = { onDeleteSuggestion(sug) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLostItemCard(
    item: LostItem,
    onToggleClaim: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.isClaimed) StatusPresentGreenBg else StatusAbsentOrangeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (item.isClaimed) "Entregado a Familia" else "En Custodia",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isClaimed) StatusPresentGreen else StatusAbsentOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Categoría: ${item.category} • Ubicación: ${item.location ?: item.room}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = item.description, fontSize = 13.sp, color = TextPrimary, lineHeight = 17.sp)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleClaim,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.isClaimed) "Marcar en Custodia" else "Marcar como Entregado",
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AlertRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminSuggestionCard(
    suggestion: ParentSuggestion,
    onAnswer: () -> Unit,
    onDelete: () -> Unit
) {
    val isAnswered = suggestion.status == SuggestionStatus.ATENDIDA

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.subject,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(suggestion.status.bgColor))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = suggestion.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(suggestion.status.color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Familia: ${suggestion.parentName} • Alumno: ${suggestion.childName} (${suggestion.roomName})",
                fontSize = 12.sp,
                color = BrandBlue,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "Fecha: ${suggestion.date} a las ${suggestion.time}", fontSize = 11.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = suggestion.content, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)

            if (!suggestion.response.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlueContainer)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Respuesta de ${suggestion.responderName ?: "Dirección"} (${suggestion.responseDate ?: "Hoy"}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = suggestion.response,
                            fontSize = 12.sp,
                            color = BrandBlueDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAnswer,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAnswered) Color(0xFF64748B) else BrandBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = if (isAnswered) "Editar Respuesta" else "Responder Queja", fontSize = 12.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AlertRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 4: LOGS Y REGISTRO DE AUDITORÍA
// ---------------------------------------------------------------------------

@Composable
private fun AdminLogsTab(
    logs: List<AdminLog>,
    selectedFilter: String,
    searchQuery: String,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bitácora y Registros del Sistema",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Registro en tiempo real de asistencia, actividades realizadas, mensajes y circulares.",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar en bitácora por alumno o acción...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = BorderSubtle
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categories Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "TODOS" to "Todos",
                "ASISTENCIA" to "Ingresos/Egresos",
                "ACTIVIDAD" to "Actividades",
                "MENSAJE" to "Mensajes",
                "AVISO" to "Avisos",
                "SUGERENCIA" to "Sugerencias",
                "OBJETO" to "Objetos",
                // RF-14 / RF-10: quién vio cada foto y qué fotos se eliminaron
                "FOTO" to "Fotos"
            )
            items(categories) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No hay registros con estos criterios.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    AdminLogCard(item = log)
                }
            }
        }
    }
}

@Composable
private fun AdminLogCard(item: AdminLog) {
    val (catBg, catText, catIcon) = when (item.category.uppercase()) {
        "ASISTENCIA" -> Triple(StatusPresentGreenBg, StatusPresentGreen, Icons.Default.DirectionsWalk)
        "ACTIVIDAD" -> Triple(BrandBlueContainer, BrandBlue, Icons.Default.ChildCare)
        "MENSAJE" -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), Icons.Default.Chat)
        "AVISO" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Campaign)
        "SUGERENCIA" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.MailOutline)
        "FOTO" -> Triple(Color(0xFFFCE7F3), Color(0xFFBE185D), Icons.Default.PhotoCamera)
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Receipt)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(catBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(catIcon, contentDescription = null, tint = catText, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = catText
                        )
                    }
                    Text(text = item.timestamp, fontSize = 11.sp, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.description, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Registrado por: ${item.actorName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    if (!item.roomName.isNullOrBlank()) {
                        Text(text = " • Sala: ${item.roomName}", fontSize = 11.sp, color = BrandBlue)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 5: ESTADÍSTICAS Y ANALÍTICAS
// ---------------------------------------------------------------------------

@Composable
private fun AdminEstadisticasTab(
    stats: AdminStats,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Métricas y Estadísticas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Indicadores cuantitativos consolidados del centro escolar.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Recalcular", tint = BrandBlue)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Asistencia
        StatsCardSection(title = "Asistencia y Presencia Hoy", icon = Icons.Default.CheckCircle, color = StatusPresentGreen) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Alumnos Presentes", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.presentCount} de ${stats.totalChildren}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tasa de Asistencia", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.attendanceRate}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusPresentGreen)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (stats.attendanceRate / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = StatusPresentGreen,
                trackColor = StatusPresentGreenBg,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Alumnos ausentes reportados: ${stats.absentCount}", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Comedor y Alimentación
        StatsCardSection(title = "Comedor y Nutrición", icon = Icons.Default.Restaurant, color = BrandBlue) {
            Text(
                text = "${stats.childrenFedCount} alumnos participaron en las tomas alimenticias",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatsMiniBadge("Desayunos", "${stats.breakfastCount}")
                StatsMiniBadge("Comidas", "${stats.lunchCount}")
                StatsMiniBadge("Colaciones", "${stats.snackCount}")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Sueño y Siestas
        StatsCardSection(title = "Descanso y Siestas", icon = Icons.Default.Bedtime, color = Color(0xFF7C3AED)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Niños con siesta registrada", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.childrenNappedCount} alumnos", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Promedio de sueño", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.averageNapMinutes} min / niño", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            val hours = stats.totalNapMinutes / 60
            val mins = stats.totalNapMinutes % 60
            Text(
                text = "Tiempo de descanso acumulado en salas: ${hours}h ${mins}m",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Ingresos, Egresos y Recogidas
        StatsCardSection(title = "Control de Ingresos y Salidas", icon = Icons.Default.DepartureBoard, color = StatusAbsentOrange) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatsMiniBadge("Ingresos Hoy", "${stats.ingresosCount}")
                StatsMiniBadge("Egresos Realizados", "${stats.egresosCount}")
                StatsMiniBadge("Pendientes de Retiro", "${stats.pendingPickupsCount}")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Higiene, Salud y Atención
        StatsCardSection(title = "Higiene y Salud", icon = Icons.Default.MedicalServices, color = AlertRed) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Control de Esfínteres / Pañal", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.diaperChangesCount} registrados", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Incidentes Escolares", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.incidentsCount} reportados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (stats.incidentsCount > 0) AlertRed else StatusPresentGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Atención a Familias y Custodia
        StatsCardSection(title = "Atención a Familias y Custodia", icon = Icons.Default.SupportAgent, color = Color(0xFF0D9488)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sugerencias Recibidas", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.totalSuggestions} (${stats.attendedSuggestions} resueltas)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Objetos Perdidos", fontSize = 12.sp, color = TextSecondary)
                    Text("${stats.unclaimedLostObjects} en custodia (${stats.claimedLostObjects} devueltos)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatsCardSection(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatsMiniBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AppBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ---------------------------------------------------------------------------
// DIÁLOGOS DE CREACIÓN Y EDICIÓN
// ---------------------------------------------------------------------------

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, role: String, phone: String?, pass: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("EDUCADORA") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("Password123!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EDUCADORA" to "Educadora", "FAMILIAR" to "Familiar", "DIRECCION" to "Dirección").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role == rKey,
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña Inicial") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onConfirm(name, email, role, phone.ifBlank { null }, password)
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditUserDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, role: String, phone: String?, pass: String?) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var email by remember { mutableStateOf(user.email) }
    var role by remember { mutableStateOf(user.role) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EDUCADORA" to "Educadora", "FAMILIAR" to "Familiar", "DIRECCION" to "Dirección").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role.equals(rKey, ignoreCase = true),
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nueva Contraseña (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onConfirm(name, email, role, phone.ifBlank { null }, password.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, summary: String, content: String, author: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("Dirección del Centro") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publicar Aviso a Toda la Comunidad", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Comunicado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Resumen Corto") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido Completo de la Circular") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, summary.ifBlank { title }, content, author)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Publicar y Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditAnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onConfirm: (title: String, summary: String, content: String, author: String) -> Unit
) {
    var title by remember { mutableStateOf(announcement.title) }
    var summary by remember { mutableStateOf(announcement.summary) }
    var content by remember { mutableStateOf(announcement.fullContent) }
    var author by remember { mutableStateOf(announcement.author) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar Aviso", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Resumen") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido Completo") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, summary, content, author)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CreateLostObjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, cat: String, loc: String?, room: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Ropa") }
    var location by remember { mutableStateOf("Patio Principal") }
    var room by remember { mutableStateOf("Sala 1A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Objeto Perdido", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del Objeto (ej. Suéter azul)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción (detalles, marcas)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lugar donde se encontró") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc.ifBlank { title }, category, location, room)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar en Custodia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AnswerSuggestionDialog(
    suggestion: ParentSuggestion,
    onDismiss: () -> Unit,
    onConfirm: (status: String, response: String) -> Unit
) {
    var responseText by remember { mutableStateOf(suggestion.response ?: "") }
    var selectedStatus by remember { mutableStateOf("ATENDIDA") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Responder a ${suggestion.parentName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Asunto: ${suggestion.subject}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(suggestion.content, fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("Respuesta Oficial de la Dirección") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ATENDIDA" to "Marcar Atendida", "EN_REVISION" to "En Revisión").forEach { (stKey, stLabel) ->
                        FilterChip(
                            selected = selectedStatus == stKey,
                            onClick = { selectedStatus = stKey },
                            label = { Text(stLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (responseText.isNotBlank()) {
                        onConfirm(selectedStatus, responseText)
                    }
                },
                enabled = responseText.isNotBlank()
            ) {
                Text("Enviar Respuesta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// GESTIÓN DE HIJOS / ALUMNOS ASOCIADOS A FAMILIARES
// ---------------------------------------------------------------------------

@Composable
private fun AdminChildrenManagementDialog(
    parentUser: AdminUser,
    availableCenterChildren: List<AdminChildSummary>,
    onDismiss: () -> Unit,
    onLinkChild: (childId: String) -> Unit,
    onUnlinkChild: (childId: String) -> Unit,
    onCreateAndLinkChild: (fullName: String, room: String, bloodType: String?, pediatrician: String?, emergencyPhone: String?, medicalNotes: String?) -> Unit,
    onUpdateChild: (childId: String, fullName: String?, room: String?, bloodType: String?, pediatrician: String?, emergencyPhone: String?, medicalNotes: String?, allergyAlert: String?, birthDate: String?) -> Unit
) {
    var showCreateChildDialog by remember { mutableStateOf(false) }
    var childToEdit by remember { mutableStateOf<AdminChildSummary?>(null) }
    var childToUnlink by remember { mutableStateOf<AdminChildSummary?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alumnos de la Familia",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${parentUser.displayName} (${parentUser.email})",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Botón registrar alumno directamente a esta cuenta
                Button(
                    onClick = { showCreateChildDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir Alumno a esta Familia", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (parentUser.children.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Esta cuenta familiar no tiene alumnos asociados todavía.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pulsa en \"Añadir Alumno a esta Familia\" para registrar a un nuevo hijo.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parentUser.children.forEach { child ->
                            ChildManagementCard(
                                child = child,
                                onEdit = { childToEdit = child },
                                onUnlink = { childToUnlink = child }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )

    // Modal: Confirmación Eliminar Alumno
    childToUnlink?.let { child ->
        AlertDialog(
            onDismissRequest = { childToUnlink = null },
            title = { Text("Eliminar Alumno", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Deseas dar de baja y eliminar a \"${child.fullName}\" de la cuenta familiar de ${parentUser.displayName}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnlinkChild(child.id)
                        childToUnlink = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Eliminar Alumno")
                }
            },
            dismissButton = {
                TextButton(onClick = { childToUnlink = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Create New Child
    if (showCreateChildDialog) {
        AdminCreateChildDialog(
            onDismiss = { showCreateChildDialog = false },
            onConfirm = { fullName, room, bloodType, pediatrician, emergencyPhone, medicalNotes ->
                onCreateAndLinkChild(fullName, room, bloodType, pediatrician, emergencyPhone, medicalNotes)
                showCreateChildDialog = false
            }
        )
    }

    // Modal: Edit Child Profile
    childToEdit?.let { child ->
        AdminEditChildDialog(
            child = child,
            onDismiss = { childToEdit = null },
            onConfirm = { childId, fullName, room, bloodType, pediatrician, emergencyPhone, medicalNotes, allergyAlert, birthDate ->
                onUpdateChild(childId, fullName, room, bloodType, pediatrician, emergencyPhone, medicalNotes, allergyAlert, birthDate)
                childToEdit = null
            }
        )
    }
}

@Composable
private fun ChildManagementCard(
    child: AdminChildSummary,
    onEdit: () -> Unit,
    onUnlink: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Avatar, Name, Room, Presence
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(child.avatarBgColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = child.avatarInitials.ifBlank { child.fullName.take(2).uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(child.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(child.roomText.ifBlank { "Sin sala" }, fontSize = 11.sp, color = TextSecondary)
                        if (child.groupText.isNotBlank()) {
                            Text("•", fontSize = 11.sp, color = TextMuted)
                            Text(child.groupText, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (child.isPresent) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (child.isPresent) "En Centro" else "Ausente",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (child.isPresent) Color(0xFF15803D) else TextMuted
                    )
                }
            }

            // Backend Data Card
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChildDetailRow(label = "Matrícula:", value = child.studentId ?: "—")
                    val birthAndAge = listOfNotNull(child.birthDate, child.ageText.ifBlank { null }).joinToString(" · ").ifEmpty { "—" }
                    ChildDetailRow(label = "Nacimiento:", value = birthAndAge)
                    if (!child.enrollmentDate.isNullOrBlank()) {
                        ChildDetailRow(label = "Inscripción:", value = child.enrollmentDate)
                    }
                    ChildDetailRow(label = "Grupo Sanguíneo:", value = child.bloodType ?: "—")
                    ChildDetailRow(
                        label = "Pediatra:",
                        value = child.pediatrician?.let { p ->
                            if (!child.pediatricianPhone.isNullOrBlank()) "$p (${child.pediatricianPhone})" else p
                        } ?: "—"
                    )
                    ChildDetailRow(
                        label = "Tel. Emergencia:",
                        value = child.emergencyPhone?.let { e ->
                            if (!child.emergencyPhoneRelation.isNullOrBlank()) "$e (${child.emergencyPhoneRelation})" else e
                        } ?: "—"
                    )
                    if (!child.motherName.isNullOrBlank() || !child.fatherName.isNullOrBlank()) {
                        val parentContacts = listOfNotNull(
                            child.motherName?.let { "Madre: $it ${child.motherPhone ?: ""}".trim() },
                            child.fatherName?.let { "Padre: $it ${child.fatherPhone ?: ""}".trim() }
                        ).joinToString(", ")
                        ChildDetailRow(label = "Familiares:", value = parentContacts)
                    }
                    if (child.authorizedPickups.isNotEmpty()) {
                        ChildDetailRow(label = "Autorizados:", value = child.authorizedPickups.joinToString(", "))
                    }
                    if (!child.medicalNotes.isNullOrBlank()) {
                        ChildDetailRow(label = "Notas Médicas:", value = child.medicalNotes)
                    }
                    if (!child.habitsNotes.isNullOrBlank()) {
                        ChildDetailRow(label = "Hábitos / Sueño:", value = child.habitsNotes)
                    }
                    if (!child.generalNotes.isNullOrBlank()) {
                        ChildDetailRow(label = "Notas Generales:", value = child.generalNotes)
                    }
                }
            }

            // Actions: Unlink / Edit
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onUnlink,
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar Alumno", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar Expediente", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ChildDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun AdminCreateChildDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        room: String,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var roomText by remember { mutableStateOf("Sala 1A") }
    var bloodType by remember { mutableStateOf("O+") }
    var pediatrician by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var medicalNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Alumno", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo del Menor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    label = { Text("Sala / Aula (ej. Sala 1A, Lactantes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Grupo Sanguíneo (ej. O+, A+, B+)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pediatrician,
                    onValueChange = { pediatrician = it },
                    label = { Text("Pediatra / Centro Médico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Teléfono de Emergencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Alergias o Notas Médicas") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onConfirm(
                            fullName.trim(),
                            roomText.trim().ifBlank { "Sala 1A" },
                            bloodType.trim().ifBlank { null },
                            pediatrician.trim().ifBlank { null },
                            emergencyPhone.trim().ifBlank { null },
                            medicalNotes.trim().ifBlank { null }
                        )
                    }
                },
                enabled = fullName.isNotBlank()
            ) {
                Text("Guardar y Añadir a la Familia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AdminEditChildDialog(
    child: AdminChildSummary,
    onDismiss: () -> Unit,
    onConfirm: (
        childId: String,
        fullName: String?,
        room: String?,
        bloodType: String?,
        pediatrician: String?,
        emergencyPhone: String?,
        medicalNotes: String?,
        allergyAlert: String?,
        birthDate: String?
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(child.fullName) }
    var roomText by remember { mutableStateOf(child.roomText) }
    var birthDate by remember { mutableStateOf(child.birthDate ?: "") }
    var bloodType by remember { mutableStateOf(child.bloodType ?: "") }
    var pediatrician by remember { mutableStateOf(child.pediatrician ?: "") }
    var emergencyPhone by remember { mutableStateOf(child.emergencyPhone ?: "") }
    var allergyAlert by remember { mutableStateOf(child.allergyAlert ?: "") }
    var medicalNotes by remember { mutableStateOf(child.medicalNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Expediente del Menor", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { },
                    label = { Text("Nombre Completo") },
                    supportingText = { Text("Registro oficial escolar (No modificable)", color = TextSecondary) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    label = { Text("Sala / Grupo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Fecha de Nacimiento (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Grupo Sanguíneo") },
                    placeholder = { Text("Ej: O+, A+, B+...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pediatrician,
                    onValueChange = { pediatrician = it },
                    label = { Text("Pediatra / Hospital") },
                    placeholder = { Text("Ej: Dra. Sofía Morales") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Teléfono de Emergencia") },
                    placeholder = { Text("+52 55 1234 5678") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Notas Médicas / Alergias y Cuidados") },
                    placeholder = { Text("Esquema de vacunación, alergias o cuidados...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        child.id,
                        fullName.trim(),
                        roomText.trim().ifBlank { null },
                        bloodType.trim().ifBlank { null },
                        pediatrician.trim().ifBlank { null },
                        emergencyPhone.trim().ifBlank { null },
                        medicalNotes.trim().ifBlank { null },
                        null,
                        birthDate.trim().ifBlank { null }
                    )
                }
            ) {
                Text("Guardar Expediente")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

