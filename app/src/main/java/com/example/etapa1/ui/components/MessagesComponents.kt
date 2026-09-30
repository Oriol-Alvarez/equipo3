package com.example.etapa1.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
    backgroundColor: Color = BrandBlue,
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
                            text = mode.label,
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
                    text = chat.lastMessage,
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
                        backgroundColor = BrandBlue
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
                    text = chat.lastMessage,
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
                        backgroundColor = BrandBlue
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
                    text = chatSummary.lastMessage,
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
                        backgroundColor = BrandBlue
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
    onSendMessage: (String, Uri?, String?, String?) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                            IconButton(onClick = { /* Llamada */ }) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Llamar",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { /* Menú de opciones */ }) {
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
}

/**
 * Burbuja de mensaje para trabajadores. En grupos muestra avatar del remitente (WhatsApp style)
 * y nombre destacado dentro de la burbuja.
 */
@Composable
private fun WorkerChatMessageBubble(
    msg: WorkerChatMessage,
    isGroupChat: Boolean
) {
    val context = LocalContext.current
    if (msg.isOutgoing) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = msg.time,
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(ChatOutgoingBubble)
                    .clickable(enabled = msg.fileUri != null) {
                        msg.fileUri?.let { uriString ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uriString), msg.fileMimeType ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (msg.fileUri != null) {
                        if (msg.fileMimeType?.startsWith("image/") == true) {
                            AsyncImage(
                                model = msg.fileUri,
                                contentDescription = "Imagen adjunta",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
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
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (msg.message.isNotBlank()) {
                        Text(
                            text = msg.message,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            lineHeight = 18.sp
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

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = msg.time,
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(CardBackground)
                            .border(
                                width = 1.dp,
                                color = BorderSubtle,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = msg.fileUri != null) {
                                msg.fileUri?.let { uriString ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(uriString), msg.fileMimeType ?: "*/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
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
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
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
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (msg.message.isNotBlank()) {
                                Text(
                                    text = msg.message,
                                    fontSize = 13.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Chat individual (1 a 1), exactamente igual a ChatBitacoraScreen
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = msg.time,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(CardBackground)
                        .border(
                            width = 1.dp,
                            color = BorderSubtle,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = msg.fileUri != null) {
                            msg.fileUri?.let { uriString ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uriString), msg.fileMimeType ?: "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        if (msg.fileUri != null) {
                            if (msg.fileMimeType?.startsWith("image/") == true) {
                                AsyncImage(
                                    model = msg.fileUri,
                                    contentDescription = "Imagen adjunta",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = if (msg.message.isNotBlank()) 6.dp else 0.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
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
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (msg.message.isNotBlank()) {
                            Text(
                                text = msg.message,
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
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
