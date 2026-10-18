"""Módulo 9: Grupos Familiares (/family-groups)"""

import uuid
from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..acceso import es_personal, hijos_de
from ..db import get_db
from ..deps import Identidad, requiere_educadora, usuario_actual
from ..errors import ApiError
from ..horario import hora_12h
from ..models import (
    Child,
    ChildParent,
    FamilyGroup,
    FamilyGroupMember,
    FamilyGroupMessage,
    Usuario,
)
from ..schemas import (
    AttachmentData,
    CandidateOut,
    CreateFamilyGroupRequest,
    FamilyGroupMessageOut,
    FamilyGroupOut,
    SendFamilyGroupMessageRequest,
)

router = APIRouter(prefix="/family-groups", tags=["family-groups"])


def _grupo_visible(group_id: str, quien: Identidad, db: Session) -> FamilyGroup:
    """El personal ve todos los grupos; una familia solo los grupos donde está alguno de sus hijos."""
    grupo = db.get(FamilyGroup, group_id)
    if grupo is None:
        raise ApiError(404, f"No existe el grupo con id {group_id}", "grupo_inexistente")
    if not es_personal(quien):
        hijos = hijos_de(quien, db)
        miembro = db.scalar(
            select(FamilyGroupMember.id).where(
                FamilyGroupMember.group_id == group_id, FamilyGroupMember.child_id.in_(hijos)
            )
        ) if hijos else None
        if miembro is None:
            raise ApiError(403, "No perteneces a este grupo", "sin_acceso_grupo")
    return grupo


