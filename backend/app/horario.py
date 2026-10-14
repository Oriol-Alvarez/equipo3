"""Hora local de la estancia (Ciudad de México, UTC-6 sin horario de verano)."""

from datetime import date, datetime, timedelta, timezone

ZONA_ESTANCIA = timezone(timedelta(hours=-6), "America/Mexico_City")


def ahora() -> datetime:
    return datetime.now(ZONA_ESTANCIA)


def hora_12h() -> str:
    """Ej. '09:05 AM' (formato que usa la app en los chats)."""
    return ahora().strftime("%I:%M %p")


def fecha_y_hora_corta() -> str:
    """Ej. '19/09 07:40 PM' (formato de las observaciones semanales)."""
    return ahora().strftime("%d/%m %I:%M %p")


def hoy() -> date:
    return ahora().date()
