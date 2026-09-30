package com.example.etapa1.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.MoodState
import com.example.etapa1.ui.components.AccidentesSectionContent
import com.example.etapa1.ui.components.ActivityCategoryGrid
import com.example.etapa1.ui.components.AlimentacionSectionContent
import com.example.etapa1.ui.components.ChildAvatar
import com.example.etapa1.ui.components.DescansoSectionContent
import com.example.etapa1.ui.components.EstadoAnimoSectionContent
import com.example.etapa1.ui.components.FuncionesExcretorasSectionContent
import com.example.etapa1.ui.components.MedicalAlertBanner
import com.example.etapa1.ui.components.SaludSectionContent
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.model.MockDataRepository

import com.example.etapa1.ui.state.NewActivityViewModel

@Composable
fun NewActivityScreen(
    viewModel: NewActivityViewModel,
    child: Child,
    onBack: () -> Unit,
    onSubmitActivity: (category: ActivityCategory, summaryBadge: String, description: String) -> Unit = { _, _, _ -> }
) {
    NewActivityScreen(
        child = child,
        onBack = onBack,
        onSubmitActivity = { cat, badge, desc ->
            val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            viewModel.saveActivity(child.id, currentTime, cat.label, badge, desc)
            onSubmitActivity(cat, badge, desc)
        }
    )
}

