"""Módulo 3: Alumnos y Expedientes Médicos (/children, /parents)"""

import json
import uuid

from fastapi import APIRouter, Depends, File, Form, UploadFile
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import almacen
from ..db import get_db
from ..deps import Identidad, requiere_familiar, usuario_actual
from ..errors import ApiError
from ..models import Child, ChildParent, ChildProfile, Room, RoomEducator, StaffMember, Usuario
from ..schemas import (
    ChildFullProfileOut,
    ChildOut,
    ChildProfileUpdate,
    EducatorOut,
    MedicalCertificateOut,
)

router = APIRouter(tags=["children"])


def resolver_sala_y_educadora(db: Session, room_id: str):
    room = db.get(Room, room_id)
    room_text = room.name if room else room_id

    re = db.scalar(select(RoomEducator).where(RoomEducator.room_id == room_id).limit(1))
    educator_name = None
    educator_role = None
    educator_initials = None
    educator_color = None

    if re:
        staff = db.scalar(select(StaffMember).where(StaffMember.user_id == re.educator_id).limit(1))
        if staff:
            educator_name = staff.name
            educator_role = re.role_title or staff.role
            educator_initials = staff.avatar_initials
            educator_color = staff.avatar_bg_color
        else:
            u = db.get(Usuario, re.educator_id)
            if u:
                educator_name = u.display_name.split(" (")[0].strip()
                educator_role = re.role_title
                educator_initials = "".join([part[0] for part in educator_name.split()[:2]]).upper() or "ED"
                educator_color = 4278364369

    if not educator_name:
        educator_name = "Laura Gómez"
        educator_role = "Educadora Titular"
        educator_initials = "LG"
        educator_color = 4278364369

    return room_text, educator_name, educator_role, educator_initials, educator_color


def mapear_child_out(db: Session, a: Child) -> ChildOut:
    room_text, ed_name, ed_role, ed_initials, ed_color = resolver_sala_y_educadora(db, a.room_id)
    return ChildOut(
        id=a.id,
        fullName=a.full_name,
        shortName=a.short_name,
        ageText=a.age_text,
        roomText=room_text,
        groupText=a.group_text,
        statusText=a.status_text,
        isPresent=a.is_present,
        arrivalTime=a.arrival_time,
        allergyAlert=a.allergy_alert,
        avatarInitials=a.avatar_initials,
        avatarBgColor=a.avatar_bg_color,
        educatorName=ed_name,
        educatorRole=ed_role,
        educatorAvatarInitials=ed_initials,
        educatorAvatarBgColor=ed_color,
    )


@router.get("/parents/me/children", response_model=list[ChildOut])
def listar_hijos_de_familiar(
    quien: Identidad = Depends(requiere_familiar),
    db: Session = Depends(get_db),
) -> list[ChildOut]:
    """Devuelve los alumnos asociados a la cuenta del familiar autenticado."""
    hijos_ids = db.scalars(select(ChildParent.child_id).where(ChildParent.parent_user_id == quien.id)).all()
    alumnos = db.scalars(select(Child).where(Child.id.in_(hijos_ids))).all() if hijos_ids else []

    # Si no tiene asignados y es DIRECCION o prueba, mostramos todos
    if not alumnos and quien.role == "DIRECCION":
        alumnos = db.scalars(select(Child)).all()

    return [mapear_child_out(db, a) for a in alumnos]


