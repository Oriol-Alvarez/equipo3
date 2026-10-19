"""Notificaciones push con Firebase Cloud Messaging (API HTTP v1).

Solo se envían si FIREBASE_CREDENTIALS apunta al archivo JSON de la cuenta de servicio
del proyecto de Firebase (Consola de Firebase > Configuración del proyecto > Cuentas de
servicio > Generar nueva clave privada). Sin ese archivo todo sigue funcionando y
simplemente no se manda nada.

El envío se hace en un hilo aparte para no hacer esperar a quien manda el mensaje.
"""

import json
import logging
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Iterable
from pathlib import Path
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from .config import settings
from .models import ChildParent, DeviceToken, RoomEducator, Usuario

logger = logging.getLogger(__name__)

ALCANCE = "https://www.googleapis.com/auth/firebase.messaging"

# Las pruebas lo ponen en False para enviar en el mismo hilo y revisar el resultado
ENVIAR_EN_HILO = True

_cache_token: dict[str, Any] = {"valor": None, "expira": 0.0}
_lock = threading.Lock()


# ---------------------------------------------------------------------------
# Destinatarios
# ---------------------------------------------------------------------------

def familias_del_nino(db: Session, child_id: str) -> set[str]:
    return set(db.scalars(select(ChildParent.parent_user_id).where(ChildParent.child_id == child_id)).all())


def personal_de_la_sala(db: Session, room_id: str) -> set[str]:
    """Educadoras asignadas a la sala; si no hay, toda la dirección."""
    ids = set(db.scalars(select(RoomEducator.educator_id).where(RoomEducator.room_id == room_id)).all())
    if not ids:
        ids = set(db.scalars(select(Usuario.id).where(Usuario.role == "DIRECCION")).all())
    return ids


def todas_las_familias(db: Session) -> set[str]:
    return set(db.scalars(select(Usuario.id).where(Usuario.role == "FAMILIAR")).all())


# ---------------------------------------------------------------------------
# Envío
# ---------------------------------------------------------------------------

def configurado() -> bool:
    ruta = settings.firebase_credentials.strip()
    return bool(ruta) and Path(ruta).is_file()


def notificar(db: Session, usuarios: Iterable[str], titulo: str, cuerpo: str, datos: dict[str, str] | None = None,
              excepto: str | None = None) -> None:
    """Manda una notificación a los teléfonos registrados de esos usuarios (menos [excepto])."""
    if not configurado():
        return
    destinatarios = {u for u in usuarios if u and u != excepto}
    if not destinatarios:
        return
    tokens = list(db.scalars(select(DeviceToken.device_token).where(DeviceToken.user_id.in_(destinatarios))).all())
    if not tokens:
        return
    datos_texto = {k: str(v) for k, v in (datos or {}).items()}
    if ENVIAR_EN_HILO:
        threading.Thread(target=_despachar, args=(tokens, titulo, cuerpo, datos_texto), daemon=True).start()
    else:
        _despachar(tokens, titulo, cuerpo, datos_texto)


def _despachar(tokens: list[str], titulo: str, cuerpo: str, datos: dict[str, str]) -> None:
    try:
        credenciales = json.loads(Path(settings.firebase_credentials).read_text(encoding="utf-8"))
        acceso = _token_de_acceso(credenciales)
        proyecto = credenciales["project_id"]
    except Exception as e:  # credenciales mal formadas, sin red, etc.
        logger.warning("No se pudieron enviar notificaciones push: %s", e)
        return
    invalidos = []
    for token in tokens:
        if not _enviar_uno(proyecto, acceso, token, titulo, cuerpo, datos):
            invalidos.append(token)
    if invalidos:
        _borrar_tokens(invalidos)


def _token_de_acceso(credenciales: dict[str, Any]) -> str:
    """Token OAuth2 de la cuenta de servicio (se reutiliza mientras no venza)."""
    with _lock:
        if _cache_token["valor"] and _cache_token["expira"] > time.time() + 60:
            return _cache_token["valor"]
    import jwt  # PyJWT; firmar RS256 requiere el paquete "cryptography"

    ahora = int(time.time())
    url_token = credenciales.get("token_uri", "https://oauth2.googleapis.com/token")
    asercion = jwt.encode(
        {"iss": credenciales["client_email"], "scope": ALCANCE, "aud": url_token, "iat": ahora, "exp": ahora + 3600},
        credenciales["private_key"],
        algorithm="RS256",
    )
    cuerpo = urllib.parse.urlencode(
        {"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer", "assertion": asercion}
    ).encode()
    with urllib.request.urlopen(urllib.request.Request(url_token, data=cuerpo), timeout=10) as r:
        respuesta = json.loads(r.read())
    with _lock:
        _cache_token["valor"] = respuesta["access_token"]
        _cache_token["expira"] = time.time() + int(respuesta.get("expires_in", 3600))
        return _cache_token["valor"]


def _enviar_uno(proyecto: str, acceso: str, token: str, titulo: str, cuerpo: str, datos: dict[str, str]) -> bool:
    """Devuelve False si el token ya no es válido (la app se desinstaló o cambió de token)."""
    mensaje = {
        "message": {
            "token": token,
            "notification": {"title": titulo, "body": cuerpo[:200]},
            "data": {"title": titulo, "body": cuerpo[:200], **datos},
            "android": {"priority": "high"},
        }
    }
    peticion = urllib.request.Request(
        f"https://fcm.googleapis.com/v1/projects/{proyecto}/messages:send",
        data=json.dumps(mensaje).encode(),
        headers={"Authorization": f"Bearer {acceso}", "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(peticion, timeout=10):
            return True
    except urllib.error.HTTPError as e:
        if e.code in (400, 404):
            return False
        logger.warning("FCM respondió %s al enviar una notificación", e.code)
        return True
    except Exception as e:
        logger.warning("No se pudo enviar una notificación push: %s", e)
        return True


def _borrar_tokens(tokens: list[str]) -> None:
    from .db import SessionLocal

    with SessionLocal() as db:
        for fila in db.scalars(select(DeviceToken).where(DeviceToken.device_token.in_(tokens))).all():
            db.delete(fila)
        db.commit()
