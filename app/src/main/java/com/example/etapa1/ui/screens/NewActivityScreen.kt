package com.example.etapa1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.ExcretionType
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.MealType
import com.example.etapa1.model.MockDataRepository
import com.example.etapa1.model.MoodState
import com.example.etapa1.ui.components.AccidentesSectionContent
import com.example.etapa1.ui.components.ActivityCategoryGrid
import com.example.etapa1.ui.components.AlimentacionSectionContent
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.components.DescansoSectionContent
import com.example.etapa1.ui.components.EstadoAnimoSectionContent
import com.example.etapa1.ui.components.FuncionesExcretorasSectionContent
import com.example.etapa1.ui.components.SaludSectionContent
import com.example.etapa1.ui.components.SectionCardContainer
import com.example.etapa1.ui.state.NewActivityViewModel
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

@Composable
fun NewActivityScreen(
    viewModel: NewActivityViewModel,
    child: Child,
    targetChildIds: List<String>? = null,
    groupId: String? = null,
    onBack: () -> Unit,
    onSubmitActivity: (category: ActivityCategory, summaryBadge: String, description: String, shareWithFamilyGroups: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val isGroup = targetChildIds != null && targetChildIds.isNotEmpty()
    NewActivityScreen(
        child = child,
        isGroupActivity = isGroup,
        onBack = onBack,
        onSubmitActivityDetailed = { cat, badge, desc, shareWithGroups, mealType, foodCons, excType, excAviso, excNoAplica ->
            val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            if (isGroup) {
                viewModel.saveGroupActivity(
                    groupId = groupId ?: child.id,
                    targetChildIds = targetChildIds,
                    time = currentTime,
                    category = cat,
                    summaryBadge = badge,
                    description = desc,
                    mealType = mealType,
                    foodConsumption = foodCons,
                    excretionType = excType,
                    excretionAviso = excAviso,
                    excretionNoAplica = excNoAplica
                )
            } else {
                viewModel.saveActivity(
                    time = currentTime,
                    category = cat,
                    summaryBadge = badge,
                    description = desc,
                    childId = child.id,
                    shareWithFamilyGroups = shareWithGroups,
                    mealType = mealType,
                    foodConsumption = foodCons,
                    excretionType = excType,
                    excretionAviso = excAviso,
                    excretionNoAplica = excNoAplica
                )
            }
            onSubmitActivity(cat, badge, desc, shareWithGroups)
        }
    )
}

@Composable
fun NewActivityScreen(
    child: Child,
    isGroupActivity: Boolean = false,
    onBack: () -> Unit,
    onSubmitActivity: (category: ActivityCategory, summaryBadge: String, description: String, shareWithFamilyGroups: Boolean) -> Unit
) {
    NewActivityScreen(
        child = child,
        isGroupActivity = isGroupActivity,
        onBack = onBack,
        onSubmitActivityDetailed = { cat, badge, desc, shareWithGroups, _, _, _, _, _ ->
            onSubmitActivity(cat, badge, desc, shareWithGroups)
        }
    )
}

@Composable
fun NewActivityScreen(
    child: Child,
    isGroupActivity: Boolean = false,
    onBack: () -> Unit,
    onSubmitActivityDetailed: (
        category: ActivityCategory,
        summaryBadge: String,
        description: String,
        shareWithFamilyGroups: Boolean,
        mealType: MealType?,
        foodConsumption: FoodConsumptionOption?,
        excretionType: ExcretionType?,
        excretionAviso: Boolean?,
        excretionNoAplica: Boolean?
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ActivityCategory?>(null) }

    // Estado 1: Alimentación (Registro por comida una a una)
    var selectedMeal by remember { mutableStateOf<MealType?>(null) }
    var mealConsumptionOption by remember { mutableStateOf<FoodConsumptionOption?>(null) }
    var alimentacionObservaciones by remember { mutableStateOf("") }

    // Estado 2: Descanso
    var descansoDurmio by remember { mutableStateOf<Boolean?>(null) }
    var descansoMinutos by remember { mutableStateOf("60") }
    var descansoComentarios by remember { mutableStateOf("") }

    // Estado 3: Funciones Excretoras (Registro individual en el momento)
    var selectedExcretionType by remember { mutableStateOf<ExcretionType?>(null) }
    var controlNoAplica by remember { mutableStateOf(false) }
    var controlAviso by remember { mutableStateOf<Boolean?>(null) }
    var excretorasObservaciones by remember { mutableStateOf("") }

    // Estado 4: Estado de Ánimo
    var selectedMood by remember { mutableStateOf<MoodState?>(null) }
    var lloro by remember { mutableStateOf(false) }
    var peleo by remember { mutableStateOf(false) }
    var participo by remember { mutableStateOf(true) }
    var animoObservaciones by remember { mutableStateOf("") }

    // Estado 5: Accidentes
    var tuvoAccidente by remember { mutableStateOf<Boolean?>(null) }
    var accidenteDescripcion by remember { mutableStateOf("") }
    var accidenteFolio by remember { mutableStateOf("") }

    // Estado 6: Salud
    var presentoProblemaSalud by remember { mutableStateOf<Boolean?>(null) }
    var saludCual by remember { mutableStateOf("") }
    var saludAtencion by remember { mutableStateOf("") }
    var saludObservaciones by remember { mutableStateOf("") }

    // Estado 7: Otros
    var otrasObservaciones by remember { mutableStateOf("") }

    val isReadyToSubmit = when (selectedCategory) {
        ActivityCategory.ALIMENTACION -> selectedMeal != null && mealConsumptionOption != null
        ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> descansoDurmio != null
        ActivityCategory.FUNCIONES_EXCRETORAS -> selectedExcretionType != null && (controlNoAplica || controlAviso != null)
        ActivityCategory.ESTADO_ANIMO -> selectedMood != null
        ActivityCategory.ACCIDENTES -> tuvoAccidente != null
        ActivityCategory.SALUD -> presentoProblemaSalud != null
        ActivityCategory.OTROS -> true
        null -> false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Surface(
                color = CardBackground,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BrandBlue
                        )
                    }

                    Text(
                        text = "Registrar Actividad",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera con Avatar y Nombre del Niño
                ChildAvatar(
                    initials = child.avatarInitials,
                    bgColor = Color(child.avatarBgColor),
                    size = 58.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = child.shortName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "${child.ageText} • ${child.groupText}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Categoría
                ActivityCategoryGrid(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Formulario dinámico según categoría seleccionada
                when (selectedCategory) {
                    ActivityCategory.ALIMENTACION -> {
                        AlimentacionSectionContent(
                            selectedMeal = selectedMeal,
                            onMealChange = { selectedMeal = it },
                            consumptionOption = mealConsumptionOption,
                            onConsumptionChange = { mealConsumptionOption = it },
                            observaciones = alimentacionObservaciones,
                            onObservacionesChange = { alimentacionObservaciones = it }
                        )
                    }

                    ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> {
                        DescansoSectionContent(
                            durmio = descansoDurmio,
                            onDurmioChange = { descansoDurmio = it },
                            minutosSiesta = descansoMinutos,
                            onMinutosSiestaChange = { descansoMinutos = it },
                            comentarios = descansoComentarios,
                            onComentariosChange = { descansoComentarios = it }
                        )
                    }

                    ActivityCategory.FUNCIONES_EXCRETORAS -> {
                        FuncionesExcretorasSectionContent(
                            selectedType = selectedExcretionType,
                            onTypeChange = { selectedExcretionType = it },
                            controlNoAplica = controlNoAplica,
                            onControlNoAplicaChange = { controlNoAplica = it },
                            aviso = controlAviso,
                            onAvisoChange = { controlAviso = it },
                            observaciones = excretorasObservaciones,
                            onObservacionesChange = { excretorasObservaciones = it }
                        )
                    }

                    ActivityCategory.ESTADO_ANIMO -> {
                        EstadoAnimoSectionContent(
                            selectedMood = selectedMood,
                            onMoodChange = { selectedMood = it },
                            lloro = lloro,
                            onLloroChange = { lloro = it },
                            peleo = peleo,
                            onPeleoChange = { peleo = it },
                            participo = participo,
                            onParticipoChange = { participo = it },
                            observaciones = animoObservaciones,
                            onObservacionesChange = { animoObservaciones = it }
                        )
                    }

                    ActivityCategory.ACCIDENTES -> {
                        AccidentesSectionContent(
                            tuvoAccidente = tuvoAccidente,
                            onTuvoAccidenteChange = { tuvoAccidente = it },
                            descripcion = accidenteDescripcion,
                            onDescripcionChange = { accidenteDescripcion = it },
                            folio = accidenteFolio,
                            onFolioChange = { accidenteFolio = it }
                        )
                    }

                    ActivityCategory.SALUD -> {
                        SaludSectionContent(
                            presentoProblema = presentoProblemaSalud,
                            onPresentoProblemaChange = { presentoProblemaSalud = it },
                            cualProblema = saludCual,
                            onCualProblemaChange = { saludCual = it },
                            atencionProporcionada = saludAtencion,
                            onAtencionProporcionadaChange = { saludAtencion = it },
                            observaciones = saludObservaciones,
                            onObservacionesChange = { saludObservaciones = it }
                        )
                    }

                    ActivityCategory.OTROS -> {
                        SectionCardContainer(
                            title = "Otras Actividades",
                            icon = Icons.Default.MoreHoriz,
                            subtitle = "Registra cualquier otra actividad realizada durante la jornada:"
                        ) {
                            OutlinedTextField(
                                value = otrasObservaciones,
                                onValueChange = { otrasObservaciones = it },
                                label = { Text("Descripción de la actividad", fontSize = 12.sp) },
                                placeholder = { Text("Juego libre, actividad pedagógica, estimulación temprana...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppBackground,
                                    unfocusedContainerColor = AppBackground,
                                    focusedBorderColor = BrandBlue,
                                    unfocusedBorderColor = BorderSubtle
                                )
                            )
                        }
                    }

                    null -> {
                        // Esperando selección de categoría
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón Registrar en Bitácora con revelado progresivo
                AnimatedVisibility(visible = isReadyToSubmit) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isGroupActivity) {
                                "Esta actividad se registrará en la bitácora diaria de todos los niños del grupo y se enviará al chat grupal."
                            } else {
                                "Esta actividad se registrará en la bitácora diaria del alumno y se enviará al chat del familiar."
                            },
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val currentCategory = selectedCategory ?: return@Button
                                val (summaryBadge, description) = when (currentCategory) {
                                    ActivityCategory.ALIMENTACION -> {
                                        val meal = selectedMeal ?: MealType.DESAYUNO
                                        val consumption = mealConsumptionOption ?: FoodConsumptionOption.TODO
                                        val consumptionText = when (consumption) {
                                            FoodConsumptionOption.TODO -> "Comió todo"
                                            FoodConsumptionOption.POCO -> "Comió poco"
                                            FoodConsumptionOption.NADA -> "No comió"
                                            FoodConsumptionOption.PIDIO_MAS -> "Pidió más"
                                        }
                                        val badge = "${meal.label}: $consumptionText"
                                        val desc = buildString {
                                            append("${meal.label}: $consumptionText.")
                                            if (alimentacionObservaciones.isNotBlank()) {
                                                append(" Observaciones: $alimentacionObservaciones")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> {
                                        val durmio = descansoDurmio ?: true
                                        val badge = if (durmio) "$descansoMinutos min siesta" else "No durmió"
                                        val desc = buildString {
                                            if (durmio) {
                                                append("Durmió durante la siesta un tiempo de $descansoMinutos minutos.")
                                            } else {
                                                append("No durmió durante el tiempo de descanso.")
                                            }
                                            if (descansoComentarios.isNotBlank()) {
                                                append(" Comentarios: $descansoComentarios")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.FUNCIONES_EXCRETORAS -> {
                                        val excType = selectedExcretionType ?: ExcretionType.PIPI
                                        val controlLabel = if (controlNoAplica) "Usa pañal" else if (controlAviso == true) "Avisó: Sí" else "Avisó: No"
                                        val badge = "${excType.label} • $controlLabel"
                                        val desc = buildString {
                                            append("Función excretora: ${excType.label}. ")
                                            if (controlNoAplica) {
                                                append("Control de esfínteres: No aplica (usa pañal).")
                                            } else {
                                                append("Control de esfínteres: Avisó: ${if (controlAviso == true) "Sí" else "No"}.")
                                            }
                                            if (excretorasObservaciones.isNotBlank()) {
                                                append(" Observaciones: $excretorasObservaciones")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.ESTADO_ANIMO -> {
                                        val mood = selectedMood ?: MoodState.FELIZ
                                        val badge = "${mood.emoji} ${mood.label}"
                                        val desc = buildString {
                                            append("Estadía: ${mood.label}. En actividades: ¿Lloró?: ${if (lloro) "Sí" else "No"}, ¿Peleó?: ${if (peleo) "Sí" else "No"}, ¿Participó?: ${if (participo) "Sí" else "No"}.")
                                            if (animoObservaciones.isNotBlank()) {
                                                append(" Observaciones: $animoObservaciones")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.ACCIDENTES -> {
                                        val hadAccident = tuvoAccidente ?: false
                                        val badge = if (hadAccident) "Accidente reportado" else "Sin accidentes"
                                        val desc = buildString {
                                            if (hadAccident) {
                                                append("Accidente reportado. Folio: ${accidenteFolio.ifBlank { "S/F" }}. Detalle: $accidenteDescripcion")
                                            } else {
                                                append("No tuvo ningún accidente durante la jornada escolar.")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.SALUD -> {
                                        val hadHealth = presentoProblemaSalud ?: false
                                        val badge = if (hadHealth) "Atención brindada" else "Salud óptima"
                                        val desc = buildString {
                                            if (hadHealth) {
                                                append("Problema: $saludCual. Atención: $saludAtencion.")
                                                if (saludObservaciones.isNotBlank()) {
                                                    append(" Observaciones: $saludObservaciones")
                                                }
                                            } else {
                                                append("No presentó problemas de salud. Estado físico general óptimo.")
                                            }
                                        }
                                        Pair(badge, desc)
                                    }

                                    ActivityCategory.OTROS -> {
                                        Pair("General", otrasObservaciones.ifBlank { "Actividad escolar registrada." })
                                    }
                                }

                                onSubmitActivityDetailed(
                                    currentCategory,
                                    summaryBadge,
                                    description,
                                    false,
                                    if (currentCategory == ActivityCategory.ALIMENTACION) selectedMeal else null,
                                    if (currentCategory == ActivityCategory.ALIMENTACION) mealConsumptionOption else null,
                                    if (currentCategory == ActivityCategory.FUNCIONES_EXCRETORAS) selectedExcretionType else null,
                                    if (currentCategory == ActivityCategory.FUNCIONES_EXCRETORAS) controlAviso else null,
                                    if (currentCategory == ActivityCategory.FUNCIONES_EXCRETORAS) controlNoAplica else null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registrar en Bitácora",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewActivityScreenPreview() {
    Etapa1Theme {
        NewActivityScreen(
            child = MockDataRepository.mateoGarcia,
            onBack = {},
            onSubmitActivity = { _, _, _, _ -> }
        )
    }
}
