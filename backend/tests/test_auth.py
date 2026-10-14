"""Pruebas unitarias de Autenticación, Emisión de Tokens JWT y Rotación de Sesiones."""

from fastapi.testclient import TestClient

from .conftest import Persona


def test_login_familiar_exitoso(cliente: TestClient):
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "familiar@sonrisas.edu",
            "password": "Password123!",
            "role": "FAMILIAR",
        },
    )
    assert r.status_code == 200
    data = r.json()
    assert "token" in data
    assert "refreshToken" in data
    assert data["user"]["role"] == "FAMILIAR"
    assert data["user"]["email"] == "familiar@sonrisas.edu"


def test_login_educadora_exitoso(cliente: TestClient):
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "educadora@sonrisas.edu",
            "password": "Password123!",
            "role": "EDUCADORA",
        },
    )
    assert r.status_code == 200
    data = r.json()
    assert data["user"]["role"] == "EDUCADORA"
    assert "Laura Gómez" in data["user"]["displayName"]


def test_login_credenciales_invalidas_da_401(cliente: TestClient):
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "familiar@sonrisas.edu",
            "password": "ContraseñaIncorrecta123",
            "role": "FAMILIAR",
        },
    )
    assert r.status_code == 401
    assert r.json()["code"] == "credenciales_invalidas"


def test_login_rol_discrepante_da_403(cliente: TestClient):
    # La cuenta familiar intenta iniciar sesión con el selector en EDUCADORA
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "familiar@sonrisas.edu",
            "password": "Password123!",
            "role": "EDUCADORA",
        },
    )
    assert r.status_code == 403
    assert r.json()["code"] == "rol_invalido"


def test_refresh_token_exitoso_y_rotacion(cliente: TestClient):
    # Login para obtener token
    r_login = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "familiar@sonrisas.edu",
            "password": "Password123!",
            "role": "FAMILIAR",
        },
    )
    assert r_login.status_code == 200
    refresh_1 = r_login.json()["refreshToken"]

    # Refrescar
    r_ref = cliente.post("/api/v1/auth/refresh", json={"refreshToken": refresh_1})
    assert r_ref.status_code == 200
    data_ref = r_ref.json()
    assert "token" in data_ref

    # Intentar reusar el mismo refresh token (debe fallar por rotación)
    r_reuse = cliente.post("/api/v1/auth/refresh", json={"refreshToken": refresh_1})
    assert r_reuse.status_code == 401
    assert r_reuse.json()["code"] == "refresh_invalido"


def test_logout_revoca_sesion(cliente: TestClient):
    r_login = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "educadora@sonrisas.edu",
            "password": "Password123!",
            "role": "EDUCADORA",
        },
    )
    refresh_tok = r_login.json()["refreshToken"]

    # Logout
    r_out = cliente.post("/api/v1/auth/logout", json={"refreshToken": refresh_tok})
    assert r_out.status_code == 204

    # Una vez revocado, ya no debe permitir refresh
    r_check = cliente.post("/api/v1/auth/refresh", json={"refreshToken": refresh_tok})
    assert r_check.status_code == 401


def test_me_retorna_perfil_autenticado(cliente: TestClient, familiar: Persona):
    r = cliente.get("/api/v1/auth/me", headers=familiar.headers)
    assert r.status_code == 200
    data = r.json()
    assert data["id"] == familiar.id
    assert data["role"] == "FAMILIAR"
    assert data["email"] == "familiar@sonrisas.edu"
