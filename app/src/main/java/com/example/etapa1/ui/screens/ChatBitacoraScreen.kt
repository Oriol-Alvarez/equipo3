package com.example.etapa1.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.FloatingActionButton
import com.example.etapa1.model.WorkerChat
import com.example.etapa1.model.WorkerChatMessage
import com.example.etapa1.ui.components.WorkerChatMessageBubble
import com.example.etapa1.ui.components.ActivityTimelineCard
import com.example.etapa1.ui.components.parseGroupActivityMessage
import androidx.compose.material.icons.filled.DoneAll
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import java.io.File
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.ChatEventChipBg
import com.example.etapa1.ui.theme.ChatEventChipText
import com.example.etapa1.ui.theme.ChatOutgoingBubble
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.theme.Etapa1Theme
import androidx.compose.runtime.collectAsState
import com.example.etapa1.ui.state.ChatBitacoraViewModel

@Composable
fun ChatBitacoraScreen(
    viewModel: ChatBitacoraViewModel,
    child: Child,
    isWorkerRole: Boolean = true,
    onBack: () -> Unit,
    onChildInfoClick: () -> Unit = {},
    onNewActivityClick: () -> Unit,
    onAttendanceClick: () -> Unit = {}
) {
    val timelineItems by viewModel.timelineItems.collectAsState()
    val conexionEstado by viewModel.conexionEstado.collectAsState()
    val cambiosFicha by viewModel.cambiosFicha.collectAsState()
    val perfil = remember(child.id, cambiosFicha) { viewModel.perfil(child) }

    ChatBitacoraScreen(
        child = child,
        timelineItems = timelineItems,
        conexionEstado = conexionEstado,
        childProfile = perfil,
        isWorkerRole = isWorkerRole,
        onBack = onBack,
        onChildInfoClick = onChildInfoClick,
        onNewActivityClick = onNewActivityClick,
        onAttendanceClick = onAttendanceClick,
        onSendMessage = { text, uri, name, mime ->
            viewModel.sendMessage(
                text = text,
                fileUri = uri?.toString(),
                fileName = name,
                fileMimeType = mime,
                senderRole = if (isWorkerRole) com.example.etapa1.model.UserRole.EDUCADORA else com.example.etapa1.model.UserRole.FAMILIAR,
                childId = child.id
            )
        }
    )
}

