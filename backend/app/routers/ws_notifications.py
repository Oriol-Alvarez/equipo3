"""Módulo 14: Notificaciones Push y WebSockets (/notifications, /ws)"""

import json
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

router = APIRouter(tags=["notifications-and-websockets"])


class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, event: str, data: dict[str, Any]):
        mensaje = json.dumps({"event": event, "data": data}, ensure_ascii=False)
        for connection in list(self.active_connections):
            try:
                await connection.send_text(mensaje)
            except Exception:
                self.disconnect(connection)


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


@router.websocket("/ws/v1/connect")
async def websocket_connect(websocket: WebSocket, token: str = Query(...)):
    """Conexión WebSocket en tiempo real para eventos de mensajería, asistencia y anuncios."""
    try:
        payload = verify_jwt(token, settings.jwt_secret)
        usuario_id = payload["sub"]
    except Exception:
        await websocket.close(code=4001)
        return

    await ws_manager.connect(websocket)
    try:
        # Enviar mensaje de bienvenida / confirmación de conexión
        await websocket.send_text(
            json.dumps(
                {
                    "event": "connection:established",
                    "data": {"userId": usuario_id, "status": "online"},
                }
            )
        )
        while True:
            # Mantener conexión viva y procesar pings del cliente
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_text("pong")
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket)
    except Exception:
        ws_manager.disconnect(websocket)
