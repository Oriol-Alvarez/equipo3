"""Planeación y menú diario (/daily-plans).

La educadora publica (o reemplaza) la planeación de un día; las familias la consultan.
Las fechas van en formato ISO "YYYY-MM-DD".
"""

import json
from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import DailyPlan, Room
from ..schemas import DailyMenuData, DailyPlanOut, PlannedActivityData, SaveDailyPlanRequest

router = APIRouter(prefix="/daily-plans", tags=["daily-plans"])


def validar_fecha(texto: str) -> str:
    try:
        return date.fromisoformat(texto).isoformat()
    except ValueError:
        raise ApiError(422, f"Fecha no válida: {texto}", "fecha_invalida", 'Usa el formato "YYYY-MM-DD".')


def _a_salida(p: DailyPlan) -> DailyPlanOut:
    return DailyPlanOut(
        date=p.date,
        roomId=p.room_id,
        menu=DailyMenuData(breakfast=p.breakfast, snack=p.snack, lunch=p.lunch, notes=p.notes),
        activities=[PlannedActivityData(**a) for a in json.loads(p.activities or "[]")],
        updatedBy=p.updated_by,
    )


@router.get("", response_model=list[DailyPlanOut])
def listar_planeaciones(
    desde: str | None = Query(default=None, alias="from"),
    hasta: str | None = Query(default=None, alias="to"),
    roomId: str = Query(default="sala_1a"),
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[DailyPlanOut]:
    consulta = select(DailyPlan).where(DailyPlan.room_id == roomId).order_by(DailyPlan.date)
    if desde:
        consulta = consulta.where(DailyPlan.date >= validar_fecha(desde))
    if hasta:
        consulta = consulta.where(DailyPlan.date <= validar_fecha(hasta))
    return [_a_salida(p) for p in db.scalars(consulta).all()]


@router.get("/{fecha}", response_model=DailyPlanOut)
def obtener_planeacion(
    fecha: str,
    roomId: str = Query(default="sala_1a"),
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> DailyPlanOut:
    plan = db.get(DailyPlan, (roomId, validar_fecha(fecha)))
    if plan is None:
        raise ApiError(404, "Todavía no hay planeación publicada para ese día", "sin_planeacion")
    return _a_salida(plan)


@router.put("/{fecha}", response_model=DailyPlanOut)
def guardar_planeacion(
    fecha: str,
    body: SaveDailyPlanRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> DailyPlanOut:
    """Publica o reemplaza la planeación del día."""
    dia = validar_fecha(fecha)
    if db.get(Room, body.roomId) is None:
        raise ApiError(404, f"No existe la sala {body.roomId}", "sala_inexistente")

    actividades = sorted(body.activities, key=lambda a: a.time)
    plan = db.get(DailyPlan, (body.roomId, dia))
    if plan is None:
        try:
            with db.begin_nested():
                plan = DailyPlan(room_id=body.roomId, date=dia, breakfast="", snack="", lunch="", updated_by="")
                db.add(plan)
                db.flush()
        except IntegrityError:
            plan = db.get(DailyPlan, (body.roomId, dia), populate_existing=True)
    plan.breakfast = body.menu.breakfast.strip()
    plan.snack = body.menu.snack.strip()
    plan.lunch = body.menu.lunch.strip()
    plan.notes = body.menu.notes.strip()
    plan.activities = json.dumps([a.model_dump() for a in actividades], ensure_ascii=False)
    plan.updated_by = quien.display_name or "Educadora"
    db.commit()
    db.refresh(plan)
    return _a_salida(plan)
