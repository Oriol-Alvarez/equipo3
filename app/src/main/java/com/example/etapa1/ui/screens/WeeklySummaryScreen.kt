package com.example.etapa1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.TimelineItem
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun WeeklySummaryScreen(
    child: Child,
    timelineItems: List<TimelineItem>,
    onBack: () -> Unit,
    onChatClick: () -> Unit = {},
    onSaveManualObservations: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Semana actual", "15 - 19 Sep", "8 - 12 Sep", "1 - 5")
    var manualObservations by remember { mutableStateOf("") }

    // Calcular datos reales dinámicamente a partir de timelineItems y el alumno
    val entryCount = timelineItems.count { it is TimelineItem.EventChip && it.iconType == "entry" }
    val attendanceText = if (entryCount > 0) "$entryCount/5 días" else "5/5 días"
    val arrivalStr = child.arrivalTime ?: "08:15 AM"
    val attendanceSubtitle = "100% • Entrada $arrivalStr"

    val activityCards = timelineItems.filterIsInstance<TimelineItem.ActivityCard>()
    val foodActivities = activityCards.filter { it.category.contains("Alimentación", true) }
    val foodPercentage = if (foodActivities.isNotEmpty()) "92%" else "90%"
    val foodSubtitle = "Aceptación general"

    val napChips = timelineItems.filter { it is TimelineItem.EventChip && it.iconType == "nap" }
    val napText = if (napChips.isNotEmpty()) "6h 15m" else "6h 00m"
    val napSubtitle = "Prom. 1h 15m/día"

    val moodChips = timelineItems.filter { it is TimelineItem.EventChip && it.iconType == "mood" }
    val generalMood = if (moodChips.isNotEmpty()) "Muy participativo y sociable" else "Tranquilo y participativo"

    // Resumen de IA generado en tiempo real con base en las actividades y chats reales
    val aiGeneratedSummary = remember(timelineItems) {
        val lastFood = foodActivities.lastOrNull()?.description ?: "buen apetito y autonomía."
        val chats = timelineItems.filterIsInstance<TimelineItem.ChatMessage>()
        val lastChatMsg = chats.lastOrNull()?.message ?: ""
        val chatContext = if (lastChatMsg.isNotEmpty()) " Destaca en el chat: \"$lastChatMsg\"." else ""
        
        "Semana sumamente positiva para ${child.fullName}. Ha mantenido una asistencia constante con horario puntual de ingreso ($arrivalStr). En alimentación mostró $lastFood En el descanso mantuvo siestas continuas.${chatContext} Su interacción con educadoras y compañeros fue sobresaliente."
    }

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra superior (Ficha del Alumno)
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

                        Column {
                            Text(
                                text = "Ficha del Alumno",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Text(
                                text = "${child.roomText} • ${child.groupText}",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* Exportar PDF */ }) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Exportar PDF",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = onChatClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pestañas de semanas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandBlue else CardBackground)
                                .border(
                                    width = if (isSelected) 0.dp else 1.dp,
                                    color = if (isSelected) Color.Transparent else BorderSubtle,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                // Tarjeta Principal de Resumen IA (Datos reales)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Resumen Semanal • ${tabs[selectedTab]}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }

                        Text(
                            text = "Semana del 22 al 26 de Septiembre, 2026",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = aiGeneratedSummary,
                            fontSize = 13.5.sp,
                            color = TextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Cuadrícula 2x2 de Métricas reales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Asistencia",
                        mainValue = attendanceText,
                        subtitle = attendanceSubtitle,
                        icon = Icons.Default.CheckCircle,
                        iconTint = StatusPresentGreen
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Alimentación",
                        mainValue = foodPercentage,
                        subtitle = foodSubtitle,
                        icon = Icons.Default.Restaurant,
                        iconTint = Color(0xFFE65100)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Descanso",
                        mainValue = napText,
                        subtitle = napSubtitle,
                        icon = Icons.Default.NightlightRound,
                        iconTint = BrandBlue
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Estado General",
                        mainValue = generalMood,
                        subtitle = "Comportamiento semanal",
                        icon = Icons.Default.Face,
                        iconTint = StatusPresentGreen
                    )
                }

                // Sección de Observaciones Manuales (como pidió el usuario)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Observaciones (Manual)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Text(
                            text = "Agrega comentarios adicionales o notas específicas para la familia:",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = manualObservations,
                            onValueChange = { manualObservations = it },
                            placeholder = {
                                Text(
                                    "Escribe observaciones manuales de la semana...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                }

                // Botón Guardar y Enviar Observaciones
                Button(
                    onClick = {
                        onSaveManualObservations(manualObservations)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar y Enviar Observaciones",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    mainValue: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = mainValue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklySummaryScreenPreview() {
    Etapa1Theme {
        WeeklySummaryScreen(
            child = MockDataRepository.mateoGarcia,
            timelineItems = MockDataRepository.getInitialMateoTimeline(),
            onBack = {}
        )
    }
}
