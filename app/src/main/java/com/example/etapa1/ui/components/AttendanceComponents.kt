package com.example.etapa1.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.MedicationPrescriptionOption
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AlertRedBorder
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Estado observable para el formulario de Ingreso (Sección II).
 */
class IngresoFormState(
    assistantName: String = "María Rodríguez Hernández",
    entryTime: String = "",
    delivererName: String = "Carlos García Pérez",
    receiverStaffName: String = "María Rodríguez H."
) {
    var assistantName by mutableStateOf(assistantName)
    var entryTime by mutableStateOf(entryTime)
    var delivererName by mutableStateOf(delivererName)
    var hasDelivererSignature by mutableStateOf(false)
    var receiverStaffName by mutableStateOf(receiverStaffName)
    var hasReceiverStaffSignature by mutableStateOf(false)
    var isChildSick by mutableStateOf(false)
    var medicationOption by mutableStateOf(MedicationPrescriptionOption.NO_APLICA)
    var hasSymptoms by mutableStateOf(false)
    var symptomsDetail by mutableStateOf("")
    var isClean by mutableStateOf(true)
    var backpackComplete by mutableStateOf(true)
    var goodPhysicalCondition by mutableStateOf(true)
    var injuryDetail by mutableStateOf("")
    var observations by mutableStateOf("")
}

/**
 * Estado observable para el formulario de Egreso (Sección IV).
 */
class EgresoFormState(
    exitTime: String = "",
    receiverTutorName: String = "Carlos García Pérez",
    delivererStaffName: String = "María Rodríguez H."
) {
    var goodPhysicalCondition by mutableStateOf(true)
    var injuryDetail by mutableStateOf("")
    var exitTime by mutableStateOf(exitTime)
    var receiverTutorName by mutableStateOf(receiverTutorName)
    var hasReceiverTutorSignature by mutableStateOf(false)
    var isClean by mutableStateOf(true)
    var backpackComplete by mutableStateOf(true)
    var observations by mutableStateOf("")
    var delivererStaffName by mutableStateOf(delivererStaffName)
    var hasDelivererStaffSignature by mutableStateOf(false)
}

/**
 * Sección II: Formulario modular de Ingreso a la Estancia Infantil.
 */
