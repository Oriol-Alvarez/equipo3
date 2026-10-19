"""Pruebas del WebSocket de Chat y Notificaciones en tiempo real."""

import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

from .conftest import Persona


def test_ws_sin_token_es_rechazado_con_1008(cliente: TestClient):
    with pytest.raises(WebSocketDisconnect) as exc:
        with cliente.websocket_connect("/api/v1/ws/v1/connect"):
            pass
    assert exc.value.code == 1008


def test_ws_con_token_invalido_es_rechazado_con_1008(cliente: TestClient):
    with pytest.raises(WebSocketDisconnect) as exc:
        with cliente.websocket_connect("/api/v1/ws/v1/connect?token=token_invalido"):
            pass
    assert exc.value.code == 1008


def test_ws_con_token_valido_recibe_bienvenida_y_ping(cliente: TestClient, educadora: Persona):
    with cliente.websocket_connect(
        "/api/v1/ws/v1/connect",
        headers={"Authorization": f"Bearer {educadora.token}"},
    ) as ws:
        msg = ws.receive_json()
        assert msg["event"] == "connection:established" or msg["tipo"] == "bienvenida"
        assert msg["yo"] == educadora.id

        ws.send_text("ping")
        resp = ws.receive_text()
        assert resp == "pong"


def test_ws_recibe_evento_al_enviar_mensaje(cliente: TestClient, educadora: Persona):
    with cliente.websocket_connect(
        "/api/v1/ws/v1/connect",
        headers={"Authorization": f"Bearer {educadora.token}"},
    ) as ws:
        # Bienvenida
        ws.receive_json()

        # Enviar mensaje de trabajadores vía REST
        r = cliente.post(
            "/api/v1/chats/workers/wchat_1/messages",
            headers=educadora.headers,
            json={"message": "Mensaje en vivo"},
        )
        assert r.status_code == 201

        # El WebSocket debe recibir el broadcast en tiempo real
        evento = ws.receive_json()
        assert evento["event"] == "chat:message:new"
        assert evento["data"]["chatId"] == "wchat_1"
        assert evento["data"]["message"]["message"] == "Mensaje en vivo"
