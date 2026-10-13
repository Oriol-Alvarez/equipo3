"""Dependencias de FastAPI para autenticación y control de acceso basado en roles (RBAC)."""

from dataclasses import dataclass

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .config import settings
from .errors import ApiError
from .security import TokenInvalido, verify_jwt


@dataclass
class Identidad:
    id: str
    email: str
    role: str  # "EDUCADORA" | "FAMILIAR" | "DIRECCION"
    display_name: str
    exp: int


# Esquema HTTPBearer con auto_error=False para devolver respuestas 401 con el contrato del curso
_bearer = HTTPBearer(auto_error=False)


def usuario_actual(credenciales: HTTPAuthorizationCredentials | None = Depends(_bearer)) -> Identidad:
    """Extrae y verifica el token JWT de la cabecera `Authorization: Bearer <token>`."""
    if credenciales is None:
        raise ApiError(
            401,
            "Falta el token de acceso",
            "falta_token",
            "Incluye la cabecera `Authorization: Bearer <accessToken>`.",
        )

    token = credenciales.credentials
    try:
        payload = verify_jwt(token, settings.jwt_secret)
    except TokenInvalido as e:
        if e.reason == "expirado":
            raise ApiError(
                401,
                "El token de acceso expiró",
                "token_expirado",
                "Solicita un nuevo token mediante POST /auth/refresh.",
            )
        raise ApiError(
            401,
            "El token no es válido",
            "token_invalido",
            "La firma no coincide o el token está corrupto.",
        )

    return Identidad(
        id=payload["sub"],
        email=payload.get("email", ""),
        role=payload.get("role", "FAMILIAR"),
        display_name=payload.get("displayName", ""),
        exp=payload.get("exp", 0),
    )


def requiere_educadora(quien: Identidad = Depends(usuario_actual)) -> Identidad:
    """Requiere que el usuario tenga rol EDUCADORA o DIRECCION."""
    if quien.role not in ("EDUCADORA", "DIRECCION"):
        raise ApiError(
            403,
            "Permiso denegado: solo el personal educativo o directivo puede realizar esta acción",
            "rol_insuficiente",
            f'Tu rol actual es "{quien.role}". Se requiere "EDUCADORA" o "DIRECCION".',
        )
    return quien


def requiere_familiar(quien: Identidad = Depends(usuario_actual)) -> Identidad:
    """Requiere que el usuario tenga rol FAMILIAR o DIRECCION."""
    if quien.role not in ("FAMILIAR", "DIRECCION"):
        raise ApiError(
            403,
            "Permiso denegado: esta acción está reservada para familiares",
            "rol_insuficiente",
            f'Tu rol actual es "{quien.role}". Se requiere "FAMILIAR".',
        )
    return quien


def requiere_direccion(quien: Identidad = Depends(usuario_actual)) -> Identidad:
    """Requiere rol administrativo DIRECCION."""
    if quien.role != "DIRECCION":
        raise ApiError(
            403,
            "Permiso denegado: solo la dirección del centro puede realizar esta acción",
            "rol_insuficiente",
            f'Tu rol actual es "{quien.role}". Se requiere "DIRECCION".',
        )
    return quien
