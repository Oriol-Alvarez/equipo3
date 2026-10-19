"""Módulo 12: Catálogo de Objetos Perdidos (/lost-objects)"""

import uuid

from fastapi import APIRouter, Depends, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import LostObject
from ..schemas import (
    ClaimLostObjectOut,
    CreateLostObjectRequest,
    LostObjectOut,
)

router = APIRouter(prefix="/lost-objects", tags=["lost-objects"])


@router.get("", response_model=list[LostObjectOut])
def listar_objetos_perdidos(
    category: str | None = None,
    isClaimed: bool | None = None,
    search: str | None = None,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[LostObjectOut]:
    """Lista los objetos extraviados encontrados en el centro infantil."""
    query = select(LostObject).order_by(LostObject.created_at.desc())
    if category and category.lower() != "todos":
        query = query.where(LostObject.category.ilike(category))
    if isClaimed is not None:
        query = query.where(LostObject.is_claimed == isClaimed)
    if search:
        termino = f"%{search.strip().lower()}%"
        query = query.where(LostObject.title.ilike(termino) | LostObject.description.ilike(termino))

    filas = db.scalars(query).all()
    return [
        LostObjectOut(
            id=o.id,
            title=o.title,
            description=o.description,
            category=o.category,
            location=o.location,
            imageUri=o.image_uri,
            room=o.room,
            isClaimed=o.is_claimed,
        )
        for o in filas
    ]


@router.post("", response_model=LostObjectOut, status_code=201)
def publicar_objeto_perdido(
    body: CreateLostObjectRequest,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> LostObjectOut:
    """Registra un objeto perdido o encontrado. Lo puede reportar el personal o una familia."""
    if not body.title.strip() or not body.description.strip():
        raise ApiError(422, "El título y la descripción del objeto son obligatorios")

    obj_id = f"lost_{uuid.uuid4().hex[:8]}"
    objeto = LostObject(
        id=obj_id,
        title=body.title.strip(),
        description=body.description.strip(),
        category=body.category,
        location=body.location,
        image_uri=body.imageUri,
        room=body.room,
        is_claimed=False,
    )
    db.add(objeto)
    db.commit()

    return LostObjectOut(
        id=objeto.id,
        title=objeto.title,
        description=objeto.description,
        category=objeto.category,
        location=objeto.location,
        imageUri=objeto.image_uri,
        room=objeto.room,
        isClaimed=objeto.is_claimed,
    )


@router.patch("/{id}/claim", response_model=ClaimLostObjectOut)
def reclamar_objeto_perdido(
    id: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ClaimLostObjectOut:
    """Marca un objeto como reclamado por parte de la familia o personal."""
    objeto = db.get(LostObject, id)
    if objeto is None:
        raise ApiError(404, f"No existe el objeto con id {id}")

    objeto.is_claimed = True
    db.commit()
    return ClaimLostObjectOut(id=id, isClaimed=True)


@router.delete("/{id}", status_code=204)
def eliminar_objeto_perdido(
    id: str,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> Response:
    """Elimina definitivamente un objeto cuando ha sido entregado en persona en la estancia."""
    objeto = db.get(LostObject, id)
    if objeto is None:
        raise ApiError(404, f"No existe el objeto con id {id}")

    db.delete(objeto)
    db.commit()
    return Response(status_code=204)
