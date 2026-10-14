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
    role: str = "FAMILIAR"  # EDUCADORA | FAMILIAR | DIRECCION


class RegisterRequest(BaseModel):
    email: str
    password: str
    role: str = "FAMILIAR"
    displayName: str
    phone: str | None = None
    codigoProfesor: str | None = None


class UserProfileOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: str
    email: str
    role: str
    displayName: str = Field(validation_alias="display_name")
    phone: str | None = None
    avatarUrl: str | None = Field(default=None, validation_alias="avatar_url")


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


class ChildFullProfileOut(BaseModel):
    childId: str
    birthDate: str
    enrollmentDate: str
    studentId: str
    bloodType: str
    pediatrician: str
    pediatricianPhone: str
    emergencyPhone: str
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


class StaffGlobalChatOut(BaseModel):
    id: str
    title: str
    membersCount: int
    messages: list[WorkerChatMessageOut] = []


class SendGlobalMessageRequest(BaseModel):
    message: str


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
    message: str
    isOutgoing: bool
    fileUri: str | None = None


class SendBitacoraMessageRequest(BaseModel):
    message: str
    fileUri: str | None = None
    fileName: str | None = None
    fileMimeType: str | None = None


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
    response: str
    responderName: str


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
