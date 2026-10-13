"""Módulo 5: Bitácora Diaria, Línea de Tiempo y Actividades (/children/{id}/...)"""

import uuid
from typing import Any

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import Activity, AttendanceRecord, BitacoraMessage, Child, TimelineEvent
from ..schemas import (
    BitacoraDaySummary,
    NewActivityRequest,
    NewActivityResponse,
    QuickEventRequest,
    TimelineItemOut,
)

router = APIRouter(tags=["bitacora"])


@router.get("/children/{childId}/timeline", response_model=list[TimelineItemOut])
def obtener_linea_de_tiempo(
    childId: str,
    date: str = "12/10/2026",
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[TimelineItemOut]:
    """Devuelve la cronología del día para el alumno combinando eventos, chats y actividades pedagógicas."""
    items: list[TimelineItemOut] = []

    # 1. Asistencia como chips de inicio/salida
    asistencias = db.scalars(
        select(AttendanceRecord).where(
            AttendanceRecord.child_id == childId,
            AttendanceRecord.date == date,
        )
    ).all()
    for att in asistencias:
        texto = f"{'Ingreso' if att.is_ingreso else 'Egreso'}: {att.time}"
        icono = "entry" if att.is_ingreso else "exit"
        items.append(
            TimelineItemOut(
                type="EVENT_CHIP",
                id=att.id,
                time=att.time,
                text=texto,
                iconType=icono,
            )
        )

    # 2. Eventos rápidos guardados
    eventos = db.scalars(
        select(TimelineEvent).where(
            TimelineEvent.child_id == childId,
            TimelineEvent.date == date,
        )
    ).all()
    for ev in eventos:
        items.append(
            TimelineItemOut(
                type="EVENT_CHIP",
                id=ev.id,
                time=ev.time,
                text=ev.text,
                iconType=ev.icon_type,
            )
        )

    # 3. Actividades del día
    actividades = db.scalars(
        select(Activity).where(
            Activity.child_id == childId,
            Activity.date == date,
        )
    ).all()
    for act in actividades:
        portion_text = act.portion_option or "Completo"
        items.append(
            TimelineItemOut(
                type="ACTIVITY_CARD",
                id=act.id,
                time=act.time,
                category=act.category,
                portionLabel=portion_text,
                description=act.description,
            )
        )

    # 4. Mensajes relevantes en bitácora
    mensajes = db.scalars(select(BitacoraMessage).where(BitacoraMessage.child_id == childId)).all()
    for m in mensajes:
        items.append(
            TimelineItemOut(
                type="CHAT_MESSAGE",
                id=m.id,
                time=m.time,
                message=m.message,
                isOutgoing=m.sender_id == _.id,
                fileUri=m.file_uri,
                fileName=m.file_name,
                fileMimeType=m.file_mime_type,
            )
        )

    return items


@router.get("/children/{childId}/bitacoras", response_model=list[BitacoraDaySummary])
def historial_bitacoras(
    childId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[BitacoraDaySummary]:
    """Obtiene el historial de bitácoras por fecha agrupando ingreso, egreso y actividades."""
    actividades = db.scalars(select(Activity).where(Activity.child_id == childId)).all()
    actividades_dict: list[dict[str, Any]] = [
        {
            "id": a.id,
            "time": a.time,
            "title": a.title,
            "category": a.category,
            "portion": a.portion_option or "MEDIO",
            "description": a.description,
        }
        for a in actividades
    ]

    return [
        BitacoraDaySummary(
            date="12/10/2026",
            dayLabel="Hoy, 12 Oct",
            isToday=True,
            ingreso={
                "time": "08:15 AM",
                "delivererName": "María López (Madre)",
                "receiverName": "Laura Gómez (Educadora)",
                "physicalCondition": "Buen estado general.",
                "hasSymptoms": False,
                "symptomsDetail": "",
                "observations": "Llegó contento y descansado.",
            },
            egreso=None,
            activities=actividades_dict,
        )
    ]


@router.post("/children/{childId}/activities", response_model=NewActivityResponse, status_code=201)
def registrar_actividad(
    childId: str,
    body: NewActivityRequest,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> NewActivityResponse:
    """Registra una nueva actividad realizada por el menor (alimentación, siesta, etc.)."""
    alumno = db.get(Child, childId)
    if alumno is None:
        raise ApiError(404, f"No existe el alumno con id {childId}")

    act_id = f"act_{uuid.uuid4().hex[:8]}"
    actividad = Activity(
        id=act_id,
        child_id=childId,
        date="12/10/2026",
        time=body.time,
        category=body.category,
        meal_type=body.mealType,
        consumption_option=body.consumptionOption,
        portion_option=body.portionOption,
        title=body.title,
        description=body.description,
        nap_minutes=body.napMinutes,
        mood_state=body.moodState,
        excretory_function=body.excretoryFunction,
    )
    db.add(actividad)
    db.commit()

    portion_label = body.portionOption or "Completo"
    timeline_item = TimelineItemOut(
        type="ACTIVITY_CARD",
        id=act_id,
        time=body.time,
        category=body.category,
        portionLabel=portion_label,
        description=body.description,
    )

    return NewActivityResponse(id=act_id, status="CREADA", timelineItem=timeline_item)


@router.post("/children/{childId}/timeline/events", response_model=TimelineItemOut, status_code=201)
def registrar_evento_rapido(
    childId: str,
    body: QuickEventRequest,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> TimelineItemOut:
    """Registra un evento rápido (chip) en la línea de tiempo del menor."""
    ev_id = f"chip_{uuid.uuid4().hex[:8]}"
    evento = TimelineEvent(
        id=ev_id,
        child_id=childId,
        date="12/10/2026",
        time=body.time,
        text=body.text,
        icon_type=body.iconType,
    )
    db.add(evento)
    db.commit()

    return TimelineItemOut(
        type="EVENT_CHIP",
        id=ev_id,
        time=body.time,
        text=body.text,
        iconType=body.iconType,
    )
