package com.example.etapa1.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.model.FamilyGroupCandidate
import com.example.etapa1.model.FamilyGroupsSampleData
import com.example.etapa1.model.GroupViewer
import com.example.etapa1.model.MessageAttachment
import com.example.etapa1.model.WorkerChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FamilyGroupsUiState(
    /** null mientras la pantalla todavía no indica quién está viendo. */
    val viewer: GroupViewer? = null,
    val groups: List<WorkerChat> = emptyList(),
    val candidates: List<FamilyGroupCandidate> = emptyList(),
    val canCreateGroups: Boolean = false,
    val errorMessage: String? = null
)

class FamilyGroupsViewModel(
    private val familyGroupsRepository: FamilyGroupsRepository,
    messagesRepository: MessagesRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val viewer = MutableStateFlow<GroupViewer?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FamilyGroupsUiState> = combine(
        familyGroupsRepository.groups,
        messagesRepository.parentChats,
        viewer,
        errorMessage
    ) { groups, parentChats, currentViewer, error ->
        if (currentViewer == null) {
            FamilyGroupsUiState()
        } else {
            val childrenById = (childRepository.childrenSala1A.value + childRepository.parentChildren.value)
                .associateBy { it.id }
            FamilyGroupsUiState(
                viewer = currentViewer,
                groups = FamilyGroupsPresenter.groupsVisibleTo(groups, currentViewer)
                    .map { FamilyGroupsPresenter.toChat(it, currentViewer, childrenById) },
                candidates = FamilyGroupsPresenter.candidatesFrom(parentChats),
                canCreateGroups = FamilyGroupsPresenter.canCreateGroups(currentViewer),
                errorMessage = error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FamilyGroupsUiState()
    )

    fun useEducatorViewer() {
        viewer.value = GroupViewer.Educadora(FamilyGroupsSampleData.EDUCADORA)
    }

    fun useFamilyViewer() {
        viewer.value = GroupViewer.Familia(
            participant = FamilyGroupsSampleData.FAMILIA_GARCIA,
            childIds = childRepository.parentChildren.value.map { it.id }
        )
    }

    /** Devuelve true si el grupo se creó; si no, deja el motivo en [FamilyGroupsUiState.errorMessage]. */
    fun createGroup(title: String, memberChildIds: List<String>): Boolean {
        val current = viewer.value
        if (current == null || !FamilyGroupsPresenter.canCreateGroups(current)) {
            errorMessage.value = "Solo la educadora puede crear grupos."
            return false
        }
        return familyGroupsRepository.createGroup(title, memberChildIds, current.participant).fold(
            onSuccess = {
                errorMessage.value = null
                true
            },
            onFailure = {
                errorMessage.value = it.message
                false
            }
        )
    }

    fun sendMessage(
        groupId: String,
        text: String,
        fileUri: String?,
        fileName: String?,
        fileMimeType: String?
    ) {
        val current = viewer.value ?: return
        val group = familyGroupsRepository.groups.value.find { it.id == groupId } ?: return
        // Una familia solo puede escribir en los grupos donde está su hijo o hija.
        if (!FamilyGroupsPresenter.isVisibleTo(group, current)) return

        val attachment = fileUri?.let { MessageAttachment(uri = it, name = fileName, mimeType = fileMimeType) }
        familyGroupsRepository.sendMessage(groupId, current.participant, text, attachment)
    }

    fun clearError() {
        errorMessage.value = null
    }
}
