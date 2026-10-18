"""Módulo 4: Control de Asistencia (/attendance)"""

import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from .daily_logs import _registro
from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import AttendanceRecord, Child
from ..schemas import (
    AttendanceRecordOut,
    AttendanceResponse,
    EgresoLogData,
    EgresoRequest,
    IngresoLogData,
    IngresoRequest,
)

router = APIRouter(prefix="/attendance", tags=["attendance"])


@router.post("/ingreso", response_model=AttendanceResponse, status_code=201)
def registrar_ingreso(
    body: IngresoRequest,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> AttendanceResponse:
    """Registra el ingreso formal del alumno y actualiza su estado a Presente."""
    alumno = db.get(Child, body.childId)
    if alumno is None:
        raise ApiError(404, f"No existe el alumno con id {body.childId}", "alumno_no_encontrado")

    record_id = f"att_{uuid.uuid4().hex[:12]}"
    nuevo_registro = AttendanceRecord(
        id=record_id,
        child_id=body.childId,
        is_ingreso=True,
        date=body.date,
        day_of_week=body.dayOfWeek,
        time=body.entryTime,
        status="REGISTRADO",
        assistant_name=body.assistantName,
        is_child_sick=body.isChildSick,
        medication_option=body.medicationOption,
        has_symptoms=body.hasSymptoms,
        symptoms_detail=body.symptomsDetail,
        is_clean=body.isClean,
        backpack_complete=body.backpackComplete,
        good_physical_condition=body.goodPhysicalCondition,
        injury_detail=body.injuryDetail,
        observations=body.observations,
        deliverer_name=body.delivererName,
        deliverer_signature_data=body.delivererSignatureData,
        receiver_staff_name=body.receiverStaffName,
        receiver_staff_signature_data=body.receiverStaffSignatureData,
    )
    db.add(nuevo_registro)

    # Actualizar estado del alumno en sala
    alumno.is_present = True
    alumno.arrival_time = body.entryTime
    alumno.status_text = f"{body.entryTime} • Presente"

    # Sincronización con la bitácora oficial (DailyLog)
    med_opt = body.medicationOption if body.medicationOption in {"SI", "NO", "NO_APLICA"} else "NO_APLICA"
    ingreso_log = IngresoLogData(
        time=body.entryTime,
        assistantName=body.assistantName or "",
        deliveredBy=body.delivererName or "",
        receivedBy=body.receiverStaffName or "",
        isSick=body.isChildSick,
        medicationWithPrescription=med_opt,
        hasSymptoms=body.hasSymptoms,
        symptoms=body.symptomsDetail or "",
        arrivedClean=body.isClean,
        backpackComplete=body.backpackComplete,
        goodPhysicalCondition=body.goodPhysicalCondition,
        injuries=body.injuryDetail or "",
        observations=body.observations or "",
    )
    daily_log = _registro(body.childId, body.date, db)
    daily_log.ingreso = ingreso_log.model_dump_json()

    db.commit()

    return AttendanceResponse(
        id=record_id,
        childId=body.childId,
        isIngreso=True,
        status="REGISTRADO",
        recordedAt=datetime.now(timezone.utc).isoformat(),
    )


@router.post("/egreso", response_model=AttendanceResponse, status_code=201)
def registrar_egreso(
    body: EgresoRequest,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> AttendanceResponse:
    """Registra la entrega/egreso formal del alumno y actualiza su estado de asistencia."""
    alumno = db.get(Child, body.childId)
    if alumno is None:
        raise ApiError(404, f"No existe el alumno con id {body.childId}", "alumno_no_encontrado")

    record_id = f"att_{uuid.uuid4().hex[:12]}"
    nuevo_registro = AttendanceRecord(
        id=record_id,
        child_id=body.childId,
        is_ingreso=False,
        date=body.date,
        time=body.exitTime,
        status="REGISTRADO",
        good_physical_condition=body.goodPhysicalCondition,
        injury_detail=body.injuryDetail,
        is_clean=body.isClean,
        backpack_complete=body.backpackComplete,
        observations=body.observations,
        receiver_tutor_name=body.receiverTutorName,
        receiver_tutor_signature_data=body.receiverTutorSignatureData,
        deliverer_staff_name=body.delivererStaffName,
        deliverer_staff_signature_data=body.delivererStaffSignatureData,
    )
    db.add(nuevo_registro)

    # Actualizar estado del alumno
    alumno.is_present = False
    alumno.status_text = f"{body.exitTime} • Retirado"

    # Sincronización con la bitácora oficial (DailyLog)
    egreso_log = EgresoLogData(
        time=body.exitTime,
        goodPhysicalCondition=body.goodPhysicalCondition,
        injuries=body.injuryDetail or "",
        collectedBy=body.receiverTutorName or "",
        deliveredClean=body.isClean,
        belongingsComplete=body.backpackComplete,
        observations=body.observations or "",
        deliveredByStaff=body.delivererStaffName or "",
    )
    daily_log = _registro(body.childId, body.date, db)
    daily_log.egreso = egreso_log.model_dump_json()

    db.commit()

    return AttendanceResponse(
        id=record_id,
        childId=body.childId,
        isIngreso=False,
        status="REGISTRADO",
        recordedAt=datetime.now(timezone.utc).isoformat(),
    )


@router.get("/records", response_model=list[AttendanceRecordOut])
def consultar_asistencias(
    childId: str | None = None,
    date: str | None = None,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[AttendanceRecordOut]:
    """Consulta el historial de registros de asistencia filtrado por alumno y fecha."""
    query = select(AttendanceRecord).order_by(AttendanceRecord.recorded_at.desc())
    if childId:
        query = query.where(AttendanceRecord.child_id == childId)
    if date:
        query = query.where(AttendanceRecord.date == date)

    registros = db.scalars(query.limit(100)).all()
    return [
        AttendanceRecordOut(
            id=r.id,
            childId=r.child_id,
            isIngreso=r.is_ingreso,
            date=r.date,
            dayOfWeek=r.day_of_week,
            time=r.time,
            observations=r.observations,
        )
        for r in registros
    ]
