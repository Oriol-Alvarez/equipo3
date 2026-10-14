"""Modelos de datos SQLAlchemy 2.0 para Sonrisas.
Representa todas las entidades requeridas en API_ENTRYPOINTS.md:
usuarios, sesiones, salas, alumnos, asistencia, bitácoras, chats, grupos y objetos perdidos.
"""

from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .db import Base


class Usuario(Base):
    __tablename__ = "usuarios"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    email: Mapped[str] = mapped_column(String(120), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(Text)
    role: Mapped[str] = mapped_column(String(20))  # "EDUCADORA", "FAMILIAR", "DIRECCION"
    display_name: Mapped[str] = mapped_column(String(120))
    phone: Mapped[str | None] = mapped_column(String(30), nullable=True)
    avatar_url: Mapped[str | None] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class Sesion(Base):
    __tablename__ = "sesiones"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    usuario_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"), index=True)
    refresh_hash: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    expires_at: Mapped[int] = mapped_column(BigInteger)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class Room(Base):
    __tablename__ = "rooms"

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    name: Mapped[str] = mapped_column(String(64))
    level: Mapped[str] = mapped_column(String(64))
    capacity: Mapped[int] = mapped_column(Integer, default=15)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class RoomEducator(Base):
    __tablename__ = "room_educators"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    room_id: Mapped[str] = mapped_column(ForeignKey("rooms.id", ondelete="CASCADE"), index=True)
    educator_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    role_title: Mapped[str] = mapped_column(String(64), default="Educadora Titular")


class Child(Base):
    __tablename__ = "children"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    full_name: Mapped[str] = mapped_column(String(120))
    short_name: Mapped[str] = mapped_column(String(60))
    age_text: Mapped[str] = mapped_column(String(40))
    room_id: Mapped[str] = mapped_column(ForeignKey("rooms.id"), index=True)
    group_text: Mapped[str] = mapped_column(String(60))
    status_text: Mapped[str] = mapped_column(String(60), default="No ha ingresado")
    is_present: Mapped[bool] = mapped_column(Boolean, default=False)
    arrival_time: Mapped[str | None] = mapped_column(String(20), nullable=True)
    allergy_alert: Mapped[str | None] = mapped_column(String(120), nullable=True)
    avatar_initials: Mapped[str] = mapped_column(String(10), default="MG")
    avatar_bg_color: Mapped[int] = mapped_column(BigInteger, default=4294948685)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class ChildParent(Base):
    __tablename__ = "child_parents"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    parent_user_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"), index=True)


class ChildProfile(Base):
    __tablename__ = "child_profiles"

    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), primary_key=True)
    birth_date: Mapped[str] = mapped_column(String(30), default="")
    enrollment_date: Mapped[str] = mapped_column(String(30), default="")
    student_id: Mapped[str] = mapped_column(String(60), default="")
    blood_type: Mapped[str] = mapped_column(String(10), default="")
    pediatrician: Mapped[str] = mapped_column(String(120), default="")
    pediatrician_phone: Mapped[str] = mapped_column(String(30), default="")
    emergency_phone: Mapped[str] = mapped_column(String(30), default="")
    mother_name: Mapped[str] = mapped_column(String(120), default="")
    mother_phone: Mapped[str] = mapped_column(String(30), default="")
    father_name: Mapped[str] = mapped_column(String(120), default="")
    father_phone: Mapped[str] = mapped_column(String(30), default="")
    authorized_pickups: Mapped[str] = mapped_column(Text, default="[]")  # JSON string
    medical_notes: Mapped[str] = mapped_column(Text, default="")
    general_notes: Mapped[str] = mapped_column(Text, default="")
    habits_notes: Mapped[str] = mapped_column(Text, default="")
    medical_certificate_uri: Mapped[str | None] = mapped_column(String(255), nullable=True)
    medical_certificate_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    medical_certificate_date: Mapped[str | None] = mapped_column(String(30), nullable=True)