@Composable
fun IngresoFormSection(
    attendanceDate: String,
    onDateChange: (String) -> Unit,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    state: IngresoFormState
) {
    val daysOfWeek = listOf("L", "M", "M", "J", "V")
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Fecha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = attendanceDate,
                    onValueChange = onDateChange,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    daysOfWeek.forEachIndexed { index, dayLetter ->
                        val isSelected = selectedDayIndex == index
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BrandBlue else BrandBlueContainer)
                                .clickable { onDaySelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = dayLetter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else BrandBlue)
                        }
                    }
                }
            }
        }

        AttendanceTextField(
            label = "Nombre completo de la Asistente al cuidado de la (el) niña (o):",
            value = state.assistantName,
            onValueChange = { state.assistantName = it }
        )

        AttendanceTextField(
            label = "Hora de entrada:",
            value = state.entryTime,
            onValueChange = { state.entryTime = it },
            leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = BrandBlue, modifier = Modifier.size(18.dp)) },
            trailingText = "hrs."
        )

        SignatureFieldGroup(
            title = "Nombre y firma de quien entrega a la niña o el niño:",
            name = state.delivererName,
            onNameChange = { state.delivererName = it },
            namePlaceholder = "Nombre de quien entrega",
            signatureLabel = "Firma de quien entrega:",
            onSignedChanged = { state.hasDelivererSignature = it }
        )

        SignatureFieldGroup(
            title = "Nombre y firma del personal de la E.I., quien recibe a la niña o el niño en la E.I.:",
            name = state.receiverStaffName,
            onNameChange = { state.receiverStaffName = it },
            namePlaceholder = "Nombre del personal de la E.I.",
            signatureLabel = "Firma del personal receptor de la E.I.:",
            onSignedChanged = { state.hasReceiverStaffSignature = it }
        )

        HorizontalDivider(color = BorderSubtle)

        BinaryChoiceRow(
            question = "¿La niña o el niño está enferma (o)?",
            value = state.isChildSick,
            onValueChange = { state.isChildSick = it }
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "En el caso de que la niña o el niño ingrese enfermo se dejó medicamento con receta médica y especificaciones",
                fontSize = 12.5.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedicationPrescriptionOption.values().forEach { option ->
                    val isSelected = state.medicationOption == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandBlue else BrandBlueContainer)
                            .clickable { state.medicationOption = option }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = option.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else BrandBlue)
                    }
                }
            }
        }

        BinaryChoiceRow(
            question = "La niña o el niño ingresa con síntomas de enfermedad",
            value = state.hasSymptoms,
            onValueChange = { state.hasSymptoms = it }
        )

        if (state.hasSymptoms) {
            AttendanceTextField(
                label = "¿Cuál(es)?",
                value = state.symptomsDetail,
                onValueChange = { state.symptomsDetail = it },
                placeholder = "Especifique los síntomas presentados...",
                singleLine = false,
                maxLines = 2
            )
        }

        BinaryChoiceRow(
            question = "¿La niña o el niño se presentó limpia (o)?",
            value = state.isClean,
            onValueChange = { state.isClean = it }
        )

        BinaryChoiceRow(
            question = "¿La niña o el niño trajo su mochila completa según sus necesidades?",
            value = state.backpackComplete,
            onValueChange = { state.backpackComplete = it }
        )

        BinaryChoiceRow(
            question = "¿La niña o el niño se presentó en buen estado físico?",
            value = state.goodPhysicalCondition,
            onValueChange = {
                state.goodPhysicalCondition = it
                if (it) state.injuryDetail = ""
            }
        )

        if (!state.goodPhysicalCondition) {
            InjurySpecificationField(
                value = state.injuryDetail,
                onValueChange = { state.injuryDetail = it }
            )
        }

        AttendanceTextField(
            label = "Observaciones para las (os) Madres, Padres o personas autorizadas (opcional):",
            value = state.observations,
            onValueChange = { state.observations = it },
            placeholder = "Escriba observaciones relevantes al ingreso (opcional)...",
            singleLine = false,
            maxLines = 3
        )
    }
}

/**
 * Sección IV: Formulario modular de Egreso de la Estancia Infantil.
 */
@Composable
fun EgresoFormSection(state: EgresoFormState) {
    SectionCard {
        BinaryChoiceRow(
            question = "¿La niña o el niño se entrega en buen estado físico?",
            value = state.goodPhysicalCondition,
            onValueChange = {
                state.goodPhysicalCondition = it
                if (it) state.injuryDetail = ""
            }
        )

        if (!state.goodPhysicalCondition) {
            InjurySpecificationField(
                value = state.injuryDetail,
                onValueChange = { state.injuryDetail = it }
            )
        }

        AttendanceTextField(
            label = "Hora de salida:",
            value = state.exitTime,
            onValueChange = { state.exitTime = it },
            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = BrandBlue, modifier = Modifier.size(18.dp)) },
            trailingText = "hrs."
        )

        SignatureFieldGroup(
            title = "Nombre y firma de quien recibe a la niña o niño:",
            name = state.receiverTutorName,
            onNameChange = { state.receiverTutorName = it },
            namePlaceholder = "Nombre de quien recoge a la niña/niño",
            signatureLabel = "Firma de quien recibe:",
            onSignedChanged = { state.hasReceiverTutorSignature = it }
        )

        HorizontalDivider(color = BorderSubtle)

        BinaryChoiceRow(
            question = "¿La niña o el niño se entregó limpia(o)?",
            value = state.isClean,
            onValueChange = { state.isClean = it }
        )

        BinaryChoiceRow(
            question = "¿La niña o el niño se entregó con pertenencias completas (mochila completa)?",
            value = state.backpackComplete,
            onValueChange = { state.backpackComplete = it }
        )

        AttendanceTextField(
            label = "Observaciones (opcional):",
            value = state.observations,
            onValueChange = { state.observations = it },
            placeholder = "Observaciones sobre el día o la salida del niño/a (opcional)...",
            singleLine = false,
            maxLines = 3
        )

        SignatureFieldGroup(
            title = "Nombre y firma del personal de la E.I., quien entrega a la niña o el niño en la E.I.:",
            name = state.delivererStaffName,
            onNameChange = { state.delivererStaffName = it },
            namePlaceholder = "Nombre del personal que entrega",
            signatureLabel = "Firma del personal que entrega:",
            onSignedChanged = { state.hasDelivererStaffSignature = it }
        )
    }
}

