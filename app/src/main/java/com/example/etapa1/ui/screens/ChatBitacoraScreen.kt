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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onBack: () -> Unit,
    onChildInfoClick: () -> Unit = {},
    onNewActivityClick: () -> Unit,
    onAttendanceClick: () -> Unit = {}
) {
    val timelineItems by viewModel.timelineItems.collectAsState()

    ChatBitacoraScreen(
        child = child,
        timelineItems = timelineItems,
        onBack = onBack,
        onChildInfoClick = onChildInfoClick,
        onNewActivityClick = onNewActivityClick,
        onAttendanceClick = onAttendanceClick,
        onSendMessage = viewModel::sendMessage
    )
}

@Composable
fun ChatBitacoraScreen(
    child: Child,
    timelineItems: List<TimelineItem>,
    onBack: () -> Unit,
    onChildInfoClick: () -> Unit = {},
    onNewActivityClick: () -> Unit,
    onAttendanceClick: () -> Unit = {},
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when items change
    LaunchedEffect(timelineItems.size) {
        if (timelineItems.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.size - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        floatingActionButton = {
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
        },
        bottomBar = {
            // Barra de entrada de mensajes
            Surface(
                color = CardBackground,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Abrir cámara / Tomar foto */ }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Tomar foto",
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
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText)
                                messageText = ""
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
                        IconButton(onClick = onAttendanceClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = "Pasar Asistencia",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = { /* Llamada */ }) {
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.message,
                fontSize = 13.5.sp,
                color = if (msg.isOutgoing) Color.White else TextPrimary,
                lineHeight = 18.sp
            )
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
            onSendMessage = {}
        )
    }
}

