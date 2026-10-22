"""RF-11 / RNF-05: reintentos sin duplicados con la cabecera `Idempotency-Key`.

La app guarda en el teléfono lo que la educadora registra sin internet y lo manda cuando
vuelve la conexión. Cada registro lleva una llave única que no cambia entre reintentos.

- Primera vez que llega una llave: se atiende normal y, si salió bien (2xx), se guarda la respuesta.
- Si la misma persona vuelve a mandar la misma llave: se devuelve la respuesta guardada sin
  ejecutar de nuevo la operación (no se duplica la asistencia, la actividad ni el mensaje).
- La llave se guarda junto con el usuario del token, así nadie puede "adivinar" la respuesta de otro.
"""

import hashlib

from fastapi import Request
from fastapi.responses import JSONResponse, Response
from sqlalchemy.exc import IntegrityError

from .config import settings
from .db import SessionLocal
from .models import IdempotencyRecord
from .security import verify_jwt

METODOS_CON_EFECTO = {"POST", "PUT", "PATCH", "DELETE"}
LARGO_MAXIMO_LLAVE = 100


def _usuario_del_token(request: Request) -> str | None:
    cabecera = request.headers.get("authorization", "")
    if not cabecera.lower().startswith("bearer "):
        return None
    try:
        return str(verify_jwt(cabecera[7:].strip(), settings.jwt_secret)["sub"])
    except Exception:
        return None  # el endpoint responderá 401 con el formato normal


def _id_registro(usuario: str, llave: str) -> str:
    return hashlib.sha256(f"{usuario}:{llave}".encode()).hexdigest()


async def idempotencia(request: Request, call_next):
    llave = request.headers.get("idempotency-key", "").strip()
    if request.method not in METODOS_CON_EFECTO or not llave:
        return await call_next(request)

    if len(llave) > LARGO_MAXIMO_LLAVE:
        return JSONResponse(
            {"error": "La llave de reintento es demasiado larga", "code": "llave_invalida",
             "hint": f"Idempotency-Key admite hasta {LARGO_MAXIMO_LLAVE} caracteres."},
            status_code=400,
        )

    usuario = _usuario_del_token(request)
    if usuario is None:
        return await call_next(request)

    rid = _id_registro(usuario, llave)
    with SessionLocal() as db:
        previo = db.get(IdempotencyRecord, rid)
        if previo is not None:
            if previo.method != request.method or previo.path != request.url.path:
                return JSONResponse(
                    {"error": "Esa llave de reintento ya se usó en otra operación", "code": "llave_reutilizada",
                     "hint": "Genera una Idempotency-Key nueva para cada registro."},
                    status_code=422,
                )
            return Response(
                content=previo.body,
                status_code=previo.status_code,
                media_type=previo.content_type,
                headers={"Idempotent-Replayed": "true"},
            )

    respuesta = await call_next(request)
    if not (200 <= respuesta.status_code < 300):
        return respuesta

    cuerpo = b"".join([trozo async for trozo in respuesta.body_iterator])
    tipo = respuesta.headers.get("content-type", "application/json")
    try:
        texto = cuerpo.decode("utf-8")
    except UnicodeDecodeError:
        texto = None

    if texto is not None:
        with SessionLocal() as db:
            db.add(
                IdempotencyRecord(
                    id=rid,
                    user_id=usuario,
                    method=request.method,
                    path=request.url.path,
                    status_code=respuesta.status_code,
                    body=texto,
                    content_type=tipo,
                )
            )
            try:
                db.commit()
            except IntegrityError:
                db.rollback()  # otro reintento simultáneo ya la guardó

    cabeceras = {k: v for k, v in respuesta.headers.items() if k.lower() != "content-length"}
    return Response(content=cuerpo, status_code=respuesta.status_code, headers=cabeceras)
