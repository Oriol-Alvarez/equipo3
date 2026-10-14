"""Registro diario por niño (/daily-logs): fuente de la hoja oficial "Bitácora de filtro diario".

Cada día de cada niño tiene tres apartados (ingreso, estadía y egreso) que se guardan por separado,
igual que en la app. Solo el personal escribe; la familia puede consultar los de sus hijos.
"""

import json
from datetime import date, timedelta

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..acceso import exigir_alumno_visible, ids_visibles
from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..horario import hoy
from ..models import DailyLog
from ..schemas import DailyLogOut, EgresoLogData, EstadiaLogData, IngresoLogData
from .daily_plans import validar_fecha

router = APIRouter(tags=["daily-logs"])

# Opciones válidas (mismos nombres que los enums de la app)
COMIDA = {"NADA", "POCO", "TODO", "PIDIO_MAS"}
ESFINTERES = {"NO_APLICA", "AVISO_SI", "AVISO_NO"}
ANIMO = {"FELIZ", "TRISTE", "ENOJADO"}
MEDICAMENTO = {"SI", "NO", "NO_APLICA"}


def _cargar(texto: str | None, modelo):
    return modelo.model_validate(json.loads(texto)) if texto else None


def _a_salida(r: DailyLog) -> DailyLogOut:
    return DailyLogOut(
        childId=r.child_id,
        date=r.date,
        ingreso=_cargar(r.ingreso, IngresoLogData),
        estadia=_cargar(r.estadia, EstadiaLogData),
        egreso=_cargar(r.egreso, EgresoLogData),
    )


def _opcion(valor: str | None, validas: set[str], campo: str) -> None:
    if valor is not None and valor not in validas:
        raise ApiError(422, f"Valor no válido en {campo}: {valor}", "opcion_invalida", f"Opciones: {sorted(validas)}")


def _registro(child_id: str, dia: str, db: Session) -> DailyLog:
    r = db.get(DailyLog, (child_id, dia))
    if r is None:
        r = DailyLog(child_id=child_id, date=dia)
        db.add(r)
    return r


@router.get("/daily-logs", response_model=list[DailyLogOut])
def listar_registros(
    desde: str | None = Query(default=None, alias="from"),
    hasta: str | None = Query(default=None, alias="to"),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[DailyLogOut]:
    """Registros de todos los niños visibles para el usuario (por defecto, las últimas 4 semanas)."""
    fin = validar_fecha(hasta) if hasta else hoy().isoformat()
    inicio = validar_fecha(desde) if desde else (date.fromisoformat(fin) - timedelta(days=28)).isoformat()
    consulta = (
        select(DailyLog)
        .where(DailyLog.date >= inicio, DailyLog.date <= fin)
        .order_by(DailyLog.date, DailyLog.child_id)
    )
    visibles = ids_visibles(quien, db)
    if visibles is not None:
        if not visibles:
            return []
        consulta = consulta.where(DailyLog.child_id.in_(visibles))
    return [_a_salida(r) for r in db.scalars(consulta).all()]


@router.get("/children/{childId}/daily-logs", response_model=list[DailyLogOut])
def registros_de_alumno(
    childId: str,
    desde: str | None = Query(default=None, alias="from"),
    hasta: str | None = Query(default=None, alias="to"),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[DailyLogOut]:
    exigir_alumno_visible(quien, childId, db)
    consulta = select(DailyLog).where(DailyLog.child_id == childId).order_by(DailyLog.date)
    if desde:
        consulta = consulta.where(DailyLog.date >= validar_fecha(desde))
    if hasta:
        consulta = consulta.where(DailyLog.date <= validar_fecha(hasta))
    return [_a_salida(r) for r in db.scalars(consulta).all()]


@router.put("/children/{childId}/daily-logs/{fecha}/ingreso", response_model=DailyLogOut)
def guardar_ingreso(
    childId: str,
    fecha: str,
    body: IngresoLogData,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> DailyLogOut:
    exigir_alumno_visible(quien, childId, db)
    _opcion(body.medicationWithPrescription, MEDICAMENTO, "medicationWithPrescription")
    r = _registro(childId, validar_fecha(fecha), db)
    r.ingreso = body.model_dump_json()
    db.commit()
    db.refresh(r)
    return _a_salida(r)


@router.put("/children/{childId}/daily-logs/{fecha}/estadia", response_model=DailyLogOut)
def guardar_estadia(
    childId: str,
    fecha: str,
    body: EstadiaLogData,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> DailyLogOut:
    exigir_alumno_visible(quien, childId, db)
    for campo in ("breakfast", "snack", "lunch"):
        _opcion(getattr(body, campo), COMIDA, campo)
    _opcion(body.sphincterControl, ESFINTERES, "sphincterControl")
    _opcion(body.mood, ANIMO, "mood")
    r = _registro(childId, validar_fecha(fecha), db)
    r.estadia = body.model_dump_json()
    db.commit()
    db.refresh(r)
    return _a_salida(r)


@router.put("/children/{childId}/daily-logs/{fecha}/egreso", response_model=DailyLogOut)
def guardar_egreso(
    childId: str,
    fecha: str,
    body: EgresoLogData,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> DailyLogOut:
    exigir_alumno_visible(quien, childId, db)
    dia = validar_fecha(fecha)
    r = _registro(childId, dia, db)
    if r.ingreso:
        entrada = json.loads(r.ingreso).get("time")
        if entrada and body.time < entrada:
            raise ApiError(
                422,
                f"La hora de salida ({body.time}) no puede ser antes de la entrada ({entrada}).",
                "salida_antes_de_entrada",
            )
    r.egreso = body.model_dump_json()
    db.commit()
    db.refresh(r)
    return _a_salida(r)
