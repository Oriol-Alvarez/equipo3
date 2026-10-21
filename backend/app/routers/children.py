"""Módulo 3: Alumnos y Expedientes Médicos (/children, /parents)"""

import json
import uuid

from fastapi import APIRouter, Depends, File, Form, UploadFile
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import almacen
from ..acceso import exigir_alumno_visible
from ..db import get_db
from ..deps import Identidad, requiere_familiar, usuario_actual
from ..errors import ApiError
from ..fichas import educadora_de_sala, ficha, fichas
from ..models import Child, ChildParent, ChildProfile, Room
from ..schemas import (
    ChildFullProfileOut,
    ChildOut,
    ChildProfileUpdate,
    EducatorOut,
    MedicalCertificateOut,
)

router = APIRouter(tags=["children"])


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

    return fichas(alumnos, db)


@router.get("/children/{childId}", response_model=ChildOut)
def obtener_alumno(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildOut:
    """Obtiene la ficha básica de un alumno."""
    alumno = exigir_alumno_visible(quien, childId, db)
    return ficha(alumno, db)


@router.get("/children/{childId}/educator", response_model=EducatorOut)
def obtener_educador_de_alumno(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> EducatorOut:
    """Devuelve la educadora asignada a la sala del alumno."""
    alumno = exigir_alumno_visible(quien, childId, db)
    educadora = educadora_de_sala(db, alumno.room_id)
    if educadora is None:
        raise ApiError(404, "La sala del alumno no tiene educadora asignada", "sin_educadora")
    sala = db.get(Room, alumno.room_id)
    return EducatorOut(
        id=educadora.id,
        name=educadora.name,
        role=educadora.role,
        avatarInitials=educadora.initials,
        avatarBgColor=educadora.color,
        roomId=alumno.room_id,
        roomName=sala.name if sala else alumno.room_id,
    )


@router.get("/children/{childId}/full-profile", response_model=ChildFullProfileOut)
def obtener_expediente(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildFullProfileOut:
    """Devuelve el expediente escolar y médico completo del menor.

    Si el alumno existe pero aún no tiene expediente, se devuelve vacío para que la
    familia o la educadora lo puedan llenar desde la app.
    """
    exigir_alumno_visible(quien, childId, db)
    perfil = db.get(ChildProfile, childId) or ChildProfile(child_id=childId)

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
        emergencyPhoneRelation=perfil.emergency_phone_relation or "",
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
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ChildFullProfileOut:
    """Actualiza datos del expediente médico y de contacto del menor."""
    exigir_alumno_visible(quien, childId, db)
    perfil = db.get(ChildProfile, childId)
    if perfil is None:
        perfil = ChildProfile(child_id=childId)
        db.add(perfil)

    if body.bloodType is not None:
        perfil.blood_type = body.bloodType
    if body.pediatrician is not None:
        perfil.pediatrician = body.pediatrician
    if body.pediatricianPhone is not None:
        perfil.pediatrician_phone = body.pediatricianPhone
    if body.medicalNotes is not None:
        perfil.medical_notes = body.medicalNotes
    if body.habitsAndPedagogicalNotes is not None:
        perfil.general_notes = body.habitsAndPedagogicalNotes
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
    return obtener_expediente(childId, quien, db)


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
