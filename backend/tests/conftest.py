"""Configuración común y fixtures de prueba para pytest."""

import base64
import os
import uuid
from collections.abc import Callable, Iterator
from dataclasses import dataclass

import pytest
from fastapi.testclient import TestClient

# Asegurar configuración para entorno de pruebas
os.environ["JWT_SECRET"] = "super-secret-jwt-key-for-sonrisas-app-tc2007b"
os.environ["CODIGO_PROFESOR"] = "sonrisas-docente-2026"

import socket

def _postgres_disponible() -> bool:
    """Verifica si PostgreSQL está accesible en localhost o en la red de Docker."""
    if "DATABASE_URL" in os.environ and "@db:" in os.environ["DATABASE_URL"]:
        return True
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(0.5)
    try:
        s.connect(("127.0.0.1", 5432))
        s.close()
        return True
    except Exception:
        return False

if not _postgres_disponible():
    # Permite ejecutar `pytest` en la máquina local sin necesidad de levantar Docker primero
    os.environ["DATABASE_URL"] = "sqlite:///./test_sonrisas.db"

from app.config import settings
from app.db import Base, SessionLocal, engine
from app.main import app
from app.seed import seed_database

# Imagen PNG válida de 2x2 píxeles
PNG_CHICO = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGPgi/ECIgYIBQASzgLRQRXuCAAAAABJRU5ErkJggg=="
)


@dataclass
class Persona:
    id: str
    email: str
    role: str
    token: str

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.token}"}


@pytest.fixture(scope="session", autouse=True)
def setup_database():
    """Crea las tablas y carga los datos de prueba antes de la sesión de tests."""
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed_database(db)
    yield


@pytest.fixture(scope="session")
def cliente() -> Iterator[TestClient]:
    """TestClient de FastAPI que simula peticiones HTTP a la aplicación en memoria."""
    with TestClient(app) as c:
        yield c


@pytest.fixture
def registrar(cliente: TestClient) -> Callable[..., Persona]:
    """Crea una persona nueva con credenciales únicas."""

    def _registrar(nombre: str, role: str = "FAMILIAR") -> Persona:
        random_suffix = uuid.uuid4().hex[:6]
        email = f"{nombre}.{random_suffix}@sonrisas.edu"
        cuerpo = {
            "email": email,
            "password": "Password123!",
            "role": role,
            "displayName": f"Usuario {nombre.capitalize()}",
        }
        if role in ("EDUCADORA", "DIRECCION"):
            cuerpo["codigoProfesor"] = settings.codigo_profesor

        r = cliente.post("/api/v1/auth/register", json=cuerpo)
        assert r.status_code == 201, r.text
        data = r.json()
        return Persona(
            id=data["user"]["id"],
            email=email,
            role=data["user"]["role"],
            token=data["token"],
        )

    return _registrar


@pytest.fixture
def familiar(cliente: TestClient) -> Persona:
    """Fixture que inicia sesión como el familiar predeterminado."""
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "familiar@sonrisas.edu",
            "password": "Password123!",
            "role": "FAMILIAR",
        },
    )
    assert r.status_code == 200, r.text
    data = r.json()
    return Persona(
        id=data["user"]["id"],
        email=data["user"]["email"],
        role="FAMILIAR",
        token=data["token"],
    )


@pytest.fixture
def educadora(cliente: TestClient) -> Persona:
    """Fixture que inicia sesión como la educadora predeterminada."""
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "educadora@sonrisas.edu",
            "password": "Password123!",
            "role": "EDUCADORA",
        },
    )
    assert r.status_code == 200, r.text
    data = r.json()
    return Persona(
        id=data["user"]["id"],
        email=data["user"]["email"],
        role="EDUCADORA",
        token=data["token"],
    )


@pytest.fixture
def direccion(cliente: TestClient) -> Persona:
    """Fixture que inicia sesión como la dirección predeterminada."""
    r = cliente.post(
        "/api/v1/auth/login",
        json={
            "email": "direccion@sonrisas.edu",
            "password": "Password123!",
            "role": "DIRECCION",
        },
    )
    assert r.status_code == 200, r.text
    data = r.json()
    return Persona(
        id=data["user"]["id"],
        email=data["user"]["email"],
        role="DIRECCION",
        token=data["token"],
    )
