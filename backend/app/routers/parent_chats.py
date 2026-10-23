"""Módulo 8: Chat Bitácora Individual (Educadora <-> Familia)"""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..acceso import es_personal, exigir_alumno_visible
from ..db import get_db
from ..notificaciones_push import familias_del_nino, notificar, personal_de_la_sala
from ..lecturas import clave_bitacora, marcar_leido, momento_utc, no_leidos_bitacora
from ..horario import ahora, hoy
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..models import BitacoraMessage, Child, ChildParent, MediaFile, Usuario
from ..schemas import (
    BitacoraMessageOut,
    ParentChatSummaryOut,
    SendBitacoraMessageRequest,
)

router = APIRouter(tags=["parent-chats"])


@router.get("/rooms/{roomId}/parent-chats", response_model=list[ParentChatSummaryOut])
def listar_chats_familias_de_sala(
    roomId: str,
    quien: Identidad = Depends(requiere_educadora),
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

        # Si el último mensaje es de quien consulta, se muestra como "Yo: ..."
        if ultimo is None:
            last_msg = "Sin mensajes aún."
        elif ultimo.sender_id == quien.id:
            last_msg = f"Yo: {ultimo.message}"
        else:
            last_msg = ultimo.message

        resultado.append(
            ParentChatSummaryOut(
                childId=a.id,
                childName=a.full_name,
                parentName=nombre_padre,
                lastMessage=last_msg,
                lastMessageTime=ultimo.time if ultimo else "",
                unreadCount=no_leidos_bitacora(db, quien.id, a.id),
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
    exigir_alumno_visible(quien, childId, db)
    mensajes = db.scalars(select(BitacoraMessage).where(BitacoraMessage.child_id == childId)).all()
    return [
        BitacoraMessageOut(
            id=m.id,
            senderId=m.sender_id,
            senderName=m.sender_name,
            time=m.time,
            date=m.created_at.strftime("%d/%m/%Y") if m.created_at else hoy().strftime("%d/%m/%Y"),
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
    alumno = exigir_alumno_visible(quien, childId, db)

    hora_actual = body.time or ahora().strftime("%I:%M %p")
    fecha_actual = body.date or hoy().strftime("%d/%m/%Y")
    msg_id = f"msg_{uuid.uuid4().hex[:8]}"
    nuevo_msg = BitacoraMessage(
        created_at=momento_utc(),
        id=msg_id,
        child_id=childId,
        sender_id=quien.id,
        sender_name=quien.display_name,
        time=hora_actual,
        date=fecha_actual,
        message=body.message,
        file_uri=body.fileUri,
        file_name=body.fileName,
        file_mime_type=body.fileMimeType,
    )
    db.add(nuevo_msg)
    db.commit()

    # Push: a la familia si escribe el personal; a la educadora de la sala si escribe la familia
    destinatarios = familias_del_nino(db, childId) if es_personal(quien) else personal_de_la_sala(db, alumno.room_id)
    notificar(db, destinatarios, f"Mensaje sobre {alumno.short_name}",
              f"{quien.display_name}: {body.message or 'Archivo adjunto'}",
              {"tipo": "bitacora", "childId": childId}, excepto=quien.id)

    from .ws_notifications import familias_de, personal_y_usuarios, ws_manager
    msg_dict = {
        "id": msg_id,
        "childId": childId,
        "chatId": childId,
        "senderId": quien.id,
        "senderName": quien.display_name,
        "time": hora_actual,
        "date": fecha_actual,
        "message": body.message,
        "fileUri": body.fileUri,
        "fileName": body.fileName,
        "fileMimeType": body.fileMimeType,
        "isOutgoing": False,
    }
    ws_manager.broadcast_sync(
        "chat:bitacora:new",
        {"childId": childId, "chatId": childId, "message": msg_dict},
        personal_y_usuarios(familias_de(db, {childId})),
    )

    return BitacoraMessageOut(
        id=msg_id,
        senderId=quien.id,
        senderName=quien.display_name,
        time=hora_actual,
        date=fecha_actual,
        message=body.message,
        isOutgoing=True,
        fileUri=body.fileUri,
    )


@router.put("/chats/bitacora/{childId}/read", status_code=204)
def marcar_chat_bitacora_leido(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> None:
    """Marca como leídos los mensajes del chat de bitácora de este niño para el usuario actual."""
    exigir_alumno_visible(quien, childId, db)
    marcar_leido(db, quien.id, clave_bitacora(childId))


@router.delete("/chats/bitacora/{childId}/messages/{messageId}/photo", status_code=204)
@router.delete("/chats/bitacora/{childId}/messages/{messageId}", status_code=204)
def eliminar_foto_o_mensaje_bitacora(
    childId: str,
    messageId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> None:
    """Permite a familias y personal eliminar fotos o mensajes en el chat de bitácora."""
    exigir_alumno_visible(quien, childId, db)

    msg = db.get(BitacoraMessage, messageId)
    if msg is None or msg.child_id != childId:
        raise ApiError(404, f"No existe el mensaje con id {messageId}", "mensaje_inexistente")

    if msg.file_uri:
        from .media import eliminar_archivo_media
        eliminar_archivo_media(db, msg.file_uri)

        if msg.message and msg.message.strip():
            msg.file_uri = None
            msg.file_name = None
            msg.file_mime_type = None
        else:
            db.delete(msg)
    else:
        db.delete(msg)

    db.commit()

    from .ws_notifications import familias_de, personal_y_usuarios, ws_manager
    ws_manager.broadcast_sync(
        "chat:bitacora:delete",
        {"childId": childId, "chatId": childId, "messageId": messageId},
        personal_y_usuarios(familias_de(db, {childId})),
    )