@Composable
fun ChatBitacoraScreen(
    child: Child? = null,
    workerChat: WorkerChat? = null,
    timelineItems: List<TimelineItem> = emptyList(),
    conexionEstado: com.example.etapa1.domain.EstadoConexion = com.example.etapa1.domain.EstadoConexion.Conectado,
    isWorkerRole: Boolean = true,
    canShareActivity: Boolean = false,
    onBack: () -> Unit,
    onChildInfoClick: () -> Unit = {},
    onNewActivityClick: () -> Unit = {},
    onAttendanceClick: () -> Unit = {},
    /** Expediente real para los teléfonos del botón de llamar; null usa los datos de ejemplo (vista previa). */
    childProfile: com.example.etapa1.model.ChildFullProfile? = null,
    onSendMessage: (String, Uri?, String?, String?) -> Unit,
    onSendActivity: ((category: String, badge: String, description: String) -> Unit)? = null
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isGroupChat = workerChat?.let { it.isGroup || it.isGlobal } ?: false
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileMimeType by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showCallDialog by remember { mutableStateOf(false) }

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

    // Auto scroll to bottom when items change
    val currentItemCount = if (workerChat != null) workerChat.messages.size else timelineItems.size
    LaunchedEffect(currentItemCount) {
        if (currentItemCount > 0) {
            listState.animateScrollToItem(currentItemCount - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            // Barra de entrada de mensajes
            Surface(
                color = CardBackground,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val isGroupChat = workerChat?.let { it.isGroup || it.isGlobal } ?: false
                    val canAddActivity = isWorkerRole && (child != null || isGroupChat)
                    if (canAddActivity) {
                        Button(
                            onClick = onNewActivityClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Añadir Actividad",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
            // Top Bar
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
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = TextPrimary
                            )
                        }

                        if (workerChat != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                if (workerChat.isGlobal || workerChat.isGroup) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(workerChat.avatarBgColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    ChildAvatar(
                                        initials = workerChat.avatarInitials,
                                        bgColor = Color(workerChat.avatarBgColor),
                                        size = 38.dp,
                                        showStatusDot = false
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = workerChat.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = workerChat.subtitle,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        } else if (child != null) {
                            // Datos reales de la educadora asignada (vienen del servidor con el niño)
                            val educatorName = child.educatorName?.ifBlank { null } ?: "Educadora de ${child.roomText.ifBlank { "sala" }}"
                            val educatorInitials = child.educatorAvatarInitials?.ifBlank { null } ?: "ED"
                            val educatorBgColor = child.educatorAvatarBgColor ?: 0xFF00897B
                            val educatorRole = (child.educatorRole?.ifBlank { null } ?: "Educadora").substringBefore(" •").trim()
                            val roomName = child.roomText.ifBlank { "Sala" }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onChildInfoClick() }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                if (!isWorkerRole) {
                                    ChildAvatar(
                                        initials = educatorInitials,
                                        bgColor = Color(educatorBgColor),
                                        size = 38.dp,
                                        showStatusDot = false
                                    )
                                } else {
                                    ChildAvatar(
                                        initials = child.avatarInitials,
                                        bgColor = Color(child.avatarBgColor),
                                        size = 38.dp,
                                        showStatusDot = true,
                                        isPresent = child.isPresent
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = if (!isWorkerRole) educatorName else child.shortName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (!isWorkerRole) {
                                            "$educatorRole • $roomName"
                                        } else {
                                            "${child.ageText} • $roomName"
                                        },
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWorkerRole && child != null) {
                            IconButton(onClick = onAttendanceClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = "Pasar Asistencia",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (!isGroupChat) {
                            IconButton(onClick = {
                                if (isWorkerRole && child != null) {
                                    showCallDialog = true
                                } else {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+525555123456"))
                                    context.startActivity(intent)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = if (isWorkerRole && child != null) "Llamar a familiares" else "Llamar a educador",
                                    tint = AlertRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Timeline de Mensajes y Actividades
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                if (workerChat != null) {
                    items(workerChat.messages, key = { it.id }) { msg ->
                        WorkerChatMessageBubble(
                            msg = msg,
                            isGroupChat = workerChat.isGroup || workerChat.isGlobal
                        )
                    }
                } else {
                    itemsIndexed(timelineItems, key = { _, item -> item.id }) { index, item ->
                        val showDateHeader = if (index == 0) {
                            item.date.isNotBlank()
                        } else {
                            item.date.isNotBlank() && item.date != timelineItems[index - 1].date
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showDateHeader) {
                                DateHeaderChip(dateStr = item.date)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            when (item) {
                                is TimelineItem.EventChip -> {
                                    TimelineChip(item)
                                }
                                is TimelineItem.ChatMessage -> {
                                    ChatMessageBubble(item, isWorkerRole)
                                }
                                is TimelineItem.ActivityCard -> {
                                    ActivityTimelineCard(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCallDialog && child != null) {
        ParentCallDialog(
            child = child,
            profile = childProfile,
            onDismiss = { showCallDialog = false }
        )
    }
}

@Composable
fun ParentCallDialog(
    child: Child,
    profile: com.example.etapa1.model.ChildFullProfile? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val childProfile = profile ?: remember(child) { MockDataRepository.getChildFullProfile(child) }

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
                Text(
                    text = "Llamar a familiares",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                var hasContacts = false

                // Contacto de Emergencia guardado en el perfil del menor
                if (childProfile.emergencyPhone.isNotBlank()) {
                    hasContacts = true
                    ContactRow(
                        name = "Contacto de Emergencia",
                        phone = childProfile.emergencyPhone,
                        relation = childProfile.emergencyPhoneRelation.ifBlank { "Emergencia" },
                        onCall = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${childProfile.emergencyPhone.replace(" ", "")}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                // Madre
                if (childProfile.motherName.isNotBlank() && childProfile.motherPhone.isNotBlank()) {
                    if (hasContacts) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    hasContacts = true
                    ContactRow(
                        name = childProfile.motherName,
                        phone = childProfile.motherPhone,
                        relation = "Madre",
                        onCall = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${childProfile.motherPhone.replace(" ", "")}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                // Padre
                if (childProfile.fatherName.isNotBlank() && childProfile.fatherPhone.isNotBlank()) {
                    if (hasContacts) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    hasContacts = true
                    ContactRow(
                        name = childProfile.fatherName,
                        phone = childProfile.fatherPhone,
                        relation = "Padre",
                        onCall = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${childProfile.fatherPhone.replace(" ", "")}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                if (!hasContacts) {
                    Text(
                        text = "No hay teléfonos de contacto guardados en el perfil del niño.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Cerrar", color = BrandBlue)
                }
            }
        }
    }
}

@Composable
fun ContactRow(name: String, phone: String, relation: String, onCall: () -> Unit) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)), // Light blue
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = BrandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = "$relation • $phone",
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        IconButton(
            onClick = onCall,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)) // Light green
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Llamar",
                tint = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun DateHeaderChip(dateStr: String) {
    val displayDate = remember(dateStr) {
        try {
            val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
            if (dateStr == today) {
                "Hoy"
            } else {
                val parsed = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).parse(dateStr)
                if (parsed != null) {
                    val outFormat = java.text.SimpleDateFormat("d 'de' MMMM, yyyy", java.util.Locale("es", "ES"))
                    outFormat.format(parsed)
                } else {
                    dateStr
                }
            }
        } catch (_: Exception) {
            dateStr
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8EEF5),
            shadowElevation = 0.dp
        ) {
            Text(
                text = displayDate,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TimelineChip(chip: TimelineItem.EventChip) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chip.time,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 3.dp)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ChatEventChipBg)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (chip.iconType) {
                "entry" -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = StatusPresentGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
                "mood" -> {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = StatusPresentGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
                "nap" -> {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = chip.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ChatEventChipText
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(msg: TimelineItem.ChatMessage, isWorkerRole: Boolean = true) {
    val actData = remember(msg.message) { parseGroupActivityMessage(msg.message) }
    val isOutgoing = if (msg.senderRole != null) {
        if (isWorkerRole) msg.senderRole == com.example.etapa1.model.UserRole.EDUCADORA
        else msg.senderRole == com.example.etapa1.model.UserRole.FAMILIAR
    } else {
        if (isWorkerRole) msg.isOutgoing else !msg.isOutgoing
    }

    if (actData != null) {
        val (category, badge, desc) = actData
        ActivityTimelineCard(
            time = msg.time,
            category = category,
            portionLabel = badge,
            description = desc,
            authorName = if (isWorkerRole) (if (isOutgoing) "Tú" else "Familiar") else (if (isOutgoing) "Tú" else "Educadora"),
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            color = if (isOutgoing) ChatOutgoingBubble else CardBackground,
            border = if (isOutgoing) null else BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(min = 70.dp, max = 300.dp)
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
                                tint = if (isOutgoing) Color.White else BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.fileName ?: "Archivo adjunto",
                                fontSize = 12.sp,
                                color = if (isOutgoing) Color.White else BrandBlue,
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
                        color = if (isOutgoing) Color.White else TextPrimary,
                        lineHeight = 19.sp
                    )
                }

                // Hora de envío y estado de entrega integrado abajo a la derecha
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.time,
                        fontSize = 10.5.sp,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else TextMuted
                    )
                    if (isOutgoing) {
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
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBitacoraScreenPreview() {
    Etapa1Theme {
        ChatBitacoraScreen(
            child = MockDataRepository.mateoGarcia,
            timelineItems = MockDataRepository.getInitialMateoTimeline(),
            onBack = {},
            onNewActivityClick = {},
            onSendMessage = { _, _, _, _ -> }
        )
    }
}

