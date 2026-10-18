package com.example.etapa1.data.repository.impl

import android.content.Context
import android.net.Uri
import com.example.etapa1.data.remote.AttachmentDto
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.CreateFamilyGroupDto
import com.example.etapa1.data.remote.EventoServidor
import com.example.etapa1.data.remote.FamilyGroupDto
import com.example.etapa1.data.remote.FamilyGroupMessageDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SendFamilyGroupMessageDto
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.model.FamilyGroupCandidate
import com.example.etapa1.model.FamilyGroupChat
import com.example.etapa1.model.FamilyGroupMessage
import com.example.etapa1.model.GroupParticipant
import com.example.etapa1.model.MessageAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Grupos con familias guardados en el backend (/family-groups).
 *
 * Los cambios se ven al instante en la pantalla y se mandan al servidor en segundo plano;
 * al terminar se vuelve a cargar el grupo para quedar igual que el servidor.
 * Las fotos y archivos se suben primero a /media/upload y el mensaje guarda la URL del servidor.
 */
class NetworkFamilyGroupsRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager,
    private val context: Context,
    private val chatConexion: ChatConexion? = null
) : FamilyGroupsRepository {

    /** Los cambios se mandan al servidor en orden, uno por uno. */
    private val writes = RemoteSync.SerialQueue()

    private val _groups = MutableStateFlow<List<FamilyGroupChat>>(emptyList())
    override val groups: StateFlow<List<FamilyGroupChat>> = _groups.asStateFlow()

    private val _candidates = MutableStateFlow<List<FamilyGroupCandidate>>(emptyList())
    override val serverCandidates: StateFlow<List<FamilyGroupCandidate>> = _candidates.asStateFlow()

    private var localCounter = 0

    init {
        refresh()
        observarWebSocket()
    }

    private fun observarWebSocket() {
        val conexion = chatConexion ?: return
        RemoteSync.launch("Escuchar mensajes de grupos familiares en WebSocket") {
            conexion.eventos.collect { evento ->
                if (evento is EventoServidor.NuevoMensajeGrupo) {
                    val domainMsg = evento.message.toDomain()
                    _groups.update { current ->
                        current.map { group ->
                            if (group.id == evento.groupId) {
                                if (group.messages.any { it.id == domainMsg.id }) group
                                else group.copy(messages = listOf(domainMsg) + group.messages)
                            } else group
                        }
                    }
                }
            }
        }
    }

    override fun currentParticipant(): GroupParticipant? {
        val id = tokenManager.userId ?: return null
        return GroupParticipant(id = id, displayName = tokenManager.userDisplayName ?: "Yo")
    }

    override fun refresh() {
        RemoteSync.launch("Cargar grupos") {
            val dtos = api.getFamilyGroups().bodyOrLog("GET /family-groups") ?: return@launch
            val loaded = dtos.map { dto ->
                val messages = api.getFamilyGroupMessages(dto.id).bodyOrLog("GET mensajes ${dto.id}").orEmpty()
                dto.toDomain(messages.map { it.toDomain() })
            }
            // El servidor ya los manda del más reciente al más antiguo. Se conservan los grupos y
            // mensajes que se están enviando en este momento para que no desaparezcan de la pantalla.
            _groups.update { current -> mergePending(loaded, current) }
        }
        if (tokenManager.userRole?.uppercase() != "FAMILIAR") {
            RemoteSync.launch("Cargar candidatos") {
                api.getFamilyGroupCandidates().bodyOrLog("GET /family-groups/candidates")?.let { list ->
                    _candidates.value = list.map {
                        FamilyGroupCandidate(it.childId, it.childName, it.parentName, it.avatarInitials, it.avatarBgColor)
                    }
                }
            }
        }
    }

    override fun createGroup(
        title: String,
        memberChildIds: List<String>,
        creator: GroupParticipant
    ): Result<FamilyGroupChat> {
        val cleanTitle = title.trim()
        val members = memberChildIds.distinct()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe un nombre para el grupo."))
        }
        if (cleanTitle.length > MockFamilyGroupsRepositoryImpl.MAX_TITLE_LENGTH) {
            return Result.failure(
                IllegalArgumentException(
                    "El nombre del grupo puede tener hasta ${MockFamilyGroupsRepositoryImpl.MAX_TITLE_LENGTH} caracteres."
                )
            )
        }
        if (members.isEmpty()) {
            return Result.failure(IllegalArgumentException("Elige al menos una familia para el grupo."))
        }

        // Grupo provisional mientras responde el servidor
        val temp = FamilyGroupChat(
            id = nextLocalId(LOCAL_GROUP),
            title = cleanTitle,
            memberChildIds = members,
            createdById = creator.id,
            messages = listOf(
                FamilyGroupMessage(nextLocalId(LOCAL_MESSAGE), creator.id, creator.displayName, "Grupo «$cleanTitle» creado.", now())
            )
        )
        _groups.update { listOf(temp) + it }

        writes.enqueue("Crear grupo") {
            val created = api.createFamilyGroup(CreateFamilyGroupDto(cleanTitle, members)).bodyOrLog("POST /family-groups")
            if (created == null) {
                _groups.update { list -> list.filter { it.id != temp.id } }
                return@enqueue
            }
            val messages = api.getFamilyGroupMessages(created.id).bodyOrLog("GET mensajes").orEmpty()
            val real = created.toDomain(messages.map { it.toDomain() })
            _groups.update { list -> listOf(real) + list.filter { it.id != temp.id && it.id != real.id } }
        }
        return Result.success(temp)
    }

    override fun sendMessage(
        groupId: String,
        sender: GroupParticipant,
        text: String,
        attachment: MessageAttachment?
    ): Boolean {
        val cleanText = text.trim()
        if (cleanText.isEmpty() && attachment == null) return false
        val group = _groups.value.find { it.id == groupId } ?: return false

        val pending = FamilyGroupMessage(
            id = nextLocalId(LOCAL_MESSAGE),
            senderId = sender.id,
            senderName = sender.displayName,
            text = cleanText,
            time = now(),
            attachment = attachment
        )
        _groups.update { list -> listOf(group.copy(messages = group.messages + pending)) + list.filter { it.id != groupId } }

        writes.enqueue("Enviar mensaje") {
            val uploaded = attachment?.let { uploadAttachment(it) }
            if (attachment != null && uploaded == null) {
                removeMessage(groupId, pending.id)
                return@enqueue
            }
            val sent = api.sendFamilyGroupMessage(groupId, SendFamilyGroupMessageDto(cleanText, uploaded))
                .bodyOrLog("POST mensaje")
            if (sent == null) {
                removeMessage(groupId, pending.id)
                return@enqueue
            }
            replaceMessage(groupId, pending.id, sent.toDomain())
        }
        return true
    }

    /** Sube el archivo local (content://) al servidor. Si ya es una URL del servidor, la deja igual. */
    private suspend fun uploadAttachment(attachment: MessageAttachment): AttachmentDto? {
        val name = attachment.name ?: "archivo"
        val mime = attachment.mimeType ?: "application/octet-stream"
        if (attachment.uri.startsWith("http") || attachment.uri.startsWith("/")) {
            return AttachmentDto(attachment.uri, name, mime)
        }
        val bytes = context.contentResolver.openInputStream(Uri.parse(attachment.uri))?.use { it.readBytes() }
            ?: return null
        val part = MultipartBody.Part.createFormData("file", name, bytes.toRequestBody(mime.toMediaTypeOrNull()))
        val folder = "family-groups".toRequestBody("text/plain".toMediaTypeOrNull())
        val media = api.uploadMedia(part, folder).bodyOrLog("POST /media/upload") ?: return null
        return AttachmentDto(uri = media.url, name = name, mimeType = media.mimeType)
    }

    private fun replaceMessage(groupId: String, localId: String, message: FamilyGroupMessage) {
        _groups.update { list ->
            list.map { g ->
                if (g.id == groupId) {
                    g.copy(messages = g.messages.filter { it.id != localId && it.id != message.id } + message)
                } else {
                    g
                }
            }
        }
    }

    private fun mergePending(loaded: List<FamilyGroupChat>, current: List<FamilyGroupChat>): List<FamilyGroupChat> {
        val pendingGroups = current.filter { it.id.startsWith(LOCAL_GROUP) }
        val pendingMessages = current.associate { g -> g.id to g.messages.filter { it.id.startsWith(LOCAL_MESSAGE) } }
        return pendingGroups + loaded.map { g ->
            val pending = pendingMessages[g.id].orEmpty()
            if (pending.isEmpty()) g else g.copy(messages = g.messages + pending)
        }
    }

    private fun removeMessage(groupId: String, localId: String) {
        _groups.update { list ->
            list.map { g -> if (g.id == groupId) g.copy(messages = g.messages.filter { it.id != localId }) else g }
        }
    }

    private fun nextLocalId(prefix: String): String {
        localCounter += 1
        return "$prefix${System.currentTimeMillis()}_$localCounter"
    }

    private fun now(): String = SimpleDateFormat("hh:mm a", Locale.US).format(Date())

    private fun FamilyGroupDto.toDomain(messages: List<FamilyGroupMessage>) = FamilyGroupChat(
        id = id,
        title = title,
        memberChildIds = memberChildIds,
        createdById = createdById,
        avatarBgColor = avatarBgColor,
        messages = messages
    )

    private fun FamilyGroupMessageDto.toDomain() = FamilyGroupMessage(
        id = id,
        senderId = senderId,
        senderName = senderName,
        text = text,
        time = time,
        attachment = attachment?.let {
            MessageAttachment(uri = RemoteSync.absoluteUrl(it.uri), name = it.name, mimeType = it.mimeType)
        }
    )

    private companion object {
        const val LOCAL_GROUP = "fgroup_local_"
        const val LOCAL_MESSAGE = "fgm_local_"
    }
}
