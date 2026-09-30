package com.example.etapa1.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.state.ParentChildSelectionViewModel
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
fun ParentChildSelectionScreen(
    viewModel: ParentChildSelectionViewModel,
    unreadAnnouncementsCount: Int = 0,
    onChildSelected: (Child) -> Unit,
    onNavigateToAnnouncements: () -> Unit = {}
) {
    val children by viewModel.children.collectAsState()

    ParentChildSelectionScreen(
        children = children,
        unreadAnnouncementsCount = unreadAnnouncementsCount,
        onChildSelected = onChildSelected,
        onNavigateToAnnouncements = onNavigateToAnnouncements
    )
}

@Composable
fun ParentChildSelectionScreen(
    children: List<Child> = MockDataRepository.parentChildren,
    unreadAnnouncementsCount: Int = 0,
    onChildSelected: (Child) -> Unit,
    onNavigateToAnnouncements: () -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar - Centrado con estética de Sonrisas de Cristal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
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
                            contentDescription = "Anuncios",
                            tint = BrandBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "Mis Hijos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { /* Refrescar */ },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizar",
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            Spacer(modifier = Modifier.height(10.dp))

            // Lista de Hijos registrados
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "HIJOS REGISTRADOS (${children.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                items(children, key = { it.id }) { child ->
                    ParentChildCard(
                        child = child,
                        onClick = { onChildSelected(child) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ParentChildCard(
    child: Child,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales e indicador
            ChildAvatar(
                initials = child.avatarInitials,
                bgColor = Color(child.avatarBgColor),
                size = 54.dp,
                showStatusDot = true,
                isPresent = child.isPresent
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.fullName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${child.ageText} • ${child.roomText}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chip de estado de asistencia
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
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
                        text = if (child.isPresent) "En el centro • Llegó ${child.arrivalTime ?: "08:15 AM"}" else "No ha ingresado hoy",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (child.isPresent) StatusPresentGreen else StatusAbsentOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Flecha indicadora
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ver inicio de ${child.shortName}",
                tint = BrandBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ParentChildSelectionScreenPreview() {
    Etapa1Theme {
        ParentChildSelectionScreen(
            children = MockDataRepository.parentChildren,
            unreadAnnouncementsCount = 2,
            onChildSelected = {}
        )
    }
}
