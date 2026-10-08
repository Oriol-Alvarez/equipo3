package com.example.etapa1.model

import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate

/**
 * Planeación y menú del día de una sala: lo publica la educadora y lo consultan las familias.
 * Los datos son simulados y viven solo mientras el proceso de la app está activo.
 */
data class DailyMenu(
    val breakfast: String,
    val snack: String,
    val lunch: String,
    /** Avisos de cocina, por ejemplo menús especiales por alergias. Opcional. */
    val notes: String = ""
)

data class PlannedActivity(
    /** Hora en formato 24 h "HH:mm". */
    val time: String,
    val title: String
)

data class DailyPlan(
    val date: SimpleDate,
    val menu: DailyMenu,
    val activities: List<PlannedActivity>,
    val updatedBy: String = "Educadora Sala 1A"
)

object DailyPlanSampleData {

    private val menus = listOf(
        DailyMenu(
            breakfast = "Avena con plátano y leche",
            snack = "Gajos de mandarina",
            lunch = "Crema de calabaza, arroz con pollo y agua de jamaica",
            notes = "Mateo García: menú sin nuez."
        ),
        DailyMenu(
            breakfast = "Hot cakes de avena con fruta",
            snack = "Yogur natural",
            lunch = "Sopa de fideo, tortitas de verdura y agua de pepino"
        ),
        DailyMenu(
            breakfast = "Huevo revuelto con jitomate y tortilla",
            snack = "Pepino y jícama",
            lunch = "Lentejas con zanahoria, arroz y agua de limón",
            notes = "Diego Fernández: leche deslactosada."
        ),
        DailyMenu(
            breakfast = "Pan integral con frijoles y queso fresco",
            snack = "Manzana picada",
            lunch = "Caldo de verduras, pescado empapelado y puré de papa"
        ),
        DailyMenu(
            breakfast = "Licuado de fresa y galletas de avena",
            snack = "Papaya",
            lunch = "Pasta con verduras, pollo deshebrado y agua de melón"
        )
    )

    private val activities = listOf(
        listOf(
            PlannedActivity("09:00", "Asamblea de bienvenida y canciones"),
            PlannedActivity("10:00", "Motricidad fina: ensartar cuentas grandes"),
            PlannedActivity("11:30", "Juego libre en el patio")
        ),
        listOf(
            PlannedActivity("09:00", "Asamblea: los colores"),
            PlannedActivity("10:00", "Pintura con dedos sobre mural"),
            PlannedActivity("11:30", "Cuentacuentos con títeres")
        ),
        listOf(
            PlannedActivity("09:00", "Asamblea: los animales de la granja"),
            PlannedActivity("10:00", "Circuito de psicomotricidad"),
            PlannedActivity("11:30", "Música y movimiento")
        ),
        listOf(
            PlannedActivity("09:00", "Asamblea: el clima"),
            PlannedActivity("10:00", "Taller sensorial con texturas"),
            PlannedActivity("11:30", "Jardín: regar las plantas")
        ),
        listOf(
            PlannedActivity("09:00", "Asamblea y repaso de la semana"),
            PlannedActivity("10:00", "Construcción con bloques"),
            PlannedActivity("11:30", "Mini fiesta de cumpleaños del mes")
        )
    )

    /** Planeación de lunes a viernes de la semana actual, para que la demo siempre tenga datos. */
    fun weekFor(today: SimpleDate): List<DailyPlan> =
        SchoolWeek.schoolWeek(today).mapIndexed { index, date ->
            DailyPlan(date = date, menu = menus[index], activities = activities[index])
        }
}
