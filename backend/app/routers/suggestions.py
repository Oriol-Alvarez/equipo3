"""Módulo 11: Buzón de Sugerencias y Quejas (/suggestions)"""

import time
import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora, requiere_familiar, usuario_actual
from ..errors import ApiError
from ..models import Child, Suggestion
from ..schemas import (
    CreateSuggestionRequest,
    SuggestionOut,
    UpdateSuggestionStatusRequest,
)

router = APIRouter(prefix="/suggestions", tags=["suggestions"])


@router.get("", response_model=list[SuggestionOut])
def listar_sugerencias(
    status: str | None = None,
    category: str | None = None,
    search: str | None = None,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[SuggestionOut]:
    """Lista las sugerencias ciudadanas y familiares con soporte para filtros por estado y categoría."""
    query = select(Suggestion).order_by(Suggestion.created_at.desc())
    if status:
        query = query.where(Suggestion.status == status)
    if category:
        query = query.where(Suggestion.category == category)
    if search:
        termino = f"%{search.strip().lower()}%"
        query = query.where(
            Suggestion.subject.ilike(termino) | Suggestion.content.ilike(termino)
        )

    filas = db.scalars(query).all()
    return [
        SuggestionOut(
            id=s.id,
            parentName=s.parent_name,
            childName=s.child_name,
            roomName=s.room_name,
            date=s.date,
            time=s.time,
            category=s.category,
            subject=s.subject,
            content=s.content,
            status=s.status,
            response=s.response,
            responseDate=s.response_date,
            responderName=s.responder_name,
        )
        for s in filas
    ]


@router.post("", response_model=SuggestionOut, status_code=201)
def crear_sugerencia(
    body: CreateSuggestionRequest,
    quien: Identidad = Depends(requiere_familiar),
    db: Session = Depends(get_db),
) -> SuggestionOut:
    """Envía una nueva sugerencia o queja por parte de la familia del menor."""
    alumno = db.get(Child, body.childId)
    nombre_nino = alumno.full_name if alumno else "Alumno"

    sug_id = f"sug_{uuid.uuid4().hex[:8]}"
    fecha_hoy = time.strftime("%d/%m/%Y")
    hora_hoy = time.strftime("%I:%M %p")

    sugerencia = Suggestion(
        id=sug_id,
        parent_user_id=quien.id,
        parent_name=quien.display_name,
        child_id=body.childId,
        child_name=nombre_nino,
        room_name=body.roomName,
        date=fecha_hoy,
        time=hora_hoy,
        category=body.category,
        subject=body.subject,
        content=body.content,
        status="PENDIENTE",
    )
    db.add(sugerencia)
    db.commit()

    return SuggestionOut(
        id=sugerencia.id,
        parentName=sugerencia.parent_name,
        childName=sugerencia.child_name,
        roomName=sugerencia.room_name,
        date=sugerencia.date,
        time=sugerencia.time,
        category=sugerencia.category,
        subject=sugerencia.subject,
        content=sugerencia.content,
        status=sugerencia.status,
        response=None,
        responseDate=None,
        responderName=None,
    )


@router.patch("/{id}/status", response_model=SuggestionOut)
def actualizar_estado_sugerencia(
    id: str,
    body: UpdateSuggestionStatusRequest,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> SuggestionOut:
    """Responde y actualiza el estado de resolución de una sugerencia (ATENDIDA / EN_REVISION)."""
    sug = db.get(Suggestion, id)
    if sug is None:
        raise ApiError(404, f"No existe la sugerencia con id {id}")

    sug.status = body.status
    sug.response = body.response
    sug.responder_name = body.responderName
    sug.response_date = time.strftime("%d/%m/%Y %I:%M %p")
    db.commit()

    return SuggestionOut(
        id=sug.id,
        parentName=sug.parent_name,
        childName=sug.child_name,
        roomName=sug.room_name,
        date=sug.date,
        time=sug.time,
        category=sug.category,
        subject=sug.subject,
        content=sug.content,
        status=sug.status,
        response=sug.response,
        responseDate=sug.response_date,
        responderName=sug.responder_name,
    )