@router.get("/candidates", response_model=list[CandidateOut])
def listar_candidatos(
    _: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> list[CandidateOut]:
    """Lista todos los alumnos y sus tutores disponibles para formar un nuevo grupo."""
    alumnos = db.scalars(select(Child)).all()
    resultado = []
    for a in alumnos:
        cp = db.scalar(select(ChildParent).where(ChildParent.child_id == a.id))
        nombre_padre = "Tutor Familiar"
        if cp:
            tutor = db.get(Usuario, cp.parent_user_id)
            if tutor:
                nombre_padre = tutor.display_name

        resultado.append(
            CandidateOut(
                childId=a.id,
                childName=a.full_name,
                parentName=nombre_padre,
                avatarInitials=a.avatar_initials,
                avatarBgColor=a.avatar_bg_color,
            )
        )
    return resultado


@router.get("", response_model=list[FamilyGroupOut])
def listar_grupos_familiares(
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[FamilyGroupOut]:
    """Lista los grupos familiares. La educadora ve los creados por ella; la familia ve donde están sus hijos."""
    if quien.role in ("EDUCADORA", "DIRECCION"):
        grupos = db.scalars(select(FamilyGroup)).all()
    else:
        # Hijos de este familiar
        hijos_ids = db.scalars(select(ChildParent.child_id).where(ChildParent.parent_user_id == quien.id)).all()
        grupos_ids = db.scalars(select(FamilyGroupMember.group_id).where(FamilyGroupMember.child_id.in_(hijos_ids))).all()
        grupos = db.scalars(select(FamilyGroup).where(FamilyGroup.id.in_(grupos_ids))).all() if grupos_ids else []

    # Del grupo con actividad más reciente al más antiguo (igual que en la app)
    def ultima_actividad(g: FamilyGroup):
        ultimo = db.scalar(
            select(FamilyGroupMessage.created_at)
            .where(FamilyGroupMessage.group_id == g.id)
            .order_by(FamilyGroupMessage.created_at.desc())
            .limit(1)
        )
        return ultimo or g.created_at

    grupos = sorted(grupos, key=ultima_actividad, reverse=True)

    resultado = []
    for g in grupos:
        miembros = db.scalars(select(FamilyGroupMember.child_id).where(FamilyGroupMember.group_id == g.id)).all()
        resultado.append(
            FamilyGroupOut(
                id=g.id,
                title=g.title,
                memberChildIds=list(miembros),
                createdById=g.created_by_id,
                avatarBgColor=g.avatar_bg_color,
                lastMessage=g.last_message,
                lastMessageTime=g.last_message_time,
            )
        )
    return resultado


@router.post("", response_model=FamilyGroupOut, status_code=201)
def crear_grupo_familiar(
    body: CreateFamilyGroupRequest,
    quien: Identidad = Depends(requiere_educadora),
    db: Session = Depends(get_db),
) -> FamilyGroupOut:
    """Crea un nuevo grupo de conversación entre la educadora y las familias seleccionadas."""
    if not body.title.strip():
        raise ApiError(422, "El título del grupo no puede estar vacío")
    if not body.memberChildIds:
        raise ApiError(422, "Debe incluir al menos un menor en el grupo")
    miembros_ids = list(dict.fromkeys(body.memberChildIds))
    existentes = set(db.scalars(select(Child.id).where(Child.id.in_(miembros_ids))).all())
    faltantes = [c for c in miembros_ids if c not in existentes]
    if faltantes:
        raise ApiError(404, f"No existen los alumnos: {', '.join(faltantes)}", "alumno_inexistente")

    grupo_id = f"fgroup_{uuid.uuid4().hex[:8]}"
    hora_actual = hora_12h()
    grupo = FamilyGroup(
        id=grupo_id,
        title=body.title.strip(),
        created_by_id=quien.id,
        avatar_bg_color=4278225275,
        last_message="Grupo creado.",
        last_message_time=hora_actual,
    )
    db.add(grupo)
    db.flush()

    for cid in miembros_ids:
        db.add(FamilyGroupMember(group_id=grupo_id, child_id=cid))

    db.commit()

    return FamilyGroupOut(
        id=grupo.id,
        title=grupo.title,
        memberChildIds=miembros_ids,
        createdById=grupo.created_by_id,
        avatarBgColor=grupo.avatar_bg_color,
        lastMessage=grupo.last_message,
        lastMessageTime=grupo.last_message_time,
    )


@router.get("/{groupId}/messages", response_model=list[FamilyGroupMessageOut])
def obtener_mensajes_grupo_familiar(
    groupId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[FamilyGroupMessageOut]:
    """Obtiene el historial de mensajes compartidos en el grupo familiar (del más viejo al más nuevo)."""
    _grupo_visible(groupId, quien, db)
    mensajes = db.scalars(
        select(FamilyGroupMessage)
        .where(FamilyGroupMessage.group_id == groupId)
        .order_by(FamilyGroupMessage.created_at, FamilyGroupMessage.id)
    ).all()
    resultado = []
    for m in mensajes:
        adjunto = None
        if m.attachment_uri:
            adjunto = AttachmentData(
                uri=m.attachment_uri,
                name=m.attachment_name or "adjunto",
                mimeType=m.attachment_mime_type or "application/octet-stream",
            )
        resultado.append(
            FamilyGroupMessageOut(
                id=m.id,
                senderId=m.sender_id,
                senderName=m.sender_name,
                text=m.text,
                time=m.time,
                attachment=adjunto,
            )
        )
    return resultado


@router.post("/{groupId}/messages", response_model=FamilyGroupMessageOut, status_code=201)
def enviar_mensaje_grupo_familiar(
    groupId: str,
    body: SendFamilyGroupMessageRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> FamilyGroupMessageOut:
    """Envía un mensaje o archivo adjunto al grupo familiar."""
    grupo = _grupo_visible(groupId, quien, db)
    if not body.text.strip() and body.attachment is None:
        raise ApiError(422, "El mensaje está vacío", "mensaje_vacio")

    hora_actual = hora_12h()
    msg_id = f"fgm_{uuid.uuid4().hex[:8]}"

    att_uri = body.attachment.uri if body.attachment else None
    att_name = body.attachment.name if body.attachment else None
    att_mime = body.attachment.mimeType if body.attachment else None

    nuevo_msg = FamilyGroupMessage(
        id=msg_id,
        group_id=groupId,
        sender_id=quien.id,
        sender_name=quien.display_name,
        text=body.text,
        time=hora_actual,
        attachment_uri=att_uri,
        attachment_name=att_name,
        attachment_mime_type=att_mime,
        created_at=datetime.now(),
    )
    db.add(nuevo_msg)

    # Actualizar estado del grupo
    grupo.last_message = body.text.strip() or (f"📎 {att_name}" if att_name else "📎 Archivo adjunto")
    grupo.last_message_time = hora_actual
    db.commit()

    from .ws_notifications import ws_manager
    att_dict = body.attachment.model_dump() if body.attachment else None
    ws_manager.broadcast_sync(
        "group:message:new",
        {
            "groupId": groupId,
            "chatId": groupId,
            "message": {
                "id": msg_id,
                "groupId": groupId,
                "senderId": quien.id,
                "senderName": quien.display_name,
                "text": body.text,
                "time": hora_actual,
                "attachment": att_dict,
            },
        },
    )

    return FamilyGroupMessageOut(
        id=msg_id,
        senderId=quien.id,
        senderName=quien.display_name,
        text=body.text,
        time=hora_actual,
        attachment=body.attachment,
    )
