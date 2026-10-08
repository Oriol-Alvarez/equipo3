package com.example.etapa1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyMenu
import com.example.etapa1.model.PlannedActivity
import com.example.etapa1.ui.state.ActivityDraft
import com.example.etapa1.ui.state.DailyPlanDraft
import com.example.etapa1.ui.state.TodayPlanSummary
import com.example.etapa1.ui.theme.AppBackground
import com.example.etapa1.ui.theme.BorderSubtle
import com.example.etapa1.ui.theme.BrandBlue
import com.example.etapa1.ui.theme.BrandBlueContainer
import com.example.etapa1.ui.theme.CardBackground
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextMuted
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

private val MenuOrange = Color(0xFFE65100)

/** Acceso rápido con el menú y la primera actividad de hoy (sala de la educadora e inicio familiar). */
@Composable
fun TodayPlanCard(
    summary: TodayPlanSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.EventNote,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Planeación y menú · ${summary.title}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary.lunch?.let { "Comida: $it" } ?: "Todavía no se publica el menú de este día.",
                fontSize = 12.5.sp,
                color = TextPrimary,
                maxLines = 1
            )
            summary.nextActivity?.let {
                Text(text = it, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ver planeación",
            tint = TextMuted
        )
    }
}

@Composable
fun SchoolDayChips(
    weekDays: List<SimpleDate>,
    selectedDate: SimpleDate,
    today: SimpleDate,
    enabled: Boolean,
    onSelect: (SimpleDate) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(weekDays, key = { "${it.year}-${it.month}-${it.day}" }) { date ->
            val isSelected = date == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) BrandBlue else CardBackground)
                    .border(1.dp, if (isSelected) BrandBlue else BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onSelect(date) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SchoolWeek.shortLabel(date),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                )
                if (date == today) {
                    Text(
                        text = "Hoy",
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else BrandBlue
                    )
                }
            }
        }
    }
}

@Composable
fun DailyMenuCard(menu: DailyMenu) {
    SectionDetailCard(title = "Menú del día", icon = Icons.Default.Restaurant) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MealRow("Desayuno", menu.breakfast)
            MealRow("Colación", menu.snack)
            MealRow("Comida", menu.lunch)
            if (menu.notes.isNotBlank()) {
                Text(
                    text = "⚠️ ${menu.notes}",
                    fontSize = 12.5.sp,
                    color = MenuOrange,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun MealRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Text(text = value, fontSize = 13.5.sp, color = TextPrimary, lineHeight = 18.sp)
    }
}

@Composable
fun PlannedActivitiesCard(activities: List<PlannedActivity>) {
    SectionDetailCard(title = "Actividades planeadas", icon = Icons.AutoMirrored.Filled.EventNote) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { activity ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = activity.time,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandBlueContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = activity.title, fontSize = 13.5.sp, color = TextPrimary, lineHeight = 18.sp)
                }
            }
        }
    }
}

/** Formulario de la educadora para publicar el menú y las actividades del día. */
@Composable
fun DailyPlanEditor(
    draft: DailyPlanDraft,
    canAddActivity: Boolean,
    onMenuChange: (breakfast: String, snack: String, lunch: String, notes: String) -> Unit,
    onActivityChange: (index: Int, time: String, title: String) -> Unit,
    onAddActivity: () -> Unit,
    onRemoveActivity: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionDetailCard(title = "Menú del día", icon = Icons.Default.Restaurant) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanTextField("Desayuno", draft.breakfast) { onMenuChange(it, draft.snack, draft.lunch, draft.notes) }
                PlanTextField("Colación", draft.snack) { onMenuChange(draft.breakfast, it, draft.lunch, draft.notes) }
                PlanTextField("Comida", draft.lunch) { onMenuChange(draft.breakfast, draft.snack, it, draft.notes) }
                PlanTextField("Avisos de cocina (opcional)", draft.notes) {
                    onMenuChange(draft.breakfast, draft.snack, draft.lunch, it)
                }
            }
        }

        SectionDetailCard(title = "Actividades planeadas", icon = Icons.AutoMirrored.Filled.EventNote) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.activities.forEachIndexed { index, row ->
                    ActivityEditorRow(
                        row = row,
                        onChange = { time, title -> onActivityChange(index, time, title) },
                        onRemove = { onRemoveActivity(index) }
                    )
                }
                if (canAddActivity) {
                    TextButton(onClick = onAddActivity) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar actividad", color = BrandBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEditorRow(
    row: ActivityDraft,
    onChange: (time: String, title: String) -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = row.time,
            onValueChange = { onChange(it, row.title) },
            label = { Text("Hora", fontSize = 11.sp) },
            placeholder = { Text("09:00", fontSize = 12.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = planFieldColors(),
            modifier = Modifier.width(92.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = row.title,
            onValueChange = { onChange(row.time, it) },
            label = { Text("Actividad", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = planFieldColors(),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Quitar actividad", tint = TextMuted)
        }
    }
}

@Composable
private fun PlanTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(10.dp),
        colors = planFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun planFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppBackground,
    unfocusedContainerColor = AppBackground,
    focusedBorderColor = BrandBlue,
    unfocusedBorderColor = BorderSubtle
)

@Preview(showBackground = true)
@Composable
fun TodayPlanCardPreview() {
    Etapa1Theme {
        TodayPlanCard(
            summary = TodayPlanSummary(
                date = SimpleDate(2026, 10, 7),
                title = "Hoy · miércoles 7 de octubre",
                lunch = "Lentejas con zanahoria, arroz y agua de limón",
                nextActivity = "09:00 · Asamblea: los animales de la granja"
            ),
            onClick = {}
        )
    }
}
