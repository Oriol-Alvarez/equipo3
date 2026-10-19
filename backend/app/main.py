"""Servidor Principal FastAPI de Sonrisas App.
Implementa todos los entrypoints de la API v1 descritos en API_ENTRYPOINTS.md.
Estructura y diseño basados en la arquitectura de Practica 11.
"""

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from . import almacen, errors
from .rate_limit import limiter
from .config import settings
from .db import Base, SessionLocal, engine
from .routers import (
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
from .seed import seed_database, seed_modulos_heber


@asynccontextmanager
async def lifespan(_: FastAPI):
    """Inicialización del ciclo de vida: creación de esquemas en base de datos, preparación S3 y carga de datos."""
    Base.metadata.create_all(engine)
    almacen.preparar()

    with SessionLocal() as db:
        seed_database(db)
        seed_modulos_heber(db)

    yield

app = FastAPI(
    title="Sonrisas API - Control Escolar y Comunicación",
    version="1.0.0",
    description="Backend oficial para la aplicación móvil Sonrisas (Estancias Infantiles).",
    lifespan=lifespan,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# ---------------------------------------------------------------------------
# Middlewares
# ---------------------------------------------------------------------------

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
