"""Módulo 8: Chat Bitácora Individual (Educadora <-> Familia)"""

import time
import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import BitacoraMessage, Child, ChildParent, Usuario
from ..schemas import (
    BitacoraMessageOut,
    ParentChatSummaryOut,
    SendBitacoraMessageRequest,
)

router = APIRouter(tags=["parent-chats"])


@router.get("/rooms/{roomId}/parent-chats", response_model=list[ParentChatSummaryOut])
def listar_chats_familias_de_sala(
    roomId: str,
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[ParentChatSummaryOut]:
    """Bandeja de conversaciones individuales con las familias de los menores de una sala."""
    alumnos = db.scalars(select(Child).where(Child.room_id == roomId)).all()
    resultado = []
    for a in alumnos:
        # Buscar padre asociado
        cp = db.scalar(select(ChildParent).where(ChildParent.child_id == a.id))
        nombre_padre = "Familiar Tutor"
        if cp:
            tutor = db.get(Usuario, cp.parent_user_id)
            if tutor:
                nombre_padre = tutor.display_name

        # Buscar último mensaje
        ultimo = db.scalars(
            select(BitacoraMessage)
            .where(BitacoraMessage.child_id == a.id)
            .order_by(BitacoraMessage.created_at.desc())
        ).first()

        resultado.append(
            ParentChatSummaryOut(
                childId=a.id,
                childName=a.full_name,
                parentName=nombre_padre,
                lastMessage=ultimo.message if ultimo else "Sin mensajes aún.",
                lastMessageTime=ultimo.time if ultimo else "09:00 AM",
                unreadCount=0,
            )
        )
    return resultado


@router.get("/chats/bitacora/{childId}/messages", response_model=list[BitacoraMessageOut])
def obtener_mensajes_chat_bitacora(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[BitacoraMessageOut]:
    """Obtiene los mensajes del canal directo de comunicación diario sobre el menor."""
    mensajes = db.scalars(select(BitacoraMessage).where(BitacoraMessage.child_id == childId)).all()
    return [
        BitacoraMessageOut(
            id=m.id,
            senderId=m.sender_id,
            senderName=m.sender_name,
            time=m.time,
            message=m.message,
            isOutgoing=m.sender_id == quien.id,
            fileUri=m.file_uri,
        )
        for m in mensajes
    ]


@router.post("/chats/bitacora/{childId}/messages", response_model=BitacoraMessageOut, status_code=201)
def enviar_mensaje_chat_bitacora(
    childId: str,
    body: SendBitacoraMessageRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> BitacoraMessageOut:
    """Envía un mensaje o fotografía en el chat directo de bitácora."""
    alumno = db.get(Child, childId)
    if alumno is None:
        raise ApiError(404, f"No existe el alumno con id {childId}")

    hora_actual = body.time or time.strftime("%I:%M %p")
    msg_id = f"msg_{uuid.uuid4().hex[:8]}"
    nuevo_msg = BitacoraMessage(
        id=msg_id,
        child_id=childId,
        sender_id=quien.id,
        sender_name=quien.display_name,
        time=hora_actual,
        message=body.message,
        file_uri=body.fileUri,
        file_name=body.fileName,
        file_mime_type=body.fileMimeType,
    )
    db.add(nuevo_msg)
    db.commit()

    from .ws_notifications import ws_manager
    msg_dict = {
        "id": msg_id,
        "childId": childId,
        "chatId": childId,
        "senderId": quien.id,
        "senderName": quien.display_name,
        "time": hora_actual,
        "message": body.message,
        "fileUri": body.fileUri,
        "fileName": body.fileName,
        "fileMimeType": body.fileMimeType,
        "isOutgoing": False,
    }
    ws_manager.broadcast_sync("chat:bitacora:new", {"childId": childId, "chatId": childId, "message": msg_dict})

    return BitacoraMessageOut(
        id=msg_id,
        senderId=quien.id,
        senderName=quien.display_name,
        time=hora_actual,
        message=body.message,
        isOutgoing=True,
        fileUri=body.fileUri,
    )
