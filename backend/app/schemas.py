"""Esquemas Pydantic para validación y serialización de peticiones y respuestas.
Cumple estrictamente con las estructuras de API_ENTRYPOINTS.md.
"""

from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, Field


# ---------------------------------------------------------------------------
# Módulo 1: Autenticación y Perfil
# ---------------------------------------------------------------------------

class LoginRequest(BaseModel):
    email: str
    password: str
    role: str | None = None  # EDUCADORA | FAMILIAR | DIRECCION


class RegisterRequest(BaseModel):
    email: str
    password: str
    role: str = "FAMILIAR"
    displayName: str
    phone: str | None = None
    codigoProfesor: str | None = None


class AdminChildSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: str
    fullName: str
    shortName: str = ""
    ageText: str = ""
    roomText: str = ""
    groupText: str = ""
    isPresent: bool = False
    statusText: str = ""
    arrivalTime: str | None = None
    allergyAlert: str | None = None
    avatarInitials: str = "MG"
    avatarBgColor: int = 4294948685
    birthDate: str | None = None
    enrollmentDate: str | None = None
    studentId: str | None = None
    bloodType: str | None = None
    pediatrician: str | None = None
    pediatricianPhone: str | None = None
    emergencyPhone: str | None = None
    emergencyPhoneRelation: str | None = None
    motherName: str | None = None
    motherPhone: str | None = None
    fatherName: str | None = None
    fatherPhone: str | None = None
    authorizedPickups: list[str] = Field(default_factory=list)
    medicalNotes: str | None = None
    generalNotes: str | None = None
    habitsNotes: str | None = None


class UserProfileOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: str
    email: str
    role: str
    displayName: str = Field(validation_alias="display_name")
    phone: str | None = None
    avatarUrl: str | None = Field(default=None, validation_alias="avatar_url")
    children: list[AdminChildSummary] = Field(default_factory=list)


class LoginResponse(BaseModel):
    token: str
    refreshToken: str
    expiresIn: int
    user: UserProfileOut


class RefreshRequest(BaseModel):
    refreshToken: str


class RefreshResponse(BaseModel):
    token: str
    expiresIn: int


# ---------------------------------------------------------------------------
# Módulo 2: Salas Escolares
# ---------------------------------------------------------------------------

class RoomSummaryOut(BaseModel):
    id: str
    name: str
    level: str
    presentCount: int
    totalCount: int


class EducatorItem(BaseModel):
    id: str
    name: str
    role: str


class RoomDetailOut(BaseModel):
    id: str
    name: str
    level: str
    capacity: int
    presentCount: int
    totalCount: int
    educators: list[EducatorItem] = []


# ---------------------------------------------------------------------------
# Módulo 3: Alumnos y Expedientes Médicos
# ---------------------------------------------------------------------------

class ChildOut(BaseModel):
    id: str
    fullName: str
    shortName: str
    ageText: str
    roomText: str
    groupText: str
    statusText: str
    isPresent: bool
    arrivalTime: str | None = None
    allergyAlert: str | None = None
    avatarInitials: str = "MG"
    avatarBgColor: int = 4294948685
    educatorName: str | None = None
    educatorRole: str | None = None
    educatorAvatarInitials: str | None = None
    educatorAvatarBgColor: int | None = None


class EducatorOut(BaseModel):
    id: str
    name: str
    role: str
    avatarInitials: str = "LG"
    avatarBgColor: int = 4278364369
    roomId: str = ""
    roomName: str = ""


class ChildFullProfileOut(BaseModel):
    childId: str
    birthDate: str
    enrollmentDate: str
    studentId: str
    bloodType: str
    pediatrician: str
    pediatricianPhone: str
    emergencyPhone: str
    emergencyPhoneRelation: str = "Tutor legal"
    motherName: str
    motherPhone: str
    fatherName: str
    fatherPhone: str
    authorizedPickups: list[str] = []
    medicalNotes: str
    generalNotes: str
    medicalCertificateUri: str | None = None
    medicalCertificateName: str | None = None
    medicalCertificateDate: str | None = None


