"""Pruebas de Seguridad Criptográfica y Validación Negativa."""

import uuid

from fastapi.testclient import TestClient

from app.config import settings
from app.db import SessionLocal
from app.models import Usuario
from app.security import sign_jwt

from .conftest import Persona


def test_token_vencido_da_401(cliente: TestClient, familiar: Persona):
    # Generar un token con caducidad en el pasado (-60 segundos)
    vencido = sign_jwt(
        usuario_id=familiar.id,
        email=familiar.email,
        role=familiar.role,
        display_name="Prueba",
        ttl_s=-60,
        secret=settings.jwt_secret,
    )
    r = cliente.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {vencido}"})
    assert r.status_code == 401
    assert r.json()["code"] == "token_expirado"


def test_token_alterado_da_401(cliente: TestClient, familiar: Persona):
    # Modificar un carácter de la firma
    partes = familiar.token.split(".")
    firma_modificada = ("X" if partes[2][0] != "X" else "Y") + partes[2][1:]
    token_falso = f"{partes[0]}.{partes[1]}.{firma_modificada}"

    r = cliente.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {token_falso}"})
    assert r.status_code == 401
    assert r.json()["code"] == "token_invalido"


def test_peticion_sin_token_da_401(cliente: TestClient):
    r = cliente.get("/api/v1/auth/me")
    assert r.status_code == 401
    assert r.json()["code"] == "falta_token"
    assert "WWW-Authenticate" in r.headers


def test_contrasena_hasheada_con_argon2(familiar: Persona):
    # Verificar directamente en la base de datos que la contraseña no esté en texto plano
    with SessionLocal() as db:
        user = db.get(Usuario, familiar.id)
        assert user is not None
        assert "Password123!" not in user.password_hash
        assert user.password_hash.startswith("$argon2id$")


def test_registro_docente_sin_codigo_se_degrada_a_familiar(cliente: TestClient):
    # El usuario intenta auto-asignarse rol DIRECCION sin el código secreto
    email = f"tramposo.{uuid.uuid4().hex[:6]}@sonrisas.edu"
    r = cliente.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123!",
            "role": "DIRECCION",
            "displayName": "Usuario Tramposo",
            "codigoProfesor": "codigo_incorrecto",
        },
    )
    assert r.status_code == 201
    assert r.json()["user"]["role"] == "FAMILIAR"
