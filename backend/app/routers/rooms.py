"""Módulo 2: Salas y Aulas Escolares (/rooms)"""

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora
from ..errors import ApiError
from ..fichas import fichas
from ..models import Child, Room, RoomEducator, Usuario
from ..schemas import ChildOut, EducatorItem, RoomDetailOut, RoomSummaryOut

router = APIRouter(prefix="/rooms", tags=["rooms"])


def _contadores(room_id: str, db: Session) -> tuple[int, int]:
    """Total de alumnos de la sala y cuántos están presentes HOY según su registro diario."""
    alumnos = db.scalars(select(Child).where(Child.room_id == room_id)).all()
    return len(alumnos), sum(1 for f in fichas(alumnos, db) if f.isPresent)


@router.get("", response_model=list[RoomSummaryOut])
def listar_salas(
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[RoomSummaryOut]:
    """Lista las salas que la educadora o dirección tienen a su cargo con los contadores de asistencia."""
    salas = db.scalars(select(Room).order_by(Room.name)).all()
    resultado = []
    for s in salas:
        total, presentes = _contadores(s.id, db)
        resultado.append(
            RoomSummaryOut(
                id=s.id,
                name=s.name,
                level=s.level,
                presentCount=presentes,
                totalCount=total,
            )
        )
    return resultado


@router.get("/{roomId}", response_model=RoomDetailOut)
def obtener_sala(
    roomId: str,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> RoomDetailOut:
    """Devuelve la información detallada de una sala y sus educadoras asignadas."""
    sala = db.get(Room, roomId)
    if sala is None:
        raise ApiError(404, f"No existe la sala con id {roomId}", "sala_no_encontrada")

    total, presentes = _contadores(sala.id, db)

    educadores_filas = db.scalars(select(RoomEducator).where(RoomEducator.room_id == sala.id)).all()
    educadores = []
    for ef in educadores_filas:
        u = db.get(Usuario, ef.educator_id)
        if u:
            educadores.append(EducatorItem(id=u.id, name=u.display_name, role=ef.role_title))

    return RoomDetailOut(
        id=sala.id,
        name=sala.name,
        level=sala.level,
        capacity=sala.capacity,
        presentCount=presentes,
        totalCount=total,
        educators=educadores,
    )



@router.get("/{roomId}/children", response_model=list[ChildOut])
def listar_alumnos_de_sala(
    roomId: str,
    isPresent: bool | None = None,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[ChildOut]:
    """Lista todos los alumnos asignados a una sala, con filtro opcional de asistencia."""
    if db.get(Room, roomId) is None:
        raise ApiError(404, f"No existe la sala con id {roomId}", "sala_no_encontrada")
    alumnos = db.scalars(select(Child).where(Child.room_id == roomId).order_by(Child.full_name)).all()
    resultado = fichas(alumnos, db)
    if isPresent is not None:
        resultado = [f for f in resultado if f.isPresent == isPresent]
    return resultado
