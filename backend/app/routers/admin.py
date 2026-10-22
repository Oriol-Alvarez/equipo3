import json
import time
import uuid

from fastapi import APIRouter, Depends, Response
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_direccion
from ..errors import ApiError
from ..fichas import fichas
from ..horario import ZONA_ESTANCIA, hoy
from ..resumenes import a_fecha
from ..models import (
    Activity,
    Announcement,
    AttendanceRecord,
    BitacoraMessage,
    Child,
    ChildParent,
    ChildProfile,
    LostObject,
    PhotoAccess,
    PhotoDeletion,
    Room,
    StaffMember,
    Suggestion,
    Usuario,
)
from ..schemas import (
    AdminChildSummary,
    AdminCreateChildRequest,
    AdminLogItem,
    AdminStatsOverview,
    AdminUpdateChildRequest,
    AdminUserCreate,
    AdminUserUpdate,
    UserProfileOut,
)
from ..security import hash_password


def _fecha_hora_local(momento) -> str:
    """Timestamp de la base de datos (UTC sin zona) -> 'dd/mm/aaaa hh:mm AM' en hora de la estancia."""
    if momento is None:
        return ""
    from datetime import timezone as _tz

    if momento.tzinfo is None:
        momento = momento.replace(tzinfo=_tz.utc)
    return momento.astimezone(ZONA_ESTANCIA).strftime("%d/%m/%Y %I:%M %p")

router = APIRouter(prefix="/admin", tags=["admin"])


def _resolve_room_id(db: Session, room_text: str | None) -> str:
    raw = (room_text or "").strip()
    if raw:
        room = db.scalar(select(Room).where((Room.id == raw) | (Room.name.ilike(raw))))
        if room:
            return room.id
    first = db.scalar(select(Room).order_by(Room.id))
    return first.id if first else "sala_1a"


def _build_admin_child_summary(child: Child, profile: ChildProfile | None) -> AdminChildSummary:
    pickups: list[str] = []
    if profile and profile.authorized_pickups:
        try:
            parsed = json.loads(profile.authorized_pickups)
            if isinstance(parsed, list):
                pickups = [str(p) for p in parsed]
        except Exception:
            pickups = []

    return AdminChildSummary(
        id=child.id,
        fullName=child.full_name,
        shortName=child.short_name,
        ageText=child.age_text,
        roomText=child.room_id,
        groupText=child.group_text,
        isPresent=child.is_present,
        statusText=child.status_text,
        arrivalTime=child.arrival_time,
        allergyAlert=child.allergy_alert,
        avatarInitials=child.avatar_initials,
        avatarBgColor=child.avatar_bg_color,
        birthDate=profile.birth_date if profile else None,
        enrollmentDate=profile.enrollment_date if profile else None,
        studentId=profile.student_id if profile else None,
        bloodType=profile.blood_type if profile else None,
        pediatrician=profile.pediatrician if profile else None,
        pediatricianPhone=profile.pediatrician_phone if profile else None,
        emergencyPhone=profile.emergency_phone if profile else None,
        emergencyPhoneRelation=profile.emergency_phone_relation if profile else None,
        motherName=profile.mother_name if profile else None,
        motherPhone=profile.mother_phone if profile else None,
        fatherName=profile.father_name if profile else None,
        fatherPhone=profile.father_phone if profile else None,
        authorizedPickups=pickups,
        medicalNotes=profile.medical_notes if profile else None,
        generalNotes=profile.general_notes if profile else None,
        habitsNotes=profile.habits_notes if profile else None,
    )


# ---------------------------------------------------------------------------
# 1. Gestión de Perfiles de Usuario (Educadores y Familiares)
# ---------------------------------------------------------------------------


