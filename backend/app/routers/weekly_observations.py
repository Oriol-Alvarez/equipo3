"""Observaciones semanales (/weekly-observations y /children/{id}/weekly-observations).

La familia y la educadora agregan comentarios al resumen semanal de un niño.
El rol del autor sale del token (no lo manda la app), así una familia no puede firmar como educadora.
"""

import uuid
from ..lecturas import momento_utc

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..acceso import es_personal, exigir_alumno_visible, ids_visibles
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..horario import fecha_y_hora_corta
from ..models import WeeklyObservation
from ..schemas import (
    WEEKLY_OBSERVATION_MAX_LENGTH,
    CreateWeeklyObservationRequest,
    WeeklyObservationOut,
)

router = APIRouter(tags=["weekly-observations"])


def _a_salida(o: WeeklyObservation) -> WeeklyObservationOut:
    return WeeklyObservationOut(
        id=o.id,
        childId=o.child_id,
        weekKey=o.week_key,
        authorRole=o.author_role,
        authorName=o.author_name,
        text=o.text,
        time=o.time,
    )


@router.get("/weekly-observations", response_model=list[WeeklyObservationOut])
def listar_observaciones(
    weekKey: str | None = Query(default=None),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[WeeklyObservationOut]:
    """Todas las observaciones que el usuario puede ver (personal: todas; familia: las de sus hijos)."""
    consulta = select(WeeklyObservation).order_by(WeeklyObservation.created_at, WeeklyObservation.id)
    visibles = ids_visibles(quien, db)
    if visibles is not None:
        if not visibles:
            return []
        consulta = consulta.where(WeeklyObservation.child_id.in_(visibles))
    if weekKey:
        consulta = consulta.where(WeeklyObservation.week_key == weekKey)
    return [_a_salida(o) for o in db.scalars(consulta).all()]


@router.get("/children/{childId}/weekly-observations", response_model=list[WeeklyObservationOut])
def observaciones_de_alumno(
    childId: str,
    weekKey: str | None = Query(default=None),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[WeeklyObservationOut]:
    exigir_alumno_visible(quien, childId, db)
    consulta = (
        select(WeeklyObservation)
        .where(WeeklyObservation.child_id == childId)
        .order_by(WeeklyObservation.created_at, WeeklyObservation.id)
    )
    if weekKey:
        consulta = consulta.where(WeeklyObservation.week_key == weekKey)
    return [_a_salida(o) for o in db.scalars(consulta).all()]


@router.post("/children/{childId}/weekly-observations", response_model=WeeklyObservationOut, status_code=201)
def agregar_observacion(
    childId: str,
    body: CreateWeeklyObservationRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> WeeklyObservationOut:
    exigir_alumno_visible(quien, childId, db)

    texto = body.text.strip()
    if not texto:
        raise ApiError(422, "Escribe una observación antes de guardarla.", "observacion_vacia")
    if len(texto) > WEEKLY_OBSERVATION_MAX_LENGTH:
        raise ApiError(
            422,
            f"La observación no puede pasar de {WEEKLY_OBSERVATION_MAX_LENGTH} caracteres.",
            "observacion_larga",
        )

    obs = WeeklyObservation(
        id=f"wobs_{uuid.uuid4().hex[:10]}",
        child_id=childId,
        week_key=body.weekKey.strip(),
        author_role="EDUCADORA" if es_personal(quien) else "FAMILIA",
        author_user_id=quien.id,
        author_name=quien.display_name or ("Educadora" if es_personal(quien) else "Familia"),
        text=texto,
        time=fecha_y_hora_corta(),
        created_at=momento_utc(),
    )
    db.add(obs)
    db.commit()
    db.refresh(obs)
    return _a_salida(obs)
