"""Mensajes sin leer por usuario en los chats (bitácora familia-educadora y personal)."""

from datetime import datetime, timezone

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from .models import BitacoraMessage, ChatRead, WorkerChatMessage


def momento_utc() -> datetime:
    """Hora UTC sin zona y con microsegundos, igual que func.now() de la base de datos.

    Se usa al crear mensajes para que dos eventos en el mismo segundo se distingan
    al contar los no leídos.
    """
    return datetime.now(timezone.utc).replace(tzinfo=None)


def clave_bitacora(child_id: str) -> str:
    return f"bitacora:{child_id}"


def clave_personal(chat_id: str) -> str:
    return f"worker:{chat_id}"


def _leido_hasta(db: Session, user_id: str, clave: str) -> datetime | None:
    registro = db.get(ChatRead, (user_id, clave))
    return registro.last_read_at if registro else None


def marcar_leido(db: Session, user_id: str, clave: str) -> None:
    """Marca como leído todo lo que existe hasta este momento (según el reloj de la base de datos)."""
    ultimo = None
    if clave.startswith("bitacora:"):
        ultimo = db.scalar(
            select(func.max(BitacoraMessage.created_at)).where(BitacoraMessage.child_id == clave.split(":", 1)[1])
        )
    elif clave.startswith("worker:"):
        ultimo = db.scalar(
            select(func.max(WorkerChatMessage.created_at)).where(WorkerChatMessage.chat_id == clave.split(":", 1)[1])
        )
    if ultimo is None:
        ultimo = datetime(1970, 1, 1)
    registro = db.get(ChatRead, (user_id, clave))
    if registro is None:
        db.add(ChatRead(user_id=user_id, chat_key=clave, last_read_at=ultimo))
    elif ultimo > registro.last_read_at:
        registro.last_read_at = ultimo
    db.commit()


def no_leidos_bitacora(db: Session, user_id: str, child_id: str) -> int:
    desde = _leido_hasta(db, user_id, clave_bitacora(child_id))
    consulta = select(func.count()).select_from(BitacoraMessage).where(
        BitacoraMessage.child_id == child_id, BitacoraMessage.sender_id != user_id
    )
    if desde is not None:
        consulta = consulta.where(BitacoraMessage.created_at > desde)
    return db.scalar(consulta) or 0


def no_leidos_personal(db: Session, user_id: str, chat_id: str) -> int:
    desde = _leido_hasta(db, user_id, clave_personal(chat_id))
    consulta = select(func.count()).select_from(WorkerChatMessage).where(
        WorkerChatMessage.chat_id == chat_id, WorkerChatMessage.sender_id != user_id
    )
    if desde is not None:
        consulta = consulta.where(WorkerChatMessage.created_at > desde)
    return db.scalar(consulta) or 0
