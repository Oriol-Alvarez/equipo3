package com.example.casosdeusopadre.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.casosdeusopadre.data.models.ActivityLog
import com.example.casosdeusopadre.data.models.Child
import com.example.casosdeusopadre.ui.theme.AppShapes

// CU-02: Resumen Diario del Hijo (RF-06, RF-07, RF-08)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Status Bar
        StatusBar(isOnline = uiState.isOnline, onToggle = { viewModel.toggleNetworkMode() })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice Banner
            uiState.notice?.let { notice ->
                item {
                    NoticeBanner(noticeMessage = notice.message)
                }
            }

            // Child Selector / Profile Card
            item {
                if (uiState.children.isNotEmpty()) {
                    ChildSelector(
                        children = uiState.children,
                        selectedChild = uiState.selectedChild,
                        onChildSelected = { viewModel.selectChild(it) }
                    )
                }
            }

            // Daily Timeline
            item {
                Text(
                    text = "Actividad de hoy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.activityLogs.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(uiState.activityLogs) { log ->
                    ActivityTimelineCard(log = log)
                }
            }
        }
    }
}

@Composable
fun StatusBar(isOnline: Boolean, onToggle: () -> Unit) {
    val backgroundColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val contentColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
    val text = if (isOnline) "🟢 En línea / Sincronizado" else "🟠 Modo sin conexión"

    Surface(
        color = backgroundColor,
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoticeBanner(noticeMessage: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = AppShapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "Aviso", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = noticeMessage,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSelector(children: List<Child>, selectedChild: Child?, onChildSelected: (Child) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChildCare,
                        contentDescription = "Foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = selectedChild?.name ?: "",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${selectedChild?.age ?: 0} años",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Selector si hay más de 1 niño
            if (children.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { child ->
                        FilterChip(
                            selected = child.id == selectedChild?.id,
                            onClick = { onChildSelected(child) },
                            label = { Text(child.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTimelineCard(log: ActivityLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimelineRow(emoji = "🍲", title = "Comida", detail = log.mealStatus.replaceFirstChar { it.uppercase() })
            TimelineRow(emoji = "😴", title = "Siesta", detail = "${log.napDurationMinutes} min")
            TimelineRow(emoji = "🎭", title = "Estado de ánimo", detail = log.mood.replaceFirstChar { it.uppercase() })
            TimelineRow(emoji = "🧻", title = "Baño/Pañal", detail = log.pottyOrBath)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Observaciones:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = log.educatorObservations,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun TimelineRow(emoji: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "$title:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(130.dp))
        Text(text = detail)
    }
}

@Composable
fun EmptyState() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Aún no hay actividades registradas hoy",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
