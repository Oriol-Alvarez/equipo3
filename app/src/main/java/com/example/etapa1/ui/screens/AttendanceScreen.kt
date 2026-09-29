package com.example.etapa1.ui.screens

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.Child
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.Room
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.components.EgresoFormSection
import com.example.etapa1.ui.components.EgresoFormState
import com.example.etapa1.ui.components.IngresoFormSection
import com.example.etapa1.ui.components.IngresoFormState
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun AttendanceScreen(
    room: Room,
    children: List<Child> = MockDataRepository.childrenSala1A,
    initialChild: Child? = null,
    onBack: () -> Unit,
    onSaveAttendance: (child: Child, isIngreso: Boolean, time: String, notes: String) -> Unit = { _, _, _, _ -> }
) {
    val selectedChild = initialChild ?: children.firstOrNull() ?: MockDataRepository.mateoGarcia
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Ingreso, 1 = Egreso
    var showValidationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }

    // Fecha y hora automáticas
    val calendar = remember { Calendar.getInstance() }
    val currentDateStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
    }
    val currentTimeStr = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
    }
    val defaultDayIndex = remember {
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> 0
        }
    }

    var attendanceDate by remember { mutableStateOf(currentDateStr) }
    var selectedDayIndex by remember { mutableIntStateOf(defaultDayIndex) }

    val ingresoState = remember(currentTimeStr) { IngresoFormState(entryTime = currentTimeStr) }
    val egresoState = remember(currentTimeStr) { EgresoFormState(exitTime = currentTimeStr) }

    // Validación de campos obligatorios y firmas (solo el campo de observaciones puede quedar en blanco)
    val isIngresoValid = attendanceDate.isNotBlank() &&
        ingresoState.assistantName.isNotBlank() &&
        ingresoState.entryTime.isNotBlank() &&
        ingresoState.delivererName.isNotBlank() &&
        ingresoState.hasDelivererSignature &&
        ingresoState.receiverStaffName.isNotBlank() &&
        ingresoState.hasReceiverStaffSignature &&
        (!ingresoState.hasSymptoms || ingresoState.symptomsDetail.isNotBlank()) &&
        (ingresoState.goodPhysicalCondition || ingresoState.injuryDetail.isNotBlank())

    val isEgresoValid = egresoState.exitTime.isNotBlank() &&
        egresoState.receiverTutorName.isNotBlank() &&
        egresoState.hasReceiverTutorSignature &&
        egresoState.delivererStaffName.isNotBlank() &&
        egresoState.hasDelivererStaffSignature &&
        (egresoState.goodPhysicalCondition || egresoState.injuryDetail.isNotBlank())

    val isFormValid = if (activeTab == 0) isIngresoValid else isEgresoValid

    Scaffold(containerColor = AppBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Control de Asistencia",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                    Text(
                        text = "${room.name} • ${room.level}",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Ficha del alumno seleccionado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChildAvatar(
                            initials = selectedChild.avatarInitials,
                            bgColor = Color(selectedChild.avatarBgColor),
                            size = 46.dp,
                            showStatusDot = true,
                            isPresent = selectedChild.isPresent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedChild.fullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${selectedChild.ageText} • ${selectedChild.groupText}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Selector de pestañas: Ingreso vs Egreso
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandBlueContainer)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Registro de Ingreso", "Registro de Egreso").forEachIndexed { index, title ->
                            val isSelected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) BrandBlue else Color.Transparent)
                                    .clickable { activeTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else BrandBlue,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Formularios modulares
                if (activeTab == 0) {
                    IngresoFormSection(
                        attendanceDate = attendanceDate,
                        onDateChange = { attendanceDate = it },
                        selectedDayIndex = selectedDayIndex,
                        onDaySelected = { selectedDayIndex = it },
                        state = ingresoState
                    )
                } else {
                    EgresoFormSection(state = egresoState)
                }

                // Botón principal
                Button(
                    onClick = {
                        val isIngreso = activeTab == 0
                        if (!isFormValid) {
                            showValidationDialog = true
                            return@Button
                        }
                        val timeStr = if (isIngreso) "${ingresoState.entryTime} AM" else "${egresoState.exitTime} PM"
                        val notes = if (isIngreso) ingresoState.observations else egresoState.observations
                        onSaveAttendance(selectedChild, isIngreso, timeStr, notes)
                        successDialogMessage = if (isIngreso) {
                            "Se registró exitosamente el Ingreso de ${selectedChild.fullName} a las ${ingresoState.entryTime} hrs."
                        } else {
                            "Se registró exitosamente el Egreso de ${selectedChild.fullName} a las ${egresoState.exitTime} hrs."
                        }
                        showSuccessDialog = true
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
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeTab == 0) "Guardar Registro de Ingreso" else "Guardar Registro de Egreso",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "* Todos los campos y firmas son obligatorios excepto observaciones",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Diálogo de advertencia cuando se intenta guardar con campos o firmas vacías
    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Campos o firmas faltantes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AlertRed,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "No es posible registrar el ${if (activeTab == 0) "Ingreso" else "Egreso"} porque hay campos o firmas sin completar.\n\nPor favor completa todos los datos requeridos y realiza las firmas correspondientes (únicamente el apartado de observaciones puede dejarse en blanco).",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showValidationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Revisar campos", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusPresentGreen,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Asistencia Guardada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlue,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = successDialogMessage,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Aceptar", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    Etapa1Theme {
        AttendanceScreen(
            room = MockDataRepository.rooms.first(),
            onBack = {}
        )
    }
}