class ChildProfileUpdate(BaseModel):
    pediatrician: str | None = None
    pediatricianPhone: str | None = None
    medicalNotes: str | None = None
    habitsAndPedagogicalNotes: str | None = None
    emergencyPhone: str | None = None
    emergencyPhoneRelation: str | None = None
    authorizedPickups: list[str] | None = None
    medicalCertificateUri: str | None = None
    medicalCertificateName: str | None = None
    medicalCertificateDate: str | None = None


class MedicalCertificateOut(BaseModel):
    certificateUri: str
    certificateName: str
    certificateDate: str


# ---------------------------------------------------------------------------
# Módulo 4: Asistencia (Ingreso y Egreso)
# ---------------------------------------------------------------------------

class IngresoRequest(BaseModel):
    childId: str
    date: str
    dayOfWeek: str = "L"
    entryTime: str
    assistantName: str | None = None
    isChildSick: bool = False
    medicationOption: str = "NO_APLICA"
    hasSymptoms: bool = False
    symptomsDetail: str = ""
    isClean: bool = True
    backpackComplete: bool = True
    goodPhysicalCondition: bool = True
    injuryDetail: str = ""
    observations: str = ""
    delivererName: str | None = None
    delivererSignatureData: str | None = None
    receiverStaffName: str | None = None
    receiverStaffSignatureData: str | None = None


class EgresoRequest(BaseModel):
    childId: str
    date: str
    exitTime: str
    goodPhysicalCondition: bool = True
    injuryDetail: str = ""
    isClean: bool = True
    backpackComplete: bool = True
    observations: str = ""
    receiverTutorName: str | None = None
    receiverTutorSignatureData: str | None = None
    delivererStaffName: str | None = None
    delivererStaffSignatureData: str | None = None


class AttendanceResponse(BaseModel):
    id: str
    childId: str
    isIngreso: bool
    status: str = "REGISTRADO"
    recordedAt: str


class AttendanceRecordOut(BaseModel):
    id: str
    childId: str
    isIngreso: bool
    date: str
    dayOfWeek: str
    time: str
    observations: str = ""


# ---------------------------------------------------------------------------
# Módulo 5: Bitácora Diaria, Línea de Tiempo y Actividades
# ---------------------------------------------------------------------------

class TimelineItemOut(BaseModel):
    type: Literal["EVENT_CHIP", "CHAT_MESSAGE", "ACTIVITY_CARD"]
    id: str
    time: str
    date: str | None = None
    # Campos para EVENT_CHIP
    text: str | None = None
    iconType: str | None = None
    # Campos para CHAT_MESSAGE
    message: str | None = None
    isOutgoing: bool | None = None
    fileUri: str | None = None
    fileName: str | None = None
    fileMimeType: str | None = None
    # Campos para ACTIVITY_CARD
    category: str | None = None
    portionLabel: str | None = None
    description: str | None = None


class NewActivityRequest(BaseModel):
    time: str
    category: str
    mealType: str | None = None
    consumptionOption: str | None = None
    portionOption: str | None = None
    title: str
    description: str
    napMinutes: int | None = None
    moodState: str | None = None
    excretoryFunction: str | None = None
    date: str | None = None


class NewActivityResponse(BaseModel):
    id: str
    status: str = "CREADA"
    timelineItem: TimelineItemOut


class QuickEventRequest(BaseModel):
    time: str
    text: str
    iconType: str  # entry, mood, nap, etc.


class BitacoraDaySummary(BaseModel):
    date: str
    dayLabel: str
    isToday: bool
    ingreso: dict[str, Any] | None = None
    egreso: dict[str, Any] | None = None
    activities: list[dict[str, Any]] = []


# ---------------------------------------------------------------------------
# Módulo 6: Resúmenes Semanales Pedagógicos
# ---------------------------------------------------------------------------

class WeeklySummaryOut(BaseModel):
    id: str
    weekLabel: str
    weekRangeText: str
    attendanceDaysCount: int
    totalSchoolDays: int
    attendancePercentageText: str
    averageArrivalTime: str
    foodIntakePercentage: str
    totalNapHours: str
    averageNapDaily: str
    moodSummary: str
    executiveSummary: str
    foodHighlights: list[str] = []
    napHighlights: str
    pedagogicalHighlights: list[str] = []
    healthAndSafetySummary: str


