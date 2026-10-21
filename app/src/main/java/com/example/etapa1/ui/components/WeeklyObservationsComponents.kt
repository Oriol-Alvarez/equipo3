package com.example.etapa1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.WeeklyObservation
import com.example.etapa1.model.WeeklySummary
import com.example.etapa1.ui.state.ObservationsSelection
import com.example.etapa1.ui.state.WeeklyObservationsUiState
import com.example.etapa1.ui.state.WeeklyObservationsViewModel
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Observaciones del resumen semanal conectadas a su ViewModel.
 * Se coloca debajo de [WeeklySummaryContent] en el inicio familiar y en la ficha del niño.
 */
@Composable
fun WeeklyObservationsPanel(
    viewModel: WeeklyObservationsViewModel,
    childId: String,
    summary: WeeklySummary,
    isCurrentWeek: Boolean,
    viewerRole: ObservationAuthorRole,
    viewerName: String
) {
    LaunchedEffect(childId, summary.weekRangeText, isCurrentWeek, viewerRole, viewerName) {
        viewModel.select(
            ObservationsSelection(
                childId = childId,
                weekKey = summary.weekRangeText,
                isCurrentWeek = isCurrentWeek,
                viewerRole = viewerRole,
                viewerName = viewerName
            )
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Mientras llega la selección de esta semana no se muestran datos de otra.
    val selection = uiState.selection
    if (selection == null || selection.childId != childId || selection.weekKey != summary.weekRangeText) return

    WeeklyObservationsSection(
        state = uiState,
        onDraftChange = viewModel::onDraftChange,
        onSubmit = { viewModel.submit() }
    )
}

@Composable
fun WeeklyObservationsSection(
    state: WeeklyObservationsUiState,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val viewer = state.selection
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Apartado de Observaciones de los Trabajadores / Educadoras
        SectionDetailCard(
            title = "Observaciones de los trabajadores",
            icon = Icons.Default.Badge
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.educatorObservations.isEmpty()) {
                    Text(
                        text = state.emptyEducatorMessage,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                } else {
                    state.educatorObservations.forEach { observation ->
                        ObservationItem(
                            observation = observation,
                            isOwn = viewer != null &&
                                observation.authorName == viewer.viewerName &&
                                observation.authorRole == viewer.viewerRole
                        )
                    }
                }

                if (state.canWriteAsEducator) {
                    ObservationDraftField(
                        draft = state.draft,
                        onDraftChange = onDraftChange,
                        maxLength = state.maxLength,
                        label = "Escribe una observación pedagógica o del desarrollo",
                        placeholder = "Ej. Mostró gran interés en las actividades plásticas y colaboró muy bien con sus compañeros.",
                        errorMessage = state.errorMessage,
                        savedMessage = state.savedMessage,
                        buttonText = "Guardar observación del personal",
                        onSubmit = onSubmit
                    )
                } else if (viewer?.viewerRole == ObservationAuthorRole.EDUCADORA && !state.canWrite) {
                    state.readOnlyHint?.let { hint ->
                        Text(text = hint, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        // 2. Apartado de Observaciones de la Familia
        SectionDetailCard(
            title = "Observaciones de la familia",
            icon = Icons.Default.FamilyRestroom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.familyObservations.isEmpty()) {
                    Text(
                        text = state.emptyFamilyMessage,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                } else {
                    state.familyObservations.forEach { observation ->
                        ObservationItem(
                            observation = observation,
                            isOwn = viewer != null &&
                                observation.authorName == viewer.viewerName &&
                                observation.authorRole == viewer.viewerRole
                        )
                    }
                }

                if (state.canWriteAsFamily) {
                    ObservationDraftField(
                        draft = state.draft,
                        onDraftChange = onDraftChange,
                        maxLength = state.maxLength,
                        label = "Escribe una observación de esta semana",
                        placeholder = "Ej. En casa durmió mejor y empezó a decir nuevas palabras.",
                        errorMessage = state.errorMessage,
                        savedMessage = state.savedMessage,
                        buttonText = "Guardar observación de la familia",
                        onSubmit = onSubmit
                    )
                } else if (viewer?.viewerRole == ObservationAuthorRole.FAMILIA && !state.canWrite) {
                    state.readOnlyHint?.let { hint ->
                        Text(text = hint, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationDraftField(
    draft: String,
    onDraftChange: (String) -> Unit,
    maxLength: Int,
    label: String,
    placeholder: String,
    errorMessage: String?,
    savedMessage: String?,
    buttonText: String,
    onSubmit: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            label = { Text(label, fontSize = 12.sp) },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            supportingText = {
                Text(
                    text = "${draft.length}/$maxLength",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 11.sp
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppBackground,
                unfocusedContainerColor = AppBackground,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = BorderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
        )

        errorMessage?.let { Text(text = it, fontSize = 12.sp, color = AlertRed) }
        savedMessage?.let { Text(text = it, fontSize = 12.sp, color = StatusPresentGreen) }

        Button(
            onClick = onSubmit,
            enabled = draft.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(buttonText, color = Color.White)
        }
    }
}

@Composable
private fun ObservationItem(
    observation: WeeklyObservation,
    isOwn: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppBackground)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandBlueContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = observation.authorRole.label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOwn) "Tú" else observation.authorName,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(text = observation.time, fontSize = 11.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = observation.text,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyObservationsSectionPreview() {
    val selection = ObservationsSelection(
        childId = "mateo_garcia",
        weekKey = "Semana del 22 al 26 de Septiembre, 2026",
        isCurrentWeek = true,
        viewerRole = ObservationAuthorRole.FAMILIA,
        viewerName = "María López (Mamá de Mateo)"
    )
    val obsFamily = WeeklyObservation(
        id = "p1",
        childId = selection.childId,
        weekKey = selection.weekKey,
        authorRole = ObservationAuthorRole.FAMILIA,
        authorName = selection.viewerName,
        text = "Esta semana durmió mejor en casa.",
        time = "24/09 08:10 PM"
    )
    val obsEducator = WeeklyObservation(
        id = "p2",
        childId = selection.childId,
        weekKey = selection.weekKey,
        authorRole = ObservationAuthorRole.EDUCADORA,
        authorName = "Laura Gómez (Educadora)",
        text = "Mateo se adaptó perfectamente a las actividades del grupo.",
        time = "24/09 03:30 PM"
    )
    Etapa1Theme {
        WeeklyObservationsSection(
            state = WeeklyObservationsUiState(
                selection = selection,
                observations = listOf(obsEducator, obsFamily),
                educatorObservations = listOf(obsEducator),
                familyObservations = listOf(obsFamily),
                canWrite = true,
                canWriteAsFamily = true,
                draft = "Empezó a decir \"agua\"."
            ),
            onDraftChange = {},
            onSubmit = {}
        )
    }
}