class AttendanceRecord(Base):
    __tablename__ = "attendance_records"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    is_ingreso: Mapped[bool] = mapped_column(Boolean)  # True = Ingreso, False = Egreso
    date: Mapped[str] = mapped_column(String(20), index=True)  # "12/10/2026"
    day_of_week: Mapped[str] = mapped_column(String(5), default="L")
    time: Mapped[str] = mapped_column(String(20))  # "08:15 AM"
    status: Mapped[str] = mapped_column(String(20), default="REGISTRADO")
    assistant_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    is_child_sick: Mapped[bool] = mapped_column(Boolean, default=False)
    medication_option: Mapped[str] = mapped_column(String(30), default="NO_APLICA")
    has_symptoms: Mapped[bool] = mapped_column(Boolean, default=False)
    symptoms_detail: Mapped[str] = mapped_column(Text, default="")
    is_clean: Mapped[bool] = mapped_column(Boolean, default=True)
    backpack_complete: Mapped[bool] = mapped_column(Boolean, default=True)
    good_physical_condition: Mapped[bool] = mapped_column(Boolean, default=True)
    injury_detail: Mapped[str] = mapped_column(Text, default="")
    observations: Mapped[str] = mapped_column(Text, default="")
    deliverer_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    deliverer_signature_data: Mapped[str | None] = mapped_column(Text, nullable=True)
    receiver_staff_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    receiver_staff_signature_data: Mapped[str | None] = mapped_column(Text, nullable=True)
    receiver_tutor_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    receiver_tutor_signature_data: Mapped[str | None] = mapped_column(Text, nullable=True)
    deliverer_staff_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    deliverer_staff_signature_data: Mapped[str | None] = mapped_column(Text, nullable=True)
    recorded_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class Activity(Base):
    __tablename__ = "activities"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    date: Mapped[str] = mapped_column(String(20), index=True)
    time: Mapped[str] = mapped_column(String(20))
    category: Mapped[str] = mapped_column(String(40))  # ALIMENTACION, DESCANSO, etc.
    meal_type: Mapped[str | None] = mapped_column(String(40), nullable=True)
    consumption_option: Mapped[str | None] = mapped_column(String(40), nullable=True)
    portion_option: Mapped[str | None] = mapped_column(String(40), nullable=True)
    title: Mapped[str] = mapped_column(String(120))
    description: Mapped[str] = mapped_column(Text)
    nap_minutes: Mapped[int | None] = mapped_column(Integer, nullable=True)
    mood_state: Mapped[str | None] = mapped_column(String(40), nullable=True)
    excretory_function: Mapped[str | None] = mapped_column(String(40), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class TimelineEvent(Base):
    __tablename__ = "timeline_events"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    date: Mapped[str] = mapped_column(String(20), index=True)
    time: Mapped[str] = mapped_column(String(20))
    text: Mapped[str] = mapped_column(String(120))
    icon_type: Mapped[str] = mapped_column(String(30))  # entry, mood, nap, etc.
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class WeeklySummary(Base):
    __tablename__ = "weekly_summaries"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    week_label: Mapped[str] = mapped_column(String(60))
    week_range_text: Mapped[str] = mapped_column(String(120))
    attendance_days_count: Mapped[int] = mapped_column(Integer, default=5)
    total_school_days: Mapped[int] = mapped_column(Integer, default=5)
    attendance_percentage_text: Mapped[str] = mapped_column(String(20), default="100%")
    average_arrival_time: Mapped[str] = mapped_column(String(20), default="08:15 AM")
    food_intake_percentage: Mapped[str] = mapped_column(String(20), default="90%")
    total_nap_hours: Mapped[str] = mapped_column(String(20), default="6h 00m")
    average_nap_daily: Mapped[str] = mapped_column(String(20), default="1h 15m/día")
    mood_summary: Mapped[str] = mapped_column(String(120), default="Alegre y participativo")
    executive_summary: Mapped[str] = mapped_column(Text)
    food_highlights: Mapped[str] = mapped_column(Text, default="[]")  # JSON
    nap_highlights: Mapped[str] = mapped_column(Text, default="")
    pedagogical_highlights: Mapped[str] = mapped_column(Text, default="[]")  # JSON
    health_and_safety_summary: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class StaffMember(Base):
    __tablename__ = "staff_members"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    name: Mapped[str] = mapped_column(String(120))
    role: Mapped[str] = mapped_column(String(60))
    avatar_initials: Mapped[str] = mapped_column(String(10), default="SM")
    avatar_bg_color: Mapped[int] = mapped_column(BigInteger, default=4287505578)
    is_online: Mapped[bool] = mapped_column(Boolean, default=True)


class WorkerChat(Base):
    __tablename__ = "worker_chats"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(120))
    subtitle: Mapped[str] = mapped_column(String(120))
    is_global: Mapped[bool] = mapped_column(Boolean, default=False)
    is_group: Mapped[bool] = mapped_column(Boolean, default=False)
    avatar_initials: Mapped[str] = mapped_column(String(10), default="WC")
    avatar_bg_color: Mapped[int] = mapped_column(BigInteger, default=4287505578)
    last_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    last_message_time: Mapped[str | None] = mapped_column(String(20), nullable=True)
    unread_count: Mapped[int] = mapped_column(Integer, default=0)


