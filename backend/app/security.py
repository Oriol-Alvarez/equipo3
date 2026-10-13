"""Módulo de seguridad criptográfica: Argon2id para contraseñas y PyJWT para tokens de sesión.
Basado en las especificaciones de seguridad de la Práctica 11.
"""

import hashlib
import secrets
import time
from typing import Any

import jwt
from pwdlib import PasswordHash

# Argon2id con parámetros recomendados de la industria
_hasher = PasswordHash.recommended()
ALGORITMO = "HS256"


def hash_password(password: str) -> str:
    """Calcula el hash Argon2id de la contraseña (incluye sal interna única)."""
    return _hasher.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    """Verifica si la contraseña coincide con el hash almacenado."""
    return _hasher.verify(password, password_hash)


def sign_jwt(
    usuario_id: str,
    email: str,
    role: str,
    display_name: str,
    ttl_s: int,
    secret: str,
) -> str:
    """Genera y firma un JWT de acceso (HS256) con el usuario, rol e instante de caducidad."""
    ahora = int(time.time())
    payload = {
        "sub": usuario_id,
        "email": email,
        "role": role,
        "displayName": display_name,
        "iat": ahora,
        "exp": ahora + ttl_s,
    }
    return jwt.encode(payload, secret, algorithm=ALGORITMO)


class TokenInvalido(Exception):
    """Excepción para tokens alterados o caducados ("expirado" | "invalido")."""

    def __init__(self, reason: str) -> None:
        super().__init__(reason)
        self.reason = reason


def verify_jwt(token: str, secret: str) -> dict[str, Any]:
    """Decodifica y valida la firma y vigencia del JWT."""
    try:
        return jwt.decode(token, secret, algorithms=[ALGORITMO])
    except jwt.ExpiredSignatureError as e:
        raise TokenInvalido("expirado") from e
    except jwt.InvalidTokenError as e:
        raise TokenInvalido("invalido") from e


def random_token() -> str:
    """Genera una cadena pseudoaleatoria segura de 32 bytes para refresh token."""
    return secrets.token_urlsafe(32)


def sha256(texto: str) -> str:
    """Calcula el hash SHA-256 de una cadena de texto."""
    return hashlib.sha256(texto.encode("utf-8")).hexdigest()
