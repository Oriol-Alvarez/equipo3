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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.StatusPresentGreenBg
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
/**
 * Sección II: Formulario modular e interactivo paso a paso de Ingreso a la Estancia Infantil.
 * Las preguntas se muestran una a una, avanzando conforme se responden.
 */
@Composable
fun IngresoFormSection(
    attendanceDate: String,
    selectedDayIndex: Int,
    state: IngresoFormState,
    currentStep: Int = 0,
    onStepChange: (Int) -> Unit = {},
    onSave: () -> Unit = {},
    onDateChange: (String) -> Unit = {},
    onDaySelected: (Int) -> Unit = {},
    authorizedPersons: List<String> = emptyList()
) {
    val stepTitles = listOf(
        "Datos de Entrada",
        "Estado de Salud",
        "Medicamentos con Receta",
        "Síntomas al Ingreso",
        "Higiene del Menor",
        "Pertenencias / Mochila",
        "Estado Físico",
        "Observaciones de Ingreso",
        "Entrega (Familiar / Tutor)",
        "Recepción (Personal E.I.)"
    )
    val totalSteps = stepTitles.size
    val daysOfWeek = listOf("L", "M", "M", "J", "V")
    val coroutineScope = rememberCoroutineScope()
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }
    var stepError by remember { mutableStateOf<String?>(null) }

    fun autoAdvance(delayMs: Long = 280L) {
        stepError = null
        autoAdvanceJob?.cancel()
        autoAdvanceJob = coroutineScope.launch {
            delay(delayMs)
            if (currentStep < totalSteps - 1) {
                onStepChange(currentStep + 1)
            }
        }
    }

    fun tryAdvance(fromStep: Int) {
        stepError = null
        autoAdvanceJob?.cancel()
        when (fromStep) {
            0 -> {
                if (state.assistantName.isBlank()) {
                    stepError = "Por favor indica el nombre de la asistente"
                    return
                }
            }
            3 -> {
                if (state.hasSymptoms && state.symptomsDetail.isBlank()) {
                    stepError = "Por favor especifica los síntomas presentados"
                    return
                }
            }
            6 -> {
                if (!state.goodPhysicalCondition && state.injuryDetail.isBlank()) {
                    stepError = "Por favor describe la lesión o parte del cuerpo afectada"
                    return
                }
            }
            8 -> {
                if (state.delivererName.isBlank()) {
                    stepError = "Por favor indica el nombre de quien entrega"
                    return
                }
                if (!state.hasDelivererSignature) {
                    stepError = "La firma de quien entrega es obligatoria"
                    return
                }
            }
        }
        if (fromStep < totalSteps - 1) {
            onStepChange(fromStep + 1)
        }
    }

    fun trySave() {
        stepError = null
        autoAdvanceJob?.cancel()
        if (state.receiverStaffName.isBlank()) {
            stepError = "Por favor indica el nombre del personal receptor"
            return
        }
        if (!state.hasReceiverStaffSignature) {
            stepError = "La firma del personal receptor es obligatoria"
            return
        }
        onSave()
    }

    SectionCard {
        FormStepHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            stepTitle = stepTitles.getOrElse(currentStep) { "" },
            onStepClick = { step ->
                autoAdvanceJob?.cancel()
                stepError = null
                onStepChange(step)
            }
        )

        HorizontalDivider(color = BorderSubtle)

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it / 2 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { it / 2 } + fadeOut()
                    )
                }
            },
            label = "IngresoStepAnimation"
        ) { step ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        // Pregunta 1: Fecha, Asistente y Hora de entrada
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Fecha de registro:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = attendanceDate,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    daysOfWeek.forEachIndexed { index, dayLetter ->
                                        val isSelected = selectedDayIndex == index
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) BrandBlue else BrandBlueContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLetter,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else BrandBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AttendanceTextField(
                            label = "Nombre completo de la Asistente al cuidado de la (el) niña (o):",
                            value = state.assistantName,
                            onValueChange = {
                                state.assistantName = it
                                stepError = null
                            }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Hora de entrada:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${state.entryTime} hrs.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    1 -> {
                        // Pregunta 2: ¿La niña o el niño está enferma (o)?
                        BinaryChoiceRow(
                            question = "¿La niña o el niño está enferma (o)?",
                            value = state.isChildSick,
                            onValueChange = { state.isChildSick = it },
                            onOptionSelected = { autoAdvance() }
                        )
                    }
                    2 -> {
                        // Pregunta 3: Medicamento con receta médica
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "En el caso de que la niña o el niño ingrese enfermo se dejó medicamento con receta médica y especificaciones",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MedicationPrescriptionOption.values().forEach { option ->
                                    val isSelected = state.medicationOption == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) BrandBlue else BrandBlueContainer)
                                            .clickable {
                                                state.medicationOption = option
                                                autoAdvance()
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option.label,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else BrandBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Pregunta 4: Síntomas de enfermedad
                        BinaryChoiceRow(
                            question = "La niña o el niño ingresa con síntomas de enfermedad",
                            value = state.hasSymptoms,
                            onValueChange = { state.hasSymptoms = it },
                            onOptionSelected = { if (!it) autoAdvance() }
                        )

                        if (state.hasSymptoms) {
                            AttendanceTextField(
                                label = "¿Cuál(es)?",
                                value = state.symptomsDetail,
                                onValueChange = {
                                    state.symptomsDetail = it
                                    stepError = null
                                },
                                placeholder = "Especifique los síntomas presentados...",
                                singleLine = false,
                                maxLines = 2
                            )
                        }
                    }
                    4 -> {
                        // Pregunta 5: Higiene
                        BinaryChoiceRow(
                            question = "¿La niña o el niño se presentó limpia (o)?",
                            value = state.isClean,
                            onValueChange = { state.isClean = it },
                            onOptionSelected = { autoAdvance() }
                        )
                    }
                    5 -> {
                        // Pregunta 6: Mochila completa
                        BinaryChoiceRow(
                            question = "¿La niña o el niño trajo su mochila completa según sus necesidades?",
                            value = state.backpackComplete,
                            onValueChange = { state.backpackComplete = it },
                            onOptionSelected = { autoAdvance() }
                        )
                    }
                    6 -> {
                        // Pregunta 7: Buen estado físico
                        BinaryChoiceRow(
                            question = "¿La niña o el niño se presentó en buen estado físico?",
                            value = state.goodPhysicalCondition,
                            onValueChange = {
                                state.goodPhysicalCondition = it
                                if (it) state.injuryDetail = ""
                            },
                            onOptionSelected = { if (it) autoAdvance() }
                        )

                        if (!state.goodPhysicalCondition) {
                            InjurySpecificationField(
                                value = state.injuryDetail,
                                onValueChange = {
                                    state.injuryDetail = it
                                    stepError = null
                                }
                            )
                        }
                    }
                    7 -> {
                        // Pregunta 8: Observaciones
                        AttendanceTextField(
                            label = "Observaciones para las (os) Madres, Padres o personas autorizadas (opcional):",
                            value = state.observations,
                            onValueChange = { state.observations = it },
                            placeholder = "Escriba observaciones relevantes al ingreso (opcional)...",
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                    8 -> {
                        // Pregunta 9: Nombre y firma de quien entrega
                        SignatureFieldGroup(
                            title = "Nombre y firma de quien entrega a la niña o el niño:",
                            name = state.delivererName,
                            onNameChange = {
                                state.delivererName = it
                                stepError = null
                            },
                            namePlaceholder = "Nombre de quien entrega",
                            signatureLabel = "Firma de quien entrega:",
                            isAlreadySigned = state.hasDelivererSignature,
                            onSignedChanged = {
                                state.hasDelivererSignature = it
                                stepError = null
                            },
                            authorizedPersons = authorizedPersons
                        )
                    }
                    9 -> {
                        // Pregunta 10: Nombre y firma del personal de la E.I.
                        SignatureFieldGroup(
                            title = "Nombre y firma del personal de la E.I., quien recibe a la niña o el niño en la E.I.:",
                            name = state.receiverStaffName,
                            onNameChange = {
                                state.receiverStaffName = it
                                stepError = null
                            },
                            namePlaceholder = "Nombre del personal de la E.I.",
                            signatureLabel = " Firma del personal receptor:",
                            isAlreadySigned = state.hasReceiverStaffSignature,
                            onSignedChanged = {
                                state.hasReceiverStaffSignature = it
                                stepError = null
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderSubtle)

        FormStepNavigationButtons(
            currentStep = currentStep,
            totalSteps = totalSteps,
            errorMessage = stepError,
            onPrevious = {
                autoAdvanceJob?.cancel()
                stepError = null
                if (currentStep > 0) onStepChange(currentStep - 1)
            },
            onNext = {
                tryAdvance(currentStep)
            },
            onSave = {
                trySave()
            },
            saveButtonText = "Guardar Registro"
        )
    }
}

/**
 * Sección IV: Formulario modular e interactivo paso a paso de Egreso de la Estancia Infantil.
 * Las preguntas se muestran una a una, avanzando conforme se responden.
 */
@Composable
fun EgresoFormSection(
    state: EgresoFormState,
    currentStep: Int = 0,
    onStepChange: (Int) -> Unit = {},
    onSave: () -> Unit = {},
    authorizedPersons: List<String> = emptyList()
) {
    val stepTitles = listOf(
        "Estado Físico al Egreso",
        "Hora de Salida",
        "Higiene al Egreso",
        "Pertenencias / Mochila",
        "Observaciones del Día",
        "Recepción (Familiar / Tutor)",
        "Entrega (Personal E.I.)"
    )
    val totalSteps = stepTitles.size
    val coroutineScope = rememberCoroutineScope()
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }
    var stepError by remember { mutableStateOf<String?>(null) }

    fun autoAdvance(delayMs: Long = 280L) {
        stepError = null
        autoAdvanceJob?.cancel()
        autoAdvanceJob = coroutineScope.launch {
            delay(delayMs)
            if (currentStep < totalSteps - 1) {
                onStepChange(currentStep + 1)
            }
        }
    }

    fun tryAdvance(fromStep: Int) {
        stepError = null
        autoAdvanceJob?.cancel()
        when (fromStep) {
            0 -> {
                if (!state.goodPhysicalCondition && state.injuryDetail.isBlank()) {
                    stepError = "Por favor describe la lesión o parte del cuerpo afectada"
                    return
                }
            }
            1 -> {
                if (state.exitTime.isBlank()) {
                    stepError = "La hora de salida es obligatoria"
                    return
                }
            }
            5 -> {
                if (state.receiverTutorName.isBlank()) {
                    stepError = "Por favor indica el nombre de quien recibe"
                    return
                }
                if (!state.hasReceiverTutorSignature) {
                    stepError = "La firma de quien recibe es obligatoria"
                    return
                }
            }
        }
        if (fromStep < totalSteps - 1) {
            onStepChange(fromStep + 1)
        }
    }

    fun trySave() {
        stepError = null
        autoAdvanceJob?.cancel()
        if (state.delivererStaffName.isBlank()) {
            stepError = "Por favor indica el nombre del personal que entrega"
            return
        }
        if (!state.hasDelivererStaffSignature) {
            stepError = "La firma del personal que entrega es obligatoria"
            return
        }
        onSave()
    }

    SectionCard {
        FormStepHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            stepTitle = stepTitles.getOrElse(currentStep) { "" },
            onStepClick = { step ->
                autoAdvanceJob?.cancel()
                stepError = null
                onStepChange(step)
            }
        )

        HorizontalDivider(color = BorderSubtle)

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it / 2 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { it / 2 } + fadeOut()
                    )
                }
            },
            label = "EgresoStepAnimation"
        ) { step ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        // Pregunta 1: Buen estado físico al egreso
                        BinaryChoiceRow(
                            question = "¿La niña o el niño se entrega en buen estado físico?",
                            value = state.goodPhysicalCondition,
                            onValueChange = {
                                state.goodPhysicalCondition = it
                                if (it) state.injuryDetail = ""
                            },
                            onOptionSelected = { if (it) autoAdvance() }
                        )

                        if (!state.goodPhysicalCondition) {
                            InjurySpecificationField(
                                value = state.injuryDetail,
                                onValueChange = {
                                    state.injuryDetail = it
                                    stepError = null
                                }
                            )
                        }
                    }
                    1 -> {
                        // Pregunta 2: Hora de salida
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hora de salida de la estancia:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${state.exitTime} hrs.",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    2 -> {
                        // Pregunta 3: Higiene al egreso
                        BinaryChoiceRow(
                            question = "¿La niña o el niño se entregó limpia(o)?",
                            value = state.isClean,
                            onValueChange = { state.isClean = it },
                            onOptionSelected = { autoAdvance() }
                        )
                    }
                    3 -> {
                        // Pregunta 4: Pertenencias completas
                        BinaryChoiceRow(
                            question = "¿La niña o el niño se entregó con pertenencias completas (mochila completa)?",
                            value = state.backpackComplete,
                            onValueChange = { state.backpackComplete = it },
                            onOptionSelected = { autoAdvance() }
                        )
                    }
                    4 -> {
                        // Pregunta 5: Observaciones
                        AttendanceTextField(
                            label = "Observaciones sobre el día o la salida (opcional):",
                            value = state.observations,
                            onValueChange = { state.observations = it },
                            placeholder = "Observaciones sobre el día o la salida del niño/a (opcional)...",
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                    5 -> {
                        // Pregunta 6: Nombre y firma quien recibe (Tutor)
                        SignatureFieldGroup(
                            title = "Nombre y firma de quien recibe a la niña o niño:",
                            name = state.receiverTutorName,
                            onNameChange = {
                                state.receiverTutorName = it
                                stepError = null
                            },
                            namePlaceholder = "Nombre de quien recoge a la niña/niño",
                            signatureLabel = "Firma de quien recibe:",
                            isAlreadySigned = state.hasReceiverTutorSignature,
                            onSignedChanged = {
                                state.hasReceiverTutorSignature = it
                                stepError = null
                            },
                            authorizedPersons = authorizedPersons
                        )
                    }
                    6 -> {
                        // Pregunta 7: Nombre y firma personal que entrega
                        SignatureFieldGroup(
                            title = "Nombre y firma del personal de la E.I., quien entrega a la niña o el niño en la E.I.:",
                            name = state.delivererStaffName,
                            onNameChange = {
                                state.delivererStaffName = it
                                stepError = null
                            },
                            namePlaceholder = "Nombre del personal que entrega",
                            signatureLabel = "Firma del personal que entrega:",
                            isAlreadySigned = state.hasDelivererStaffSignature,
                            onSignedChanged = {
                                state.hasDelivererStaffSignature = it
                                stepError = null
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderSubtle)

        FormStepNavigationButtons(
            currentStep = currentStep,
            totalSteps = totalSteps,
            errorMessage = stepError,
            onPrevious = {
                autoAdvanceJob?.cancel()
                stepError = null
                if (currentStep > 0) onStepChange(currentStep - 1)
            },
            onNext = {
                tryAdvance(currentStep)
            },
            onSave = {
                trySave()
            },
            saveButtonText = "Guardar Registro"
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

data class AuthorizedPersonOption(
    val name: String,
    val relationship: String? = null,
    val phone: String? = null
) {
    val subtitle: String?
        get() {
            val parts = mutableListOf<String>()
            if (!relationship.isNullOrBlank()) {
                parts.add(relationship)
            }
            if (!phone.isNullOrBlank()) {
                parts.add("Tel: $phone")
            }
            return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
        }
}

private fun parseAuthorizedPersonOption(raw: String): AuthorizedPersonOption {
    // 1. Extraer nombre limpio (antes de '(', '-', '•', '·', 'Tel:')
    val cleanName = raw.substringBefore("(")
        .substringBefore(" -")
        .substringBefore(" •")
        .substringBefore(" ·")
        .substringBefore("Tel:")
        .trim()

    // 2. Extraer parentesco entre paréntesis si existe: ej. (Madre), (Abuela materna)
    val relMatch = Regex("""\(([^)]+)\)""").find(raw)
    val relationship = relMatch?.groupValues?.get(1)?.trim()

    // 3. Extraer teléfono: busca prefijos comunes como 'Tel:', 'Teléfono:', 'Cel:'
    // y captura el número de teléfono sin CURP ni otros datos
    val phoneMatch = Regex("""(?:Tel|Teléfono|Cel)[:.\s]+([+0-9\s-]+)""", RegexOption.IGNORE_CASE).find(raw)
    val phone = phoneMatch?.groupValues?.get(1)
        ?.substringBefore("•")
        ?.substringBefore("·")
        ?.substringBefore(";")
        ?.trim()
        ?.trimEnd('.', ',', '-', ' ', '·', '•')

    return AuthorizedPersonOption(
        name = cleanName.ifBlank { raw.trim() },
        relationship = relationship,
        phone = phone
    )
}

@Composable
fun AttendanceAuthorizedDropdownTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    authorizedPersons: List<String> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }

    // Procesar personas autorizadas deduplicando por nombre y extrayendo parentesco y teléfono sin CURP
    val options = remember(authorizedPersons) {
        val map = linkedMapOf<String, AuthorizedPersonOption>()
        for (raw in authorizedPersons) {
            if (raw.isBlank()) continue
            val parsed = parseAuthorizedPersonOption(raw)
            val key = parsed.name.lowercase().trim()
            if (key.isBlank()) continue

            val existing = map[key]
            if (existing == null) {
                map[key] = parsed
            } else {
                map[key] = existing.copy(
                    relationship = existing.relationship ?: parsed.relationship,
                    phone = existing.phone ?: parsed.phone
                )
            }
        }
        map.values.toList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = if (placeholder.isNotEmpty()) {
                    { Text(placeholder, fontSize = 13.sp, color = TextSecondary) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (options.isNotEmpty()) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Mostrar personas autorizadas",
                            tint = BrandBlue
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            // Superposición que intercepta los clics para desplegar las opciones
            // impidiendo la escritura manual y el teclado en pantalla
            if (options.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = !expanded }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White)
                ) {
                    Text(
                        text = "Personas autorizadas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    options.forEach { option ->
                        val isSelected = option.name.equals(value.trim(), ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = option.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) BrandBlue else TextPrimary
                                    )
                                    option.subtitle?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onValueChange(option.name)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
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
    isAlreadySigned: Boolean = false,
    onSignedChanged: (Boolean) -> Unit = {},
    authorizedPersons: List<String> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (authorizedPersons.isNotEmpty()) {
            AttendanceAuthorizedDropdownTextField(
                label = title,
                value = name,
                onValueChange = onNameChange,
                placeholder = namePlaceholder,
                authorizedPersons = authorizedPersons
            )
        } else {
            AttendanceTextField(
                label = title,
                value = name,
                onValueChange = onNameChange,
                placeholder = namePlaceholder
            )
        }
        SignaturePad(
            label = signatureLabel,
            isAlreadySigned = isAlreadySigned,
            onSignedChanged = onSignedChanged
        )
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
 * Fila de selección binaria interactiva Sí / No con soporte para auto-avance.
 */
@Composable
fun BinaryChoiceRow(
    question: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    onOptionSelected: ((Boolean) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = question,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            lineHeight = 18.sp
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandBlue else BrandBlueContainer)
                        .clickable {
                            onValueChange(optionVal)
                            onOptionSelected?.invoke(optionVal)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
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
    isAlreadySigned: Boolean = false,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                if (isAlreadySigned || strokes.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "✓ Registrada", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusPresentGreen)
                }
            }
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
                    Text(
                        text = if (isAlreadySigned) "Firma guardada (puedes volver a trazar)" else "Firma digital aquí",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Cabecera de progreso para el formulario interactivo pregunta a pregunta.
 */
@Composable
fun FormStepHeader(
    currentStep: Int,
    totalSteps: Int,
    stepTitle: String,
    onStepClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandBlueContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Pregunta ${currentStep + 1} de $totalSteps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stepTitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Text(
                text = "${((currentStep + 1) * 100) / totalSteps}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )
        }

        // Barra de progreso segmentada e interactiva
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                val isCompleted = i < currentStep
                val isCurrent = i == currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCurrent -> BrandBlue
                                isCompleted -> StatusPresentGreen
                                else -> BorderSubtle
                            }
                        )
                        .clickable {
                            if (i <= currentStep) {
                                onStepClick(i)
                            }
                        }
                )
            }
        }
    }
}

/**
 * Botones de navegación inferior (Anterior, Siguiente y Guardar) para cada pregunta.
 */
@Composable
fun FormStepNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    errorMessage: String? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    saveButtonText: String = "Guardar Registro"
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = AlertRed,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Anterior", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (currentStep < totalSteps - 1) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(if (currentStep > 0) 1.2f else 1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Siguiente", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(if (currentStep > 0) 1.5f else 1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = saveButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

