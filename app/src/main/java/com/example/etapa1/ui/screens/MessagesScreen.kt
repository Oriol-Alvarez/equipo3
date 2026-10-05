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
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.ui.components.CreateFamilyGroupDialog
import com.example.etapa1.ui.components.CreateWorkerGroupDialog
import com.example.etapa1.ui.components.MessagesMode
import com.example.etapa1.ui.components.MessagesModeSwitch
import com.example.etapa1.ui.components.MessagesSearchBar
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
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToMore: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val familyGroupsState by familyGroupsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        familyGroupsViewModel.useEducatorViewer()
    }

    MessagesContent(
        uiState = uiState,
        onModeSelect = { mode ->
            viewModel.onTabSelected(if (mode == MessagesMode.TRABAJADORES) 0 else 1)
        },
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::toggleSearch,
        onSendMessage = viewModel::sendMessage,
        onNavigateToHome = onNavigateToHome,
        onNavigateToChildChat = onNavigateToChildChat,
        onNavigateToMore = onNavigateToMore,
        familyGroups = familyGroupsState.groups,
        familyGroupCandidates = familyGroupsState.candidates,
        familyGroupError = familyGroupsState.errorMessage,
        onCreateFamilyGroup = familyGroupsViewModel::createGroup,
        onSendFamilyGroupMessage = familyGroupsViewModel::sendMessage,
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
        onNavigateToHome = onNavigateToHome,
        onNavigateToChildChat = onNavigateToChildChat,
        onNavigateToMore = onNavigateToMore
    )
}

@Composable
fun MessagesContent(
    uiState: com.example.etapa1.ui.state.MessagesUiState,
    onModeSelect: (MessagesMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSendMessage: (chatId: String, text: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
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
    onDismissFamilyGroupError: () -> Unit = {}
) {
    val selectedMode = if (uiState.selectedTab == 0) MessagesMode.TRABAJADORES else MessagesMode.FAMILIAS
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var activeWorkerChat by remember { mutableStateOf<WorkerChat?>(null) }
    val workerChats = remember(uiState.workerChats) { mutableStateListOf<WorkerChat>().apply { addAll(uiState.workerChats) } }
    var globalChat by remember(uiState.globalChat) { mutableStateOf(uiState.globalChat ?: MockDataRepository.initialGlobalWorkerChat) }
    val parentChats = uiState.parentChats
    var showCreateFamilyGroupDialog by rememberSaveable { mutableStateOf(false) }
    var activeFamilyGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleFamilyGroups = FamilyGroupsPresenter.filterByQuery(familyGroups, uiState.searchQuery)

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mensajes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = TextAlign.Center
                )

                // Botón Crear Grupo (arriba izquierda): de trabajadores o de familias según el modo
                IconButton(
                    onClick = {
                        if (selectedMode == MessagesMode.TRABAJADORES) {
                            showCreateGroupDialog = true
                        } else {
                            onDismissFamilyGroupError()
                            showCreateFamilyGroupDialog = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = if (selectedMode == MessagesMode.TRABAJADORES) {
                            "Crear grupo de trabajadores"
                        } else {
                            "Crear grupo con familias"
                        },
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Botón Lupa / Búsqueda (arriba derecha)
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
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

                // Switch / Segmented Control
                MessagesModeSwitch(
                    selectedMode = selectedMode,
                    onModeChange = onModeSelect,
                    workersUnreadCount = (uiState.globalChat?.unreadCount ?: 0) + workerChats.sumOf { it.unreadCount },
                    parentsUnreadCount = parentChats.sumOf { it.unreadCount }
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
                        // 1. Chat Global de Trabajadores
                        uiState.globalChat?.let { gChat ->
                            item(key = "pinned_global_chat") {
                                PinnedGlobalWorkerChatCard(
                                    chat = gChat,
                                    onClick = { activeWorkerChat = gChat }
                                )
                            }
                        }

                        // Subtítulo de sección
                        item(key = "workers_section_header") {
                            Text(
                                text = "CHATS DIRECTOS Y GRUPOS TRABAJADORES (${workerChats.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }

                        // 2. Lista de chats individuales y de grupos
                        items(workerChats, key = { it.id }) { chat ->
                            WorkerChatItem(
                                chat = chat,
                                onClick = { activeWorkerChat = chat }
                            )
                        }
                    }

                    MessagesMode.FAMILIAS -> {
                        // Grupos con familias creados por la educadora
                        item(key = "family_groups_section_header") {
                            Text(
                                text = "GRUPOS CON FAMILIAS (${visibleFamilyGroups.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }

                        if (visibleFamilyGroups.isEmpty()) {
                            item(key = "family_groups_empty") {
                                Text(
                                    text = "Toca el botón de grupo arriba a la izquierda para crear uno.",
                                    fontSize = 12.5.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        items(visibleFamilyGroups, key = { "family_group_${it.id}" }) { group ->
                            WorkerChatItem(
                                chat = group,
                                onClick = { activeFamilyGroupId = group.id }
                            )
                        }

                        // Subtítulo de sección con indicador de sala
                        item(key = "parents_section_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CHATS DIRECTOS CON FAMILIAS (${parentChats.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )

                            }
                        }

                        // 3. Lista de chats con los padres
                        items(parentChats, key = { it.child.id }) { chatSummary ->
                            ParentChatItem(
                                chatSummary = chatSummary,
                                onClick = { onNavigateToChildChat(chatSummary.child) }
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
            staffList = MockDataRepository.staffMembers,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { groupTitle, selectedIds ->
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

    // Conversación interactiva de chat de trabajadores
    activeWorkerChat?.let { currentChat ->
        WorkerChatConversationDialog(
            chat = currentChat,
            onDismiss = { activeWorkerChat = null },
            onSendMessage = { newText, uri, name, mime ->
                val newMsg = WorkerChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    senderId = "me",
                    senderName = "Yo",
                    message = newText,
                    time = "Ahora",
                    isOutgoing = true,
                    fileUri = uri?.toString(),
                    fileName = name,
                    fileMimeType = mime
                )
                val updatedMessages = currentChat.messages + newMsg
                val updatedChat = currentChat.copy(
                    messages = updatedMessages,
                    lastMessage = if (newText.isNotBlank()) "Tú: $newText" else "Tú: [Archivo adjunto]",
                    lastMessageTime = "Ahora",
                    unreadCount = 0
                )
                if (currentChat.isGlobal) {
                    globalChat = updatedChat
                } else {
                    val index = workerChats.indexOfFirst { it.id == currentChat.id }
                    if (index >= 0) {
                        workerChats[index] = updatedChat
                    }
                }
                activeWorkerChat = updatedChat
                if (newText.isNotBlank()) {
                    onSendMessage(currentChat.id, newText)
                }
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

    // Conversación del grupo con familias (se busca por id para mostrar siempre la versión más reciente)
    val activeFamilyGroup = familyGroups.find { it.id == activeFamilyGroupId }
    if (activeFamilyGroup != null) {
        WorkerChatConversationDialog(
            chat = activeFamilyGroup,
            onDismiss = { activeFamilyGroupId = null },
            onSendMessage = { text, uri, name, mime ->
                onSendFamilyGroupMessage(activeFamilyGroup.id, text, uri?.toString(), name, mime)
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
