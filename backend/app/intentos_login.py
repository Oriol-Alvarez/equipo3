"""Bloqueo de inicio de sesión por cuenta tras varios intentos fallidos.

El límite por IP de slowapi no sirve detrás del túnel de Cloudflare: todas las
peticiones llegan con la misma IP y un solo usuario bloqueaba a todo el equipo.
Aquí se cuentan solo los intentos FALLIDOS de cada correo dentro de una ventana
de tiempo; un inicio de sesión correcto limpia el contador.
"""

import threading
import time
from collections import deque

MAX_FALLIDOS = 5
VENTANA_S = 60


class IntentosLogin:
    def __init__(self, max_fallidos: int = MAX_FALLIDOS, ventana_s: int = VENTANA_S) -> None:
        self.max_fallidos = max_fallidos
        self.ventana_s = ventana_s
        self._fallidos: dict[str, deque[float]] = {}
        self._lock = threading.Lock()

    def _limpiar(self, email: str, ahora: float) -> deque[float]:
        cola = self._fallidos.setdefault(email, deque())
        while cola and ahora - cola[0] >= self.ventana_s:
            cola.popleft()
        return cola

    def segundos_bloqueado(self, email: str) -> int:
        """0 si el correo puede intentar; si no, segundos que faltan para desbloquearse."""
        ahora = time.monotonic()
        with self._lock:
            cola = self._limpiar(email, ahora)
            if len(cola) < self.max_fallidos:
                return 0
            return max(1, int(self.ventana_s - (ahora - cola[0])) + 1)

    def registrar_fallo(self, email: str) -> None:
        with self._lock:
            self._limpiar(email, time.monotonic()).append(time.monotonic())

    def registrar_exito(self, email: str) -> None:
        with self._lock:
            self._fallidos.pop(email, None)

    def reiniciar(self) -> None:
        with self._lock:
            self._fallidos.clear()


intentos_login = IntentosLogin()
