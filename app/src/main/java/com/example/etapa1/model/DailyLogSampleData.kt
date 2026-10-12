package com.example.etapa1.model

import com.example.etapa1.domain.SchoolWeek
import com.example.etapa1.domain.SimpleDate
import java.util.Locale

/**
 * Registros diarios de ejemplo para la demo: las últimas cuatro semanas de clases hasta hoy.
 * Los valores varían por niño y por día de forma fija (sin azar), para que las pruebas
 * y la demo siempre muestren lo mismo.
 */
object DailyLogSampleData {

    const val ESTANCIA_NAME = "Estancia Infantil Sonrisas de Cristal"

    private val assistants = listOf("Laura Gómez Ríos", "Lucía Martín Pérez", "Carlos Ruiz Medina")
    private const val RECEIVING_STAFF = "Laura Gómez Ríos"
    private const val DELIVERING_STAFF = "Lucía Martín Pérez"
    private val meals = listOf(
        FoodConsumptionOption.TODO, FoodConsumptionOption.TODO, FoodConsumptionOption.POCO,
        FoodConsumptionOption.PIDIO_MAS, FoodConsumptionOption.TODO, FoodConsumptionOption.NADA
    )

    /** Nombre de la mamá o el papá de cada niño (sin el "(Mamá)" / "(Papá)"). */
    private fun parentNameOf(child: Child): String {
        if (child.id == MockDataRepository.luciaGarcia.id) return "María López Fernández"
        return MockDataRepository.getInitialParentChats()
            .find { it.child.id == child.id }
            ?.parentName
            ?.substringBefore(" (")
            ?: "Madre o padre de familia"
    }

    fun generate(children: List<Child>, today: SimpleDate, weeks: Int = 4): List<DailyLog> {
        val days = (0 until weeks)
            .flatMap { SchoolWeek.weekOf(SchoolWeek.plusDays(today, -7 * it)) }
            .filterNot { SchoolWeek.isAfter(it, today) }
            .distinct()

        return children.distinctBy { it.id }.flatMapIndexed { childIndex, child ->
            days.mapNotNull { date ->
                val seed = childIndex * 7 + date.day * 5 + date.month
                // Algunas inasistencias para que la hoja muestre días vacíos.
                if (seed % 11 == 0) return@mapNotNull null
                val isToday = date == today
                DailyLog(
                    childId = child.id,
                    date = date,
                    ingreso = ingresoFor(child, seed),
                    estadia = if (isToday) partialEstadia(seed) else estadiaFor(child, seed),
                    egreso = if (isToday) null else egresoFor(child, seed)
                )
            }
        }
    }

    private fun ingresoFor(child: Child, seed: Int): IngresoLog {
        val sick = seed % 13 == 5
        val healthy = seed % 17 != 3
        return IngresoLog(
            time = time(7, 45, seed * 7 % 40),
            assistantName = assistants[seed % assistants.size],
            deliveredBy = parentNameOf(child),
            receivedBy = RECEIVING_STAFF,
            isSick = sick,
            medicationWithPrescription = if (sick) MedicationPrescriptionOption.SI else null,
            hasSymptoms = sick,
            symptoms = if (sick) "Tos leve y escurrimiento nasal" else "",
            arrivedClean = true,
            backpackComplete = seed % 9 != 4,
            goodPhysicalCondition = healthy,
            injuries = if (healthy) "" else "Rasguño leve en rodilla derecha",
            observations = if (seed % 5 == 0) "Durmió poco en casa." else ""
        )
    }

    private fun estadiaFor(child: Child, seed: Int): EstadiaLog {
        val sick = seed % 13 == 5
        val slept = seed % 10 != 7
        val accident = seed % 23 == 7
        val sad = seed % 8 == 3
        return EstadiaLog(
            breakfast = meals[seed % meals.size],
            snack = meals[(seed + 1) % meals.size],
            lunch = meals[(seed + 2) % meals.size],
            foodNotes = if (seed % 6 == 2) "Probó la verdura nueva del menú." else "",
            slept = slept,
            napMinutes = if (slept) 55 + seed % 40 else null,
            napComments = if (slept) "" else "Estuvo inquieto a la hora de la siesta.",
            sphincterControl = when {
                child.ageText.startsWith("1 año") -> SphincterControl.NO_APLICA
                seed % 3 == 0 -> SphincterControl.AVISO_NO
                else -> SphincterControl.AVISO_SI
            },
            sphincterPeeCount = if (child.ageText.startsWith("1 año")) null else 1 + seed % 3,
            sphincterPoopCount = if (child.ageText.startsWith("1 año")) null else seed % 2,
            peeCount = 2 + seed % 3,
            poopCount = seed % 3,
            mood = if (sad) MoodState.TRISTE else MoodState.FELIZ,
            cried = sad,
            fought = seed % 19 == 4,
            participated = !sad,
            moodNotes = if (sad) "Extrañó a su familia al inicio del día." else "",
            hadAccident = accident,
            accidentDescription = if (accident) "Tropezó en el patio; golpe leve en la frente." else "",
            accidentFolio = if (accident) String.format(Locale.US, "ACC-2026-%03d", seed) else "",
            hadHealthIssue = sick,
            healthIssue = if (sick) "Tos leve" else "",
            careProvided = if (sick) "Se dio el medicamento con receta a las 12:00 y se avisó a la familia." else "",
            generalNotes = ""
        )
    }

    /** Hoy todavía no terminan las actividades: solo desayuno y colación. */
    private fun partialEstadia(seed: Int) = EstadiaLog(
        breakfast = meals[seed % meals.size],
        snack = meals[(seed + 1) % meals.size]
    )

    private fun egresoFor(child: Child, seed: Int): EgresoLog = EgresoLog(
        time = time(14, 30, seed * 5 % 60),
        goodPhysicalCondition = true,
        injuries = "",
        collectedBy = if (seed % 4 == 1) "Rosa Fernández (abuela, autorizada)" else parentNameOf(child),
        deliveredClean = true,
        belongingsComplete = true,
        observations = "",
        deliveredByStaff = DELIVERING_STAFF
    )

    private fun time(hour: Int, minute: Int, plusMinutes: Int): String {
        val total = hour * 60 + minute + plusMinutes
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }
}