# ---------------------------------------------------------------------------
# Módulo 7: Mensajería de Personal y Chat Global
# ---------------------------------------------------------------------------

class StaffMemberOut(BaseModel):
    id: str
    name: str
    role: str
    avatarInitials: str
    avatarBgColor: int
    isOnline: bool


class WorkerChatOut(BaseModel):
    id: str
    title: str
    subtitle: str
    lastMessage: str | None = None
    lastMessageTime: str | None = None
    unreadCount: int = 0
    isGlobal: bool = False
    isGroup: bool = False
    avatarInitials: str
    avatarBgColor: int


class WorkerChatMessageOut(BaseModel):
    id: str
    senderId: str
    senderName: str
    message: str
    time: str
    isOutgoing: bool


class SendWorkerMessageRequest(BaseModel):
    message: str
    fileUri: str | None = None
    fileName: str | None = None
    fileMimeType: str | None = None
    time: str | None = None


class StaffGlobalChatOut(BaseModel):
    id: str
    title: str
    membersCount: int
    messages: list[WorkerChatMessageOut] = []


class SendGlobalMessageRequest(BaseModel):
    message: str
    time: str | None = None


# ---------------------------------------------------------------------------
# Módulo 8: Chat Bitácora Individual (Educadora <-> Familia)
# ---------------------------------------------------------------------------

class ParentChatSummaryOut(BaseModel):
    childId: str
    childName: str
    parentName: str
    lastMessage: str | None = None
    lastMessageTime: str | None = None
    unreadCount: int = 0


class BitacoraMessageOut(BaseModel):
    id: str
    senderId: str
    senderName: str
    time: str
    date: str | None = None
    message: str
    isOutgoing: bool
    fileUri: str | None = None


class SendBitacoraMessageRequest(BaseModel):
    message: str
    fileUri: str | None = None
    fileName: str | None = None
    fileMimeType: str | None = None
    time: str | None = None
    date: str | None = None


# ---------------------------------------------------------------------------
# Módulo 9: Grupos Familiares
# ---------------------------------------------------------------------------

class CandidateOut(BaseModel):
    childId: str
    childName: str
    parentName: str
    avatarInitials: str
    avatarBgColor: int


class FamilyGroupOut(BaseModel):
    id: str
    title: str
    memberChildIds: list[str] = []
    createdById: str
    avatarBgColor: int
    lastMessage: str | None = None
    lastMessageTime: str | None = None


class CreateFamilyGroupRequest(BaseModel):
    title: str
    memberChildIds: list[str]


class AttachmentData(BaseModel):
    uri: str
    name: str
    mimeType: str


class FamilyGroupMessageOut(BaseModel):
    id: str
    senderId: str
    senderName: str
    text: str
    time: str
    attachment: AttachmentData | None = None


class SendFamilyGroupMessageRequest(BaseModel):
    text: str
    attachment: AttachmentData | None = None
    time: str | None = None


# ---------------------------------------------------------------------------
# Módulo 10: Avisos y Circulares Escolares
# ---------------------------------------------------------------------------

class AnnouncementOut(BaseModel):
    id: str
    title: str
    date: str
    summary: str
    fullContent: str
    isUnread: bool
    author: str


class UnreadCountOut(BaseModel):
    unreadCount: int


class ReadAnnouncementOut(BaseModel):
    id: str
    isUnread: bool


class CreateAnnouncementRequest(BaseModel):
    title: str
    summary: str
    fullContent: str
    author: str = "Dirección General"


# ---------------------------------------------------------------------------
# Módulo 11: Sugerencias y Quejas
# ---------------------------------------------------------------------------

class SuggestionOut(BaseModel):
    id: str
    parentName: str
    childName: str
    roomName: str
    date: str
    time: str
    category: str
    subject: str
    content: str
    status: str
    response: str | None = None
    responseDate: str | None = None
    responderName: str | None = None


class CreateSuggestionRequest(BaseModel):
    childId: str
    roomName: str
    category: str
    subject: str
    content: str


class UpdateSuggestionStatusRequest(BaseModel):
    status: str
    response: str | None = None
    responderName: str | None = None


# ---------------------------------------------------------------------------
# Módulo 12: Objetos Perdidos
# ---------------------------------------------------------------------------

