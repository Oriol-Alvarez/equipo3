"""Configuración de limitación de tasa (Rate Limiting) con slowapi para mitigar fuerza bruta."""
try:
    from slowapi import Limiter
    from slowapi.util import get_remote_address
    limiter = Limiter(key_func=get_remote_address)
except ImportError:
    class DummyLimiter:
        enabled: bool = True
        def limit(self, *args, **kwargs):
            return lambda fn: fn
    limiter = DummyLimiter()

