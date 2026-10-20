"""Módulo 10: Avisos y Circulares Generales (/announcements)"""

import uuid

from fastapi import APIRouter, Depends, Response
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from ..db import get_db
from ..notificaciones_push import notificar, todas_las_familias
from ..horario import ahora
from ..deps import Identidad, requiere_direccion, usuario_actual
from ..errors import ApiError
from ..models import Announcement, AnnouncementRead
from .ws_notifications import ws_manager
from ..schemas import (
    AnnouncementOut,
    CreateAnnouncementRequest,
    ReadAnnouncementOut,
    UnreadCountOut,
    UpdateAnnouncementRequest,
)

router = APIRouter(prefix="/announcements", tags=["announcements"])


@router.get("", response_model=list[AnnouncementOut])
def listar_avisos(
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[AnnouncementOut]:
    """Lista las circulares y comunicados del centro escolar con indicador de no leído."""
    avisos = db.scalars(select(Announcement).order_by(Announcement.created_at.desc())).all()
    leidos_ids = set(
        db.scalars(
            select(AnnouncementRead.announcement_id).where(AnnouncementRead.user_id == quien.id)
        ).all()
    )

    return [
        AnnouncementOut(
            id=a.id,
            title=a.title,
            date=a.date,
            summary=a.summary,
            fullContent=a.full_content,
            isUnread=a.id not in leidos_ids,
            author=a.author,
        )
        for a in avisos
    ]


@router.get("/unread-count", response_model=UnreadCountOut)
def contador_no_leidos(
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> UnreadCountOut:
    """Devuelve la cantidad de comunicados pendientes de lectura para el usuario actual."""
    total = db.scalar(select(func.count()).select_from(Announcement)) or 0
    leidos = db.scalar(
        select(func.count()).select_from(AnnouncementRead).where(AnnouncementRead.user_id == quien.id)
    ) or 0
    no_leidos = max(0, total - leidos)
    return UnreadCountOut(unreadCount=no_leidos)


@router.patch("/{id}/read", response_model=ReadAnnouncementOut)
def marcar_como_leido(
    id: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> ReadAnnouncementOut:
    """Marca una circular como leída por el usuario autenticado."""
    aviso = db.get(Announcement, id)
    if aviso is None:
        raise ApiError(404, f"No existe el aviso con id {id}")

    ya_leido = db.scalar(
        select(AnnouncementRead).where(
            AnnouncementRead.announcement_id == id,
            AnnouncementRead.user_id == quien.id,
        )
    )
    if not ya_leido:
        db.add(AnnouncementRead(announcement_id=id, user_id=quien.id))
        db.commit()

    return ReadAnnouncementOut(id=id, isUnread=False)


@router.post("", response_model=AnnouncementOut, status_code=201)
def publicar_aviso(
    body: CreateAnnouncementRequest,
    quien: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AnnouncementOut:
    """Publica un nuevo aviso general o circular (restringido a DIRECCION)."""
    if not body.title.strip() or not body.fullContent.strip():
        raise ApiError(422, "El título y el contenido son obligatorios")

    nuevo_id = f"ann_{uuid.uuid4().hex[:8]}"
    fecha_texto = ahora().strftime("%d/%m/%Y %I:%M %p")
    aviso = Announcement(
        id=nuevo_id,
        title=body.title.strip(),
        date=fecha_texto,
        summary=body.summary.strip(),
        full_content=body.fullContent.strip(),
        author=body.author,
    )
    db.add(aviso)
    db.commit()
    notificar(db, todas_las_familias(db), f"Aviso: {aviso.title}", aviso.summary or aviso.full_content,
              {"tipo": "aviso", "id": aviso.id}, excepto=quien.id)

    ws_manager.broadcast_sync(
        "announcement:new",
        {
            "id": aviso.id,
            "title": aviso.title,
            "date": aviso.date,
            "summary": aviso.summary,
            "fullContent": aviso.full_content,
            "author": aviso.author,
        },
    )

    return AnnouncementOut(
        id=aviso.id,
        title=aviso.title,
        date=aviso.date,
        summary=aviso.summary,
        fullContent=aviso.full_content,
        isUnread=True,
        author=aviso.author,
    )


@router.put("/{id}", response_model=AnnouncementOut)
def modificar_aviso(
    id: str,
    body: UpdateAnnouncementRequest,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> AnnouncementOut:
    """Modifica un aviso o circular escolar existente (restringido a DIRECCION/ADMIN)."""
    aviso = db.get(Announcement, id)
    if aviso is None:
        raise ApiError(404, f"No existe el aviso con id {id}")

    if body.title is not None:
        aviso.title = body.title.strip()
    if body.summary is not None:
        aviso.summary = body.summary.strip()
    if body.fullContent is not None:
        aviso.full_content = body.fullContent.strip()
    if body.author is not None:
        aviso.author = body.author.strip()

    db.commit()
    db.refresh(aviso)

    return AnnouncementOut(
        id=aviso.id,
        title=aviso.title,
        date=aviso.date,
        summary=aviso.summary,
        fullContent=aviso.full_content,
        isUnread=False,
        author=aviso.author,
    )


@router.delete("/{id}", status_code=204)
def eliminar_aviso(
    id: str,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> Response:
    """Elimina una circular o comunicado escolar (restringido a DIRECCION/ADMIN)."""
    aviso = db.get(Announcement, id)
    if aviso is None:
        raise ApiError(404, f"No existe el aviso con id {id}")

    db.delete(aviso)
    db.commit()
    return Response(status_code=204)