class LostObjectOut(BaseModel):
    id: str
    title: str
    description: str
    category: str
    location: str | None = None
    imageUri: str | None = None
    room: str = "Sala 1A"
    isClaimed: bool = False


class CreateLostObjectRequest(BaseModel):
    title: str
    description: str
    category: str
    location: str | None = None
    room: str = "Sala 1A"
    imageUri: str | None = None


class ClaimLostObjectOut(BaseModel):
    id: str
    isClaimed: bool


# ---------------------------------------------------------------------------
# Módulo 13: Multimedia
# ---------------------------------------------------------------------------

class MediaUploadOut(BaseModel):
    fileId: str
    url: str
    fileName: str
    mimeType: str
    sizeBytes: int


# ---------------------------------------------------------------------------
# Módulo 14: Notificaciones Push
# ---------------------------------------------------------------------------

class DeviceTokenRequest(BaseModel):
    deviceToken: str
    platform: str = "ANDROID"


# ---------------------------------------------------------------------------
# Observaciones semanales (/weekly-observations)
# ---------------------------------------------------------------------------

WEEKLY_OBSERVATION_MAX_LENGTH = 500


class WeeklyObservationOut(BaseModel):
    id: str
    childId: str
    weekKey: str
    authorRole: Literal["FAMILIA", "EDUCADORA"]
    authorName: str
    text: str
    time: str


class CreateWeeklyObservationRequest(BaseModel):
    weekKey: str = Field(min_length=1, max_length=120)
    text: str


# ---------------------------------------------------------------------------
# Planeación y menú diario (/daily-plans)
# ---------------------------------------------------------------------------

class DailyMenuData(BaseModel):
    breakfast: str = Field(min_length=1, max_length=300)
    snack: str = Field(min_length=1, max_length=300)
    lunch: str = Field(min_length=1, max_length=300)
    notes: str = Field(default="", max_length=500)


class PlannedActivityData(BaseModel):
    time: str = Field(pattern=r"^([01]\d|2[0-3]):[0-5]\d$")
    title: str = Field(min_length=1, max_length=200)


class DailyPlanOut(BaseModel):
    date: str
    roomId: str
    menu: DailyMenuData
    activities: list[PlannedActivityData]
    updatedBy: str


class SaveDailyPlanRequest(BaseModel):
    menu: DailyMenuData
    activities: list[PlannedActivityData] = Field(min_length=1, max_length=20)
    roomId: str = "sala_1a"


# ---------------------------------------------------------------------------
# Registro diario / hoja oficial "Bitácora de filtro diario" (/daily-logs)
# Los nombres de los campos son los mismos que en la app (model/DailyLog.kt).
# ---------------------------------------------------------------------------

HORA_24H = r"^([01]\d|2[0-3]):[0-5]\d$"


class IngresoLogData(BaseModel):
    model_config = ConfigDict(extra="forbid")

    time: str = Field(pattern=HORA_24H)
    assistantName: str = ""
    deliveredBy: str = ""
    receivedBy: str = ""
    isSick: bool | None = None
    medicationWithPrescription: str | None = None
    hasSymptoms: bool | None = None
    symptoms: str = ""
    arrivedClean: bool | None = None
    backpackComplete: bool | None = None
    goodPhysicalCondition: bool | None = None
    injuries: str = ""
    observations: str = ""


class EstadiaLogData(BaseModel):
    model_config = ConfigDict(extra="forbid")

    breakfast: str | None = None
    snack: str | None = None
    lunch: str | None = None
    foodNotes: str = ""
    slept: bool | None = None
    napMinutes: int | None = Field(default=None, ge=0, le=600)
    napComments: str = ""
    sphincterControl: str | None = None
    sphincterPeeCount: int | None = Field(default=None, ge=0, le=50)
    sphincterPoopCount: int | None = Field(default=None, ge=0, le=50)
    peeCount: int | None = Field(default=None, ge=0, le=50)
    poopCount: int | None = Field(default=None, ge=0, le=50)
    excretionNotes: str = ""
    mood: str | None = None
    cried: bool | None = None
    fought: bool | None = None
    participated: bool | None = None
    moodNotes: str = ""
    hadAccident: bool | None = None
    accidentDescription: str = ""
    accidentFolio: str = ""
    hadHealthIssue: bool | None = None
    healthIssue: str = ""
    careProvided: str = ""
    generalNotes: str = ""


