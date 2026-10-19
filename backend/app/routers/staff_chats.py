"""Módulo 7: Mensajería de Personal y Chat Global (/staff, /chats)"""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from ..db import get_db
from ..horario import ahora
from ..lecturas import clave_personal, marcar_leido, momento_utc, no_leidos_personal
from ..deps import Identidad, requiere_educadora
from ..errors import ApiError
from ..models import StaffMember, WorkerChat, WorkerChatMessage
from ..schemas import (
    CreateWorkerGroupRequest,
    SendGlobalMessageRequest,
    SendWorkerMessageRequest,
    StaffGlobalChatOut,
    StaffMemberOut,
    WorkerChatMessageOut,
    WorkerChatOut,
)

router = APIRouter(tags=["staff-chats"])


@router.get("/staff/members", response_model=list[StaffMemberOut])
def listar_personal(
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[StaffMemberOut]:
    """Lista todos los miembros del personal educativo y directivo de la institución."""
    miembros = db.scalars(select(StaffMember)).all()
    return [
        StaffMemberOut(
            id=m.id,
            name=m.name,
            role=m.role,
            avatarInitials=m.avatar_initials,
            avatarBgColor=m.avatar_bg_color,
            isOnline=m.is_online,
        )
        for m in miembros
    ]


@router.get("/chats/workers", response_model=list[WorkerChatOut])
def listar_chats_trabajadores(
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[WorkerChatOut]:
    """Lista las conversaciones activas entre educadoras y dirección."""
    chats = db.scalars(select(WorkerChat)).all()
    return [
        WorkerChatOut(
            id=c.id,
            title=c.title,
            subtitle=c.subtitle,
            lastMessage=c.last_message,
            lastMessageTime=c.last_message_time,
            unreadCount=no_leidos_personal(db, quien.id, c.id),
            isGlobal=c.is_global,
            isGroup=c.is_group,
            avatarInitials=c.avatar_initials,
            avatarBgColor=c.avatar_bg_color,
        )
        for c in chats
    ]


@router.post("/chats/workers", response_model=WorkerChatOut, status_code=201)
def crear_grupo_personal(
    body: CreateWorkerGroupRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> WorkerChatOut:
    """Crea un grupo de conversación del personal (lo ve todo el personal)."""
    titulo = body.title.strip()
    if not titulo:
        raise ApiError(422, "Escribe un nombre para el grupo", "titulo_vacio")
    miembros = len(set(body.memberIds) | {quien.id})
    chat = WorkerChat(
        id=f"wgroup_{uuid.uuid4().hex[:10]}",
        title=titulo,
        subtitle=f"{miembros} miembros • Grupo de personal",
        is_global=False,
        is_group=True,
        avatar_initials=titulo[:2].upper(),
        avatar_bg_color=0xFF00897B,
        last_message=f"Grupo creado por {quien.display_name}",
        last_message_time=ahora().strftime("%I:%M %p"),
    )
    db.add(chat)
    db.commit()
    return WorkerChatOut(
        id=chat.id,
        title=chat.title,
        subtitle=chat.subtitle,
        lastMessage=chat.last_message,
        lastMessageTime=chat.last_message_time,
        unreadCount=0,
        isGlobal=False,
        isGroup=True,
        avatarInitials=chat.avatar_initials,
        avatarBgColor=chat.avatar_bg_color,
    )


@router.get("/chats/workers/{chatId}/messages", response_model=list[WorkerChatMessageOut])
def mensajes_chat_trabajador(
    chatId: str,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[WorkerChatMessageOut]:
    """Obtiene el historial de mensajes de un chat interno entre trabajadores."""
    mensajes = db.scalars(select(WorkerChatMessage).where(WorkerChatMessage.chat_id == chatId)).all()
    return [
        WorkerChatMessageOut(
            id=m.id,
            senderId=m.sender_id,
            senderName=m.sender_name,
            message=m.message,
            time=m.time,
            isOutgoing=m.sender_id == quien.id,
        )
        for m in mensajes
    ]


@router.post("/chats/workers/{chatId}/messages", response_model=WorkerChatMessageOut, status_code=201)
def enviar_mensaje_trabajador(
    chatId: str,
    body: SendWorkerMessageRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> WorkerChatMessageOut:
    """Envía un mensaje de texto o adjunto a una conversación entre trabajadores."""
    chat = db.get(WorkerChat, chatId)
    if chat is None:
        raise ApiError(404, f"No existe el chat con id {chatId}")

    hora_actual = body.time or ahora().strftime("%I:%M %p")
    msg_id = f"wm_{uuid.uuid4().hex[:8]}"
    nuevo_msg = WorkerChatMessage(
        created_at=momento_utc(),
        id=msg_id,
        chat_id=chatId,
        sender_id=quien.id,
        sender_name=quien.display_name,
        message=body.message,
        time=hora_actual,
        is_outgoing=True,
        file_uri=body.fileUri,
        file_name=body.fileName,
        file_mime_type=body.fileMimeType,
    )
    db.add(nuevo_msg)

    # Actualizar último mensaje en la conversación
    chat.last_message = body.message
    chat.last_message_time = hora_actual
    db.commit()

    from .ws_notifications import solo_personal, ws_manager
    ws_manager.broadcast_sync(
        "chat:message:new",
        {
            "chatId": chatId,
            "message": {
                "id": msg_id,
                "chatId": chatId,
                "senderId": quien.id,
                "senderName": quien.display_name,
                "message": body.message,
                "time": hora_actual,
                "isOutgoing": False,
                "fileUri": body.fileUri,
                "fileName": body.fileName,
                "fileMimeType": body.fileMimeType,
            },
        },
        solo_personal(),
    )

    return WorkerChatMessageOut(
        id=msg_id,
        senderId=quien.id,
        senderName=quien.display_name,
        message=body.message,
        time=hora_actual,
        isOutgoing=True,
    )


@router.get("/chats/staff-global/messages", response_model=StaffGlobalChatOut)
def chat_global_personal(
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> StaffGlobalChatOut:
    """Obtiene el canal global institucional donde participan todas las trabajadoras."""
    chat = db.get(WorkerChat, "global_staff_chat")
    mensajes = db.scalars(select(WorkerChatMessage).where(WorkerChatMessage.chat_id == "global_staff_chat")).all() if chat else []

    msgs_out = [
        WorkerChatMessageOut(
            id=m.id,
            senderId=m.sender_id,
            senderName=m.sender_name,
            message=m.message,
            time=m.time,
            isOutgoing=m.sender_id == quien.id,
        )
        for m in mensajes
    ]

    return StaffGlobalChatOut(
        id="global_staff_chat",
        title="Chat Global de Trabajadores",
        membersCount=db.scalar(select(func.count()).select_from(StaffMember)) or 0,
        messages=msgs_out,
    )


@router.post("/chats/staff-global/messages", response_model=WorkerChatMessageOut, status_code=201)
def enviar_chat_global_personal(
    body: SendGlobalMessageRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> WorkerChatMessageOut:
    """Publica un mensaje en el canal global de trabajadores."""
    hora_actual = body.time or ahora().strftime("%I:%M %p")
    msg_id = f"gm_{uuid.uuid4().hex[:8]}"
    nuevo_msg = WorkerChatMessage(
        created_at=momento_utc(),
        id=msg_id,
        chat_id="global_staff_chat",
        sender_id=quien.id,
        sender_name=quien.display_name,
        message=body.message,
        time=hora_actual,
        is_outgoing=True,
    )
    db.add(nuevo_msg)
    db.commit()

    from .ws_notifications import solo_personal, ws_manager
    ws_manager.broadcast_sync(
        "chat:message:new",
        {
            "chatId": "global_staff_chat",
            "message": {
                "id": msg_id,
                "chatId": "global_staff_chat",
                "senderId": quien.id,
                "senderName": quien.display_name,
                "message": body.message,
                "time": hora_actual,
                "isOutgoing": False,
            },
        },
        solo_personal(),
    )

    return WorkerChatMessageOut(
        id=msg_id,
        senderId=quien.id,
        senderName=quien.display_name,
        message=body.message,
        time=hora_actual,
        isOutgoing=True,
    )


@router.put("/chats/workers/{chatId}/read", status_code=204)
def marcar_chat_trabajador_leido(
    chatId: str,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> None:
    """Marca como leídos los mensajes de un chat del personal (incluido el global) para el usuario actual."""
    if db.get(WorkerChat, chatId) is None:
        raise ApiError(404, f"No existe el chat con id {chatId}", "chat_inexistente")
    marcar_leido(db, quien.id, clave_personal(chatId))
