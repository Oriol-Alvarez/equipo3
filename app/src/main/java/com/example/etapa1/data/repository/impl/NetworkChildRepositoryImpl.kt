package com.example.etapa1.data.repository.impl

import com.example.etapa1.data.remote.BitacoraDiaDto
import com.example.etapa1.data.remote.ChildDto
import com.example.etapa1.data.remote.ChildFullProfileDto
import com.example.etapa1.data.remote.ChildProfileUpdateDto
import com.example.etapa1.data.remote.RemoteSync
import com.example.etapa1.data.remote.RemoteSync.bodyOrLog
import com.example.etapa1.data.remote.SonrisasApi
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.remote.WeeklySummaryDto
import com.example.etapa1.data.remote.WeeklySummaryFeedbackDto
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.model.ActivityCategory
import com.example.etapa1.model.Child
import com.example.etapa1.model.ChildFullProfile
import com.example.etapa1.model.DailyActivityInfo
import com.example.etapa1.model.DailyBitacora
import com.example.etapa1.model.DailyEgresoInfo
import com.example.etapa1.model.DailyIngresoInfo
import com.example.etapa1.model.PortionOption
import com.example.etapa1.model.WeeklySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Niños, expedientes, resúmenes semanales e historial diario desde el servidor.
 *
 * Las lecturas de detalle son inmediatas y devuelven lo que ya se cargó; si aún no hay
 * datos, piden la carga y regresan vacío (nunca datos de ejemplo). Cuando llega la
 * respuesta aumenta [cambios] para que las pantallas vuelvan a leer.
 */
