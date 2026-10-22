package com.example.etapa1.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import com.example.etapa1.model.TimelineItem
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.ParentChatSummary
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.model.WorkerMember
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.ChatOutgoingBubble
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

enum class MessagesMode(val label: String) {
    TRABAJADORES("Educador"),
    FAMILIAS("Familiar")
}

/**
 * Badge circular para indicar cantidad de mensajes no leídos,
 * con el número perfectamente centrado en el círculo.
 */
@Composable
fun UnreadBadgeCircle(
    count: Int,
    backgroundColor: Color = AlertRed,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeight = 11.sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

/**
 * Selector de modo (Switch / Segmented Control) entre Trabajadores y Familias
 */
@Composable
fun MessagesModeSwitch(
    selectedMode: MessagesMode,
    onModeChange: (MessagesMode) -> Unit,
    workersUnreadCount: Int = 0,
    parentsUnreadCount: Int = 0,
    workerTabLabel: String = "Personal",
    familyTabLabel: String = "Familias",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(BrandBlueContainer)
            .border(1.dp, Color(0xFFD0E1F9), RoundedCornerShape(30.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MessagesMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                val unread = if (mode == MessagesMode.TRABAJADORES) workersUnreadCount else parentsUnreadCount
                val label = if (mode == MessagesMode.TRABAJADORES) workerTabLabel else familyTabLabel

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isSelected) BrandBlue else Color.Transparent)
                        .clickable { onModeChange(mode) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else BrandBlue
                        )

                        if (unread > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            UnreadBadgeCircle(
                                count = unread,
                                backgroundColor = AlertRed
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra de búsqueda con animación de visibilidad
 */
@Composable
fun MessagesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Buscar persona, rol o niño...",
                fontSize = 13.sp,
                color = TextMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = BrandBlue,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar búsqueda",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = BorderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}

/**
 * Tarjeta anclada arriba del Chat Global de Trabajadores (diferenciada estéticamente)
 */
@Composable
fun PinnedGlobalWorkerChatCard(
    chat: WorkerChat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, BrandBlue, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6FE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar distintivo del Chat Global
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
                    .border(2.dp, Color(0xFF90CAF9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Grupo Global",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = formatActivityPreview(chat.lastMessage.ifBlank { "Canal general activo" }),
                    fontSize = 12.5.sp,
                    color = if (chat.unreadCount > 0) TextPrimary else TextSecondary,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Columna lateral derecha: Fecha arriba derecha, círculo de mensajes no leídos debajo
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chat.lastMessageTime,
                    fontSize = 11.5.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (chat.unreadCount > 0) BrandBlue else TextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (chat.unreadCount > 0) {
                    UnreadBadgeCircle(
                        count = chat.unreadCount,
                        backgroundColor = AlertRed
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Fila de chat individual o grupal de trabajador
 */
@Composable
fun WorkerChatItem(
    chat: WorkerChat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnread = chat.unreadCount > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales o icono de grupo (sin indicador debajo a la derecha)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(chat.avatarBgColor)),
                contentAlignment = Alignment.Center
            ) {
                if (chat.isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Grupo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = chat.avatarInitials,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido central: Nombre y último mensaje
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = formatActivityPreview(chat.lastMessage.ifBlank { "Sin mensajes aún." }),
                    fontSize = 12.5.sp,
                    color = if (hasUnread) TextPrimary else TextSecondary,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Columna lateral derecha: Fecha arriba, círculo de mensajes no leídos debajo
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chat.lastMessageTime,
                    fontSize = 11.5.sp,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (hasUnread) BrandBlue else TextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (hasUnread) {
                    UnreadBadgeCircle(
                        count = chat.unreadCount,
                        backgroundColor = AlertRed
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Fila de chat con familias / padres (mismo formato que trabajadores)
 */
@Composable
fun ParentChatItem(
    chatSummary: ParentChatSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnread = chatSummary.unreadCount > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales (sin indicador debajo a la derecha)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(chatSummary.child.avatarBgColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chatSummary.child.avatarInitials,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido central: Nombre y último mensaje
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatSummary.child.fullName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = formatActivityPreview(chatSummary.lastMessage.ifBlank { "Sin mensajes aún." }),
                    fontSize = 12.5.sp,
                    color = if (hasUnread) TextPrimary else TextSecondary,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Columna lateral derecha: Fecha arriba, círculo de mensajes no leídos debajo
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chatSummary.lastMessageTime,
                    fontSize = 11.5.sp,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (hasUnread) BrandBlue else TextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (hasUnread) {
                    UnreadBadgeCircle(
                        count = chatSummary.unreadCount,
                        backgroundColor = AlertRed
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Diálogo para crear un nuevo grupo de trabajadores
 */
@Composable
fun CreateWorkerGroupDialog(
    staffList: List<WorkerMember>,
    onDismiss: () -> Unit,
    onCreateGroup: (title: String, selectedIds: List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedStaffIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuevo Grupo de Trabajadores",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nombre del Grupo", fontSize = 12.sp) },
                    placeholder = { Text("Ej. Educadoras Sala Lactantes", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Seleccionar Miembros:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(staffList) { staff ->
                        val isChecked = selectedStaffIds.contains(staff.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) selectedStaffIds.remove(staff.id)
                                    else selectedStaffIds.add(staff.id)
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedStaffIds.add(staff.id)
                                    else selectedStaffIds.remove(staff.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(staff.avatarBgColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = staff.avatarInitials,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = staff.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = staff.role,
                                    fontSize = 10.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onCreateGroup(groupName.trim(), selectedStaffIds.toList())
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedStaffIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Crear Grupo", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para ver y enviar mensajes en un chat de trabajadores,
 * replicando exactamente la estructura y estética del chat de bitácora infantil.
 */
@Composable
fun WorkerChatConversationDialog(
    chat: WorkerChat,
    onDismiss: () -> Unit,
    canShareActivity: Boolean = true,
    onSendMessage: (String, Uri?, String?, String?) -> Unit,
    onSendActivity: ((category: String, badge: String, description: String) -> Unit)? = null
) {
    var avisoChat by remember { mutableStateOf<AvisoChat?>(null) }
    when (avisoChat) {
        AvisoChat.LLAMADA -> AvisoDialog(
            titulo = "Llamadas",
            texto = "Las llamadas entre el personal todavía no están disponibles. Por ahora usa los mensajes de este chat.",
            onDismiss = { avisoChat = null }
        )
        AvisoChat.INFORMACION -> AvisoDialog(
            titulo = chat.title,
            texto = buildString {
                append(chat.subtitle)
                if (chat.isGroup && chat.membersCount > 0) append("\n${chat.membersCount} participantes")
                append("\n${chat.messages.size} mensajes en la conversación")
            },
            onDismiss = { avisoChat = null }
        )
        null -> Unit
    }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showShareActivityDialog by remember { mutableStateOf(false) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileMimeType by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            selectedFileUri = currentPhotoUri
            selectedFileName = "Foto_tomada.jpg"
            selectedFileMimeType = "image/jpeg"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileMimeType = context.contentResolver.getType(it)
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    selectedFileName = cursor.getString(nameIndex)
                }
            }
        }
    }

    // Auto scroll to bottom when messages change
    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) {
            listState.animateScrollToItem(chat.messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = AppBackground,
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                // Barra de entrada de mensajes
                Surface(
                    color = CardBackground,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (selectedFileUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedFileMimeType?.startsWith("image/") == true) {
                                    AsyncImage(
                                        model = selectedFileUri,
                                        contentDescription = "Vista previa",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedFileName ?: "Archivo seleccionado",
                                    fontSize = 14.sp,
                                    color = BrandBlue,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                IconButton(onClick = { 
                                    selectedFileUri = null 
                                    selectedFileName = null
                                    selectedFileMimeType = null
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar archivo",
                                        tint = AlertRed
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                val photoFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                currentPhotoUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                takePictureLauncher.launch(currentPhotoUri!!)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Tomar foto",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Adjuntar archivo",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (canShareActivity) {
                                IconButton(onClick = { showShareActivityDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                                        contentDescription = "Compartir actividad",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = {
                                    Text(
                                        text = "Escribe un mensaje...",
                                        fontSize = 14.sp,
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppBackground,
                                    unfocusedContainerColor = AppBackground,
                                    focusedBorderColor = BrandBlue,
                                    unfocusedBorderColor = BorderSubtle
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() || selectedFileUri != null) {
                                        onSendMessage(messageText, selectedFileUri, selectedFileName, selectedFileMimeType)
                                        messageText = ""
                                        selectedFileUri = null
                                        selectedFileName = null
                                        selectedFileMimeType = null
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // TopBar de la conversación idéntica a ChatBitacoraScreen (sin botón de asistencia ni teléfono rojo)
                Surface(
                    color = CardBackground,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = TextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(chat.avatarBgColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chat.isGlobal || chat.isGroup) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = chat.avatarInitials,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = chat.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = chat.subtitle,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!(chat.isGroup || chat.isGlobal)) {
                                IconButton(onClick = { avisoChat = AvisoChat.LLAMADA }) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Llamar",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { avisoChat = AvisoChat.INFORMACION }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Lista de mensajes con espaciado uniforme
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
                ) {
                    items(chat.messages, key = { it.id }) { msg ->
                        WorkerChatMessageBubble(
                            msg = msg,
                            isGroupChat = chat.isGroup || chat.isGlobal
                        )
                    }
                }
            }
        }
    }

    if (showShareActivityDialog && canShareActivity) {
        ShareGroupActivityDialog(
            onDismiss = { showShareActivityDialog = false },
            onSendActivity = { category, badge, description ->
                if (onSendActivity != null) {
                    onSendActivity(category, badge, description)
                } else {
                    val formatted = "[ACTIVIDAD]\nCategoría: $category\nDetalle: $badge\nObservaciones: $description"
                    onSendMessage(formatted, null, null, null)
                }
            }
        )
    }
}

/**
 * Burbuja de mensaje para trabajadores y chats grupales o individuales.
 * Si el mensaje corresponde a una actividad registrada ([ACTIVIDAD]), se renderiza mediante
 * [ActivityTimelineCard], viéndose exactamente igual que en el expediente individual.
 */
@Composable
fun WorkerChatMessageBubble(
    msg: WorkerChatMessage,
    isGroupChat: Boolean
) {
    // Si el mensaje es una actividad, mostrar la tarjeta uniforme y limpia
    val actData = remember(msg.message) { parseGroupActivityMessage(msg.message) }
    if (actData != null) {
        val (category, badge, desc) = actData
        ActivityTimelineCard(
            time = msg.time,
            category = category,
            portionLabel = badge,
            description = desc,
            authorName = if (isGroupChat) (if (msg.isOutgoing) "Tú" else msg.senderName) else null,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    val context = LocalContext.current
    if (msg.isOutgoing) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                ),
                color = ChatOutgoingBubble,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(min = 70.dp, max = 300.dp)
                    .clickable(enabled = msg.fileUri != null) {
                        msg.fileUri?.let { uriString ->
                            // Se descarga con la sesión (RF-09) y se abre desde la carpeta privada
                            Adjuntos.abrir(context, uriString, msg.fileMimeType, msg.fileName)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    if (msg.fileUri != null) {
                        if (msg.fileMimeType?.startsWith("image/") == true) {
                            AsyncImage(
                                model = msg.fileUri,
                                contentDescription = "Imagen adjunta",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = "Archivo adjunto",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg.fileName ?: "Archivo adjunto",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (msg.message.isNotBlank()) {
                        Text(
                            text = msg.message,
                            fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 19.sp
                        )
                    }

                    // Hora de envío y estado de entrega integrado en la esquina inferior
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg.time,
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Entregado",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    } else {
        if (isGroupChat) {
            val (avatarInitials, avatarColor) = getWorkerSenderInfo(msg.senderId, msg.senderName)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                // Foto de perfil circular del remitente (estilo WhatsApp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarInitials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    ),
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(min = 70.dp, max = 300.dp)
                        .clickable(enabled = msg.fileUri != null) {
                            msg.fileUri?.let { uriString ->
                                // Se descarga con la sesión (RF-09) y se abre desde la carpeta privada
                                Adjuntos.abrir(context, uriString, msg.fileMimeType, msg.fileName)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = msg.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(avatarColor),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )

                        if (msg.fileUri != null) {
                            if (msg.fileMimeType?.startsWith("image/") == true) {
                                AsyncImage(
                                    model = msg.fileUri,
                                    contentDescription = "Imagen adjunta",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "Archivo adjunto",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg.fileName ?: "Archivo adjunto",
                                        fontSize = 12.sp,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (msg.message.isNotBlank()) {
                            Text(
                                text = msg.message,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp
                            )
                        }

                        // Hora de recepción abajo a la derecha
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.time,
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        } else {
            // Chat individual (1 a 1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    ),
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(min = 70.dp, max = 300.dp)
                        .clickable(enabled = msg.fileUri != null) {
                            msg.fileUri?.let { uriString ->
                                // Se descarga con la sesión (RF-09) y se abre desde la carpeta privada
                                Adjuntos.abrir(context, uriString, msg.fileMimeType, msg.fileName)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        if (msg.fileUri != null) {
                            if (msg.fileMimeType?.startsWith("image/") == true) {
                                AsyncImage(
                                    model = msg.fileUri,
                                    contentDescription = "Imagen adjunta",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "Archivo adjunto",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg.fileName ?: "Archivo adjunto",
                                        fontSize = 12.sp,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (msg.message.isNotBlank()) {
                            Text(
                                text = msg.message,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp
                            )
                        }

                        // Hora de recepción abajo a la derecha
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.time,
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Obtiene las iniciales y color de avatar de un trabajador a partir de su ID o nombre
 */
private fun getWorkerSenderInfo(senderId: String, senderName: String): Pair<String, Long> {
    val member = MockDataRepository.staffMembers.find {
        it.id == senderId || it.name.equals(senderName, ignoreCase = true)
    }
    if (member != null) {
        return Pair(member.avatarInitials, member.avatarBgColor)
    }
    val initials = senderName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "W" }
    val colors = listOf(0xFF8E24AA, 0xFF0288D1, 0xFFD81B60, 0xFF00897B, 0xFFFB8C00, 0xFF5E35B1, 0xFF43A047)
    val color = colors[kotlin.math.abs(senderName.hashCode()) % colors.size]
    return Pair(initials, color)
}

data class ActivityCategoryStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val primaryColor: Color,
    val containerColor: Color,
    val borderColor: Color
)

fun getActivityCategoryStyle(category: String): ActivityCategoryStyle {
    val cat = category.trim().lowercase()
    return when {
        cat.contains("aliment") || cat.contains("comida") || cat.contains("desayuno") -> ActivityCategoryStyle(
            icon = Icons.Default.Restaurant,
            primaryColor = Color(0xFFE65100),
            containerColor = Color(0xFFFFF3E0),
            borderColor = Color(0xFFFFCC80)
        )
        cat.contains("descanso") || cat.contains("siesta") || cat.contains("sueño") -> ActivityCategoryStyle(
            icon = Icons.Default.NightlightRound,
            primaryColor = Color(0xFF5E35B1),
            containerColor = Color(0xFFEDE7F6),
            borderColor = Color(0xFFD1C4E9)
        )
        cat.contains("excretor") || cat.contains("esfínter") || cat.contains("pañal") || cat.contains("pipi") || cat.contains("popo") -> ActivityCategoryStyle(
            icon = Icons.Default.Wc,
            primaryColor = Color(0xFF0288D1),
            containerColor = Color(0xFFE1F5FE),
            borderColor = Color(0xFFB3E5FC)
        )
        cat.contains("ánimo") || cat.contains("animo") || cat.contains("humor") || cat.contains("feliz") -> ActivityCategoryStyle(
            icon = Icons.Default.Face,
            primaryColor = Color(0xFF00897B),
            containerColor = Color(0xFFE0F2F1),
            borderColor = Color(0xFFB2DFDB)
        )
        cat.contains("accidente") -> ActivityCategoryStyle(
            icon = Icons.Default.WarningAmber,
            primaryColor = Color(0xFFD32F2F),
            containerColor = Color(0xFFFFEBEE),
            borderColor = Color(0xFFFFCDD2)
        )
        cat.contains("salud") || cat.contains("medic") || cat.contains("fiebre") -> ActivityCategoryStyle(
            icon = Icons.Default.LocalHospital,
            primaryColor = Color(0xFFC2185B),
            containerColor = Color(0xFFFCE4EC),
            borderColor = Color(0xFFF8BBD0)
        )
        cat.contains("pedagóg") || cat.contains("educat") || cat.contains("taller") -> ActivityCategoryStyle(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            primaryColor = Color(0xFF6A1B9A),
            containerColor = Color(0xFFF3E5F5),
            borderColor = Color(0xFFE1BEE7)
        )
        else -> ActivityCategoryStyle(
            icon = Icons.AutoMirrored.Filled.EventNote,
            primaryColor = Color(0xFF1565C0),
            containerColor = Color(0xFFE8F1FC),
            borderColor = Color(0xFFBBDEFB)
        )
    }
}

fun formatActivityDescription(description: String, portionLabel: String): String {
    val trimmedDesc = description.trim()
    val trimmedBadge = portionLabel.trim()

    if (trimmedBadge.isNotEmpty() && trimmedDesc.startsWith(trimmedBadge, ignoreCase = true)) {
        val remaining = trimmedDesc.substring(trimmedBadge.length).trim().removePrefix(".").trim()
        if (remaining.isNotEmpty()) {
            return remaining
        }
    }
    return trimmedDesc
}

fun formatActivityPreview(raw: String): String {
    val trimmed = raw.trim()
    val isYo = trimmed.startsWith("Yo: ")
    val content = if (isYo) trimmed.removePrefix("Yo: ").trim() else trimmed
    val parsed = parseGroupActivityMessage(content) ?: return raw
    val (cat, badge, _) = parsed
    val previewBody = if (badge.isNotBlank()) "📋 Actividad: $cat • $badge" else "📋 Actividad: $cat"
    return if (isYo) "Yo: $previewBody" else previewBody
}

/**
 * Parsea un mensaje estructurado de actividad para grupos o chats.
 */
fun parseGroupActivityMessage(raw: String): Triple<String, String, String>? {
    val trimmedRaw = raw.trim()
    if (!trimmedRaw.startsWith("[ACTIVIDAD]", ignoreCase = true)) return null
    var cat = "Actividad"
    var badge = ""
    var desc = ""
    val lines = trimmedRaw.lines()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("Categoría:", ignoreCase = true) -> cat = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Categoria:", ignoreCase = true) -> cat = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Detalle:", ignoreCase = true) -> badge = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Observaciones:", ignoreCase = true) -> desc = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Observación:", ignoreCase = true) -> desc = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Descripción:", ignoreCase = true) -> desc = trimmed.substringAfter(":").trim()
            trimmed.startsWith("Descripcion:", ignoreCase = true) -> desc = trimmed.substringAfter(":").trim()
        }
    }
    if (desc.isEmpty() && lines.size > 1) {
        desc = lines.filter {
            val l = it.trim().lowercase()
            !l.startsWith("[actividad]") &&
                !l.startsWith("categoría:") &&
                !l.startsWith("categoria:") &&
                !l.startsWith("detalle:")
        }.joinToString("\n").trim()
    }
    return Triple(cat, badge, desc)
}

/**
 * Tarjeta visual estilizada y uniforme de actividad.
 * Se utiliza de forma idéntica en el chat individual del menor y en los chats grupales,
 * asegurando un diseño limpio, profesional y consistente en toda la aplicación.
 */
@Composable
fun ActivityTimelineCard(
    time: String,
    category: String,
    portionLabel: String,
    description: String,
    authorName: String? = null,
    modifier: Modifier = Modifier
) {
    val style = remember(category) { getActivityCategoryStyle(category) }
    val cleanDescription = remember(description, portionLabel) {
        formatActivityDescription(description, portionLabel)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header: Icono + Categoría / Tiempo + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contenedor suave con color temático según la categoría
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(style.containerColor)
                            .border(1.dp, style.borderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = time,
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                            if (!authorName.isNullOrBlank()) {
                                Text(
                                    text = "• $authorName",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (portionLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = style.containerColor,
                            border = BorderStroke(1.dp, style.borderColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = portionLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = style.primaryColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (cleanDescription.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                        thickness = 0.8.dp,
                        color = BorderSubtle.copy(alpha = 0.7f)
                    )

                    Text(
                        text = cleanDescription,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineCard(
    act: TimelineItem.ActivityCard,
    modifier: Modifier = Modifier
) {
    ActivityTimelineCard(
        time = act.time,
        category = act.category,
        portionLabel = act.portionLabel,
        description = act.description,
        modifier = modifier
    )
}

/**
 * Tarjeta visual estilizada de actividad mantenida para compatibilidad.
 */
@Composable
fun GroupActivityCardBubble(
    activityData: Triple<String, String, String>,
    isOutgoing: Boolean = false
) {
    val (category, badge, desc) = activityData
    ActivityTimelineCard(
        time = "Ahora",
        category = category,
        portionLabel = badge,
        description = desc
    )
}

/**
 * Diálogo interactivo para compartir una actividad directamente en cualquier grupo familiar o de personal.
 */
@Composable
fun ShareGroupActivityDialog(
    onDismiss: () -> Unit,
    onSendActivity: (category: String, badge: String, description: String) -> Unit
) {
    val categories = listOf(
        "Alimentación" to Icons.Default.Restaurant,
        "Descanso" to Icons.Default.NightlightRound,
        "Ánimo" to Icons.Default.Face,
        "Excretoras" to Icons.Default.Wc,
        "Pedagógica" to Icons.AutoMirrored.Filled.EventNote,
        "Salud" to Icons.Default.LocalHospital
    )
    var selectedCat by remember { mutableStateOf("Alimentación") }
    var badgeText by remember { mutableStateOf("Consumo: TODO") }
    var descText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = CardBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compartir Actividad",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CATEGORÍA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { (catName, icon) ->
                        val isSelected = selectedCat == catName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrandBlueContainer else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCat = catName
                                    badgeText = when (catName) {
                                        "Alimentación" -> "Consumo: TODO"
                                        "Descanso" -> "60 min"
                                        "Ánimo" -> "Feliz y participativo"
                                        "Excretoras" -> "Control de esfínteres"
                                        "Salud" -> "Salud óptima"
                                        else -> "Actividad grupal"
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = catName,
                                    tint = if (isSelected) BrandBlue else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = catName,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandBlue else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(3).forEach { (catName, icon) ->
                        val isSelected = selectedCat == catName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrandBlueContainer else AppBackground,
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCat = catName
                                    badgeText = when (catName) {
                                        "Alimentación" -> "Consumo: TODO"
                                        "Descanso" -> "60 min"
                                        "Ánimo" -> "Feliz y participativo"
                                        "Excretoras" -> "Control de esfínteres"
                                        "Salud" -> "Salud óptima"
                                        else -> "Actividad grupal"
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = catName,
                                    tint = if (isSelected) BrandBlue else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = catName,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandBlue else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Opciones interactivas rápidas según categoría seleccionada (igual que actividades normales)
                val optionsForCategory = when (selectedCat) {
                    "Alimentación" -> listOf("Consumo: TODO", "Consumo: LA MITAD", "Consumo: POCO", "Consumo: NADA")
                    "Descanso" -> listOf("30 min", "45 min", "60 min", "90 min", "120 min")
                    "Ánimo" -> listOf("Feliz y participativo", "Tranquilo", "Inquieto", "Cansado")
                    "Excretoras" -> listOf("Control de esfínteres", "Pipí", "Popó", "Cambio de pañal")
                    "Salud" -> listOf("Salud óptima", "Revisión matutina", "Medicamento suministrado")
                    else -> listOf("Pintura y manualidades", "Cuentos y asamblea", "Música y psicomotricidad", "Juego libre")
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(optionsForCategory) { opt ->
                        val isOptSelected = badgeText == opt
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isOptSelected) BrandBlue else AppBackground,
                            border = BorderStroke(1.dp, if (isOptSelected) BrandBlue else BorderSubtle),
                            modifier = Modifier.clickable { badgeText = opt }
                        ) {
                            Text(
                                text = opt,
                                fontSize = 11.5.sp,
                                fontWeight = if (isOptSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOptSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "RESUMEN / DETALLE RÁPIDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = badgeText,
                    onValueChange = { badgeText = it },
                    placeholder = { Text("Ej. Consumo: TODO, 60 min...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DESCRIPCIÓN / OBSERVACIONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    placeholder = { Text("Observaciones o notas para el grupo...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val badge = badgeText.ifBlank { "General" }
                            val desc = descText.ifBlank { "Actividad registrada por la educadora." }
                            onSendActivity(selectedCat, badge, desc)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Enviar al grupo", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private enum class AvisoChat { LLAMADA, INFORMACION }
/**
 * Elemento de candidato para iniciar un nuevo chat con una persona/familia
 * con la que nunca antes se había hablado y no estaba en la pantalla de chats.
 */
@Composable
fun NewChatCandidateItem(
    title: String,
    subtitle: String,
    avatarInitials: String,
    avatarBgColor: Color,
    badgeText: String = "Iniciar chat",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD0E1F9), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandBlueContainer,
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = badgeText,
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}


