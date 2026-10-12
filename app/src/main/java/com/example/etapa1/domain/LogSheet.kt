package com.example.etapa1.domain

import com.example.etapa1.model.Child
import com.example.etapa1.model.DailyLog
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MedicationPrescriptionOption

/** Contenido de una casilla de un día. */
sealed interface LogSheetCell {
    /** Texto como aparece en el PDF ("" = en blanco para llenar a mano). */
    val plainText: String

    data class Text(val text: String) : LogSheetCell {
        override val plainText: String get() = text
    }

    /**
     * Opciones impresas igual que en la hoja (Sí / No, Nada / Poco / Todo / Pidió más…) con una
     * casilla cada una; se marca [selected]. Si es null quedan todas vacías para llenarlas a mano.
     */
    data class Choice(val options: List<String>, val selected: Int?) : LogSheetCell {
        override val plainText: String get() = selected?.let { options[it] } ?: ""
    }
}

/** Fila de la hoja: pregunta y una casilla por día (lunes a viernes). */
data class LogSheetRow(
    val label: String,
    val cells: List<LogSheetCell>,
    /** "Nombre y firma": fila más alta para firmar a mano debajo del nombre. */
    val isSignature: Boolean = false
) {
    /** Texto de cada día; útil para pruebas. */
    val values: List<String> get() = cells.map { it.plainText }

    /** Fila de texto a todo lo ancho, sin casillas (p. ej. "La niña o el niño consumió los alimentos durante el:"). */
    val isCaption: Boolean get() = cells.isEmpty()
}

data class LogSheetSection(val title: String, val rows: List<LogSheetRow>)

/** Una página de la hoja oficial, con sus apartados en el mismo orden. */
data class LogSheetPage(val sections: List<LogSheetSection>)

/**
 * Contenido de la "Bitácora de filtro diario" de una semana para un niño, ya convertido a texto.
 * Lo dibuja el generador de PDF; aquí no hay nada de Android para poder probarlo.
 */
data class LogSheet(
    val titleLines: List<String>,
    val estancia: String,
    val childName: String,
    /** "L", "M" o "P" (Lactantes, Maternales, Preescolar); vacío si no se sabe. */
    val group: String,
    val weekLabel: String,
    /** "Fecha: 05/10/2026" para cada día. */
    val dayHeaders: List<String>,
    val pages: List<LogSheetPage>,
    val daysWithRecords: Int
)

object LogSheetFormatter {

    val TITLE_LINES = listOf(
        "APOYOS PARA LA INCLUSIÓN EN LA PRIMERA INFANCIA",
        "PRESTACIÓN DE SERVICIOS PARA LA ATENCIÓN CUIDADO Y DESARROLLO INTEGRAL INFANTIL"
    )

    /** Letras de la hoja para lunes a viernes; se marca la del día de cada columna. */
    val DAY_LETTERS = listOf("L", "M", "M", "J", "V")

    private val YES_NO = listOf("Sí", "No")
    private val MEDICATION = listOf("Sí", "No", "No aplica")
    private val MEALS = listOf("Nada", "Poco", "Todo", "Pidió más")
    private val MOODS = listOf("Feliz", "Triste", "Enojado")
    private val SPHINCTER = listOf("No aplica", "Avisó: Sí", "Avisó: No")

    private const val INJURY_LABEL = "En caso que la niña o el niño sea entregada(o) con algún rasguño, mordida, " +
        "quemadura, golpe, rozadura, etc. Indicar la parte del cuerpo donde se presenta y especificar la lesión:"

    /** Lactantes → L, Maternales → M, Preescolar → P. */
    fun groupLetter(levelOrGroup: String): String {
        val text = levelOrGroup.lowercase()
        return when {
            "lact" in text || "beb" in text -> "L"
            "mater" in text -> "M"
            "pree" in text -> "P"
            else -> ""
        }
    }

