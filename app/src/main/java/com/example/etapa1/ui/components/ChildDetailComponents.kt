package com.example.etapa1.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyActivityInfo
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.DailyEgresoInfo
import com.example.etapa1.model.DailyIngresoInfo
import com.example.etapa1.model.WeeklySummary
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusAbsentOrange
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun ChildHeaderSummaryCard(
    child: Child
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChildAvatar(
                initials = child.avatarInitials,
                bgColor = Color(child.avatarBgColor),
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${child.ageText} • ${child.roomText}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (child.isPresent) StatusPresentGreen else StatusAbsentOrange)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = child.statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (child.isPresent) StatusPresentGreen else StatusAbsentOrange
                    )
                }
            }
        }
    }
}

@Composable
fun ChildFullDataContent(
    child: Child,
    fullProfile: ChildFullProfile,
    onEditClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón Modificar Datos (si se permite edición, ej: vista de familiares)
        if (onEditClick != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modificar datos",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Modificar Ficha del Alumno",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandBlue
                            )
                            Text(
                                text = "Pediatra, observaciones, emergencias y autorizados",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Card 1: Identificación y Matrícula
        SectionDetailCard(title = "Datos Personales y Escolares", icon = Icons.Default.Badge) {
            DetailRowItem(label = "Nombre completo", value = child.fullName)
            DetailRowItem(label = "Matrícula / ID", value = fullProfile.studentId)
            DetailRowItem(label = "Fecha de nacimiento", value = "${fullProfile.birthDate} (${child.ageText})")
            DetailRowItem(label = "Fecha de ingreso", value = fullProfile.enrollmentDate)
            DetailRowItem(label = "Sala y nivel", value = "${child.roomText} • ${child.groupText}")
        }

        // Card 2: Contacto Familiar y Personas Autorizadas
        SectionDetailCard(title = "Familia y Recogida", icon = Icons.Default.FamilyRestroom) {
            DetailRowItem(label = "Madre", value = "${fullProfile.motherName} (${fullProfile.motherPhone})")
            DetailRowItem(label = "Padre", value = "${fullProfile.fatherName} (${fullProfile.fatherPhone})")
            DetailRowItem(label = "Teléfono de emergencia", value = fullProfile.emergencyPhone)

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Personas autorizadas para recogida:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            fullProfile.authorizedPickups.forEach { person ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusPresentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = person,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // Card 3: Ficha Médica
        SectionDetailCard(title = "Salud y Cuidados Especiales", icon = Icons.Default.LocalHospital) {
            if (child.allergyAlert != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AlertRed.copy(alpha = 0.1f))
                        .border(1.dp, AlertRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = child.allergyAlert,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            DetailRowItem(label = "Grupo sanguíneo", value = fullProfile.bloodType)
            DetailRowItem(label = "Pediatra de referencia", value = "${fullProfile.pediatrician} (${fullProfile.pediatricianPhone})")
            DetailRowItem(label = "Observaciones médicas", value = fullProfile.medicalNotes)

            if (!fullProfile.medicalCertificateName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue.copy(alpha = 0.08f))
                        .border(1.dp, BrandBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Certificado Médico Oficial",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Acreditado",
                                    tint = StatusPresentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = fullProfile.medicalCertificateName,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            if (!fullProfile.medicalCertificateDate.isNullOrBlank()) {
                                Text(
                                    text = fullProfile.medicalCertificateDate,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Observaciones Generales
        SectionDetailCard(title = "Hábitos y Observaciones Pedagógicas", icon = Icons.Default.Face) {
            Text(
                text = fullProfile.generalNotes,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun DailyBitacoraContent(
    bitacoras: List<DailyBitacora>,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val currentBitacora = bitacoras.getOrNull(selectedDayIndex) ?: bitacoras.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Selector horizontal de días
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(bitacoras) { index, item ->
                    val isSelected = index == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BrandBlue else CardBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BrandBlue else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDay(index) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.dayLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }
        }

        if (currentBitacora != null) {
            // Sección: Datos de Ingreso y Egreso
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Registro de Ingreso y Egreso",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IngresoEgresoCard(
                    ingreso = currentBitacora.ingreso,
                    egreso = currentBitacora.egreso,
                    isToday = currentBitacora.isToday
                )
            }

            // Sección: Actividades registradas por la cuidadora
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actividades de la jornada",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandBlueContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${currentBitacora.activities.size} registradas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    }
                }

                if (currentBitacora.activities.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay actividades registradas en esta fecha.",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentBitacora.activities.forEach { activity ->
                            CaregiverActivityCard(activity = activity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklySummaryContent(
    summaries: List<WeeklySummary>,
    selectedWeekIndex: Int = 0,
    onSelectWeek: (Int) -> Unit = {},
    child: Child
) {
    val currentSummary = summaries.getOrNull(selectedWeekIndex) ?: summaries.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Selector horizontal de semanas (igual que en Bitácora Diaria)
        if (summaries.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(summaries) { index, item ->
                        val isSelected = index == selectedWeekIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandBlue else CardBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BrandBlue else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectWeek(index) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.weekLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (currentSummary != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta Principal: Síntesis Automática Inteligente
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrandBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandBlueContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Resumen Semanal • ${currentSummary.weekLabel}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentSummary.weekRangeText,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentSummary.executiveSummary,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 19.sp
                        )
                    }
                }

                // Métricas Rápidas en Cuadrícula (2x2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeeklyMetricCard(
                        title = "Asistencia",
                        value = "${currentSummary.attendanceDaysCount}/${currentSummary.totalSchoolDays} días",
                        subtext = "${currentSummary.attendancePercentageText} • Entrada ${currentSummary.averageArrivalTime}",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        color = StatusPresentGreen,
                        modifier = Modifier.weight(1f)
                    )

                    WeeklyMetricCard(
                        title = "Alimentación",
                        value = currentSummary.foodIntakePercentage,
                        subtext = "Aceptación general",
                        icon = Icons.Default.Restaurant,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeeklyMetricCard(
                        title = "Descanso",
                        value = currentSummary.totalNapHours,
                        subtext = "Prom. ${currentSummary.averageNapDaily}",
                        icon = Icons.Default.NightlightRound,
                        color = Color(0xFF5E35B1),
                        modifier = Modifier.weight(1f)
                    )

                    WeeklyMetricCard(
                        title = "Estado General",
                        value = currentSummary.moodSummary,
                        subtext = "Comportamiento semanal",
                        icon = Icons.Default.Face,
                        color = Color(0xFF00897B),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Alimentación y Hábitos de la Semana
                SectionDetailCard(title = "Alimentación y Hábitos de la Semana", icon = Icons.Default.Restaurant) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentSummary.foodHighlights.forEach { highlight ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = highlight,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Descanso y Siestas
                SectionDetailCard(title = "Descanso y Sueño", icon = Icons.Default.NightlightRound) {
                    Text(
                        text = currentSummary.napHighlights,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }

                // Hitos y Actividades Pedagógicas
                SectionDetailCard(title = "Actividades Pedagógicas y Aprendizaje", icon = Icons.Default.Star) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentSummary.pedagogicalHighlights.forEach { highlight ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPresentGreen,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = highlight,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Salud y Protocolos
                SectionDetailCard(title = "Salud, Bienestar y Seguridad", icon = Icons.Default.LocalHospital) {
                    Text(
                        text = currentSummary.healthAndSafetySummary,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklySummaryContent(
    summary: WeeklySummary,
    child: Child
) {
    WeeklySummaryContent(
        summaries = listOf(summary),
        selectedWeekIndex = 0,
        onSelectWeek = {},
        child = child
    )
}

@Composable
fun WeeklyMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun IngresoEgresoCard(
    ingreso: DailyIngresoInfo?,
    egreso: DailyEgresoInfo?,
    isToday: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Sección Ingreso
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(StatusPresentGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = StatusPresentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "I. Registro de Ingreso",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = ingreso?.time?.let { "Hora: $it" } ?: "Sin registro",
                        fontSize = 12.sp,
                        color = if (ingreso != null) StatusPresentGreen else TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (ingreso != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRowItem(label = "Entregado por", value = ingreso.delivererName)
                DetailRowItem(label = "Recibido por", value = ingreso.receiverName)
                DetailRowItem(label = "Estado físico", value = ingreso.physicalCondition)
                DetailRowItem(label = "Síntomas", value = if (ingreso.hasSymptoms) ingreso.symptomsDetail else "Sin síntomas aparentes")
                if (ingreso.observations.isNotBlank()) {
                    DetailRowItem(label = "Observaciones", value = ingreso.observations)
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El alumno aún no ha sido registrado en esta fecha.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = BorderSubtle
            )

            // Sección Egreso
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (egreso != null) BrandBlueContainer else StatusAbsentOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (egreso != null) BrandBlue else StatusAbsentOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "II. Registro de Egreso",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = egreso?.time?.let { "Hora: $it" } ?: if (isToday) "Pendiente de salida" else "Sin egreso registrado",
                        fontSize = 12.sp,
                        color = if (egreso != null) BrandBlue else StatusAbsentOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (egreso != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRowItem(label = "Recogido por", value = egreso.collectorName)
                DetailRowItem(label = "Entregado por", value = egreso.delivererName)
                DetailRowItem(label = "Estado físico a la salida", value = egreso.physicalCondition)
                if (egreso.observations.isNotBlank()) {
                    DetailRowItem(label = "Observaciones", value = egreso.observations)
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isToday) "El alumno continúa en la estancia bajo cuidado de la sala." else "No se asentó salida en este día.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 36.dp)
                )
            }
        }
    }
}

@Composable
fun CaregiverActivityCard(activity: DailyActivityInfo) {
    val (categoryIcon, categoryColor) = when (activity.category) {
        ActivityCategory.ALIMENTACION -> Pair(Icons.Default.Restaurant, Color(0xFFE65100))
        ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> Pair(Icons.Default.NightlightRound, Color(0xFF5E35B1))
        ActivityCategory.FUNCIONES_EXCRETORAS -> Pair(Icons.Default.Face, Color(0xFF0288D1))
        ActivityCategory.ESTADO_ANIMO -> Pair(Icons.Default.Face, Color(0xFF00897B))
        ActivityCategory.ACCIDENTES -> Pair(Icons.Default.Warning, Color(0xFFD32F2F))
        ActivityCategory.SALUD -> Pair(Icons.Default.LocalHospital, Color(0xFFC2185B))
        ActivityCategory.OTROS -> Pair(Icons.Default.MoreHoriz, Color(0xFF1E88E5))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activity.category.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activity.portion != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Porción: ${activity.portion.label} (${activity.portion.fractionText})",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = activity.time,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activity.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = activity.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SectionDetailCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
            }
            content()
        }
    }
}

@Composable
fun DetailRowItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(0.6f)
        )
    }
}
