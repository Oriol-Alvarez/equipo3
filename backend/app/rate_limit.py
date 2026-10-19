"""Configuración de limitación de tasa (Rate Limiting) con slowapi para mitigar fuerza bruta."""
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
