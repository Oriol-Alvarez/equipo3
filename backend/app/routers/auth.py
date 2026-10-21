"""Módulo 1: Autenticación y Sesión (/auth)"""

import time
import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Request, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import settings
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..intentos_login import intentos_login
from ..rate_limit import limiter
from ..models import Sesion, Usuario
from ..schemas import (
    LoginRequest,
    LoginResponse,
    RefreshRequest,
    RefreshResponse,
    RegisterRequest,
    UserProfileOut,
)
from ..security import (
    hash_password,
    random_token,
    sha256,
    sign_jwt,
    verify_password,
)

router = APIRouter(prefix="/auth", tags=["auth"])


def emitir_sesion(db: Session, user: Usuario) -> tuple[str, str, int]:
    """Genera par JWT y refresh token, persistiendo el hash de refresh."""
    ahora_epoch = int(time.time())
    access_token = sign_jwt(
        usuario_id=user.id,
        email=user.email,
        role=user.role,
        display_name=user.display_name,
        ttl_s=settings.access_ttl_s,
        secret=settings.jwt_secret,
    )
    refresh_plain = random_token()
    nueva_sesion = Sesion(
        usuario_id=user.id,
        refresh_hash=sha256(refresh_plain),
        expires_at=ahora_epoch + settings.refresh_ttl_s,
    )
    db.add(nueva_sesion)
    db.commit()
    return access_token, refresh_plain, settings.access_ttl_s


@router.post("/login", response_model=LoginResponse)
@limiter.limit("60/minute")
def login(request: Request, body: LoginRequest, db: Session = Depends(get_db)) -> LoginResponse:
    """Valida credenciales según el rol seleccionado y devuelve el token JWT y el perfil.

    Protección contra fuerza bruta: tras varios intentos fallidos se bloquea solo ese
    correo por un minuto (ver intentos_login.py). El límite por IP es amplio porque,
    detrás del túnel de Cloudflare, todos los usuarios comparten la misma IP.
    """
    email = body.email.strip().lower()
    espera = intentos_login.segundos_bloqueado(email)
    if espera:
        raise ApiError(
            429,
            "Demasiados intentos fallidos",
            "demasiados_intentos",
            f"Espera {espera} segundos antes de volver a intentar con este correo.",
        )

    user = db.scalar(select(Usuario).where(Usuario.email == email))
    if user is None or not verify_password(body.password, user.password_hash):
        intentos_login.registrar_fallo(email)
        raise ApiError(
            401,
            "Credenciales incorrectas",
            "credenciales_invalidas",
            "El correo o la contraseña no coinciden.",
        )
    intentos_login.registrar_exito(email)

    # Validar coherencia del rol solicitado si se envía
    if body.role and user.role != body.role and user.role != "DIRECCION":
        raise ApiError(
            403,
            f'El usuario no tiene el rol "{body.role}"',
            "rol_invalido",
            f'Tu cuenta tiene asignado el rol "{user.role}".',
        )

    token, refresh, ttl = emitir_sesion(db, user)
    return LoginResponse(
        token=token,
        refreshToken=refresh,
        expiresIn=ttl,
        user=UserProfileOut.model_validate(user),
    )


@router.post("/register", response_model=LoginResponse, status_code=201)
@limiter.limit("20/minute")
def register(request: Request, body: RegisterRequest, db: Session = Depends(get_db)) -> LoginResponse:
    """Registra una nueva cuenta de usuario (útil para pruebas y altas iniciales)."""
    email_limpio = body.email.strip().lower()
    existente = db.scalar(select(Usuario).where(Usuario.email == email_limpio))
    if existente:
        raise ApiError(
            409,
            f'El correo "{email_limpio}" ya está registrado',
            "email_duplicado",
            "Inicia sesión con POST /auth/login.",
        )

    # El rol DIRECCION o EDUCADORA solo se asigna si coincide el código del centro
    rol = "FAMILIAR"
    if body.role in ("EDUCADORA", "DIRECCION"):
        if body.codigoProfesor == settings.codigo_profesor:
            rol = body.role
        else:
            rol = "FAMILIAR"

    nuevo_id = f"usr_{uuid.uuid4().hex[:12]}"
    user = Usuario(
        id=nuevo_id,
        email=email_limpio,
        password_hash=hash_password(body.password),
        role=rol,
        display_name=body.displayName,
        phone=body.phone,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    token, refresh, ttl = emitir_sesion(db, user)
    return LoginResponse(
        token=token,
        refreshToken=refresh,
        expiresIn=ttl,
        user=UserProfileOut.model_validate(user),
    )


@router.post("/refresh", response_model=RefreshResponse)
def refresh(body: RefreshRequest, db: Session = Depends(get_db)) -> RefreshResponse:
    """Renueva el token de acceso expirado utilizando el token de refresco con rotación."""
    r_hash = sha256(body.refreshToken)
    sesion = db.scalar(select(Sesion).where(Sesion.refresh_hash == r_hash))
    ahora_epoch = int(time.time())

    if sesion is None or sesion.revoked_at is not None or sesion.expires_at <= ahora_epoch:
        raise ApiError(
            401,
            "El token de refresco no es válido o ha expirado",
            "refresh_invalido",
            "Debes iniciar sesión nuevamente.",
        )

    # Rotación: revocar el token usado y generar nuevo access token
    sesion.revoked_at = datetime.now(timezone.utc)
    user = db.get(Usuario, sesion.usuario_id)
    if user is None:
        raise ApiError(401, "Usuario no encontrado", "usuario_inexistente")

    token, new_refresh, ttl = emitir_sesion(db, user)
    return RefreshResponse(token=token, refreshToken=new_refresh, expiresIn=ttl)


@router.post("/logout", status_code=204)
def logout(body: RefreshRequest, db: Session = Depends(get_db)) -> Response:
    """Revoca la sesión asociada al token de refresco."""
    r_hash = sha256(body.refreshToken)
    sesion = db.scalar(select(Sesion).where(Sesion.refresh_hash == r_hash, Sesion.revoked_at.is_(None)))
    if sesion:
        sesion.revoked_at = datetime.now(timezone.utc)
        db.commit()
    return Response(status_code=204)


@router.get("/me", response_model=UserProfileOut)
def me(quien: Identidad = Depends(usuario_actual), db: Session = Depends(get_db)) -> UserProfileOut:
    """Obtiene los datos del perfil del usuario actualmente autenticado."""
    user = db.get(Usuario, quien.id)
    if user is None:
        raise ApiError(404, "Usuario no encontrado")
    return UserProfileOut.model_validate(user)


@router.delete("/sesiones", status_code=200)
def cerrar_todas_las_sesiones(quien: Identidad = Depends(usuario_actual), db: Session = Depends(get_db)) -> dict[str, int]:
    """Cierra todas las sesiones activas del usuario."""
    filas = db.scalars(select(Sesion).where(Sesion.usuario_id == quien.id, Sesion.revoked_at.is_(None))).all()
    for s in filas:
        s.revoked_at = datetime.now(timezone.utc)
    db.commit()
    return {"revoked": len(filas)}