@router.get("/users", response_model=list[UserProfileOut])
def listar_usuarios_admin(
    role: str | None = None,
    search: str | None = None,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> list[UserProfileOut]:
    """Lista todos los perfiles de usuario del sistema (personal y familias), incluyendo hijos asignados."""
    query = select(Usuario).order_by(Usuario.created_at.desc())
    if role and role.upper() != "TODOS":
        query = query.where(Usuario.role == role.upper())
    if search:
        termino = f"%{search.strip().lower()}%"
        query = query.where(Usuario.display_name.ilike(termino) | Usuario.email.ilike(termino))

    usuarios = db.scalars(query).all()

    # Pre-cargar hijos y expedientes para cada cuenta
    child_parent_links = db.scalars(select(ChildParent)).all()
    parent_to_child_ids: dict[str, list[str]] = {}
    for link in child_parent_links:
        parent_to_child_ids.setdefault(link.parent_user_id, []).append(link.child_id)

    all_children_map = {c.id: c for c in db.scalars(select(Child)).all()}
    all_profiles_map = {p.child_id: p for p in db.scalars(select(ChildProfile)).all()}

    resultados: list[UserProfileOut] = []
    for u in usuarios:
        c_ids = parent_to_child_ids.get(u.id, [])
        user_children = [
            _build_admin_child_summary(all_children_map[cid], all_profiles_map.get(cid))
            for cid in c_ids
            if cid in all_children_map
        ]
        out = UserProfileOut(
            id=u.id,
            email=u.email,
            role=u.role,
            displayName=u.display_name,
            phone=u.phone,
            avatarUrl=u.avatar_url,
            children=user_children,
        )
        resultados.append(out)

    return resultados


@router.post("/users", response_model=UserProfileOut, status_code=201)
def crear_usuario_admin(
    body: AdminUserCreate,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> UserProfileOut:
    """Crea un nuevo perfil de usuario (Familiar, Educadora, Dirección)."""
    email_limpio = body.email.strip().lower()
    existente = db.scalar(select(Usuario).where(Usuario.email == email_limpio))
    if existente:
        raise ApiError(409, f'El correo "{email_limpio}" ya está registrado', "email_duplicado")

    rol_normalizado = body.role.strip().upper()
    user_id = f"usr_{rol_normalizado.lower()}_{uuid.uuid4().hex[:6]}"
    nuevo_usuario = Usuario(
        id=user_id,
        email=email_limpio,
        password_hash=hash_password(body.password),
        role=rol_normalizado,
        display_name=body.displayName.strip(),
        phone=body.phone.strip() if body.phone else None,
        avatar_url=None,
    )
    db.add(nuevo_usuario)
    db.flush()

    if rol_normalizado in ("EDUCADORA", "DIRECCION"):
        staff_id = f"staff_{uuid.uuid4().hex[:6]}"
        db.add(
            StaffMember(
                id=staff_id,
                user_id=nuevo_usuario.id,
                name=nuevo_usuario.display_name,
                role="Dirección General" if rol_normalizado == "DIRECCION" else "Educadora Titular",
                avatar_initials="".join([w[0] for w in nuevo_usuario.display_name.split()[:2]]).upper() or "ED",
                avatar_bg_color=4287505578,
                is_online=True,
            )
        )

    db.commit()
    db.refresh(nuevo_usuario)
    return UserProfileOut.model_validate(nuevo_usuario)


@router.put("/users/{user_id}", response_model=UserProfileOut)
def modificar_usuario_admin(
    user_id: str,
    body: AdminUserUpdate,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> UserProfileOut:
    """Modifica los datos de un usuario existente."""
    user = db.get(Usuario, user_id)
    if user is None:
        raise ApiError(404, f"No existe el usuario con id {user_id}")

    if body.displayName is not None:
        user.display_name = body.displayName.strip()
    if body.phone is not None:
        user.phone = body.phone.strip()
    if body.email is not None and body.email.strip().lower() != user.email:
        email_nuevo = body.email.strip().lower()
        existente = db.scalar(select(Usuario).where(Usuario.email == email_nuevo))
        if existente:
            raise ApiError(409, f'El correo "{email_nuevo}" ya está en uso', "email_duplicado")
        user.email = email_nuevo
    if body.role is not None:
        user.role = body.role.strip().upper()
    if body.password is not None and body.password.strip():
        user.password_hash = hash_password(body.password.strip())

    db.commit()
    db.refresh(user)
    return UserProfileOut.model_validate(user)


@router.delete("/users/{user_id}", status_code=204)
def eliminar_usuario_admin(
    user_id: str,
    quien: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> Response:
    """Elimina definitivamente un perfil de usuario."""
    if user_id == quien.id:
        raise ApiError(400, "No puedes eliminar tu propia cuenta de administrador en sesión")

    user = db.get(Usuario, user_id)
    if user is None:
        raise ApiError(404, f"No existe el usuario con id {user_id}")

    db.delete(user)
    db.commit()
    return Response(status_code=204)


# ---------------------------------------------------------------------------
# 1.1 Gestión y Vinculación de Alumnos / Expedientes de Cuentas Familiares
# ---------------------------------------------------------------------------


@router.get("/children", response_model=list[AdminChildSummary])
def listar_todos_alumnos_admin(
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> list[AdminChildSummary]:
    """Lista todos los alumnos registrados en el centro con su ficha médica y escolar completa."""
    children = db.scalars(select(Child).order_by(Child.full_name)).all()
    profiles_map = {p.child_id: p for p in db.scalars(select(ChildProfile)).all()}
    return [_build_admin_child_summary(c, profiles_map.get(c.id)) for c in children]


@router.post("/users/{user_id}/children", response_model=AdminChildSummary, status_code=201)
def crear_y_vincular_alumno_admin(
    user_id: str,
    body: AdminCreateChildRequest,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AdminChildSummary:
    """Crea un nuevo alumno en el centro y lo asocia a la cuenta familiar indicada."""
    parent = db.get(Usuario, user_id)
    if parent is None:
        raise ApiError(404, f"No existe el familiar con id {user_id}")

    cid = f"child_{uuid.uuid4().hex[:8]}"
    initials = "".join([w[0] for w in body.fullName.split()[:2]]).upper() or "AL"
    short = body.shortName or body.fullName.split()[0]

    room_id = _resolve_room_id(db, body.roomText)
    sala = db.get(Room, room_id)
    child = Child(
        id=cid,
        full_name=body.fullName.strip(),
        short_name=short,
        age_text=body.ageText or "2 años",
        room_id=room_id,
        group_text=(body.groupText or "").strip() or (sala.level if sala else ""),
        status_text="No ha ingresado",
        is_present=False,
        allergy_alert=body.allergyAlert,
        avatar_initials=initials,
        avatar_bg_color=4294948685,
    )
    db.add(child)

    pickups_json = json.dumps(body.authorizedPickups) if body.authorizedPickups else "[]"
    profile = ChildProfile(
        child_id=cid,
        birth_date=body.birthDate or "",
        enrollment_date="12/10/2026",
        student_id=body.studentId or f"MAT-{uuid.uuid4().hex[:4].upper()}",
        blood_type=body.bloodType or "O+",
        pediatrician=body.pediatrician or "",
        pediatrician_phone=body.pediatricianPhone or "",
        emergency_phone=body.emergencyPhone or "",
        emergency_phone_relation=body.emergencyPhoneRelation or "Tutor",
        mother_name=body.motherName or (parent.display_name if parent.role == "FAMILIAR" else ""),
        mother_phone=body.motherPhone or (parent.phone or ""),
        father_name=body.fatherName or "",
        father_phone=body.fatherPhone or "",
        authorized_pickups=pickups_json,
        medical_notes=body.medicalNotes or "",
        general_notes=body.generalNotes or "",
        habits_notes=body.habitsNotes or "",
    )
    db.add(profile)

    link = ChildParent(child_id=cid, parent_user_id=user_id)
    db.add(link)

    db.commit()
    db.refresh(child)
    db.refresh(profile)

    return _build_admin_child_summary(child, profile)


@router.post("/users/{user_id}/children/{child_id}", response_model=AdminChildSummary)
def vincular_alumno_existente_admin(
    user_id: str,
    child_id: str,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AdminChildSummary:
    """Vincula un alumno existente del centro a una cuenta familiar."""
    parent = db.get(Usuario, user_id)
    if parent is None:
        raise ApiError(404, f"No existe el usuario con id {user_id}")

    child = db.get(Child, child_id)
    if child is None:
        raise ApiError(404, f"No existe el alumno con id {child_id}")

    link = db.scalar(
        select(ChildParent).where(ChildParent.parent_user_id == user_id, ChildParent.child_id == child_id)
    )
    if not link:
        db.add(ChildParent(child_id=child_id, parent_user_id=user_id))
        db.commit()

    profile = db.get(ChildProfile, child_id)
    return _build_admin_child_summary(child, profile)


@router.delete("/users/{user_id}/children/{child_id}", status_code=204)
def desvincular_alumno_admin(
    user_id: str,
    child_id: str,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> Response:
    """Elimina o desvincula un alumno de la cuenta familiar. Si no pertenece a otra familia, se elimina para evitar alumnos sin vincular."""
    link = db.scalar(
        select(ChildParent).where(ChildParent.parent_user_id == user_id, ChildParent.child_id == child_id)
    )
    if link:
        db.delete(link)
        db.flush()

    remaining = db.scalar(
        select(func.count()).select_from(ChildParent).where(ChildParent.child_id == child_id)
    ) or 0
    if remaining == 0:
        c = db.get(Child, child_id)
        if c:
            db.delete(c)

    db.commit()
    return Response(status_code=204)


@router.put("/children/{child_id}", response_model=AdminChildSummary)
def actualizar_alumno_expediente_admin(
    child_id: str,
    body: AdminUpdateChildRequest,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AdminChildSummary:
    """Actualiza la información escolar y el expediente médico de un alumno."""
    child = db.get(Child, child_id)
    if child is None:
        raise ApiError(404, f"No existe el alumno con id {child_id}")

    profile = db.get(ChildProfile, child_id)
    if profile is None:
        profile = ChildProfile(child_id=child_id)
        db.add(profile)

    if body.fullName is not None:
        child.full_name = body.fullName.strip()
    if body.shortName is not None:
        child.short_name = body.shortName.strip()
    if body.ageText is not None:
        child.age_text = body.ageText
    if body.roomText is not None:
        child.room_id = _resolve_room_id(db, body.roomText)
    if body.groupText is not None:
        child.group_text = body.groupText
    if body.statusText is not None:
        child.status_text = body.statusText
    if body.isPresent is not None:
        child.is_present = body.isPresent
    if body.allergyAlert is not None:
        child.allergy_alert = body.allergyAlert

    if body.birthDate is not None:
        profile.birth_date = body.birthDate
    if body.enrollmentDate is not None:
        profile.enrollment_date = body.enrollmentDate
    if body.studentId is not None:
        profile.student_id = body.studentId
    if body.bloodType is not None:
        profile.blood_type = body.bloodType
    if body.pediatrician is not None:
        profile.pediatrician = body.pediatrician
    if body.pediatricianPhone is not None:
        profile.pediatrician_phone = body.pediatricianPhone
    if body.emergencyPhone is not None:
        profile.emergency_phone = body.emergencyPhone
    if body.emergencyPhoneRelation is not None:
        profile.emergency_phone_relation = body.emergencyPhoneRelation
    if body.motherName is not None:
        profile.mother_name = body.motherName
    if body.motherPhone is not None:
        profile.mother_phone = body.motherPhone
    if body.fatherName is not None:
        profile.father_name = body.fatherName
    if body.fatherPhone is not None:
        profile.father_phone = body.fatherPhone
    if body.authorizedPickups is not None:
        profile.authorized_pickups = json.dumps(body.authorizedPickups)
    if body.medicalNotes is not None:
        profile.medical_notes = body.medicalNotes
    if body.generalNotes is not None:
        profile.general_notes = body.generalNotes
    if body.habitsNotes is not None:
        profile.habits_notes = body.habitsNotes

    db.commit()
    db.refresh(child)
    db.refresh(profile)

    return _build_admin_child_summary(child, profile)


# ---------------------------------------------------------------------------
# 2. Apartado de Logs y Auditoría Global
# ---------------------------------------------------------------------------


@router.get("/logs", response_model=list[AdminLogItem])
def obtener_logs_sistema(
    category: str | None = None,
    search: str | None = None,
    limit: int = 60,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> list[AdminLogItem]:
    """Genera una bitácora unificada de eventos escolares, mensajes, actividades y asistencia."""
    logs: list[AdminLogItem] = []

    # Map children for easy lookup
    children_map = {c.id: c for c in db.scalars(select(Child)).all()}

    # 1. Asistencia (Ingreso y Egreso)
    attendance_records = db.scalars(
        select(AttendanceRecord).order_by(AttendanceRecord.recorded_at.desc()).limit(limit)
    ).all()
    for att in attendance_records:
        child = children_map.get(att.child_id)
        c_name = child.full_name if child else att.child_id
        r_name = child.room_id if child else "General"
        is_in = att.is_ingreso
        action_title = "Ingreso al Centro" if is_in else "Egreso / Retiro del Alumno"
        deliverer = att.deliverer_name or ("Familiar autorizado" if is_in else "Educadora")
        receiver = att.receiver_staff_name or att.receiver_tutor_name or ("Educadora" if is_in else "Familiar")
        detail = (
            f"Alumno {c_name} ingresó a las {att.time}. Entregó: {deliverer}. Recibió: {receiver}."
            if is_in
            else f"Alumno {c_name} fue retirado a las {att.time}. Recibió: {receiver}."
        )
        if att.observations:
            detail += f" Obs: {att.observations}"

        logs.append(
            AdminLogItem(
                id=f"log_att_{att.id}",
                timestamp=f"{att.date} {att.time}",
                category="ASISTENCIA",
                title=action_title,
                description=detail,
                # RF-14: la cuenta que capturó el registro (los registros antiguos no la tienen)
                actorName=att.recorded_by_name or (receiver if is_in else deliverer),
                actorRole="EDUCADORA" if (att.recorded_by_name or is_in) else "FAMILIAR",
                childName=c_name,
                roomName=r_name,
            )
        )

    # 2. Actividades pedagógicas, alimentación, sueño, pañales
    activities = db.scalars(
        select(Activity).order_by(Activity.created_at.desc()).limit(limit)
    ).all()
    for act in activities:
        child = children_map.get(act.child_id)
        c_name = child.full_name if child else act.child_id
        r_name = child.room_id if child else "General"
        logs.append(
            AdminLogItem(
                id=f"log_act_{act.id}",
                timestamp=f"{act.date} {act.time}",
                category="ACTIVIDAD",
                title=f"{act.category.capitalize()}: {act.title}",
                description=act.description or f"Registro de {act.category.lower()}",
                actorName=act.created_by_name or "Personal Educativo",
                actorRole="EDUCADORA",
                childName=c_name,
                roomName=r_name,
            )
        )

    # 3. Mensajes en Bitácora Infantil
    bitacora_msgs = db.scalars(
        select(BitacoraMessage).order_by(BitacoraMessage.created_at.desc()).limit(limit // 2)
    ).all()
    for msg in bitacora_msgs:
        child = children_map.get(msg.child_id)
        c_name = child.full_name if child else msg.child_id
        logs.append(
            AdminLogItem(
                id=f"log_bmsg_{msg.id}",
                timestamp=f"{msg.date} {msg.time}",
                category="MENSAJE",
                title=f"Mensaje en Bitácora ({c_name})",
                description=msg.message,
                actorName=msg.sender_name,
                actorRole="CHAT",
                childName=c_name,
                roomName=child.room_id if child else None,
            )
        )

    # 4. Avisos Generales del Centro
    announcements = db.scalars(
        select(Announcement).order_by(Announcement.created_at.desc()).limit(15)
    ).all()
    for ann in announcements:
        logs.append(
            AdminLogItem(
                id=f"log_ann_{ann.id}",
                timestamp=ann.date,
                category="AVISO",
                title=f"Aviso Escolar: {ann.title}",
                description=ann.summary,
                actorName=ann.author,
                actorRole="DIRECCION",
                childName=None,
                roomName="Todo el Centro",
            )
        )

    # 5. Sugerencias de las Familias
    suggestions = db.scalars(
        select(Suggestion).order_by(Suggestion.created_at.desc()).limit(15)
    ).all()
    for sug in suggestions:
        logs.append(
            AdminLogItem(
                id=f"log_sug_{sug.id}",
                timestamp=f"{sug.date} {sug.time}",
                category="SUGERENCIA",
                title=f"Sugerencia ({sug.status}): {sug.subject}",
                description=sug.content,
                actorName=sug.parent_name,
                actorRole="FAMILIAR",
                childName=sug.child_name,
                roomName=sug.room_name,
            )
        )

    # 6. Objetos Perdidos Registrados
    lost_objects = db.scalars(
        select(LostObject).order_by(LostObject.created_at.desc()).limit(15)
    ).all()
    for lo in lost_objects:
        estado = "Entregado" if lo.is_claimed else "En custodia"
        logs.append(
            AdminLogItem(
                id=f"log_lo_{lo.id}",
                timestamp="Hoy",
                category="OBJETO",
                title=f"Objeto Perdido ({estado}): {lo.title}",
                description=f"{lo.description} • Ubicación: {lo.location or lo.room}",
                actorName="Control de Custodia",
                actorRole="DIRECCION",
                childName=None,
                roomName=lo.room,
            )
        )

    # 7. RF-14: quién consultó cada fotografía o archivo
    for acc in db.scalars(select(PhotoAccess).order_by(PhotoAccess.accessed_at.desc()).limit(limit)).all():
        logs.append(
            AdminLogItem(
                id=f"log_foto_{acc.id}",
                timestamp=_fecha_hora_local(acc.accessed_at),
                category="FOTO",
                title="Consulta de foto o archivo",
                description=f"{acc.user_name} abrió el archivo {acc.file_id}.",
                actorName=acc.user_name,
                actorRole=acc.user_role,
                childName=None,
                roomName=None,
            )
        )

    # 8. RF-10: fotos eliminadas a solicitud
    for bor in db.scalars(select(PhotoDeletion).order_by(PhotoDeletion.deleted_at.desc()).limit(limit)).all():
        motivo = f" Motivo: {bor.reason}" if bor.reason else ""
        logs.append(
            AdminLogItem(
                id=f"log_borrado_{bor.id}",
                timestamp=_fecha_hora_local(bor.deleted_at),
                category="FOTO",
                title="Foto eliminada",
                description=(
                    f"{bor.requested_by_name} eliminó el archivo {bor.file_id} "
                    f"({bor.references_cleared} mensaje(s) actualizados).{motivo}"
                ),
                actorName=bor.requested_by_name,
                actorRole=bor.requested_by_role,
                childName=None,
                roomName=None,
            )
        )

    # Filtrar por categoría si se especificó
    if category and category.upper() != "TODOS":
        logs = [l for l in logs if l.category.upper() == category.upper()]

    # Filtrar por término de búsqueda
    if search:
        t = search.strip().lower()
        logs = [
            l
            for l in logs
            if t in l.title.lower()
            or t in l.description.lower()
            or t in l.actorName.lower()
            or (l.childName and t in l.childName.lower())
        ]

    return logs[:limit]


# ---------------------------------------------------------------------------
# 3. Apartado de Estadísticas y Métricas Globales
# ---------------------------------------------------------------------------


@router.get("/stats", response_model=AdminStatsOverview)
def obtener_estadisticas_admin(
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AdminStatsOverview:
    """Calcula métricas e indicadores globales: asistencia, comidas, siestas, avisos, etc."""
    total_children = db.scalar(select(func.count(Child.id))) or 0
    # Presentes HOY con la misma regla que la pantalla de la sala (registro diario del día)
    estado_hoy = fichas(db.scalars(select(Child)).all(), db)
    present_count = sum(1 for f in estado_hoy if f.isPresent)
    retirados_hoy = sum(1 for f in estado_hoy if "Retirado" in (f.statusText or ""))
    absent_count = max(0, total_children - present_count)
    att_rate = round((present_count / total_children * 100), 1) if total_children > 0 else 0.0

    total_educators = (
        db.scalar(select(func.count(Usuario.id)).where(Usuario.role.in_(("EDUCADORA", "DIRECCION")))) or 0
    )
    total_families = db.scalar(select(func.count(Usuario.id)).where(Usuario.role == "FAMILIAR")) or 0

    # Cargar actividades reales registradas en la base de datos
    dia_hoy = hoy()
    all_activities = [a for a in db.scalars(select(Activity)).all() if a_fecha(a.date) == dia_hoy]

    # Comida / Alimentación (datos reales introducidos por educadoras)
    food_acts = [
        a
        for a in all_activities
        if a.category
        and any(k in a.category.upper() for k in ("ALIMENTA", "COMIDA", "NUTRICI"))
    ]
    children_fed_set = set(a.child_id for a in food_acts)
    children_fed_count = len(children_fed_set)

    breakfast_count = sum(
        1
        for a in food_acts
        if (a.meal_type and any(k in a.meal_type.upper() for k in ("DESAYUNO", "BREAKFAST")))
        or (a.title and "DESAYUNO" in a.title.upper())
        or (a.portion_option and "DESAYUNO" in a.portion_option.upper())
    )
    lunch_count = sum(
        1
        for a in food_acts
        if (a.meal_type and any(k in a.meal_type.upper() for k in ("COMIDA", "LUNCH")))
        or (a.title and ("COMIDA" in a.title.upper() or "ALMUERZO" in a.title.upper()))
        or (a.portion_option and ("COMIDA" in a.portion_option.upper() or "ALMUERZO" in a.portion_option.upper()))
    )
    snack_count = sum(
        1
        for a in food_acts
        if (a.meal_type and any(k in a.meal_type.upper() for k in ("SNACK", "COLACION", "COLACIÓN", "MERIENDA")))
        or (a.title and any(k in a.title.upper() for k in ("COLACION", "COLACIÓN", "MERIENDA")))
        or (a.portion_option and any(k in a.portion_option.upper() for k in ("COLACION", "COLACIÓN", "MERIENDA")))
    )

    # Siesta / Sueño (datos reales registrados)
    nap_acts = [
        a
        for a in all_activities
        if a.category
        and any(k in a.category.upper() for k in ("DESCANSO", "SIESTA", "SUEÑO", "SUENO"))
    ]
    children_nap_set = set(a.child_id for a in nap_acts)
    children_napped_count = len(children_nap_set)
    total_nap_minutes = sum(a.nap_minutes or 0 for a in nap_acts)
    avg_nap_minutes = (
        int(total_nap_minutes / children_napped_count) if children_napped_count > 0 else 0
    )

    # Asistencia Ingreso / Egreso (registros reales en puerta)
    # Ingresos y egresos de HOY (registro diario)
    ingresos = present_count + retirados_hoy
    egresos = retirados_hoy
    pending_pickups = present_count

    # Funciones excretoras / Control de esfínteres / Pañal (100% datos reales)
    excretory_acts = [
        a
        for a in all_activities
        if a.category
        and any(
            k in a.category.upper()
            for k in ("EXCRET", "PAÑAL", "PANAL", "ESFINTER", "ESFÍNTER")
        )
    ]
    diapers = len(excretory_acts)

    # Incidentes escolares registrados
    incident_acts = [
        a
        for a in all_activities
        if a.category
        and any(k in a.category.upper() for k in ("ACCIDENT", "INCIDENT"))
    ]
    incidents = len(incident_acts)

    # Sugerencias y quejas reales
    tot_sug = db.scalar(select(func.count(Suggestion.id))) or 0
    pend_sug = db.scalar(select(func.count(Suggestion.id)).where(Suggestion.status == "PENDIENTE")) or 0
    att_sug = db.scalar(select(func.count(Suggestion.id)).where(Suggestion.status == "ATENDIDA")) or 0

    # Objetos perdidos reales
    tot_lo = db.scalar(select(func.count(LostObject.id))) or 0
    claimed_lo = db.scalar(select(func.count(LostObject.id)).where(LostObject.is_claimed.is_(True))) or 0
    unclaimed_lo = max(0, tot_lo - claimed_lo)

    # Avisos reales emitidos
    tot_ann = db.scalar(select(func.count(Announcement.id))) or 0

    return AdminStatsOverview(
        totalChildren=total_children,
        presentCount=present_count,
        absentCount=absent_count,
        attendanceRate=att_rate,
        totalEducators=total_educators,
        totalFamilies=total_families,
        childrenFedCount=children_fed_count,
        breakfastCount=breakfast_count,
        lunchCount=lunch_count,
        snackCount=snack_count,
        childrenNappedCount=children_napped_count,
        totalNapMinutes=total_nap_minutes,
        averageNapMinutes=avg_nap_minutes,
        ingresosCount=ingresos,
        egresosCount=egresos,
        pendingPickupsCount=pending_pickups,
        diaperChangesCount=diapers,
        incidentsCount=incidents,
        totalSuggestions=tot_sug,
        pendingSuggestions=pend_sug,
        attendedSuggestions=att_sug,
        totalLostObjects=tot_lo,
        claimedLostObjects=claimed_lo,
        unclaimedLostObjects=unclaimed_lo,
        totalAnnouncements=tot_ann,
    )