@router.get("/children/{childId}", response_model=ChildOut)
def obtener_alumno(
    childId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildOut:
    """Obtiene la ficha básica de un alumno."""
    alumno = db.get(Child, childId)
    if alumno is None:
        raise ApiError(404, f"No se encontró el alumno con id {childId}", "alumno_no_encontrado")

    return mapear_child_out(db, alumno)


@router.get("/children/{childId}/educator", response_model=EducatorOut)
def obtener_educador_de_alumno(
    childId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> EducatorOut:
    """Devuelve el educador asignado a la sala del alumno."""
    alumno = db.get(Child, childId)
    if alumno is None:
        raise ApiError(404, f"No se encontró el alumno con id {childId}", "alumno_no_encontrado")

    room_text, ed_name, ed_role, ed_initials, ed_color = resolver_sala_y_educadora(db, alumno.room_id)
    re = db.scalar(select(RoomEducator).where(RoomEducator.room_id == alumno.room_id).limit(1))
    ed_id = re.educator_id if re else "usr_educadora_laura"

    return EducatorOut(
        id=ed_id,
        name=ed_name,
        role=ed_role,
        avatarInitials=ed_initials,
        avatarBgColor=ed_color,
        roomId=alumno.room_id,
        roomName=room_text,
    )


@router.get("/children/{childId}/full-profile", response_model=ChildFullProfileOut)
def obtener_expediente(
    childId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildFullProfileOut:
    """Devuelve el expediente escolar y médico completo del menor."""
    perfil = db.get(ChildProfile, childId)
    if perfil is None:
        raise ApiError(404, f"No existe expediente registrado para el alumno {childId}", "expediente_no_encontrado")

    pickups = []
    if perfil.authorized_pickups:
        try:
            pickups = json.loads(perfil.authorized_pickups)
        except Exception:
            pickups = []

    return ChildFullProfileOut(
        childId=perfil.child_id,
        birthDate=perfil.birth_date,
        enrollmentDate=perfil.enrollment_date,
        studentId=perfil.student_id,
        bloodType=perfil.blood_type,
        pediatrician=perfil.pediatrician,
        pediatricianPhone=perfil.pediatrician_phone,
        emergencyPhone=perfil.emergency_phone,
        emergencyPhoneRelation=perfil.emergency_phone_relation or "Tutor legal",
        motherName=perfil.mother_name,
        motherPhone=perfil.mother_phone,
        fatherName=perfil.father_name,
        fatherPhone=perfil.father_phone,
        authorizedPickups=pickups,
        medicalNotes=perfil.medical_notes,
        generalNotes=perfil.general_notes,
        medicalCertificateUri=perfil.medical_certificate_uri,
        medicalCertificateName=perfil.medical_certificate_name,
        medicalCertificateDate=perfil.medical_certificate_date,
    )


@router.put("/children/{childId}/full-profile", response_model=ChildFullProfileOut)
def actualizar_expediente(
    childId: str,
    body: ChildProfileUpdate,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildFullProfileOut:
    """Actualiza datos del expediente médico y de contacto del menor."""
    perfil = db.get(ChildProfile, childId)
    if perfil is None:
        perfil = ChildProfile(child_id=childId)
        db.add(perfil)

    if body.pediatrician is not None:
        perfil.pediatrician = body.pediatrician
    if body.pediatricianPhone is not None:
        perfil.pediatrician_phone = body.pediatricianPhone
    if body.medicalNotes is not None:
        perfil.medical_notes = body.medicalNotes
    if body.emergencyPhone is not None:
        perfil.emergency_phone = body.emergencyPhone
    if body.emergencyPhoneRelation is not None:
        perfil.emergency_phone_relation = body.emergencyPhoneRelation
    if body.authorizedPickups is not None:
        perfil.authorized_pickups = json.dumps(body.authorizedPickups)
    if body.medicalCertificateUri is not None:
        perfil.medical_certificate_uri = body.medicalCertificateUri
    if body.medicalCertificateName is not None:
        perfil.medical_certificate_name = body.medicalCertificateName
    if body.medicalCertificateDate is not None:
        perfil.medical_certificate_date = body.medicalCertificateDate

    db.commit()
    db.refresh(perfil)
    return obtener_expediente(childId, _, db)


@router.post("/children/{childId}/medical-certificate", response_model=MedicalCertificateOut)
def subir_certificado_medico(
    childId: str,
    file: UploadFile = File(...),
    certificateDate: str = Form("15/08/2026"),
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> MedicalCertificateOut:
    """Sube un certificado médico en formato PDF o imagen y actualiza el expediente."""
    contenido = file.file.read()
    extension = file.filename.split(".")[-1] if file.filename and "." in file.filename else "pdf"
    clave = f"cert_{uuid.uuid4().hex[:12]}.{extension}"
    tipo_mime = file.content_type or "application/pdf"

    almacen.guardar(clave, contenido, tipo_mime)

    perfil = db.get(ChildProfile, childId)
    if perfil is None:
        perfil = ChildProfile(child_id=childId)
        db.add(perfil)

    uri = f"https://cdn.sonrisas.edu/certificates/{clave}"
    perfil.medical_certificate_uri = uri
    perfil.medical_certificate_name = file.filename or clave
    perfil.medical_certificate_date = certificateDate
    db.commit()

    return MedicalCertificateOut(
        certificateUri=uri,
        certificateName=file.filename or clave,
        certificateDate=certificateDate,
    )
