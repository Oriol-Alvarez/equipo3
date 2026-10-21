package com.example.etapa1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.FamilyGroupCandidate
import androidx.compose.ui.graphics.Color
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.model.WorkerMember
import com.example.etapa1.ui.components.ActiveChildSwitcher
import com.example.etapa1.ui.components.CreateFamilyGroupDialog
import com.example.etapa1.ui.components.CreateWorkerGroupDialog
import com.example.etapa1.ui.components.MessagesMode
import com.example.etapa1.ui.components.MessagesModeSwitch
import com.example.etapa1.ui.components.MessagesSearchBar
import com.example.etapa1.ui.components.NewChatCandidateItem
import com.example.etapa1.ui.components.ParentChatItem
import com.example.etapa1.ui.components.PinnedGlobalWorkerChatCard
import com.example.etapa1.ui.components.WorkerChatConversationDialog
import com.example.etapa1.ui.components.WorkerChatItem
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.TextMuted
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.state.FamilyGroupsPresenter
import com.example.etapa1.ui.state.FamilyGroupsViewModel
import com.example.etapa1.ui.state.MessagesViewModel

import androidx.compose.runtime.collectAsState

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    familyGroupsViewModel: FamilyGroupsViewModel,
    isFamiliar: Boolean = false,
    activeChild: Child? = null,
    onSwitchChild: (() -> Unit)? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToWorkerChat: (WorkerChat) -> Unit = {},
    onNavigateToMore: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val familyGroupsState by familyGroupsViewModel.uiState.collectAsState()

    LaunchedEffect(isFamiliar) {
        viewModel.setWorkerRole(!isFamiliar)
        viewModel.recargar()
        if (isFamiliar) {
            familyGroupsViewModel.useFamilyViewer(activeChild?.let { listOf(it.id) })
        } else {
            familyGroupsViewModel.useEducatorViewer()
        }
    }

    MessagesContent(
        uiState = uiState,
        activeChild = activeChild,
        onSwitchChild = onSwitchChild,
        onModeSelect = { mode ->
            viewModel.onTabSelected(if (mode == MessagesMode.TRABAJADORES) 0 else 1)
        },
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::toggleSearch,
        onSendMessage = viewModel::sendMessage,
        onMarkWorkerChatAsRead = viewModel::markWorkerChatAsRead,
        onMarkParentChatAsRead = viewModel::markParentChatAsRead,
        onCreateWorkerGroup = viewModel::createWorkerGroup,
        onStartNewWorkerChat = viewModel::startNewWorkerChat,
        onStartNewFamilyEducatorChat = viewModel::startNewFamilyEducatorChat,
        onStartNewParentChat = viewModel::startNewParentChat,
        onNavigateToHome = onNavigateToHome,
        onNavigateToChildChat = onNavigateToChildChat,
        onNavigateToWorkerChat = onNavigateToWorkerChat,
        onNavigateToMore = onNavigateToMore,
        familyGroups = familyGroupsState.groups,
        familyGroupCandidates = familyGroupsState.candidates,
        familyGroupError = familyGroupsState.errorMessage,
        onCreateFamilyGroup = familyGroupsViewModel::createGroup,
        onSendFamilyGroupMessage = familyGroupsViewModel::sendMessage,
        onSendFamilyGroupActivity = familyGroupsViewModel::sendGroupActivity,
        onDismissFamilyGroupError = familyGroupsViewModel::clearError
    )
}

@Composable
fun MessagesScreen(
    children: List<Child> = MockDataRepository.childrenSala1A,
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToMore: () -> Unit
) {
    val state = com.example.etapa1.ui.state.MessagesUiState(
        workerChats = MockDataRepository.getInitialWorkerChats(),
        parentChats = MockDataRepository.getInitialParentChats(),
        staffMembers = MockDataRepository.staffMembers,
        globalChat = MockDataRepository.initialGlobalWorkerChat
    )

    MessagesContent(
        uiState = state,
        onModeSelect = {},
        onSearchQueryChange = {},
        onToggleSearch = {},
        onSendMessage = { _, _ -> },
        onMarkWorkerChatAsRead = {},
        onMarkParentChatAsRead = {},
        onNavigateToHome = onNavigateToHome,
        onNavigateToChildChat = onNavigateToChildChat,
        onNavigateToMore = onNavigateToMore
    )
}