class EgresoLogData(BaseModel):
    model_config = ConfigDict(extra="forbid")

    time: str = Field(pattern=HORA_24H)
    goodPhysicalCondition: bool | None = None
    injuries: str = ""
    collectedBy: str = ""
    deliveredClean: bool | None = None
    belongingsComplete: bool | None = None
    observations: str = ""
    deliveredByStaff: str = ""


class DailyLogOut(BaseModel):
    childId: str
    date: str
    ingreso: IngresoLogData | None = None
    estadia: EstadiaLogData | None = None
    egreso: EgresoLogData | None = None


# ---------------------------------------------------------------------------
# Módulo 14: Panel de Administración, Auditoría y Estadísticas (/admin)
# ---------------------------------------------------------------------------

class AdminUserCreate(BaseModel):
    email: str
    password: str
    role: str = "EDUCADORA"
    displayName: str
    phone: str | None = None


class AdminUserUpdate(BaseModel):
    displayName: str | None = None
    email: str | None = None
    phone: str | None = None
    role: str | None = None
    password: str | None = None


class UpdateAnnouncementRequest(BaseModel):
    title: str | None = None
    summary: str | None = None
    fullContent: str | None = None
    author: str | None = None


class UpdateLostObjectRequest(BaseModel):
    title: str | None = None
    description: str | None = None
    category: str | None = None
    location: str | None = None
    room: str | None = None
    isClaimed: bool | None = None


class AdminLogItem(BaseModel):
    id: str
    timestamp: str
    category: str
    title: str
    description: str
    actorName: str
    actorRole: str | None = None
    childName: str | None = None
    roomName: str | None = None


class AdminStatsOverview(BaseModel):
    totalChildren: int
    presentCount: int
    absentCount: int
    attendanceRate: float
    totalEducators: int
    totalFamilies: int
    childrenFedCount: int
    breakfastCount: int
    lunchCount: int
    snackCount: int
    childrenNappedCount: int
    totalNapMinutes: int
    averageNapMinutes: int
    ingresosCount: int
    egresosCount: int
    pendingPickupsCount: int
    diaperChangesCount: int
    incidentsCount: int
    totalSuggestions: int
    pendingSuggestions: int
    attendedSuggestions: int
    totalLostObjects: int
    claimedLostObjects: int
    unclaimedLostObjects: int
    totalAnnouncements: int


class AdminCreateChildRequest(BaseModel):
    fullName: str
    shortName: str | None = None
    ageText: str | None = "2 años"
    roomText: str | None = "Sala 1A"
    groupText: str | None = "Maternal A"
    allergyAlert: str | None = None
    birthDate: str | None = None
    studentId: str | None = None
    bloodType: str | None = None
    pediatrician: str | None = None
    pediatricianPhone: str | None = None
    emergencyPhone: str | None = None
    emergencyPhoneRelation: str | None = None
    motherName: str | None = None
    motherPhone: str | None = None
    fatherName: str | None = None
    fatherPhone: str | None = None
    medicalNotes: str | None = None
    generalNotes: str | None = None
    habitsNotes: str | None = None
    authorizedPickups: list[str] = Field(default_factory=list)


class AdminUpdateChildRequest(BaseModel):
    fullName: str | None = None
    shortName: str | None = None
    ageText: str | None = None
    roomText: str | None = None
    groupText: str | None = None
    statusText: str | None = None
    isPresent: bool | None = None
    allergyAlert: str | None = None
    birthDate: str | None = None
    enrollmentDate: str | None = None
    studentId: str | None = None
    bloodType: str | None = None
    pediatrician: str | None = None
    pediatricianPhone: str | None = None
    emergencyPhone: str | None = None
    emergencyPhoneRelation: str | None = None
    motherName: str | None = None
    motherPhone: str | None = None
    fatherName: str | None = None
    fatherPhone: str | None = None
    authorizedPickups: list[str] | None = None
    medicalNotes: str | None = None
    generalNotes: str | None = None
    habitsNotes: str | None = None

