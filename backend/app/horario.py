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


def fecha_local(momento: datetime | None) -> str:
    """Fecha 'dd/mm/aaaa' en hora de la estancia de un timestamp guardado por la base de datos.

    PostgreSQL guarda `now()` en UTC sin zona; sin convertir, lo registrado después de las
    6:00 p. m. aparecía con la fecha del día siguiente.
    """
    if momento is None:
        return hoy().strftime("%d/%m/%Y")
    if momento.tzinfo is None:
        momento = momento.replace(tzinfo=timezone.utc)
    return momento.astimezone(ZONA_ESTANCIA).strftime("%d/%m/%Y")


def minutos_del_dia(hora: str | None) -> int | None:
    """Minutos desde medianoche de '08:15', '8:15 AM', '02:30 PM' o '14:30 PM'.

    Si la hora trae más de 12 se toma como formato de 24 h y se ignora el AM/PM: versiones
    anteriores de la app mandaban '14:30 AM' o '09:00 PM' desde el formulario de asistencia.
    """
    if not hora:
        return None
    texto = hora.strip().upper().replace(".", "").replace("HRS", "").strip()
    sufijo = None
    for s in ("AM", "PM"):
        if texto.endswith(s):
            sufijo = s
            texto = texto[: -len(s)].strip()
    partes = texto.split(":")
    if len(partes) != 2 or not partes[0].isdigit() or not partes[1].isdigit():
        return None
    h, m = int(partes[0]), int(partes[1])
    if not (0 <= h <= 23 and 0 <= m <= 59):
        return None
    if sufijo and 1 <= h <= 12:
        if sufijo == "PM" and h != 12:
            h += 12
        elif sufijo == "AM" and h == 12:
            h = 0
    return h * 60 + m


def hora_12h_de(minutos: int) -> str:
    """480 -> '08:00 AM' (formato que muestra la app)."""
    return datetime(2000, 1, 1, minutos // 60, minutos % 60).strftime("%I:%M %p")