class NetworkChildRepositoryImpl(
    private val api: SonrisasApi,
    private val tokenManager: TokenManager? = null
) : ChildRepository {

    private val _salaActual = MutableStateFlow<String?>(null)
    override val salaActual: StateFlow<String?> = _salaActual.asStateFlow()

    private val _childrenOfRoom = MutableStateFlow<List<Child>>(emptyList())
    override val childrenOfRoom: StateFlow<List<Child>> = _childrenOfRoom.asStateFlow()

    private val _parentChildren = MutableStateFlow<List<Child>>(emptyList())
    override val parentChildren: StateFlow<List<Child>> = _parentChildren.asStateFlow()

    private val _cambios = MutableStateFlow(0)
    override val cambios: StateFlow<Int> = _cambios.asStateFlow()

    private val perfiles = ConcurrentHashMap<String, ChildFullProfile>()
    private val resumenes = ConcurrentHashMap<String, List<WeeklySummary>>()
    private val historiales = ConcurrentHashMap<String, List<DailyBitacora>>()
    private val cargando = ConcurrentHashMap.newKeySet<String>()

    private fun avisarCambio() = _cambios.update { it + 1 }

    private val esFamilia: Boolean get() = tokenManager?.userRole?.uppercase() == "FAMILIAR"

    override fun refresh() {
        if (tokenManager?.userRole == null) {
            // Sin datos de la sesión se intenta todo; el servidor rechaza lo que no aplica
            cargarHijos()
            _salaActual.value?.let(::cargarSala)
            return
        }
        if (esFamilia) cargarHijos() else _salaActual.value?.let(::cargarSala)
    }

    override fun limpiar() {
        _salaActual.value = null
        _childrenOfRoom.value = emptyList()
        _parentChildren.value = emptyList()
        perfiles.clear()
        resumenes.clear()
        historiales.clear()
        cargando.clear()
        avisarCambio()
    }

    override fun abrirSala(roomId: String) {
        if (_salaActual.value != roomId) {
            _salaActual.value = roomId
            _childrenOfRoom.value = emptyList()
        }
        cargarSala(roomId)
    }

    private fun cargarSala(roomId: String) {
        RemoteSync.launch("Cargar niños de la sala $roomId") {
            val lista = api.getChildrenByRoom(roomId).bodyOrLog("GET /rooms/$roomId/children") ?: return@launch
            if (_salaActual.value == roomId) _childrenOfRoom.value = lista.map { it.toDomain() }
        }
    }

    private fun cargarHijos() {
        RemoteSync.launch("Cargar hijos de la familia") {
            val lista = api.getMyChildren().bodyOrLog("GET /parents/me/children") ?: return@launch
            _parentChildren.value = lista.map { it.toDomain() }
        }
    }

    override fun getChildById(childId: String): Child? =
        _childrenOfRoom.value.find { it.id == childId } ?: _parentChildren.value.find { it.id == childId }

    override fun updateChildAttendance(childId: String, isPresent: Boolean, time: String, statusText: String) {
        val updater: (List<Child>) -> List<Child> = { lista ->
            lista.map { child ->
                if (child.id == childId) {
                    child.copy(
                        isPresent = isPresent,
                        arrivalTime = if (isPresent) time else child.arrivalTime,
                        statusText = statusText
                    )
                } else {
                    child
                }
            }
        }
        _childrenOfRoom.update(updater)
        _parentChildren.update(updater)
    }

    // ------------------------------------------------------------------ detalle del niño

    override fun cargarDetalle(childId: String) {
        if (childId.isBlank() || !cargando.add(childId)) return
        RemoteSync.launch("Cargar ficha de $childId") {
            try {
                api.getChildFullProfile(childId).bodyOrLog("GET /children/$childId/full-profile")?.let { dto ->
                    val child = getChildById(childId)
                        ?: api.getChild(childId).bodyOrLog("GET /children/$childId")?.toDomain()
                    if (child != null) perfiles[childId] = dto.toDomain(child)
                }
                api.getWeeklySummaries(childId).bodyOrLog("GET /children/$childId/weekly-summaries")?.let { dtos ->
                    resumenes[childId] = dtos.map { it.toDomain() }
                }
                api.getBitacoras(childId).bodyOrLog("GET /children/$childId/bitacoras")?.let { dtos ->
                    historiales[childId] = dtos.map { it.toDomain() }
                }
            } finally {
                cargando.remove(childId)
                avisarCambio()
            }
        }
    }

    override fun getChildFullProfile(child: Child): ChildFullProfile {
        perfiles[child.id]?.let { return it.copy(child = child) }
        cargarDetalle(child.id)
        return expedienteVacio(child)
    }

    override fun updateChildFullProfile(childId: String, profile: ChildFullProfile) {
        perfiles[childId] = profile
        avisarCambio()
        RemoteSync.launch("Guardar expediente de $childId") {
            val guardado = api.updateChildFullProfile(
                childId,
                ChildProfileUpdateDto(
                    pediatrician = profile.pediatrician,
                    pediatricianPhone = profile.pediatricianPhone,
                    medicalNotes = profile.medicalNotes,
                    habitsAndPedagogicalNotes = profile.generalNotes,
                    emergencyPhone = profile.emergencyPhone,
                    emergencyPhoneRelation = profile.emergencyPhoneRelation,
                    authorizedPickups = profile.authorizedPickups,
                    medicalCertificateUri = profile.medicalCertificateUri,
                    medicalCertificateName = profile.medicalCertificateName,
                    medicalCertificateDate = profile.medicalCertificateDate
                )
            ).bodyOrLog("PUT /children/$childId/full-profile") ?: return@launch
            perfiles[childId] = guardado.toDomain(profile.child)
            avisarCambio()
        }
    }

    override fun getWeeklySummariesForChild(child: Child): List<WeeklySummary> {
        resumenes[child.id]?.let { return it }
        cargarDetalle(child.id)
        return emptyList()
    }

    override fun getWeeklySummaryForChild(child: Child): WeeklySummary =
        getWeeklySummariesForChild(child).firstOrNull() ?: resumenCargando()

    override fun markWeeklySummaryReadStatus(
        childId: String,
        weekRangeText: String,
        isRead: Boolean,
        timestampText: String?
    ) {
        val semana = actualizarResumen(childId, weekRangeText) {
            it.copy(isReadByParent = isRead, readTimestampText = if (isRead) timestampText else null)
        } ?: return
        enviarRespuesta(childId, semana, WeeklySummaryFeedbackDto(isRead = isRead))
    }

    override fun setWeeklySummaryReaction(childId: String, weekRangeText: String, reaction: String?) {
        val limpia = reaction?.takeIf { it.isNotBlank() }
        val semana = actualizarResumen(childId, weekRangeText) { it.copy(parentReaction = limpia) } ?: return
        enviarRespuesta(childId, semana, WeeklySummaryFeedbackDto(reaction = limpia ?: ""))
    }

    /** Cambia el resumen en memoria y devuelve su id (lunes) para avisar al servidor. */
    private fun actualizarResumen(childId: String, weekRangeText: String, cambio: (WeeklySummary) -> WeeklySummary): String? {
        val lista = resumenes[childId] ?: return null
        val semana = lista.firstOrNull { it.weekRangeText == weekRangeText } ?: return null
        resumenes[childId] = lista.map { if (it.weekRangeText == weekRangeText) cambio(it) else it }
        avisarCambio()
        return semana.id.takeIf { it.isNotEmpty() }
    }

    private fun enviarRespuesta(childId: String, semana: String, cuerpo: WeeklySummaryFeedbackDto) {
        RemoteSync.launch("Responder resumen semanal $semana de $childId") {
            val dto = api.sendWeeklySummaryFeedback(childId, semana, cuerpo)
                .bodyOrLog("PUT /children/$childId/weekly-summaries/$semana/feedback") ?: return@launch
            val confirmado = dto.toDomain()
            resumenes[childId]?.let { lista ->
                resumenes[childId] = lista.map { if (it.id == confirmado.id) confirmado else it }
                avisarCambio()
            }
        }
    }

    override fun getDailyBitacorasForChild(child: Child): List<DailyBitacora> {
        historiales[child.id]?.let { return it }
        cargarDetalle(child.id)
        return emptyList()
    }

    // ------------------------------------------------------------------ conversión

    private fun ChildDto.toDomain(): Child = Child(
        id = id,
        fullName = fullName,
        shortName = shortName,
        ageText = ageText,
        roomText = roomText,
        groupText = groupText,
        statusText = statusText,
        isPresent = isPresent,
        arrivalTime = arrivalTime,
        allergyAlert = allergyAlert,
        avatarInitials = avatarInitials,
        avatarBgColor = avatarBgColor,
        birthDate = birthDate,
        roomId = roomId,
        educatorName = educatorName,
        educatorRole = educatorRole,
        educatorAvatarInitials = educatorAvatarInitials,
        educatorAvatarBgColor = educatorAvatarBgColor
    )

    private fun ChildFullProfileDto.toDomain(child: Child) = ChildFullProfile(
        child = child,
        birthDate = birthDate,
        enrollmentDate = enrollmentDate,
        studentId = studentId,
        bloodType = bloodType,
        pediatrician = pediatrician,
        pediatricianPhone = pediatricianPhone,
        emergencyPhone = emergencyPhone,
        emergencyPhoneRelation = emergencyPhoneRelation,
        motherName = motherName,
        motherPhone = motherPhone,
        fatherName = fatherName,
        fatherPhone = fatherPhone,
        authorizedPickups = authorizedPickups,
        medicalNotes = medicalNotes,
        generalNotes = generalNotes,
        medicalCertificateUri = medicalCertificateUri?.let(RemoteSync::absoluteUrl),
        medicalCertificateName = medicalCertificateName,
        medicalCertificateDate = medicalCertificateDate
    )

    private fun WeeklySummaryDto.toDomain() = WeeklySummary(
        id = id,
        weekLabel = weekLabel,
        weekRangeText = weekRangeText,
        attendanceDaysCount = attendanceDaysCount,
        totalSchoolDays = totalSchoolDays,
        attendancePercentageText = attendancePercentageText,
        averageArrivalTime = averageArrivalTime,
        foodIntakePercentage = foodIntakePercentage,
        totalNapHours = totalNapHours,
        averageNapDaily = averageNapDaily,
        moodSummary = moodSummary,
        executiveSummary = executiveSummary,
        foodHighlights = foodHighlights,
        napHighlights = napHighlights,
        pedagogicalHighlights = pedagogicalHighlights,
        healthAndSafetySummary = healthAndSafetySummary,
        isReadByParent = isReadByParent,
        readTimestampText = readTimestampText,
        parentReaction = parentReaction
    )

    private fun BitacoraDiaDto.toDomain() = DailyBitacora(
        date = date,
        dayLabel = dayLabel,
        isToday = isToday,
        ingreso = ingreso?.let {
            DailyIngresoInfo(
                time = it.time,
                delivererName = it.delivererName,
                receiverName = it.receiverName,
                physicalCondition = it.physicalCondition,
                hasSymptoms = it.hasSymptoms,
                symptomsDetail = it.symptomsDetail,
                observations = it.observations
            )
        },
        egreso = egreso?.let {
            DailyEgresoInfo(
                time = it.time,
                collectorName = it.collectorName,
                delivererName = it.delivererName,
                physicalCondition = it.physicalCondition,
                observations = it.observations
            )
        },
        activities = activities.map { a ->
            DailyActivityInfo(
                id = a.id,
                time = a.time,
                title = a.title,
                category = ActivityCategory.entries.firstOrNull { it.name == a.category } ?: ActivityCategory.OTROS,
                portion = a.portion?.let { p -> PortionOption.entries.firstOrNull { it.name == p } },
                description = a.description
            )
        }
    )

    companion object {
        /** Expediente sin datos (alumno nuevo o mientras se carga); no usa los valores de ejemplo. */
        fun expedienteVacio(child: Child) = ChildFullProfile(
            child = child,
            birthDate = "",
            enrollmentDate = "",
            studentId = "",
            bloodType = "",
            pediatrician = "",
            pediatricianPhone = "",
            emergencyPhone = "",
            emergencyPhoneRelation = "",
            motherName = "",
            motherPhone = "",
            fatherName = "",
            fatherPhone = "",
            authorizedPickups = emptyList(),
            medicalNotes = "",
            generalNotes = ""
        )

        fun resumenCargando() = WeeklySummary(
            id = "",
            weekLabel = "Semana actual",
            weekRangeText = "",
            attendanceDaysCount = 0,
            totalSchoolDays = 0,
            attendancePercentageText = "—",
            averageArrivalTime = "—",
            foodIntakePercentage = "—",
            totalNapHours = "—",
            averageNapDaily = "—",
            moodSummary = "—",
            executiveSummary = "Cargando el resumen de la semana…",
            foodHighlights = emptyList(),
            napHighlights = "",
            pedagogicalHighlights = emptyList(),
            healthAndSafetySummary = ""
        )
    }
}
