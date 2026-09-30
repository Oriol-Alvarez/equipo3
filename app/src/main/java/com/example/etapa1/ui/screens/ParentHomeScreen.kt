package com.example.etapa1.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.components.ChildFullDataContent
import com.example.etapa1.ui.components.DailyBitacoraContent
import com.example.etapa1.ui.components.MedicalAlertBanner
import com.example.etapa1.ui.components.WeeklySummaryContent
import com.example.etapa1.ui.state.ParentHomeViewModel
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusAbsentOrange
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun ParentHomeScreen(
    viewModel: ParentHomeViewModel,
    unreadAnnouncementsCount: Int = 0,
    onNavigateToChat: (Child) -> Unit,
    onNavigateToChildDetail: (Child) -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onSwitchChild: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ParentHomeScreen(
        child = uiState.selectedChild,
        allChildren = uiState.allChildren,
        unreadAnnouncementsCount = unreadAnnouncementsCount,
        onNavigateToChat = { onNavigateToChat(uiState.selectedChild) },
        onNavigateToChildDetail = { onNavigateToChildDetail(uiState.selectedChild) },
        onNavigateToAnnouncements = onNavigateToAnnouncements,
        onSwitchChild = onSwitchChild,
        onSelectChild = { child -> viewModel.selectChild(child) }
    )
}

@Composable
fun ParentHomeScreen(
    child: Child = MockDataRepository.mateoGarcia,
    allChildren: List<Child> = MockDataRepository.parentChildren,
    unreadAnnouncementsCount: Int = 0,
    onNavigateToChat: () -> Unit = {},
    onNavigateToChildDetail: () -> Unit = {},
    onNavigateToAnnouncements: () -> Unit = {},
    onSwitchChild: () -> Unit = {},
    onSelectChild: (Child) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Hoy en Vivo, 1 = Historial Bitácora, 2 = Resumen Semanal, 3 = Ficha
    val fullProfile = remember(child.id) { MockDataRepository.getChildFullProfile(child) }
    val bitacoras = remember(child.id) { MockDataRepository.getDailyBitacorasForChild(child) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    val weeklySummary = remember(child.id) { MockDataRepository.getWeeklySummaryForChild(child) }
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Barra Superior Principal
            item {
                Surface(
                    color = CardBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Botón de Anuncios del Centro
                        IconButton(
                            onClick = onNavigateToAnnouncements,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadAnnouncementsCount > 0) {
                                        Badge(
                                            containerColor = AlertRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = unreadAnnouncementsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Anuncios del centro",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Título y Centro
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Sonrisas de Cristal",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Text(
                                text = "Inicio Familiar",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Botón Cambiar de Hijo (si hay más de 1)
                        if (allChildren.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BrandBlueContainer)
                                    .clickable(onClick = onSwitchChild)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Cambiar de hijo",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cambiar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tarjeta Principal del Niño Seleccionado (Estilo Dashboard)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChildAvatar(
                                initials = child.avatarInitials,
                                bgColor = Color(child.avatarBgColor),
                                size = 60.dp,
                                showStatusDot = true,
                                isPresent = child.isPresent
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.fullName,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${child.roomText} • ${child.groupText}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (child.isPresent) StatusPresentGreen.copy(alpha = 0.12f) else StatusAbsentOrange.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (child.isPresent) StatusPresentGreen else StatusAbsentOrange)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (child.isPresent) "En el aula hoy (Ingreso: ${child.arrivalTime ?: "08:15 AM"})" else "No ha ingresado",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (child.isPresent) StatusPresentGreen else StatusAbsentOrange
                                    )
                                }
                            }
                        }

                        // Alerta médica si la tiene
                        if (child.allergyAlert != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MedicalAlertBanner(
                                text = child.allergyAlert,
                                onClick = { selectedTab = 0 }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fila de Accesos Rápidos: Chat y Llamar al Centro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                label = "Chat",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToChat
                            )

                            QuickActionButton(
                                icon = Icons.Default.Phone,
                                label = "Llamar al Centro",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+34912334455"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Pestañas de Contenido: 1. Datos del Niño (Default) | 2. Bitácora Diaria | 3. Resumen Semanal
            item {
                Surface(
                    color = CardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CardBackground,
                        contentColor = BrandBlue
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Datos del Niño",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bitácora Diaria",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Resumen Semanal",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 4. Contenido según pestaña seleccionada
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (selectedTab) {
                        0 -> ChildFullDataContent(
                            child = child,
                            fullProfile = fullProfile
                        )
                        1 -> DailyBitacoraContent(
                            bitacoras = bitacoras,
                            selectedDayIndex = selectedDayIndex,
                            onSelectDay = { index -> selectedDayIndex = index }
                        )
                        2 -> WeeklySummaryContent(
                            summary = weeklySummary,
                            child = child
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = BrandBlueContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrandBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlue,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ParentHomeScreenPreview() {
    Etapa1Theme {
        ParentHomeScreen()
    }
}
