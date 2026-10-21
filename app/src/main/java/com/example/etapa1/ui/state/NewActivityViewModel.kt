package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.DailyActivityInfo
import com.example.etapa1.model.ExcretionType
import com.example.etapa1.model.FamilyGroupsSampleData
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.MealType
import com.example.etapa1.model.PortionOption
import com.example.etapa1.model.SphincterControl

class NewActivityViewModel(
    private val bitacoraRepository: BitacoraRepository,
    private val familyGroupsRepository: FamilyGroupsRepository? = null,
    private val childRepository: ChildRepository? = null,
    private val dailyLogRepository: DailyLogRepository? = null
) : ViewModel() {

    fun saveActivity(
        time: String,
        category: ActivityCategory,
        summaryBadge: String,
        description: String,
        childId: String,
        shareWithFamilyGroups: Boolean = false,
        mealType: MealType? = null,
        foodConsumption: FoodConsumptionOption? = null,
        excretionType: ExcretionType? = null,
        excretionAviso: Boolean? = null,
        excretionNoAplica: Boolean? = null
    ) {
        // 1. Agregar a la línea de tiempo del chat de bitácora
        bitacoraRepository.addActivityCard(
            childId = childId,
            time = time,
            category = category.label,
            portionLabel = summaryBadge,
            description = description
        )

        // 2. Agregar a la lista funcional de actividades del día del alumno
        val portionOption = if (category == ActivityCategory.ALIMENTACION && foodConsumption != null) {
            when (foodConsumption) {
                FoodConsumptionOption.TODO -> PortionOption.TODO
                FoodConsumptionOption.POCO -> PortionOption.POCO
                FoodConsumptionOption.NADA -> PortionOption.POCO
                FoodConsumptionOption.PIDIO_MAS -> PortionOption.TODO
            }
        } else null

        val activityTitle = when (category) {
            ActivityCategory.ALIMENTACION -> mealType?.label ?: "Alimentación"
            ActivityCategory.FUNCIONES_EXCRETORAS -> excretionType?.label ?: "Función Excretora"
            ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> "Descanso / Siesta"
            ActivityCategory.ESTADO_ANIMO -> "Estado de Ánimo"
            ActivityCategory.ACCIDENTES -> "Reporte de Accidente"
            ActivityCategory.SALUD -> "Salud y Bienestar"
            ActivityCategory.OTROS -> "Actividad General"
        }

        val activityInfo = DailyActivityInfo(
            id = "act_${System.currentTimeMillis()}_${childId.take(10)}",
            time = time,
            title = activityTitle,
            category = category,
            portion = portionOption,
            description = description
        )
        com.example.etapa1.model.MockDataRepository.addDailyActivity(childId, activityInfo)

        // 3. Actualizar la estadía en DailyLogRepository (para guardar la bitácora y exportar en PDF)
        dailyLogRepository?.updateEstadia(childId, SimpleDate.today()) { current ->
            when (category) {
                ActivityCategory.ALIMENTACION -> {
                    var updated = current
                    if (mealType != null && foodConsumption != null) {
                        updated = when (mealType) {
                            MealType.DESAYUNO -> updated.copy(breakfast = foodConsumption)
                            MealType.COLACION -> updated.copy(snack = foodConsumption)
                            MealType.COMIDA -> updated.copy(lunch = foodConsumption)
                        }
                    }
                    val obs = description.substringAfter("Observaciones: ", "").trim()
                    if (obs.isNotBlank()) {
                        val newNotes = if (updated.foodNotes.isBlank()) obs else "${updated.foodNotes}; $obs"
                        updated = updated.copy(foodNotes = newNotes)
                    }
                    updated
                }

                ActivityCategory.FUNCIONES_EXCRETORAS -> {
                    var updated = current
                    val isPee = excretionType == ExcretionType.PIPI || excretionType == ExcretionType.AMBOS
                    val isPoop = excretionType == ExcretionType.POPO || excretionType == ExcretionType.AMBOS

                    if (isPee) {
                        updated = updated.copy(peeCount = (updated.peeCount ?: 0) + 1)
                        if (excretionNoAplica != true && excretionAviso == true) {
                            updated = updated.copy(sphincterPeeCount = (updated.sphincterPeeCount ?: 0) + 1)
                        }
                    }

                    if (isPoop) {
                        updated = updated.copy(poopCount = (updated.poopCount ?: 0) + 1)
                        if (excretionNoAplica != true && excretionAviso == true) {
                            updated = updated.copy(sphincterPoopCount = (updated.sphincterPoopCount ?: 0) + 1)
                        }
                    }

                    if (excretionNoAplica == true) {
                        if (updated.sphincterControl == null) {
                            updated = updated.copy(sphincterControl = SphincterControl.NO_APLICA)
                        }
                    } else if (excretionAviso != null) {
                        if (excretionAviso) {
                            updated = updated.copy(sphincterControl = SphincterControl.AVISO_SI)
                        } else if (updated.sphincterControl != SphincterControl.AVISO_SI) {
                            updated = updated.copy(sphincterControl = SphincterControl.AVISO_NO)
                        }
                    }

                    val obs = description.substringAfter("Observaciones: ", "").trim()
                    if (obs.isNotBlank()) {
                        val newNotes = if (updated.excretionNotes.isBlank()) obs else "${updated.excretionNotes}; $obs"
                        updated = updated.copy(excretionNotes = newNotes)
                    }
                    updated
                }

                ActivityCategory.DESCANSO, ActivityCategory.SIESTA -> {
                    val mins = description.substringAfter("un tiempo de ", "").substringBefore(" minutos").toIntOrNull()
                    val obs = description.substringAfter("Comentarios: ", "").trim()
                    current.copy(
                        slept = !description.contains("No durmió"),
                        napMinutes = mins ?: current.napMinutes,
                        napComments = if (obs.isNotBlank()) {
                            if (current.napComments.isBlank()) obs else "${current.napComments}; $obs"
                        } else current.napComments
                    )
                }

                ActivityCategory.ESTADO_ANIMO -> {
                    val obs = description.substringAfter("Observaciones: ", "").trim()
                    current.copy(
                        cried = description.contains("¿Lloró?: Sí"),
                        fought = description.contains("¿Peleó?: Sí"),
                        participated = description.contains("¿Participó?: Sí"),
                        moodNotes = if (obs.isNotBlank()) {
                            if (current.moodNotes.isBlank()) obs else "${current.moodNotes}; $obs"
                        } else current.moodNotes
                    )
                }

                ActivityCategory.ACCIDENTES -> {
                    val hadAcc = !description.contains("No tuvo ningún accidente")
                    val folio = description.substringAfter("Folio: ", "").substringBefore(". Detalle:").trim()
                    val det = description.substringAfter(". Detalle: ", "").trim()
                    current.copy(
                        hadAccident = hadAcc,
                        accidentFolio = if (hadAcc) folio else current.accidentFolio,
                        accidentDescription = if (hadAcc) det else current.accidentDescription
                    )
                }

                ActivityCategory.SALUD -> {
                    val hadIssue = !description.contains("No presentó problemas de salud")
                    val prob = description.substringAfter("Problema: ", "").substringBefore(". Atención:").trim()
                    val atn = description.substringAfter(". Atención: ", "").substringBefore(". Observaciones:").trim()
                    val obs = description.substringAfter("Observaciones: ", "").trim()
                    current.copy(
                        hadHealthIssue = hadIssue,
                        healthIssue = if (hadIssue) prob else current.healthIssue,
                        careProvided = if (hadIssue) atn else current.careProvided,
                        generalNotes = if (obs.isNotBlank()) {
                            if (current.generalNotes.isBlank()) obs else "${current.generalNotes}; $obs"
                        } else current.generalNotes
                    )
                }

                ActivityCategory.OTROS -> current
            }
        }

        // 4. Compartir con grupos familiares si fue solicitado
        if (shareWithFamilyGroups && familyGroupsRepository != null) {
            val participant = familyGroupsRepository.currentParticipant() ?: FamilyGroupsSampleData.EDUCADORA
            val groups = familyGroupsRepository.groups.value
            val targetGroups = groups.filter { it.memberChildIds.contains(childId) }.ifEmpty { groups }
            val activityText = "[ACTIVIDAD]\nCategoría: ${category.label}\nDetalle: $summaryBadge\nObservaciones: $description"

            for (group in targetGroups) {
                familyGroupsRepository.sendMessage(
                    groupId = group.id,
                    sender = participant,
                    text = activityText,
                    attachment = null
                )
            }
        }

        // 5. Notificar al repositorio de niños para recargar el resumen semanal y la ficha
        childRepository?.cargarDetalle(childId)
    }

    /**
     * Registra una actividad colectiva para todos los niños del grupo en sus bitácoras individuales.
     */
    fun saveGroupActivity(
        groupId: String,
        targetChildIds: List<String>,
        time: String,
        category: ActivityCategory,
        summaryBadge: String,
        description: String,
        mealType: MealType? = null,
        foodConsumption: FoodConsumptionOption? = null,
        excretionType: ExcretionType? = null,
        excretionAviso: Boolean? = null,
        excretionNoAplica: Boolean? = null
    ) {
        for (childId in targetChildIds) {
            saveActivity(
                time = time,
                category = category,
                summaryBadge = summaryBadge,
                description = description,
                childId = childId,
                shareWithFamilyGroups = false,
                mealType = mealType,
                foodConsumption = foodConsumption,
                excretionType = excretionType,
                excretionAviso = excretionAviso,
                excretionNoAplica = excretionNoAplica
            )
        }
    }

    /** Sobrecarga para compatibilidad con llamadas y pruebas existentes. */
    fun saveActivity(
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        childId: String,
        shareWithFamilyGroups: Boolean = false
    ) {
        val catEnum = ActivityCategory.values().find {
            it.label.equals(category, ignoreCase = true) || it.name.equals(category, ignoreCase = true)
        } ?: ActivityCategory.OTROS

        saveActivity(
            time = time,
            category = catEnum,
            summaryBadge = portionLabel,
            description = description,
            childId = childId,
            shareWithFamilyGroups = shareWithFamilyGroups
        )
    }
}
