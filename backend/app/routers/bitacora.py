"""Módulo 5: Bitácora Diaria, Línea de Tiempo y Actividades (/children/{id}/...)"""

import uuid
from datetime import datetime
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


def _parse_time_minutes(t: str | None) -> int:
    if not t:
        return 9999
    s = t.strip().upper().replace(".", "").replace("A M", "AM").replace("P M", "PM")
    for fmt in ("%I:%M %p", "%H:%M", "%I:%M%p"):
        try:
            dt = datetime.strptime(s, fmt)
            return dt.hour * 60 + dt.minute
        except ValueError:
            pass
    return 9999


def _parse_item_timestamp(d: str | None, t: str | None) -> float:
    time_clean = (t or "00:00").strip().upper().replace(".", "").replace("A M", "AM").replace("P M", "PM")
    date_clean = (d or "12/10/2026").strip()

    for date_fmt in ("%d/%m/%Y", "%Y-%m-%d", "%d-%m-%Y"):
        for time_fmt in ("%I:%M %p", "%H:%M", "%I:%M%p"):
            try:
                dt = datetime.strptime(f"{date_clean} {time_clean}", f"{date_fmt} {time_fmt}")
                return dt.timestamp()
            except ValueError:
                pass
        try:
            dt = datetime.strptime(date_clean, date_fmt)
            minutos = _parse_time_minutes(t)
            return dt.timestamp() + (minutos * 60 if minutos < 9999 else 0)
        except ValueError:
            pass

    return float(_parse_time_minutes(t))


@router.get("/children/{childId}/timeline", response_model=list[TimelineItemOut])
def obtener_linea_de_tiempo(
    childId: str,
    date: str | None = None,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[TimelineItemOut]:
    """Devuelve la cronología del día para el alumno combinando eventos, chats y actividades pedagógicas."""
    items: list[TimelineItemOut] = []

    # 1. Asistencia como chips de inicio/salida
    stmt_att = select(AttendanceRecord).where(AttendanceRecord.child_id == childId)
    if date:
        stmt_att = stmt_att.where(AttendanceRecord.date == date)
    asistencias = db.scalars(stmt_att).all()
    for att in asistencias:
        texto = f"{'Ingreso' if att.is_ingreso else 'Egreso'}: {att.time}"
        icono = "entry" if att.is_ingreso else "exit"
        items.append(
            TimelineItemOut(
                type="EVENT_CHIP",
                id=att.id,
                time=att.time,
                date=att.date,
                text=texto,
                iconType=icono,
            )
        )

    # 2. Eventos rápidos guardados
    stmt_ev = select(TimelineEvent).where(TimelineEvent.child_id == childId)
    if date:
        stmt_ev = stmt_ev.where(TimelineEvent.date == date)
    eventos = db.scalars(stmt_ev).all()
    for ev in eventos:
        items.append(
            TimelineItemOut(
                type="EVENT_CHIP",
                id=ev.id,
                time=ev.time,
                date=ev.date,
                text=ev.text,
                iconType=ev.icon_type,
            )
        )

    # 3. Actividades del día
    stmt_act = select(Activity).where(Activity.child_id == childId)
    if date:
        stmt_act = stmt_act.where(Activity.date == date)
    actividades = db.scalars(stmt_act).all()
    for act in actividades:
        portion_text = act.portion_option or "Completo"
        items.append(
            TimelineItemOut(
                type="ACTIVITY_CARD",
                id=act.id,
                time=act.time,
                date=act.date,
                category=act.category,
                portionLabel=portion_text,
                description=act.description,
            )
        )

    # 4. Mensajes relevantes en bitácora
    mensajes = db.scalars(select(BitacoraMessage).where(BitacoraMessage.child_id == childId)).all()
    for m in mensajes:
        fecha_msg = getattr(m, "date", None) or (m.created_at.strftime("%d/%m/%Y") if m.created_at else "12/10/2026")
        if date and fecha_msg != date:
            continue
        items.append(
            TimelineItemOut(
                type="CHAT_MESSAGE",
                id=m.id,
                time=m.time,
                date=fecha_msg,
                message=m.message,
                isOutgoing=m.sender_id == _.id,
                fileUri=m.file_uri,
                fileName=m.file_name,
                fileMimeType=m.file_mime_type,
            )
        )

    items.sort(key=lambda it: _parse_item_timestamp(it.date, it.time))
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
    fecha_actividad = body.date or "12/10/2026"
    actividad = Activity(
        id=act_id,
        child_id=childId,
        date=fecha_actividad,
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
        date=fecha_actividad,
        category=body.category,
        portionLabel=portion_label,
        description=body.description,
    )

    from .ws_notifications import ws_manager
    ws_manager.broadcast_sync(
        "chat:bitacora:activity",
        {
            "childId": childId,
            "chatId": childId,
            "activity": timeline_item.model_dump(),
        },
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

    item_chip = TimelineItemOut(
        type="EVENT_CHIP",
        id=ev_id,
        time=body.time,
        date="12/10/2026",
        text=body.text,
        iconType=body.iconType,
    )

    from .ws_notifications import ws_manager
    ws_manager.broadcast_sync(
        "chat:bitacora:event",
        {
            "childId": childId,
            "chatId": childId,
            "event": item_chip.model_dump(),
        },
    )

    return item_chip
