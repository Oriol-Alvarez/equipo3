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
from ..acceso import exigir_alumno_visible, ids_visibles
from ..horario import hora_12h_de, minutos_del_dia
from ..models import AttendanceRecord, Child
from ..schemas import (
    AttendanceRecordOut,
    AttendanceResponse,
    EgresoLogData,
    EgresoRequest,
    IngresoLogData,
    IngresoRequest,
)

def _minutos(hora: str) -> int:
    """'8:15 AM', '14:30' o '14:30 AM' -> minutos del día. Si no se entiende, 422."""
    minutos = minutos_del_dia(hora)
    if minutos is None:
        raise ApiError(422, f"Hora no válida: {hora}", "hora_invalida", "Usa el formato 08:15 o 08:15 AM.")
    return minutos


def _hora_24(hora: str) -> str:
    """'8:15 AM' u '08:15' -> '08:15' (formato de la hoja oficial). Si no se entiende, 422."""
    minutos = _minutos(hora)
    return f"{minutos // 60:02d}:{minutos % 60:02d}"


def _hora_12(hora: str) -> str:
    """Cualquier formato aceptado -> '02:30 PM' (como se muestra en la app)."""
    return hora_12h_de(_minutos(hora))


router = APIRouter(prefix="/attendance", tags=["attendance"])


@router.post("/ingreso", response_model=AttendanceResponse, status_code=201)
def registrar_ingreso(
    body: IngresoRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> AttendanceResponse:
    """Registra el ingreso formal del alumno y actualiza su estado a Presente."""
    alumno = exigir_alumno_visible(quien, body.childId, db)
    hora = _hora_12(body.entryTime)

    record_id = f"att_{uuid.uuid4().hex[:12]}"
    nuevo_registro = AttendanceRecord(
        id=record_id,
        child_id=body.childId,
        is_ingreso=True,
        date=body.date,
        day_of_week=body.dayOfWeek,
        time=hora,
        recorded_by_id=quien.id,
        recorded_by_name=quien.display_name or quien.email,
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
    alumno.arrival_time = hora
    alumno.status_text = f"{hora} • Presente"

    # Sincronización con la bitácora oficial (DailyLog)
    med_opt = body.medicationOption if body.medicationOption in {"SI", "NO", "NO_APLICA"} else "NO_APLICA"
    ingreso_log = IngresoLogData(
        time=_hora_24(body.entryTime),
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
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> AttendanceResponse:
    """Registra la entrega/egreso formal del alumno y actualiza su estado de asistencia."""
    alumno = exigir_alumno_visible(quien, body.childId, db)
    hora = _hora_12(body.exitTime)

    record_id = f"att_{uuid.uuid4().hex[:12]}"
    nuevo_registro = AttendanceRecord(
        id=record_id,
        child_id=body.childId,
        is_ingreso=False,
        date=body.date,
        time=hora,
        recorded_by_id=quien.id,
        recorded_by_name=quien.display_name or quien.email,
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
    alumno.status_text = f"{hora} • Retirado"

    # Sincronización con la bitácora oficial (DailyLog)
    egreso_log = EgresoLogData(
        time=_hora_24(body.exitTime),
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
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[AttendanceRecordOut]:
    """Consulta el historial de registros de asistencia filtrado por alumno y fecha.

    RF-07: una familia solo recibe la asistencia de sus propios hijos (antes recibía la de todos).
    """
    query = select(AttendanceRecord).order_by(AttendanceRecord.recorded_at.desc())
    visibles = ids_visibles(quien, db)
    if visibles is not None:
        query = query.where(AttendanceRecord.child_id.in_(visibles))
    if childId:
        exigir_alumno_visible(quien, childId, db)
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
