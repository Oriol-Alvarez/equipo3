"""Módulo 2: Salas y Aulas Escolares (/rooms)"""

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora
from ..errors import ApiError
from ..models import Child, Room, RoomEducator, Usuario
from ..schemas import ChildOut, EducatorItem, RoomDetailOut, RoomSummaryOut

router = APIRouter(prefix="/rooms", tags=["rooms"])


@router.get("", response_model=list[RoomSummaryOut])
def listar_salas(
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[RoomSummaryOut]:
    """Lista las salas que la educadora o dirección tienen a su cargo con los contadores de asistencia."""
    salas = db.scalars(select(Room)).all()
    resultado = []
    for s in salas:
        total = db.scalar(select(func.count()).select_from(Child).where(Child.room_id == s.id)) or 0
        presentes = db.scalar(select(func.count()).select_from(Child).where(Child.room_id == s.id, Child.is_present.is_(True))) or 0
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

    total = db.scalar(select(func.count()).select_from(Child).where(Child.room_id == sala.id)) or 0
    presentes = db.scalar(select(func.count()).select_from(Child).where(Child.room_id == sala.id, Child.is_present.is_(True))) or 0

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


from .children import mapear_child_out


@router.get("/{roomId}/children", response_model=list[ChildOut])
def listar_alumnos_de_sala(
    roomId: str,
    isPresent: bool | None = None,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[ChildOut]:
    """Lista todos los alumnos asignados a una sala, con filtro opcional de asistencia."""
    query = select(Child).where(Child.room_id == roomId)
    if isPresent is not None:
        query = query.where(Child.is_present == isPresent)

    alumnos = db.scalars(query).all()
    return [mapear_child_out(db, a) for a in alumnos]
