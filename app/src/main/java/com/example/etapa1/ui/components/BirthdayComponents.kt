package com.example.etapa1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etapa1.domain.BirthdayCalculator
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.domain.UpcomingBirthday
import com.example.etapa1.ui.theme.Etapa1Theme
import com.example.etapa1.ui.theme.TextPrimary
import com.example.etapa1.ui.theme.TextSecondary

private val BirthdayPink = Color(0xFFD81B60)
private val BirthdayBackground = Color(0xFFFCE4EC)
private val BirthdayBorder = Color(0xFFF8BBD0)

/**
 * Aviso para la educadora con los cumpleaños de la sala de hoy y de los próximos días.
 * Tocar un niño abre su bitácora, por ejemplo para felicitar a la familia.
 */
@Composable
fun RoomBirthdaysCard(
    birthdays: List<UpcomingBirthday>,
    onChildClick: (childId: String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BirthdayBackground)
            .border(1.dp, BirthdayBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = BirthdayPink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (birthdays.any { it.isToday }) "¡Hoy hay cumpleaños en la sala!" else "Cumpleaños de esta semana",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BirthdayPink
            )
        }

        birthdays.forEach { birthday ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onChildClick(birthday.childId) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (birthday.isToday) BirthdayPink else BirthdayBorder)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${birthday.childName} cumple ${BirthdayCalculator.ageText(birthday.turningAge)}",
                        fontSize = 13.sp,
                        fontWeight = if (birthday.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = BirthdayCalculator.formatDate(birthday.date),
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = BirthdayCalculator.whenText(birthday.daysUntil),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (birthday.isToday) BirthdayPink else TextSecondary
                )
            }
        }
    }
}

/** Felicitación o cuenta regresiva para la familia en el inicio de su hijo o hija. */
@Composable
fun FamilyBirthdayBanner(birthday: UpcomingBirthday) {
    val firstName = birthday.childName.substringBefore(" ")
    val title = if (birthday.isToday) {
        "¡Feliz cumpleaños, $firstName! 🎉"
    } else {
        "${BirthdayCalculator.whenText(birthday.daysUntil)} es el cumpleaños de $firstName 🎂"
    }
    val subtitle = if (birthday.isToday) {
        "Hoy cumple ${BirthdayCalculator.ageText(birthday.turningAge)}. Toda la estancia le desea un día muy especial."
    } else {
        "Cumplirá ${BirthdayCalculator.ageText(birthday.turningAge)} el ${BirthdayCalculator.formatDate(birthday.date)}."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BirthdayBackground)
            .border(1.dp, BirthdayBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Cake,
            contentDescription = null,
            tint = BirthdayPink,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BirthdayPink)
            Text(text = subtitle, fontSize = 12.5.sp, color = TextPrimary, lineHeight = 17.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoomBirthdaysCardPreview() {
    Etapa1Theme {
        RoomBirthdaysCard(
            birthdays = listOf(
                UpcomingBirthday("mia_ramirez", "Mia Ramírez", SimpleDate(2026, 10, 9), 0, 2),
                UpcomingBirthday("bruno_benitez", "Bruno Benítez", SimpleDate(2026, 10, 11), 2, 2)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyBirthdayBannerPreview() {
    Etapa1Theme {
        FamilyBirthdayBanner(
            UpcomingBirthday("mateo_garcia", "Mateo García", SimpleDate(2027, 4, 14), 3, 3)
        )
    }
}