    /**
     * Arma la hoja de la semana. [logs] trae un registro (o null) por cada día de [week],
     * en el mismo orden.
     */
    fun build(
        child: Child,
        levelOrGroup: String,
        estancia: String,
        week: List<SimpleDate>,
        logs: List<DailyLog?>
    ): LogSheet {
        require(week.size == logs.size) { "Debe haber un registro (o null) por cada día" }
        val ingresos = logs.map { it?.ingreso }
        val estadias = logs.map { it?.estadia }
        val egresos = logs.map { it?.egreso }

        val ingresoSection = LogSheetSection(
            title = "II. Registro de Ingreso de la niña o niño a la Estancia Infantil",
            rows = listOf(
                text("Nombre completo de la Asistente al cuidado de la (el) niña (o)", ingresos) { it.assistantName },
                text("Hora de entrada:", ingresos) { hours(it.time) },
                signature("Nombre y firma de quien entrega a la niña o el niño:", ingresos) { it.deliveredBy },
                signature("Nombre y firma del personal de la E.I., quien recibe a la niña o el niño en la E.I.:", ingresos) {
                    it.receivedBy
                },
                choice("La niña o el niño está enferma (o)", YES_NO, ingresos) { yesNo(it.isSick) },
                choice(
                    "En el caso de que la niña o el niño ingrese enfermo se dejó medicamento con receta médica y especificaciones",
                    MEDICATION, ingresos
                ) { medication(it) },
                choice("La niña o el niño ingresa con síntomas de enfermedad", YES_NO, ingresos) { yesNo(it.hasSymptoms) },
                text("¿Cuál(es)?", ingresos) { if (it.hasSymptoms == true) it.symptoms.trim() else "" },
                choice("La niña o el niño se presentó limpia (o)", YES_NO, ingresos) { yesNo(it.arrivedClean) },
                choice("La niña o el niño trajo su mochila completa según sus necesidades:", YES_NO, ingresos) {
                    yesNo(it.backpackComplete)
                },
                choice("La niña o el niño se presentó en buen estado físico:", YES_NO, ingresos) {
                    yesNo(it.goodPhysicalCondition)
                },
                text(INJURY_LABEL, ingresos) { injury(it.goodPhysicalCondition, it.injuries) },
                text("Observaciones para las (os) Madres, Padres o personas autorizadas:", ingresos) { it.observations }
            )
        )

        val egresoSection = LogSheetSection(
            title = "IV. Registro de egreso de la niña o niño de la Estancia Infantil",
            rows = listOf(
                choice("¿La niña o el niño se entrega en buen estado físico?", YES_NO, egresos) {
                    yesNo(it.goodPhysicalCondition)
                },
                text(INJURY_LABEL, egresos) { injury(it.goodPhysicalCondition, it.injuries) },
                text("Hora de salida:", egresos) { hours(it.time) },
                signature("Nombre y firma de quien recibe a la niña o el niño:", egresos) { it.collectedBy },
                choice("La niña o el niño se entregó limpia(o)", YES_NO, egresos) { yesNo(it.deliveredClean) },
                choice("La niña o el niño se entregó con pertenencias completas (mochila completa)", YES_NO, egresos) {
                    yesNo(it.belongingsComplete)
                },
                text("Observaciones:", egresos) { it.observations },
                signature("Nombre y firma del personal de la E.I., quien entrega a la niña o el niño en la E.I.:", egresos) {
                    it.deliveredByStaff
                }
            )
        )

        val estadiaSections = listOf(
            LogSheetSection(
                "Alimentación",
                listOf(
                    caption("La niña o el niño consumió los alimentos durante el:"),
                    choice("Desayuno", MEALS, estadias) { it.breakfast?.ordinal },
                    choice("Colación", MEALS, estadias) { it.snack?.ordinal },
                    choice("Comida", MEALS, estadias) { it.lunch?.ordinal },
                    text("Observaciones Generales:", estadias) { it.foodNotes }
                )
            ),
            LogSheetSection(
                "Descanso",
                listOf(
                    choice("¿La niña o el niño durmió?", YES_NO, estadias) { yesNo(it.slept) },
                    text("Tiempo de siesta (minutos)", estadias) { it.napMinutes?.let { m -> "$m minutos" } ?: "" },
                    text("Comentarios", estadias) { it.napComments }
                )
            ),
            LogSheetSection(
                "Funciones Excretoras",
                listOf(
                    choice("Control de esfínteres", SPHINCTER, estadias) { it.sphincterControl?.ordinal },
                    text("Control de esfínteres · Número de veces (pipí / popó)", estadias) {
                        counts(it.sphincterPeeCount, it.sphincterPoopCount)
                    },
                    text("Pipí y/o Popó · Número de veces (pipí / popó)", estadias) { counts(it.peeCount, it.poopCount) },
                    text("Observaciones:", estadias) { it.excretionNotes }
                )
            ),
            LogSheetSection(
                "Estado de ánimo",
                listOf(
                    choice("¿Durante su estadía, la niña o el niño estuvo?", MOODS, estadias) { it.mood?.ordinal },
                    caption("¿Durante las actividades?"),
                    choice("¿Lloró?", YES_NO, estadias) { yesNo(it.cried) },
                    choice("¿Peleó?", YES_NO, estadias) { yesNo(it.fought) },
                    choice("¿Participó?", YES_NO, estadias) { yesNo(it.participated) },
                    text("Observaciones:", estadias) { it.moodNotes }
                )
            ),
            LogSheetSection(
                "Accidentes",
                listOf(
                    choice("¿La niña o el niño tuvo algún accidente?", YES_NO, estadias) { yesNo(it.hadAccident) },
                    text("Descripción breve del accidente:", estadias) { it.accidentDescription },
                    text("Folio de reporte de accidentes:", estadias) { it.accidentFolio }
                )
            ),
            LogSheetSection(
                "Salud",
                listOf(
                    choice("¿La niña o el niño presentó algún problema de salud?", YES_NO, estadias) {
                        yesNo(it.hadHealthIssue)
                    },
                    text("¿Cuál?", estadias) { it.healthIssue },
                    text("Atención proporcionada", estadias) { it.careProvided },
                    text("Observaciones Generales:", estadias) { it.generalNotes }
                )
            )
        )

        return LogSheet(
            titleLines = TITLE_LINES,
            estancia = estancia,
            childName = child.fullName,
            group = groupLetter(levelOrGroup),
            weekLabel = "Semana del ${SchoolWeek.numericLabel(week.first())} al ${SchoolWeek.numericLabel(week.last())}",
            dayHeaders = week.map { "Fecha: ${SchoolWeek.numericLabel(it)}" },
            pages = listOf(
                LogSheetPage(listOf(ingresoSection, egresoSection)),
                LogSheetPage(listOf(LogSheetSection("III. Registro de estadía de la niña o niño en la Estancia Infantil", emptyList())) + estadiaSections)
            ),
            daysWithRecords = logs.count { it != null }
        )
    }