@Composable
fun MessagesContent(
    uiState: com.example.etapa1.ui.state.MessagesUiState,
    activeChild: Child? = null,
    onSwitchChild: (() -> Unit)? = null,
    onModeSelect: (MessagesMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSendMessage: (chatId: String, text: String) -> Unit,
    onMarkWorkerChatAsRead: (chatId: String) -> Unit = {},
    onMarkParentChatAsRead: (childId: String) -> Unit = {},
    /** Crea el grupo en el servidor; null = solo en pantalla (vista previa). */
    onCreateWorkerGroup: ((title: String, memberIds: List<String>) -> Unit)? = null,
    onStartNewWorkerChat: (WorkerMember) -> WorkerChat = { WorkerChat(id = it.id, title = it.name, subtitle = it.role, lastMessage = "Sin mensajes aún.", lastMessageTime = "Ahora") },
    onStartNewFamilyEducatorChat: (WorkerMember) -> WorkerChat = { WorkerChat(id = it.id, title = it.name, subtitle = it.role, lastMessage = "Sin mensajes aún.", lastMessageTime = "Ahora") },
    onStartNewParentChat: (Child) -> ParentChatSummary = { ParentChatSummary(it, "Familiar Tutor", "Sin mensajes aún.", "Ahora", 0) },
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToWorkerChat: (WorkerChat) -> Unit = {},
    onNavigateToMore: () -> Unit,
    familyGroups: List<WorkerChat> = emptyList(),
    familyGroupCandidates: List<FamilyGroupCandidate> = emptyList(),
    familyGroupError: String? = null,
    onCreateFamilyGroup: (title: String, childIds: List<String>) -> Boolean = { _, _ -> false },
    onSendFamilyGroupMessage: (
        groupId: String,
        text: String,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onSendFamilyGroupActivity: (
        groupId: String,
        category: String,
        badge: String,
        description: String
    ) -> Unit = { _, _, _, _ -> },
    onDismissFamilyGroupError: () -> Unit = {}
) {
    val selectedMode = if (uiState.selectedTab == 0) MessagesMode.TRABAJADORES else MessagesMode.FAMILIAS
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val workerChats = remember(uiState.workerChats) { mutableStateListOf<WorkerChat>().apply { addAll(uiState.workerChats) } }
    var globalChat by remember(uiState.globalChat) { mutableStateOf(uiState.globalChat ?: MockDataRepository.initialGlobalWorkerChat) }
    val parentChats = remember(uiState.parentChats) { mutableStateListOf<ParentChatSummary>().apply { addAll(uiState.parentChats) } }

    LaunchedEffect(uiState.workerChats) {
        workerChats.clear()
        workerChats.addAll(uiState.workerChats)
    }

    LaunchedEffect(uiState.globalChat) {
        uiState.globalChat?.let { globalChat = it }
    }

    LaunchedEffect(uiState.parentChats) {
        parentChats.clear()
        parentChats.addAll(uiState.parentChats)
    }
    var showCreateFamilyGroupDialog by rememberSaveable { mutableStateOf(false) }
    val visibleFamilyGroups = FamilyGroupsPresenter.filterByQuery(familyGroups, uiState.searchQuery)

    // Familia: solo se muestra el chat con la educadora del hijo activo (no el de sus otros hijos)
    val visibleStaffChats = if (!uiState.isWorkerRole && activeChild != null && uiState.searchQuery.isBlank()) {
        workerChats.filter { chat ->
            !chat.id.startsWith("fchat_") || chat.id == "fchat_${activeChild.id}"
        }
    } else {
        workerChats
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón arriba izquierda: Crear Grupo para trabajadora, o Buscar para familiar
                Box(
                    modifier = Modifier.widthIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.isWorkerRole) {
                        IconButton(
                            onClick = {
                                if (selectedMode == MessagesMode.TRABAJADORES) {
                                    showCreateGroupDialog = true
                                } else {
                                    onDismissFamilyGroupError()
                                    showCreateFamilyGroupDialog = true
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = if (selectedMode == MessagesMode.TRABAJADORES) {
                                    "Crear grupo de trabajadores"
                                } else {
                                    "Crear grupo con familias"
                                },
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Centro: Título de la pantalla
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mensajes",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Botón arriba derecha: Buscar para trabajadora, o ActiveChildSwitcher para familiar
                Box(
                    modifier = Modifier.widthIn(min = 40.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (uiState.isWorkerRole) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else if (activeChild != null && onSwitchChild != null) {
                        ActiveChildSwitcher(
                            child = activeChild,
                            onSwitchClick = onSwitchChild
                        )
                    }
                }
            }

            // Barra de búsqueda y Switch de modo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                // Campo de búsqueda desplegable
                AnimatedVisibility(visible = uiState.isSearchOpen) {
                    MessagesSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onClose = onToggleSearch,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val workersUnread = if (uiState.isWorkerRole) {
                    (if (globalChat.isGlobal) globalChat.unreadCount else 0) + workerChats.sumOf { it.unreadCount }
                } else {
                    visibleStaffChats.sumOf { it.unreadCount }
                }

                val parentsUnread = if (uiState.isWorkerRole) {
                    parentChats.sumOf { it.unreadCount }
                } else {
                    visibleFamilyGroups.sumOf { it.unreadCount }
                }

                // Switch / Segmented Control
                MessagesModeSwitch(
                    selectedMode = selectedMode,
                    onModeChange = onModeSelect,
                    workersUnreadCount = workersUnread,
                    parentsUnreadCount = parentsUnread,
                    workerTabLabel = if (uiState.isWorkerRole) "Personal" else "Educador",
                    familyTabLabel = if (uiState.isWorkerRole) "Familias y Alumnos" else "Grupos"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de contenido según el modo seleccionado
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedMode) {
                    MessagesMode.TRABAJADORES -> {
                        // 1. Chat Global de Trabajadores (solo para personal)
                        if (uiState.isWorkerRole) {
                            item(key = "pinned_global_chat") {
                                PinnedGlobalWorkerChatCard(
                                    chat = globalChat,
                                    onClick = {
                                        val readChat = globalChat.copy(unreadCount = 0)
                                        globalChat = readChat
                                        onMarkWorkerChatAsRead(readChat.id)
                                        onNavigateToWorkerChat(readChat)
                                    }
                                )
                            }
                        }

                        // Sección: Contactos nuevos para iniciar chat cuando se busca
                        if (uiState.isSearchOpen && uiState.availableStaffToChat.isNotEmpty()) {
                            item(key = "new_staff_candidates_header") {
                                Text(
                                    text = if (uiState.isWorkerRole) "INICIAR NUEVO CHAT CON PERSONAL" else "INICIAR NUEVO CHAT CON EDUCADOR",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(uiState.availableStaffToChat, key = { "candidate_staff_${it.id}" }) { member ->
                                NewChatCandidateItem(
                                    title = member.name,
                                    subtitle = member.role,
                                    avatarInitials = member.avatarInitials,
                                    avatarBgColor = Color(member.avatarBgColor),
                                    badgeText = "Iniciar chat",
                                    onClick = {
                                        val educatorName = activeChild?.educatorName.orEmpty()
                                        if (!uiState.isWorkerRole && activeChild != null &&
                                            educatorName.isNotBlank() && member.name.equals(educatorName, ignoreCase = true)) {
                                            onNavigateToChildChat(activeChild)
                                        } else {
                                            val newChat = if (uiState.isWorkerRole) {
                                                onStartNewWorkerChat(member)
                                            } else {
                                                onStartNewFamilyEducatorChat(member)
                                            }
                                            val idx = workerChats.indexOfFirst { it.id == newChat.id }
                                            if (idx < 0) {
                                                workerChats.add(0, newChat)
                                            }
                                            onNavigateToWorkerChat(newChat)
                                        }
                                    }
                                )
                            }
                        }

                        // Si la educadora busca alumnos / familias desde esta pestaña (ej. Mateo)
                        if (uiState.isWorkerRole && uiState.isSearchOpen && parentChats.isNotEmpty()) {
                            item(key = "search_worker_parent_chats_header") {
                                Text(
                                    text = "CHATS CON FAMILIAS Y ALUMNOS (${parentChats.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(parentChats, key = { "worker_search_parent_${it.child.id}" }) { chatSummary ->
                                ParentChatItem(
                                    chatSummary = chatSummary,
                                    onClick = {
                                        val idx = parentChats.indexOfFirst { it.child.id == chatSummary.child.id }
                                        if (idx >= 0) {
                                            parentChats[idx] = chatSummary.copy(unreadCount = 0)
                                        }
                                        onMarkParentChatAsRead(chatSummary.child.id)
                                        onNavigateToChildChat(chatSummary.child)
                                    }
                                )
                            }
                        }

                        if (uiState.isWorkerRole && uiState.isSearchOpen && uiState.availableChildrenToChat.isNotEmpty()) {
                            item(key = "search_worker_new_parent_candidates_header") {
                                Text(
                                    text = "INICIAR NUEVO CHAT CON FAMILIA",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(uiState.availableChildrenToChat, key = { "worker_search_cand_child_${it.id}" }) { child ->
                                NewChatCandidateItem(
                                    title = "${child.fullName} (${child.roomText})",
                                    subtitle = "Tutor legal • Iniciar conversación",
                                    avatarInitials = child.shortName.take(2).uppercase(),
                                    avatarBgColor = Color(0xFF1976D2),
                                    badgeText = "Iniciar chat",
                                    onClick = {
                                        val newChatSummary = onStartNewParentChat(child)
                                        val idx = parentChats.indexOfFirst { it.child.id == child.id }
                                        if (idx < 0) {
                                            parentChats.add(0, newChatSummary)
                                        }
                                        onNavigateToChildChat(child)
                                    }
                                )
                            }
                        }

                        // Subtítulo de sección
                        item(key = "workers_section_header") {
                            Text(
                                text = if (uiState.isWorkerRole) {
                                    "CHATS DIRECTOS Y GRUPOS TRABAJADORES (${workerChats.size})"
                                } else {
                                    "CHATS CON EDUCADORES Y PERSONAL (${visibleStaffChats.size})"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }

                        // 2. Lista de chats individuales y de grupos
                        items(visibleStaffChats, key = { it.id }) { chat ->
                            WorkerChatItem(
                                chat = chat,
                                onClick = {
                                    val educatorName = activeChild?.educatorName.orEmpty()
                                    val isAssignedEducatorChat = !uiState.isWorkerRole && activeChild != null &&
                                        (chat.id == "fchat_${activeChild.id}" ||
                                         (educatorName.isNotBlank() && chat.title.equals(educatorName, ignoreCase = true)))

                                    val readChat = chat.copy(unreadCount = 0)
                                    val idx = workerChats.indexOfFirst { it.id == chat.id }
                                    if (idx >= 0) {
                                        workerChats[idx] = readChat
                                    }
                                    onMarkWorkerChatAsRead(chat.id)

                                    if (isAssignedEducatorChat) {
                                        onNavigateToChildChat(activeChild)
                                    } else {
                                        onNavigateToWorkerChat(readChat)
                                    }
                                }
                            )
                        }
                    }

                    MessagesMode.FAMILIAS -> {
                        // Si es familiar y busca personal/educador en esta pestaña, también mostrárselo
                        if (!uiState.isWorkerRole && uiState.isSearchOpen && uiState.availableStaffToChat.isNotEmpty()) {
                            item(key = "new_staff_candidates_family_header") {
                                Text(
                                    text = "INICIAR NUEVO CHAT CON EDUCADOR",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(uiState.availableStaffToChat, key = { "candidate_staff_fam_${it.id}" }) { member ->
                                NewChatCandidateItem(
                                    title = member.name,
                                    subtitle = member.role,
                                    avatarInitials = member.avatarInitials,
                                    avatarBgColor = Color(member.avatarBgColor),
                                    badgeText = "Iniciar chat",
                                    onClick = {
                                        val educatorName = activeChild?.educatorName.orEmpty()
                                        if (!uiState.isWorkerRole && activeChild != null &&
                                            educatorName.isNotBlank() && member.name.equals(educatorName, ignoreCase = true)) {
                                            onNavigateToChildChat(activeChild)
                                        } else {
                                            val newChat = onStartNewFamilyEducatorChat(member)
                                            val idx = workerChats.indexOfFirst { it.id == newChat.id }
                                            if (idx < 0) {
                                                workerChats.add(0, newChat)
                                            }
                                            onNavigateToWorkerChat(newChat)
                                        }
                                    }
                                )
                            }
                        }

                        // Si es educadora y busca familias que no están en la lista
                        if (uiState.isWorkerRole && uiState.isSearchOpen && uiState.availableChildrenToChat.isNotEmpty()) {
                            item(key = "new_parent_candidates_header") {
                                Text(
                                    text = "INICIAR NUEVO CHAT CON FAMILIA",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(uiState.availableChildrenToChat, key = { "candidate_child_${it.id}" }) { child ->
                                NewChatCandidateItem(
                                    title = "${child.fullName} (${child.roomText})",
                                    subtitle = "Tutor legal • Iniciar conversación",
                                    avatarInitials = child.shortName.take(2).uppercase(),
                                    avatarBgColor = Color(0xFF1976D2),
                                    badgeText = "Iniciar chat",
                                    onClick = {
                                        val newChatSummary = onStartNewParentChat(child)
                                        val idx = parentChats.indexOfFirst { it.child.id == child.id }
                                        if (idx < 0) {
                                            parentChats.add(0, newChatSummary)
                                        }
                                        onNavigateToChildChat(child)
                                    }
                                )
                            }
                        }

                        // Subtítulo de sección con indicador de sala (para personal con chats directos con familias y alumnos)
                        if (uiState.isWorkerRole) {
                            item(key = "parents_section_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CHATS DIRECTOS CON FAMILIAS Y ALUMNOS (${parentChats.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Lista de chats con los padres y alumnos
                            items(parentChats, key = { it.child.id }) { chatSummary ->
                                ParentChatItem(
                                    chatSummary = chatSummary,
                                    onClick = {
                                        val idx = parentChats.indexOfFirst { it.child.id == chatSummary.child.id }
                                        if (idx >= 0) {
                                            parentChats[idx] = chatSummary.copy(unreadCount = 0)
                                        }
                                        onMarkParentChatAsRead(chatSummary.child.id)
                                        onNavigateToChildChat(chatSummary.child)
                                    }
                                )
                            }
                        }

                        // Grupos con familias creados por la educadora
                        item(key = "family_groups_section_header") {
                            Text(
                                text = "GRUPOS CON FAMILIAS (${visibleFamilyGroups.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }

                        if (visibleFamilyGroups.isEmpty()) {
                            item(key = "family_groups_empty") {
                                Text(
                                    text = if (uiState.isWorkerRole) "Toca el botón de grupo arriba a la izquierda para crear uno." else "No perteneces a ningún grupo familiar por el momento.",
                                    fontSize = 12.5.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        items(visibleFamilyGroups, key = { "family_group_${it.id}" }) { group ->
                            WorkerChatItem(
                                chat = group,
                                onClick = { onNavigateToWorkerChat(group) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de Creación de Grupo
    if (showCreateGroupDialog) {
        CreateWorkerGroupDialog(
            staffList = uiState.staffMembers,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = create@{ groupTitle, selectedIds ->
                if (onCreateWorkerGroup != null) {
                    onCreateWorkerGroup(groupTitle, selectedIds)
                    showCreateGroupDialog = false
                    return@create
                }
                val newGroup = WorkerChat(
                    id = "group_${System.currentTimeMillis()}",
                    title = groupTitle,
                    subtitle = "${selectedIds.size} miembros • Grupo de personal",
                    lastMessage = "Grupo creado por el educador.",
                    lastMessageTime = "Ahora",
                    unreadCount = 0,
                    isGlobal = false,
                    isGroup = true,
                    membersCount = selectedIds.size,
                    avatarInitials = groupTitle.take(2).uppercase(),
                    avatarBgColor = 0xFF00897B,
                    messages = listOf(
                        WorkerChatMessage(
                            id = "msg_${System.currentTimeMillis()}",
                            senderId = "me",
                            senderName = "Yo",
                            message = "Grupo '$groupTitle' creado con éxito.",
                            time = "Ahora",
                            isOutgoing = true
                        )
                    )
                )
                workerChats.add(0, newGroup)
                showCreateGroupDialog = false
            }
        )
    }

    // Modal de creación de grupo con familias
    if (showCreateFamilyGroupDialog) {
        CreateFamilyGroupDialog(
            candidates = familyGroupCandidates,
            errorMessage = familyGroupError,
            onDismiss = { showCreateFamilyGroupDialog = false },
            onCreateGroup = { title, childIds ->
                if (onCreateFamilyGroup(title, childIds)) {
                    showCreateFamilyGroupDialog = false
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesScreenPreview() {
    Etapa1Theme {
        MessagesScreen(
            children = MockDataRepository.childrenSala1A,
            onNavigateToHome = {},
            onNavigateToChildChat = {},
            onNavigateToMore = {}
        )
    }
}