class WorkerChatMessage(Base):
    __tablename__ = "worker_chat_messages"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    chat_id: Mapped[str] = mapped_column(ForeignKey("worker_chats.id", ondelete="CASCADE"), index=True)
    sender_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    sender_name: Mapped[str] = mapped_column(String(120))
    message: Mapped[str] = mapped_column(Text)
    time: Mapped[str] = mapped_column(String(20))
    is_outgoing: Mapped[bool] = mapped_column(Boolean, default=False)
    file_uri: Mapped[str | None] = mapped_column(String(255), nullable=True)
    file_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    file_mime_type: Mapped[str | None] = mapped_column(String(60), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class BitacoraMessage(Base):
    __tablename__ = "bitacora_messages"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)
    sender_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    sender_name: Mapped[str] = mapped_column(String(120))
    time: Mapped[str] = mapped_column(String(20))
    message: Mapped[str] = mapped_column(Text)
    file_uri: Mapped[str | None] = mapped_column(String(255), nullable=True)
    file_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    file_mime_type: Mapped[str | None] = mapped_column(String(60), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class FamilyGroup(Base):
    __tablename__ = "family_groups"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(120))
    created_by_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    avatar_bg_color: Mapped[int] = mapped_column(BigInteger, default=4278225275)
    last_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    last_message_time: Mapped[str | None] = mapped_column(String(20), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class FamilyGroupMember(Base):
    __tablename__ = "family_group_members"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    group_id: Mapped[str] = mapped_column(ForeignKey("family_groups.id", ondelete="CASCADE"), index=True)
    child_id: Mapped[str] = mapped_column(ForeignKey("children.id", ondelete="CASCADE"), index=True)


class FamilyGroupMessage(Base):
    __tablename__ = "family_group_messages"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    group_id: Mapped[str] = mapped_column(ForeignKey("family_groups.id", ondelete="CASCADE"), index=True)
    sender_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    sender_name: Mapped[str] = mapped_column(String(120))
    text: Mapped[str] = mapped_column(Text)
    time: Mapped[str] = mapped_column(String(20))
    attachment_uri: Mapped[str | None] = mapped_column(String(255), nullable=True)
    attachment_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    attachment_mime_type: Mapped[str | None] = mapped_column(String(60), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class Announcement(Base):
    __tablename__ = "announcements"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(120))
    date: Mapped[str] = mapped_column(String(30))
    summary: Mapped[str] = mapped_column(Text)
    full_content: Mapped[str] = mapped_column(Text)
    author: Mapped[str] = mapped_column(String(120))
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class AnnouncementRead(Base):
    __tablename__ = "announcement_reads"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    announcement_id: Mapped[str] = mapped_column(ForeignKey("announcements.id", ondelete="CASCADE"), index=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"), index=True)
    read_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class Suggestion(Base):
    __tablename__ = "suggestions"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    parent_user_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"))
    parent_name: Mapped[str] = mapped_column(String(120))
    child_id: Mapped[str] = mapped_column(String(64))
    child_name: Mapped[str] = mapped_column(String(120))
    room_name: Mapped[str] = mapped_column(String(60))
    date: Mapped[str] = mapped_column(String(20))
    time: Mapped[str] = mapped_column(String(20))
    category: Mapped[str] = mapped_column(String(40))
    subject: Mapped[str] = mapped_column(String(150))
    content: Mapped[str] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(30), default="PENDIENTE")  # PENDIENTE, EN_REVISION, ATENDIDA
    response: Mapped[str | None] = mapped_column(Text, nullable=True)
    response_date: Mapped[str | None] = mapped_column(String(30), nullable=True)
    responder_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class LostObject(Base):
    __tablename__ = "lost_objects"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(120))
    description: Mapped[str] = mapped_column(Text)
    category: Mapped[str] = mapped_column(String(40))
    location: Mapped[str | None] = mapped_column(String(120), nullable=True)
    image_uri: Mapped[str | None] = mapped_column(String(255), nullable=True)
    room: Mapped[str] = mapped_column(String(60), default="Sala 1A")
    is_claimed: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class MediaFile(Base):
    __tablename__ = "media_files"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    file_name: Mapped[str] = mapped_column(String(120))
    mime_type: Mapped[str] = mapped_column(String(60))
    size_bytes: Mapped[int] = mapped_column(Integer)
    url: Mapped[str] = mapped_column(String(255))
    folder: Mapped[str] = mapped_column(String(60), default="general")
    user_id: Mapped[str | None] = mapped_column(String(64), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class DeviceToken(Base):
    __tablename__ = "device_tokens"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("usuarios.id", ondelete="CASCADE"), index=True)
    device_token: Mapped[str] = mapped_column(String(255), unique=True)
    platform: Mapped[str] = mapped_column(String(30), default="ANDROID")
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())
