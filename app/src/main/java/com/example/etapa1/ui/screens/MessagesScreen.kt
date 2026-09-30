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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
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
import com.example.etapa1.ui.state.MessagesViewModel

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToMore: () -> Unit
) {
    MessagesScreen(
        onNavigateToHome = onNavigateToHome,
        onNavigateToChildChat = onNavigateToChildChat,
        onNavigateToMore = onNavigateToMore
    )
}

@Composable
fun MessagesScreen(
    children: List<Child> = MockDataRepository.childrenSala1A,
    onNavigateToHome: () -> Unit,
    onNavigateToChildChat: (Child) -> Unit,
    onNavigateToMore: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(MessagesMode.TRABAJADORES) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var activeWorkerChat by remember { mutableStateOf<WorkerChat?>(null) }

    // Estados de chats de trabajadores
    var globalChat by remember { mutableStateOf(MockDataRepository.initialGlobalWorkerChat) }
    val workerChats = remember {
        mutableStateListOf<WorkerChat>().apply {
            addAll(MockDataRepository.getInitialWorkerChats())
        }
    }

    // Estados de chats de familias
    val parentChats = remember {
        mutableStateListOf<ParentChatSummary>().apply {
            addAll(MockDataRepository.getInitialParentChats())
        }
    }

    // Filtrado de trabajadores
    val filteredWorkerChats = remember(searchQuery, workerChats.size) {
        if (searchQuery.isBlank()) workerChats
        else workerChats.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    val showGlobalChat = remember(searchQuery) {
        searchQuery.isBlank() ||
                globalChat.title.contains(searchQuery, ignoreCase = true) ||
                globalChat.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    // Filtrado de familias
    val filteredParentChats = remember(searchQuery, parentChats.size) {
        if (searchQuery.isBlank()) parentChats
        else parentChats.filter {
            it.child.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.parentName.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
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
            // Header Top Bar - Centrado 
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

                // Botón Crear Grupo de Trabajadores (arriba izquierda)
                if (selectedMode == MessagesMode.TRABAJADORES) {
                    IconButton(
                        onClick = { showCreateGroupDialog = true },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Crear Grupo",
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Botón Lupa / Búsqueda (arriba derecha)
                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
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
                AnimatedVisibility(visible = isSearchOpen) {
                    MessagesSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClose = {
                            searchQuery = ""
                            isSearchOpen = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Switch / Segmented Control
                MessagesModeSwitch(
                    selectedMode = selectedMode,
                    onModeChange = { selectedMode = it },
                    workersUnreadCount = globalChat.unreadCount + workerChats.sumOf { it.unreadCount },
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
                        // 1. Chat Global de Trabajadores (Anclado y diferenciado estéticamente)
                        if (showGlobalChat) {
                            item(key = "pinned_global_chat") {
                                PinnedGlobalWorkerChatCard(
                                    chat = globalChat,
                                    onClick = { activeWorkerChat = globalChat }
                                )
                            }
                        }

                        // Subtítulo de sección
                        item(key = "workers_section_header") {
                            Text(
                                text = "CHATS DIRECTOS Y GRUPOS TRABAJADORES (${filteredWorkerChats.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }

                        // 2. Lista de chats individuales y de grupos
                        items(filteredWorkerChats, key = { it.id }) { chat ->
                            WorkerChatItem(
                                chat = chat,
                                onClick = { activeWorkerChat = chat }
                            )
                        }
                    }

                    MessagesMode.FAMILIAS -> {
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
                                    text = "CHATS DIRECTOS Y GRUPOS FAMILIARES (${filteredParentChats.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )

                            }
                        }

                        // 3. Lista de chats con los padres (redirigen a la bitácora)
                        items(filteredParentChats, key = { it.child.id }) { chatSummary ->
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
            onSendMessage = { newText ->
                val newMsg = WorkerChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    senderId = "me",
                    senderName = "Yo",
                    message = newText,
                    time = "Ahora",
                    isOutgoing = true
                )
                val updatedMessages = currentChat.messages + newMsg
                val updatedChat = currentChat.copy(
                    messages = updatedMessages,
                    lastMessage = "Tú: $newText",
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
