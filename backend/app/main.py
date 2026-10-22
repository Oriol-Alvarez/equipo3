"""Servidor Principal FastAPI de Sonrisas App.
Implementa todos los entrypoints de la API v1 descritos en API_ENTRYPOINTS.md.
Estructura y diseño basados en la arquitectura de Practica 11.
"""

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
try:
    from slowapi import _rate_limit_exceeded_handler
    from slowapi.errors import RateLimitExceeded
except ImportError:
    _rate_limit_exceeded_handler = None
    RateLimitExceeded = None

from . import almacen, errors
from .carga_inicial import carga_inicial_produccion
from .idempotencia import idempotencia
from .rate_limit import limiter
from .config import settings
from .db import Base, SessionLocal, engine
from .routers import (
    admin,
    announcements,
    attendance,
    auth,
    bitacora,
    children,
    daily_logs,
    daily_plans,
    family_groups,
    lost_objects,
    media,
    parent_chats,
    rooms,
    staff_chats,
    suggestions,
    weekly_observations,
    weekly_summaries,
    ws_notifications,
)
from .seed import seed_database, seed_modulos_heber, seed_salas_adicionales


from sqlalchemy import text


def _aplicar_migraciones_compatibilidad() -> None:
    """Añade columnas nuevas si las tablas ya existían previamente sin necesidad de purgar volúmenes."""
    try:
        with engine.connect() as conn:
            if engine.dialect.name == "postgresql":
                conn.execute(text("ALTER TABLE child_profiles ADD COLUMN IF NOT EXISTS emergency_phone_relation VARCHAR(60);"))
                conn.execute(text("ALTER TABLE bitacora_messages ADD COLUMN IF NOT EXISTS date VARCHAR(20) DEFAULT '';"))
                # RF-14: quién capturó cada registro (columnas nuevas de la entrega final)
                for tabla in ("activities", "timeline_events"):
                    conn.execute(text(f"ALTER TABLE {tabla} ADD COLUMN IF NOT EXISTS created_by_id VARCHAR(64);"))
                    conn.execute(text(f"ALTER TABLE {tabla} ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(120);"))
                conn.execute(text("ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS recorded_by_id VARCHAR(64);"))
                conn.execute(text("ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS recorded_by_name VARCHAR(120);"))
                conn.commit()
    except Exception:
        pass


@asynccontextmanager
async def lifespan(_: FastAPI):
    """Inicialización del ciclo de vida: creación de esquemas en base de datos, preparación S3 y carga de datos."""
    Base.metadata.create_all(engine)
    _aplicar_migraciones_compatibilidad()
    almacen.preparar()

    with SessionLocal() as db:
        if settings.datos_demo:
            # Desarrollo y demostración: cuentas y niños de ejemplo
            seed_database(db)
            seed_modulos_heber(db)
            seed_salas_adicionales(db)
        else:
            # Estancia real: solo la cuenta de Dirección y las salas (plan de implantación)
            carga_inicial_produccion(db)

    yield

app = FastAPI(
    title="Sonrisas API - Control Escolar y Comunicación",
    version="1.0.0",
    description="Backend oficial para la aplicación móvil Sonrisas (Estancias Infantiles).",
    lifespan=lifespan,
)

app.state.limiter = limiter
if RateLimitExceeded is not None and _rate_limit_exceeded_handler is not None:
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# ---------------------------------------------------------------------------
# Middlewares
# ---------------------------------------------------------------------------

# RF-11: los reintentos de la cola sin conexión no duplican registros (Idempotency-Key).
# Se registra antes que las cabeceras de seguridad para que las respuestas repetidas también las lleven.
app.middleware("http")(idempotencia)


@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    """Integración de cabeceras de seguridad en HTTP."""
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    response.headers["Content-Security-Policy"] = "default-src 'self'"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Cross-Origin-Resource-Policy"] = "same-origin"
    return response

# Instalación del manejador de errores estandarizado
errors.instalar(app)

# Configuración de CORS para llamadas desde emuladores Android, web y pruebas
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Inclusión de Routers (prefijo oficial /api/v1)
# ---------------------------------------------------------------------------
routers_list = [
    admin.router,
    auth.router,
    rooms.router,
    children.router,
    attendance.router,
    bitacora.router,
    weekly_summaries.router,
    weekly_observations.router,
    daily_plans.router,
    daily_logs.router,
    staff_chats.router,
    parent_chats.router,
    family_groups.router,
    announcements.router,
    suggestions.router,
    lost_objects.router,
    media.router,
    ws_notifications.router,
]

for r in routers_list:
    app.include_router(r, prefix="/api/v1")
    app.include_router(r, prefix="/api")

@app.get("/health")
@app.get("/api/health")
def health_check() -> dict:
    return {
        "status": "healthy",
        "app": "Sonrisas API - Backend Estancias Infantiles",
        "version": "1.0.0",
        "database": settings.database_url.split("://")[0],
        "s3Bucket": settings.s3_bucket,
        "auth": {
            "tokenType": "Bearer",
            "accessTtlSeconds": settings.access_ttl_s,
            "rolesDisponibles": ["EDUCADORA", "FAMILIAR", "DIRECCION"],
        },
    }
