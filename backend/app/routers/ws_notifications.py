import asyncio
import json
import logging
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any

from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import settings
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..models import ChildParent, DeviceToken
from ..schemas import DeviceTokenRequest
from ..security import verify_jwt

logger = logging.getLogger(__name__)
router = APIRouter(tags=["notifications-and-websockets"])


ROLES_PERSONAL = ("EDUCADORA", "DIRECCION")

# Filtro de destinatarios: recibe (user_id, role) y decide si esa conexión recibe el evento
Destinatarios = Callable[[str, str], bool]


def solo_personal() -> Destinatarios:
    return lambda _uid, rol: rol in ROLES_PERSONAL


def personal_y_usuarios(usuarios: set[str]) -> Destinatarios:
    """El personal y los usuarios indicados (por ejemplo, las familias de un niño)."""
    return lambda uid, rol: rol in ROLES_PERSONAL or uid in usuarios


def familias_de(db: Session, child_ids: set[str]) -> set[str]:
    """Ids de usuario de los familiares de los niños indicados."""
    if not child_ids:
        return set()
    return set(
        db.scalars(select(ChildParent.parent_user_id).where(ChildParent.child_id.in_(child_ids))).all()
    )


@dataclass
class Conexion:
    websocket: WebSocket
    user_id: str = ""
    role: str = ""


class ConnectionManager:
    """Conexiones abiertas. Cada evento se envía solo a quien le corresponde verlo."""

    def __init__(self):
        self.conexiones: list[Conexion] = []
        self._loop: asyncio.AbstractEventLoop | None = None

    @property
    def active_connections(self) -> list[WebSocket]:
        return [c.websocket for c in self.conexiones]

    async def connect(self, websocket: WebSocket, user_id: str = "", role: str = ""):
        self._loop = asyncio.get_running_loop()
        await websocket.accept()
        self.conexiones.append(Conexion(websocket, user_id, role))

    def disconnect(self, websocket: WebSocket):
        self.conexiones = [c for c in self.conexiones if c.websocket is not websocket]

    async def broadcast(self, event: str, data: dict[str, Any], para: Destinatarios | None = None):
        payload = {
            "event": event,
            "tipo": event,
            "data": data,
            **data,
        }
        mensaje = json.dumps(payload, ensure_ascii=False)
        for conexion in list(self.conexiones):
            if para is not None and not para(conexion.user_id, conexion.role):
                continue
            try:
                await conexion.websocket.send_text(mensaje)
            except Exception:
                self.disconnect(conexion.websocket)

    def broadcast_sync(self, event: str, data: dict[str, Any], para: Destinatarios | None = None):
        try:
            loop = self._loop or asyncio.get_running_loop()
            if loop and loop.is_running():
                asyncio.run_coroutine_threadsafe(self.broadcast(event, data, para), loop)
        except RuntimeError:
            try:
                loop = asyncio.get_event_loop()
                if loop.is_running():
                    asyncio.run_coroutine_threadsafe(self.broadcast(event, data, para), loop)
                else:
                    loop.run_until_complete(self.broadcast(event, data, para))
            except Exception as e:
                logger.debug("No se pudo emitir evento por websocket: %s", e)


ws_manager = ConnectionManager()


@router.post("/notifications/device-token", status_code=200)
def registrar_token_dispositivo(
    body: DeviceTokenRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    """Registra el token FCM del dispositivo Android para recibir notificaciones push."""
    existente = db.scalar(select(DeviceToken).where(DeviceToken.device_token == body.deviceToken))
    if existente:
        existente.user_id = quien.id
        existente.platform = body.platform
    else:
        db.add(
            DeviceToken(
                user_id=quien.id,
                device_token=body.deviceToken,
                platform=body.platform,
            )
        )
    db.commit()
    return {"status": "success", "message": "Token de dispositivo registrado exitosamente"}


@router.post("/notifications/device-token/unregister", status_code=204)
def desvincular_token_dispositivo(
    body: DeviceTokenRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> None:
    """Al cerrar sesión: este teléfono deja de recibir notificaciones de esta cuenta."""
    existente = db.scalar(select(DeviceToken).where(DeviceToken.device_token == body.deviceToken))
    if existente is not None and existente.user_id == quien.id:
        db.delete(existente)
        db.commit()


async def _handle_websocket_connection(websocket: WebSocket, token: str | None = None):
    """Conexión WebSocket en tiempo real para eventos de mensajería, asistencia y anuncios."""
    auth_header = websocket.headers.get("authorization", "")
    bearer_token = auth_header.removeprefix("Bearer ").strip() if auth_header else None
    jwt_token = token or bearer_token

    if not jwt_token:
        # Cerrar ANTES de aceptar: el cliente recibe un 403 y no llega a entrar
        await websocket.close(code=1008)
        return

    try:
        payload = verify_jwt(jwt_token, settings.jwt_secret)
        usuario_id = payload.get("sub", "")
        rol = payload.get("role", "")
    except Exception:
        # Cerrar ANTES de aceptar: rechaza con 403
        await websocket.close(code=1008)
        return

    await ws_manager.connect(websocket, usuario_id, rol)
    try:
        # Enviar mensaje de bienvenida / confirmación de conexión
        await websocket.send_text(
            json.dumps(
                {
                    "tipo": "bienvenida",
                    "event": "connection:established",
                    "yo": usuario_id,
                    "data": {"userId": usuario_id, "role": rol, "status": "online"},
                },
                ensure_ascii=False,
            )
        )
        while True:
            raw_data = await websocket.receive_text()
            if raw_data == "ping":
                await websocket.send_text("pong")
                continue

            try:
                msg_json = json.loads(raw_data)
                tipo = msg_json.get("tipo") or msg_json.get("event")
                texto = str(msg_json.get("texto") or msg_json.get("message") or "").strip()
                chat_id = str(msg_json.get("chatId") or msg_json.get("chat_id") or "global_staff_chat")
                if tipo in ("mensaje", "chat:send") and texto and rol in ROLES_PERSONAL:
                    msg_payload = {
                        "chatId": chat_id,
                        "senderId": usuario_id,
                        "senderName": payload.get("displayName", "Personal"),
                        "message": texto,
                        "time": "Ahora",
                        "isOutgoing": False,
                    }
                    await ws_manager.broadcast(
                        "chat:message:new", {"chatId": chat_id, "message": msg_payload}, solo_personal()
                    )
            except Exception:
                pass
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket)
    except Exception:
        ws_manager.disconnect(websocket)


@router.websocket("/ws/v1/connect")
async def websocket_connect_ws(websocket: WebSocket, token: str | None = Query(default=None)):
    await _handle_websocket_connection(websocket, token)


@router.websocket("/chat")
async def websocket_connect_chat(websocket: WebSocket, token: str | None = Query(default=None)):
    await _handle_websocket_connection(websocket, token)

