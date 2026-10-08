package com.example.etapa1.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.ui.components.DailyMenuCard
import com.example.etapa1.ui.components.DailyPlanEditor
import com.example.etapa1.ui.components.PlannedActivitiesCard
import com.example.etapa1.ui.components.SchoolDayChips
import com.example.etapa1.ui.state.DailyPlanPresenter
import com.example.etapa1.ui.state.DailyPlanUiState
import com.example.etapa1.ui.state.DailyPlanViewModel
import com.example.etapa1.ui.theme.AlertRed
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.StatusPresentGreen
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

/**
 * Planeación y menú de la semana. La educadora puede publicar o editar cada día;
 * la familia solo lo consulta.
 */
@Composable
fun DailyPlanScreen(
    viewModel: DailyPlanViewModel,
    canEdit: Boolean,
    onBack: () -> Unit
) {
    LaunchedEffect(canEdit) { viewModel.setCanEdit(canEdit) }
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isEditing) { viewModel.cancelEditing() }

    DailyPlanContent(
        state = uiState,
        onBack = onBack,
        onSelectDay = viewModel::selectDay,
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onSave = { viewModel.save() },
        onMenuChange = { breakfast, snack, lunch, notes ->
            viewModel.updateDraft { it.copy(breakfast = breakfast, snack = snack, lunch = lunch, notes = notes) }
        },
        onActivityChange = viewModel::updateActivity,
        onAddActivity = viewModel::addActivity,
        onRemoveActivity = viewModel::removeActivity
    )
}

@Composable
fun DailyPlanContent(
    state: DailyPlanUiState,
    onBack: () -> Unit,
    onSelectDay: (SimpleDate) -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSave: () -> Unit,
    onMenuChange: (String, String, String, String) -> Unit,
    onActivityChange: (Int, String, String) -> Unit,
    onAddActivity: () -> Unit,
    onRemoveActivity: (Int) -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(color = CardBackground, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = if (state.isEditing) onCancelEditing else onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = if (state.isEditing) "Editar planeación" else "Planeación y menú",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        textAlign = TextAlign.Center
                    )
                    if (state.canEdit && !state.isEditing && state.plan != null) {
                        IconButton(onClick = onStartEditing, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar planeación", tint = BrandBlue)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "days") {
                    SchoolDayChips(
                        weekDays = state.weekDays,
                        selectedDate = state.selectedDate,
                        today = state.today,
                        enabled = !state.isEditing,
                        onSelect = onSelectDay
                    )
                }

                item(key = "title") {
                    Text(text = state.dayTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                state.savedMessage?.let { message ->
                    item(key = "saved") { Text(text = message, fontSize = 12.5.sp, color = StatusPresentGreen) }
                }

                if (state.isEditing) {
                    item(key = "editor") {
                        DailyPlanEditor(
                            draft = state.draft,
                            canAddActivity = state.draft.activities.size < DailyPlanPresenter.MAX_ACTIVITIES,
                            onMenuChange = onMenuChange,
                            onActivityChange = onActivityChange,
                            onAddActivity = onAddActivity,
                            onRemoveActivity = onRemoveActivity
                        )
                    }
                    state.errorMessage?.let { message ->
                        item(key = "error") { Text(text = message, fontSize = 12.5.sp, color = AlertRed) }
                    }
                    item(key = "actions") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onCancelEditing, shape = RoundedCornerShape(20.dp)) {
                                Text("Cancelar", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onSave,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Publicar", color = Color.White)
                            }
                        }
                    }
                } else {
                    val plan = state.plan
                    if (plan == null) {
                        item(key = "empty") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Todavía no se publica la planeación ni el menú de este día.",
                                    fontSize = 13.5.sp,
                                    color = TextSecondary
                                )
                                if (state.canEdit) {
                                    Button(
                                        onClick = onStartEditing,
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Publicar planeación", color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "menu") { DailyMenuCard(plan.menu) }
                        item(key = "activities") { PlannedActivitiesCard(plan.activities) }
                        item(key = "author") {
                            Text(text = "Publicado por ${plan.updatedBy}", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
