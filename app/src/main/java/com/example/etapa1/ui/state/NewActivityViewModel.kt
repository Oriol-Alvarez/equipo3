package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.model.FamilyGroupsSampleData

class NewActivityViewModel(
    private val bitacoraRepository: BitacoraRepository,
    private val familyGroupsRepository: FamilyGroupsRepository? = null
) : ViewModel() {

    fun saveActivity(
        time: String,
        category: String,
        portionLabel: String,
        description: String,
        childId: String,
        shareWithFamilyGroups: Boolean = false
    ) {
        bitacoraRepository.addActivityCard(
            childId = childId,
            time = time,
            category = category,
            portionLabel = portionLabel,
            description = description
        )

        if (shareWithFamilyGroups && familyGroupsRepository != null) {
            val participant = familyGroupsRepository.currentParticipant() ?: FamilyGroupsSampleData.EDUCADORA
            val groups = familyGroupsRepository.groups.value
            val targetGroups = groups.filter { it.memberChildIds.contains(childId) }.ifEmpty { groups }
            val activityText = "[ACTIVIDAD]\nCategoría: $category\nDetalle: $portionLabel\nObservaciones: $description"

            for (group in targetGroups) {
                familyGroupsRepository.sendMessage(
                    groupId = group.id,
                    sender = participant,
                    text = activityText,
                    attachment = null
                )
            }
        }
    }
}