    // --- Conversión de respuestas ----------------------------------------------------

    /** Posición en las opciones Sí / No (null = sin contestar). */
    fun yesNo(value: Boolean?): Int? = when (value) {
        true -> 0
        false -> 1
        null -> null
    }

    /** Si no está enferma(o) la respuesta es "No aplica", igual que en la hoja. */
    private fun medication(ingreso: IngresoLog): Int? = when (ingreso.isSick) {
        false -> MedicationPrescriptionOption.NO_APLICA.ordinal
        true -> ingreso.medicationWithPrescription?.ordinal
        null -> null
    }

    private fun hours(time: String): String = if (time.isBlank()) "" else "$time hrs."

    /** Sin lesión se escribe "NA", como en la hoja de ejemplo. */
    private fun injury(goodCondition: Boolean?, injuries: String): String = when {
        injuries.isNotBlank() -> injuries.trim()
        goodCondition == true -> "NA"
        else -> ""
    }

    private fun counts(pee: Int?, poop: Int?): String =
        if (pee == null && poop == null) "" else "pipí: ${pee ?: "—"}   popó: ${poop ?: "—"}"

    private fun caption(label: String) = LogSheetRow(label, emptyList())

    /** Los días sin registro quedan en blanco para poder llenarlos a mano si hace falta. */
    private fun <T> text(label: String, items: List<T?>, value: (T) -> String): LogSheetRow =
        LogSheetRow(label, items.map { item -> LogSheetCell.Text(item?.let(value)?.trim() ?: "") })

    private fun <T> signature(label: String, items: List<T?>, name: (T) -> String): LogSheetRow =
        LogSheetRow(label, items.map { item -> LogSheetCell.Text(item?.let(name)?.trim() ?: "") }, isSignature = true)

    private fun <T> choice(label: String, options: List<String>, items: List<T?>, selected: (T) -> Int?): LogSheetRow =
        LogSheetRow(label, items.map { item -> LogSheetCell.Choice(options, item?.let(selected)) })
}
