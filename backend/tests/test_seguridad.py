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


def test_registro_publico_siempre_asigna_rol_familiar(cliente: TestClient):
    # Cualquier intento de auto-asignarse rol DIRECCION mediante registro público se fuerza a FAMILIAR
    email = f"tramposo.{uuid.uuid4().hex[:6]}@sonrisas.edu"
    r = cliente.post(
        "/api/v1/auth/register",
        json={
            "email": email,
            "password": "Password123!",
            "role": "DIRECCION",
            "displayName": "Usuario Tramposo",
        },
    )
    assert r.status_code == 201
    assert r.json()["user"]["role"] == "FAMILIAR"


def _login(cliente: TestClient, email: str, password: str):
    return cliente.post("/api/v1/auth/login", json={"email": email, "password": password})


def test_cinco_fallos_bloquean_solo_esa_cuenta(cliente: TestClient):
    # Cinco contraseñas incorrectas seguidas para el mismo correo
    for _ in range(5):
        assert _login(cliente, "familiar@sonrisas.edu", "incorrecta").status_code == 401

    # El sexto intento se bloquea aunque la contraseña sea correcta
    r = _login(cliente, "familiar@sonrisas.edu", "Password123!")
    assert r.status_code == 429
    assert r.json()["code"] == "demasiados_intentos"

    # Otra cuenta desde la misma IP sigue entrando sin problema
    assert _login(cliente, "educadora@sonrisas.edu", "Password123!").status_code == 200


def test_login_correcto_reinicia_el_contador(cliente: TestClient):
    for _ in range(4):
        assert _login(cliente, "familiar@sonrisas.edu", "incorrecta").status_code == 401
    assert _login(cliente, "familiar@sonrisas.edu", "Password123!").status_code == 200
    # Tras el acceso correcto el contador vuelve a cero: otros 4 fallos no bloquean
    for _ in range(4):
        assert _login(cliente, "familiar@sonrisas.edu", "incorrecta").status_code == 401
    assert _login(cliente, "familiar@sonrisas.edu", "Password123!").status_code == 200


def test_muchos_logins_correctos_no_bloquean(cliente: TestClient):
    # Antes, 5 inicios de sesión por minuto desde la misma IP bloqueaban a todos
    for _ in range(10):
        assert _login(cliente, "educadora@sonrisas.edu", "Password123!").status_code == 200
