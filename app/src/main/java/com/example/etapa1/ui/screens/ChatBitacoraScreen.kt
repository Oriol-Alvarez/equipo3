package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.FloatingActionButton
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
import com.example.etapa1.ui.components.MedicalAlertBanner
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

    ChatBitacoraScreen(
        child = child,
        timelineItems = timelineItems,
        isWorkerRole = isWorkerRole,
        onBack = onBack,
        onChildInfoClick = onChildInfoClick,
        onNewActivityClick = onNewActivityClick,
        onAttendanceClick = onAttendanceClick,
        onSendMessage = { text, uri, name, mime ->
            viewModel.sendMessage(text, uri?.toString(), name, mime)
        }
    )
}

@Composable
fun ChatBitacoraScreen(
    child: Child,
    timelineItems: List<TimelineItem>,
    isWorkerRole: Boolean = true,
    onBack: () -> Unit,
    onChildInfoClick: () -> Unit = {},
    onNewActivityClick: () -> Unit,
    onAttendanceClick: () -> Unit = {},
    onSendMessage: (String, Uri?, String?, String?) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
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
    LaunchedEffect(timelineItems.size) {
        if (timelineItems.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.size - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        floatingActionButton = {
            if (isWorkerRole) {
                FloatingActionButton(
                    onClick = onNewActivityClick,
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nueva Actividad",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onChildInfoClick() }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            ChildAvatar(
                                initials = child.avatarInitials,
                                bgColor = Color(child.avatarBgColor),
                                size = 38.dp,
                                showStatusDot = true,
                                isPresent = child.isPresent
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = child.shortName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${child.ageText} • ${child.roomText}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWorkerRole) {
                            IconButton(onClick = onAttendanceClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = "Pasar Asistencia",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = { showCallDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Llamar",
                                tint = AlertRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                    }
                }
            }

            // Banner Alerta médica
            if (child.allergyAlert != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    MedicalAlertBanner(text = child.allergyAlert)
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
                items(timelineItems, key = { it.id }) { item ->
                    when (item) {
                        is TimelineItem.EventChip -> {
                            TimelineChip(item)
                        }
                        is TimelineItem.ChatMessage -> {
                            ChatMessageBubble(item)
                        }
                        is TimelineItem.ActivityCard -> {
                            ActivityTimelineCard(item)
                        }
                    }
                }
            }
        }
    }
    
    if (showCallDialog) {
        ParentCallDialog(
            child = child,
            onDismiss = { showCallDialog = false }
        )
    }
}

@Composable
fun ParentCallDialog(
    child: Child,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val childProfile = remember(child) { MockDataRepository.getChildFullProfile(child) }

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

                // Madre
                if (childProfile.motherName.isNotBlank() && childProfile.motherPhone.isNotBlank()) {
                    ContactRow(
                        name = childProfile.motherName,
                        phone = childProfile.motherPhone,
                        relation = "Madre",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${childProfile.motherPhone.replace(" ", "")}"))
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Padre
                if (childProfile.fatherName.isNotBlank() && childProfile.fatherPhone.isNotBlank()) {
                    ContactRow(
                        name = childProfile.fatherName,
                        phone = childProfile.fatherPhone,
                        relation = "Padre",
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${childProfile.fatherPhone.replace(" ", "")}"))
                            context.startActivity(intent)
                        }
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
private fun ChatMessageBubble(msg: TimelineItem.ChatMessage) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
    ) {
        Text(
            text = msg.time,
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = 2.dp
            )
        )

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (msg.isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (msg.isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(if (msg.isOutgoing) ChatOutgoingBubble else CardBackground)
                .border(
                    width = if (msg.isOutgoing) 0.dp else 1.dp,
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
                                tint = if (msg.isOutgoing) Color.White else BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.fileName ?: "Archivo adjunto",
                                fontSize = 12.sp,
                                color = if (msg.isOutgoing) Color.White else BrandBlue,
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
                        color = if (msg.isOutgoing) Color.White else TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineCard(act: TimelineItem.ActivityCard) {
    val (categoryIcon, categoryTint) = when (act.category) {
        "Alimentación" -> Pair(Icons.Default.Restaurant, Color(0xFFE65100))
        "Descanso", "Siesta" -> Pair(Icons.Default.NightlightRound, Color(0xFF5E35B1))
        "Funciones Excretoras" -> Pair(Icons.Default.Wc, Color(0xFF0288D1))
        "Estado de Ánimo" -> Pair(Icons.Default.Face, Color(0xFF00897B))
        "Accidentes" -> Pair(Icons.Default.WarningAmber, Color(0xFFD32F2F))
        "Salud" -> Pair(Icons.Default.LocalHospital, Color(0xFFC2185B))
        else -> Pair(Icons.AutoMirrored.Filled.EventNote, Color(0xFF1E88E5))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = act.time,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 3.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEFF5FD))
                .border(1.dp, Color(0xFFD3E4F9), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Etiqueta lateral izquierda
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlueContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = act.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            if (act.portionLabel.isNotBlank()) {
                                Text(
                                    text = act.portionLabel,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Descripción de la actividad
                Text(
                    text = act.description,
                    fontSize = 12.5.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )
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

