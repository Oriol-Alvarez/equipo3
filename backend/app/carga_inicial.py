"""Carga inicial para una estancia real (DATOS_DEMO=false).

La estancia no tiene datos previos que migrar (hoy todo vive en WhatsApp), así que el sistema
arranca vacío: solo se crean la cuenta de Dirección y las salas. Desde el panel de Dirección se
dan de alta educadoras, familias y niños, igual que hoy lo hace la responsable en su página.
"""

import logging
import re
import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from .config import settings
from .models import Room, Usuario
from .security import hash_password

log = logging.getLogger(__name__)


def _id_sala(nombre: str) -> str:
    limpio = re.sub(r"[^a-z0-9]+", "_", nombre.lower()).strip("_")
    return f"sala_{limpio or uuid.uuid4().hex[:6]}"[:32]


def carga_inicial_produccion(db: Session) -> bool:
    """Crea la cuenta de Dirección y las salas si la base está vacía. Devuelve True si cargó algo."""
    if db.scalar(select(Usuario).limit(1)) is not None:
        return False
    correo = settings.direccion_email.strip().lower()
    contrasena = settings.direccion_password
    if not correo or len(contrasena) < 10:
        log.warning(
            "DATOS_DEMO=false pero falta DIRECCION_EMAIL o DIRECCION_PASSWORD (mínimo 10 caracteres): "
            "no se creó ninguna cuenta."
        )
        return False

    db.add(
        Usuario(
            id=f"usr_{uuid.uuid4().hex[:12]}",
            email=correo,
            password_hash=hash_password(contrasena),
            role="DIRECCION",
            display_name=settings.direccion_nombre.strip() or "Dirección",
        )
    )
    for parte in settings.salas_iniciales.split(","):
        if not parte.strip():
            continue
        nombre, _, nivel = parte.partition(":")
        nombre = nombre.strip()
        sala_id = _id_sala(nombre)
        if db.get(Room, sala_id) is None:
            db.add(Room(id=sala_id, name=nombre, level=nivel.strip() or "General", capacity=15))
    db.commit()
    log.info("Carga inicial lista: cuenta de Dirección %s y salas %s", correo, settings.salas_iniciales)
    return True