/**
 * Tarjeta contenedor para las secciones del formulario de asistencia.
 */
@Composable
fun SectionCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBlueContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }
            content()
        }
    }
}

/**
 * Campo de texto estilizado reutilizable para el formulario de asistencia.
 */
@Composable
fun AttendanceTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingText: String? = null,
    isAlert: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isAlert) AlertRed else TextPrimary,
            lineHeight = 17.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, fontSize = 13.sp) } } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            maxLines = maxLines,
            leadingIcon = leadingIcon,
            trailingIcon = trailingText?.let {
                { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, modifier = Modifier.padding(end = 12.dp)) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = if (isAlert) AlertRed else BrandBlue,
                unfocusedBorderColor = if (isAlert) AlertRedBorder else BorderSubtle
            )
        )
    }
}

/**
 * Agrupación de nombre de persona y su lienzo de firma digital.
 */
@Composable
fun SignatureFieldGroup(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    namePlaceholder: String,
    signatureLabel: String,
    onSignedChanged: (Boolean) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AttendanceTextField(
            label = title,
            value = name,
            onValueChange = onNameChange,
            placeholder = namePlaceholder
        )
        SignaturePad(label = signatureLabel, onSignedChanged = onSignedChanged)
    }
}

/**
 * Campo destacado para especificar lesiones físicas cuando el estado físico no es óptimo.
 */
@Composable
fun InjurySpecificationField(
    value: String,
    onValueChange: (String) -> Unit
) {
    AttendanceTextField(
        label = "En caso que la niña o el niño sea entregada(o) con algún rasguño, mordida, quemadura, golpe, rozadura, etc. Indicar la parte del cuerpo donde se presenta y especificar la lesión:",
        value = value,
        onValueChange = onValueChange,
        placeholder = "Describa parte del cuerpo y especifique la lesión...",
        singleLine = false,
        maxLines = 3,
        isAlert = true
    )
}

/**
 * Fila de selección binaria interactiva Sí / No.
 */
@Composable
fun BinaryChoiceRow(
    question: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = question,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            lineHeight = 17.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(true to "Sí", false to "No").forEach { (optionVal, label) ->
                val isSelected = value == optionVal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandBlue else BrandBlueContainer)
                        .clickable { onValueChange(optionVal) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BrandBlue
                    )
                }
            }
        }
    }
}

/**
 * Panel de firma digital interactivo con soporte para trazo táctil y botón de borrado.
 */
@Composable
fun SignaturePad(
    label: String,
    onSignedChanged: (Boolean) -> Unit = {}
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            if (strokes.isNotEmpty() || currentStroke.isNotEmpty()) {
                Text(
                    text = "Limpiar firma",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertRed,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            strokes.clear()
                            currentStroke = emptyList()
                            onSignedChanged(false)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFAFAFA))
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> currentStroke = listOf(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) {
                                strokes.add(currentStroke)
                                currentStroke = emptyList()
                                onSignedChanged(true)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(24f, size.height - 20f),
                    end = Offset(size.width - 24f, size.height - 20f),
                    strokeWidth = 1f
                )
                (strokes + listOf(currentStroke)).forEach { stroke ->
                    if (stroke.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.first().x, stroke.first().y)
                            for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
                        }
                        drawPath(path, color = Color(0xFF0F172A), style = Stroke(width = 3.5f))
                    }
                }
            }
            if (strokes.isEmpty() && currentStroke.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Firma digital aquí", fontSize = 11.5.sp, color = TextMuted)
                }
            }
        }
    }
}
