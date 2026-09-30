package com.example.casosdeusopadre.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.casosdeusopadre.ui.screens.certificate.ChildSelectorSimple
import com.example.casosdeusopadre.ui.theme.AppShapes

// CU-05 Resumen semanal y reporte mensual (RF-12, RF-18)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Reportes y Resúmenes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.children.isNotEmpty()) {
            ChildSelectorSimple(
                children = uiState.children,
                selectedChild = uiState.selectedChild,
                onChildSelected = { viewModel.selectChild(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Segmented Control (Tabs)
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(AppShapes.medium)
            ) {
                Tab(
                    selected = uiState.selectedTab == ReportTab.WEEKLY,
                    onClick = { viewModel.selectTab(ReportTab.WEEKLY) },
                    text = { Text("Resumen Semanal", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == ReportTab.MONTHLY_AI,
                    onClick = { viewModel.selectTab(ReportTab.MONTHLY_AI) },
                    text = { Text("Reporte Mensual IA", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content Area
            when (uiState.selectedTab) {
                ReportTab.WEEKLY -> {
                    WeeklyReportView(
                        weeklyData = uiState.weeklyData,
                        onExportClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, uiState.exportSummaryText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    )
                }
                ReportTab.MONTHLY_AI -> {
                    MonthlyAiReportView(
                        reportText = uiState.monthlyAiReport,
                        isFinished = uiState.isMonthFinished
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyReportView(weeklyData: List<WeeklyDayData>, onExportClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Desempeño de la semana",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Día", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Asistencia", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("Alimentación", fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()

            // Table Rows
            weeklyData.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.dayName, modifier = Modifier.weight(1f))
                    
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day.present) {
                            Icon(Icons.Default.Check, contentDescription = "Presente", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Close, contentDescription = "Ausente", tint = Color(0xFFF44336), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Text(
                        text = day.mealPattern,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                        color = if (day.present) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Button (RF-12)
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = AppShapes.medium
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar / Compartir Resumen")
            }
        }
    }
}

@Composable
fun MonthlyAiReportView(reportText: String, isFinished: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = AppShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isFinished) Icons.Default.AutoAwesome else Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (isFinished) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = reportText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                color = if (isFinished) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
