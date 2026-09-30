package com.example.etapa1.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.MoodState
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Grid 2x3 para seleccionar entre las 6 categorías oficiales de la bitácora
 */
@Composable
fun ActivityCategoryGrid(
    selectedCategory: ActivityCategory,
    onCategorySelected: (ActivityCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        Triple(ActivityCategory.ALIMENTACION, "Alimentación", Icons.Default.Restaurant),
        Triple(ActivityCategory.DESCANSO, "Descanso", Icons.Default.NightlightRound),
        Triple(ActivityCategory.FUNCIONES_EXCRETORAS, "Funciones Excretoras", Icons.Default.Wc),
        Triple(ActivityCategory.ESTADO_ANIMO, "Estado de Ánimo", Icons.Default.Face),
        Triple(ActivityCategory.ACCIDENTES, "Accidentes", Icons.Default.WarningAmber),
        Triple(ActivityCategory.SALUD, "Salud", Icons.Default.LocalHospital)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in categories.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val first = categories[i]
                CategoryCardTile(
                    title = first.second,
                    icon = first.third,
                    isSelected = selectedCategory == first.first || (first.first == ActivityCategory.DESCANSO && selectedCategory == ActivityCategory.SIESTA),
                    onClick = { onCategorySelected(first.first) },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < categories.size) {
                    val second = categories[i + 1]
                    CategoryCardTile(
                        title = second.second,
                        icon = second.third,
                        isSelected = selectedCategory == second.first,
                        onClick = { onCategorySelected(second.first) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCardTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) BrandBlue else CardBackground
    val border = if (isSelected) BrandBlue else BorderSubtle
    val contentColor = if (isSelected) Color.White else TextPrimary
    val iconColor = if (isSelected) Color.White else BrandBlue

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// 1. SECCIÓN: ALIMENTACIÓN
// -------------------------------------------------------------
@Composable
fun AlimentacionSectionContent(
    desayunoOption: FoodConsumptionOption,
    onDesayunoChange: (FoodConsumptionOption) -> Unit,
    colacionOption: FoodConsumptionOption,
    onColacionChange: (FoodConsumptionOption) -> Unit,
    comidaOption: FoodConsumptionOption,
    onComidaChange: (FoodConsumptionOption) -> Unit,
    observaciones: String,
    onObservacionesChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Alimentación",
        icon = Icons.Default.Restaurant,
        subtitle = "La niña o el niño consumió los alimentos durante el:"
    ) {
        MealConsumptionRow(
            mealName = "Desayuno",
            selectedOption = desayunoOption,
            onOptionSelected = onDesayunoChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        MealConsumptionRow(
            mealName = "Colación",
            selectedOption = colacionOption,
            onOptionSelected = onColacionChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        MealConsumptionRow(
            mealName = "Comida",
            selectedOption = comidaOption,
            onOptionSelected = onComidaChange
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            label = { Text("Observaciones Generales", fontSize = 12.sp) },
            placeholder = { Text("Detalle de apetito, alimentos rechazados, etc.", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(86.dp),
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

@Composable
private fun MealConsumptionRow(
    mealName: String,
    selectedOption: FoodConsumptionOption,
    onOptionSelected: (FoodConsumptionOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = mealName,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FoodConsumptionOption.values().forEach { option ->
                val isSelected = selectedOption == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) BrandBlue else BrandBlueContainer)
                        .border(1.dp, if (isSelected) BrandBlue else Color(0xFFD4E3F7), RoundedCornerShape(8.dp))
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BrandBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. SECCIÓN: DESCANSO
// -------------------------------------------------------------
@Composable
fun DescansoSectionContent(
    durmio: Boolean,
    onDurmioChange: (Boolean) -> Unit,
    minutosSiesta: String,
    onMinutosSiestaChange: (String) -> Unit,
    comentarios: String,
    onComentariosChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Descanso",
        icon = Icons.Default.NightlightRound
    ) {

        AnimatedVisibility(visible = durmio) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    text = "Tiempo de siesta (minutos)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("30", "45", "60", "90").forEach { min ->
                        val isSelected = minutosSiesta == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandBlue else BrandBlueContainer)
                                .border(1.dp, if (isSelected) BrandBlue else Color(0xFFD4E3F7), RoundedCornerShape(8.dp))
                                .clickable { onMinutosSiestaChange(min) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$min min",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BrandBlue
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = comentarios,
            onValueChange = onComentariosChange,
            label = { Text("Comentarios de descanso", fontSize = 12.sp) },
            placeholder = { Text("Si durmió tranquilo, si despertó de buen humor...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(86.dp),
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

// -------------------------------------------------------------
// 3. SECCIÓN: FUNCIONES EXCRETORAS
// -------------------------------------------------------------
@Composable
fun FuncionesExcretorasSectionContent(
    controlNoAplica: Boolean,
    onControlNoAplicaChange: (Boolean) -> Unit,
    aviso: Boolean,
    onAvisoChange: (Boolean) -> Unit,
    vecesPipi: Int,
    onPipiChange: (Int) -> Unit,
    vecesPopo: Int,
    onPopoChange: (Int) -> Unit,
    observaciones: String,
    onObservacionesChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Funciones Excretoras",
        icon = Icons.Default.Wc
    ) {
        Text(
            text = "Control de esfínteres",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlue
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppBackground)
                .clickable { onControlNoAplicaChange(!controlNoAplica) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = controlNoAplica,
                onCheckedChange = onControlNoAplicaChange,
                colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "No aplica (usa pañal)",
                fontSize = 12.5.sp,
                color = TextPrimary
            )
        }

        AnimatedVisibility(visible = !controlNoAplica) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "¿Avisó?",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                YesNoSegmentSelector(
                    selected = aviso,
                    onSelect = onAvisoChange
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Pipí y/o Popó — Número de veces",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberCounterCard(
                label = "Pipí",
                count = vecesPipi,
                onIncrement = { onPipiChange(vecesPipi + 1) },
                onDecrement = { if (vecesPipi > 0) onPipiChange(vecesPipi - 1) },
                modifier = Modifier.weight(1f)
            )

            NumberCounterCard(
                label = "Popó",
                count = vecesPopo,
                onIncrement = { onPopoChange(vecesPopo + 1) },
                onDecrement = { if (vecesPopo > 0) onPopoChange(vecesPopo - 1) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            label = { Text("Observaciones", fontSize = 12.sp) },
            placeholder = { Text("Consistencia, cambio de pañal, incidentes...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(86.dp),
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

// -------------------------------------------------------------
// 4. SECCIÓN: ESTADO DE ÁNIMO
// -------------------------------------------------------------
@Composable
fun EstadoAnimoSectionContent(
    selectedMood: MoodState,
    onMoodChange: (MoodState) -> Unit,
    lloro: Boolean,
    onLloroChange: (Boolean) -> Unit,
    peleo: Boolean,
    onPeleoChange: (Boolean) -> Unit,
    participo: Boolean,
    onParticipoChange: (Boolean) -> Unit,
    observaciones: String,
    onObservacionesChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Estado de ánimo",
        icon = Icons.Default.Face
    ) {
        Text(
            text = "¿Durante su estadía, la niña o el niño estuvo?",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoodState.values().forEach { mood ->
                val isSelected = selectedMood == mood
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandBlueContainer else AppBackground)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandBlue else BorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onMoodChange(mood) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = mood.emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mood.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandBlue else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿Durante las actividades?",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        ActivityYesNoRow(label = "¿Lloró?", value = lloro, onChange = onLloroChange)
        Spacer(modifier = Modifier.height(6.dp))
        ActivityYesNoRow(label = "¿Peleó?", value = peleo, onChange = onPeleoChange)
        Spacer(modifier = Modifier.height(6.dp))
        ActivityYesNoRow(label = "¿Participó?", value = participo, onChange = onParticipoChange)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            label = { Text("Observaciones de conducta", fontSize = 12.sp) },
            placeholder = { Text("Actitud frente a los compañeros y maestras...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(86.dp),
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

// -------------------------------------------------------------
// 5. SECCIÓN: ACCIDENTES
// -------------------------------------------------------------
@Composable
fun AccidentesSectionContent(
    tuvoAccidente: Boolean,
    onTuvoAccidenteChange: (Boolean) -> Unit,
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    folio: String,
    onFolioChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Accidentes",
        icon = Icons.Default.WarningAmber
    ) {


        Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            OutlinedTextField(
                value = descripcion,
                onValueChange = onDescripcionChange,
                label = { Text("Descripción breve del accidente", fontSize = 12.sp) },
                placeholder = { Text("Ej. Raspón leve en la rodilla izquierda jugando...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(86.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppBackground,
                    unfocusedContainerColor = AppBackground,
                    focusedBorderColor = AlertRed,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = folio,
                onValueChange = onFolioChange,
                label = { Text("Folio de reporte de accidentes", fontSize = 12.sp) },
                placeholder = { Text("Ej. ACC-2026-089", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppBackground,
                    unfocusedContainerColor = AppBackground,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )
        }

    }
}

// -------------------------------------------------------------
// 6. SECCIÓN: SALUD
// -------------------------------------------------------------
@Composable
fun SaludSectionContent(
    presentoProblema: Boolean,
    onPresentoProblemaChange: (Boolean) -> Unit,
    cualProblema: String,
    onCualProblemaChange: (String) -> Unit,
    atencionProporcionada: String,
    onAtencionProporcionadaChange: (String) -> Unit,
    observaciones: String,
    onObservacionesChange: (String) -> Unit
) {
    SectionCardContainer(
        title = "Salud",
        icon = Icons.Default.LocalHospital
    ) {



        Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            OutlinedTextField(
                value = cualProblema,
                onValueChange = onCualProblemaChange,
                label = { Text("¿Cuál?", fontSize = 12.sp) },
                placeholder = { Text("Fiebre 37.8°C, vómito, cólico...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppBackground,
                    unfocusedContainerColor = AppBackground,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = atencionProporcionada,
                onValueChange = onAtencionProporcionadaChange,
                label = { Text("Atención proporcionada", fontSize = 12.sp) },
                placeholder = { Text("Toma de temperatura, aviso a padres, reposo...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(86.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppBackground,
                    unfocusedContainerColor = AppBackground,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderSubtle
                )
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            label = { Text("Observaciones Generales", fontSize = 12.sp) },
            placeholder = { Text("Indicaciones de medicamentos o seguimiento...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(86.dp),
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

// -------------------------------------------------------------
// COMPONENTES AUXILIARES REUTILIZABLES
// -------------------------------------------------------------

@Composable
fun SectionCardContainer(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun YesNoSegmentSelector(
    selected: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandBlueContainer)
            .border(1.dp, Color(0xFFD4E3F7), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) BrandBlue else Color.Transparent)
                .clickable { onSelect(true) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sí",
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else BrandBlue
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (!selected) BrandBlue else Color.Transparent)
                .clickable { onSelect(false) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No",
                fontSize = 12.sp,
                fontWeight = if (!selected) FontWeight.Bold else FontWeight.Medium,
                color = if (!selected) Color.White else BrandBlue
            )
        }
    }
}

@Composable
private fun ActivityYesNoRow(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = TextPrimary
        )
        YesNoSegmentSelector(selected = value, onSelect = onChange)
    }
}

@Composable
private fun NumberCounterCard(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppBackground)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BrandBlueContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Restar",
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "$count",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BrandBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Sumar",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
