import asyncio
import json
import logging
from typing import Any

from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import settings
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..models import DeviceToken
from ..schemas import DeviceTokenRequest
from ..security import verify_jwt

logger = logging.getLogger(__name__)
router = APIRouter(tags=["notifications-and-websockets"])


class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []
        self._loop: asyncio.AbstractEventLoop | None = None

    async def connect(self, websocket: WebSocket):
        self._loop = asyncio.get_running_loop()
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, event: str, data: dict[str, Any]):
        payload = {
            "event": event,
            "tipo": event,
            "data": data,
            **data,
        }
        mensaje = json.dumps(payload, ensure_ascii=False)
        for connection in list(self.active_connections):
            try:
                await connection.send_text(mensaje)
            except Exception:
                self.disconnect(connection)

    def broadcast_sync(self, event: str, data: dict[str, Any]):
        try:
            loop = self._loop or asyncio.get_running_loop()
            if loop and loop.is_running():
                asyncio.run_coroutine_threadsafe(self.broadcast(event, data), loop)
        except RuntimeError:
            try:
                loop = asyncio.get_event_loop()
                if loop.is_running():
                    asyncio.run_coroutine_threadsafe(self.broadcast(event, data), loop)
                else:
                    loop.run_until_complete(self.broadcast(event, data))
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

    await ws_manager.connect(websocket)
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
                if tipo in ("mensaje", "chat:send") and texto:
                    msg_payload = {
                        "chatId": chat_id,
                        "senderId": usuario_id,
                        "senderName": payload.get("displayName", "Personal"),
                        "message": texto,
                        "time": "Ahora",
                        "isOutgoing": False,
                    }
                    await ws_manager.broadcast("chat:message:new", {"chatId": chat_id, "message": msg_payload})
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

