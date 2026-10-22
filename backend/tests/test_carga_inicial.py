"""Plan de implantación: con DATOS_DEMO=false la base arranca sin datos de ejemplo."""

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.carga_inicial import carga_inicial_produccion
from app.config import settings
from app.db import Base
from app.models import Child, Room, Usuario
from app.security import verify_password


def _base_vacia() -> Session:
    motor = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(motor)
    return Session(motor)


def test_carga_inicial_crea_solo_direccion_y_salas(monkeypatch):
    monkeypatch.setattr(settings, "direccion_email", "Responsable@Estancia.mx")
    monkeypatch.setattr(settings, "direccion_password", "UnaClaveLarga2026")
    monkeypatch.setattr(settings, "direccion_nombre", "Responsable de la estancia")
    monkeypatch.setattr(settings, "salas_iniciales", "Sala 1A:Lactantes, Sala 2B:Maternales A")
    with _base_vacia() as db:
        assert carga_inicial_produccion(db) is True
        usuarios = db.scalars(select(Usuario)).all()
        assert len(usuarios) == 1
        assert usuarios[0].email == "responsable@estancia.mx"
        assert usuarios[0].role == "DIRECCION"
        assert verify_password("UnaClaveLarga2026", usuarios[0].password_hash)
        assert sorted(s.name for s in db.scalars(select(Room))) == ["Sala 1A", "Sala 2B"]
        assert db.scalars(select(Child)).all() == []
        # Si ya hay datos no vuelve a cargar nada
        assert carga_inicial_produccion(db) is False


def test_carga_inicial_sin_contrasena_segura_no_crea_cuenta(monkeypatch):
    monkeypatch.setattr(settings, "direccion_email", "responsable@estancia.mx")
    monkeypatch.setattr(settings, "direccion_password", "corta")
    with _base_vacia() as db:
        assert carga_inicial_produccion(db) is False
        assert db.scalars(select(Usuario)).all() == []