@Composable
fun NewActivityScreen(
    child: Child,
    onBack: () -> Unit,
    onSubmitActivity: (category: ActivityCategory, summaryBadge: String, description: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ActivityCategory.ALIMENTACION) }

    // Estado 1: Alimentación
    var desayunoOption by remember { mutableStateOf(FoodConsumptionOption.TODO) }
    var colacionOption by remember { mutableStateOf(FoodConsumptionOption.TODO) }
    var comidaOption by remember { mutableStateOf(FoodConsumptionOption.TODO) }
    var alimentacionObservaciones by remember(child.id) {
        mutableStateOf("${child.fullName} disfrutó mucho su comida de hoy y pidió un poco más de fruta.")
    }

    // Estado 2: Descanso
    var descansoDurmio by remember { mutableStateOf(true) }
    var descansoMinutos by remember { mutableStateOf("60") }
    var descansoComentarios by remember { mutableStateOf("Durmió tranquilo con su mantita.") }

    // Estado 3: Funciones Excretoras
    var controlNoAplica by remember { mutableStateOf(false) }
    var controlAviso by remember { mutableStateOf(true) }
    var vecesPipi by remember { mutableIntStateOf(2) }
    var vecesPopo by remember { mutableIntStateOf(1) }
    var excretorasObservaciones by remember { mutableStateOf("") }

    // Estado 4: Estado de Ánimo
    var selectedMood by remember { mutableStateOf(MoodState.FELIZ) }
    var lloro by remember { mutableStateOf(false) }
    var peleo by remember { mutableStateOf(false) }
    var participo by remember { mutableStateOf(true) }
    var animoObservaciones by remember { mutableStateOf("Muy participativo en la asamblea y juegos.") }

    // Estado 5: Accidentes
    var tuvoAccidente by remember { mutableStateOf(false) }
    var accidenteDescripcion by remember { mutableStateOf("") }
    var accidenteFolio by remember { mutableStateOf("") }

    // Estado 6: Salud
    var presentoProblemaSalud by remember { mutableStateOf(false) }
    var saludCual by remember { mutableStateOf("") }
    var saludAtencion by remember { mutableStateOf("") }
    var saludObservaciones by remember { mutableStateOf("") }

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
                        text = "Nueva Actividad",
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

                if (child.allergyAlert != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MedicalAlertBanner(
                        text = child.allergyAlert,
                        onClick = { /* Alerta alergias */ }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Categoría (Grid 2x3 de la bitácora oficial)
                ActivityCategoryGrid(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Formulario dinámico según categoría seleccionada
                when (selectedCategory) {
                    ActivityCategory.ALIMENTACION -> {
                        AlimentacionSectionContent(
                            desayunoOption = desayunoOption,
                            onDesayunoChange = { desayunoOption = it },
                            colacionOption = colacionOption,
                            onColacionChange = { colacionOption = it },
                            comidaOption = comidaOption,
                            onComidaChange = { comidaOption = it },
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
                            controlNoAplica = controlNoAplica,
                            onControlNoAplicaChange = { controlNoAplica = it },
                            aviso = controlAviso,
                            onAvisoChange = { controlAviso = it },
                            vecesPipi = vecesPipi,
                            onPipiChange = { vecesPipi = it },
                            vecesPopo = vecesPopo,
                            onPopoChange = { vecesPopo = it },
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
                        AlimentacionSectionContent(
                            desayunoOption = desayunoOption,
                            onDesayunoChange = { desayunoOption = it },
                            colacionOption = colacionOption,
                            onColacionChange = { colacionOption = it },
                            comidaOption = comidaOption,
                            onComidaChange = { comidaOption = it },
                            observaciones = alimentacionObservaciones,
                            onObservacionesChange = { alimentacionObservaciones = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Esta actividad se registrará en la bitácora y se enviará al chat del familiar.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Registrar en Bitácora
                Button(
                    onClick = {
                        val (summaryBadge, description) = when (selectedCategory) {
                            ActivityCategory.ALIMENTACION -> {
                                val badge = "Comida: ${comidaOption.label}"
                                val desc = buildString {
                                    append("Desayuno: ${desayunoOption.label}, Colación: ${colacionOption.label}, Comida: ${comidaOption.label}.")
                                    if (alimentacionObservaciones.isNotBlank()) {
                                        append(" Observaciones: $alimentacionObservaciones")
                                    }
                                }
                                Pair(badge, desc)
                            }

                            ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> {
                                val badge = if (descansoDurmio) "$descansoMinutos min siesta" else "No durmió"
                                val desc = buildString {
                                    if (descansoDurmio) {
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
                                val badge = "Pipí: $vecesPipi • Popó: $vecesPopo"
                                val desc = buildString {
                                    if (controlNoAplica) {
                                        append("Control de esfínteres: No aplica. ")
                                    } else {
                                        append("Control de esfínteres: Avisó: ${if (controlAviso) "Sí" else "No"}. ")
                                    }
                                    append("Número de veces: pipí: $vecesPipi, popó: $vecesPopo.")
                                    if (excretorasObservaciones.isNotBlank()) {
                                        append(" Observaciones: $excretorasObservaciones")
                                    }
                                }
                                Pair(badge, desc)
                            }

                            ActivityCategory.ESTADO_ANIMO -> {
                                val badge = "${selectedMood.emoji} ${selectedMood.label}"
                                val desc = buildString {
                                    append("Estadía: ${selectedMood.label}. En actividades: ¿Lloró?: ${if (lloro) "Sí" else "No"}, ¿Peleó?: ${if (peleo) "Sí" else "No"}, ¿Participó?: ${if (participo) "Sí" else "No"}.")
                                    if (animoObservaciones.isNotBlank()) {
                                        append(" Observaciones: $animoObservaciones")
                                    }
                                }
                                Pair(badge, desc)
                            }

                            ActivityCategory.ACCIDENTES -> {
                                val badge = if (tuvoAccidente) "Accidente reportado" else "Sin accidentes"
                                val desc = buildString {
                                    if (tuvoAccidente) {
                                        append("Accidente reportado. Folio: ${accidenteFolio.ifBlank { "S/F" }}. Detalle: $accidenteDescripcion")
                                    } else {
                                        append("No tuvo ningún accidente durante la jornada escolar.")
                                    }
                                }
                                Pair(badge, desc)
                            }

                            ActivityCategory.SALUD -> {
                                val badge = if (presentoProblemaSalud) "Atención brindada" else "Salud óptima"
                                val desc = buildString {
                                    if (presentoProblemaSalud) {
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
                                Pair("General", alimentacionObservaciones.ifBlank { "Actividad registrada." })
                            }
                        }

                        onSubmitActivity(selectedCategory, summaryBadge, description)
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
            onSubmitActivity = { _, _, _ -> }
        )
    }
}
