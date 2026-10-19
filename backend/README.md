# Backend Sonrisas - Control Escolar y Comunicación en Estancias Infantiles

Servidor backend desarrollado con **FastAPI**, **PostgreSQL 16**, **RustFS (S3)** y empaquetado en **Docker Compose**, siguiendo la arquitectura de referencia de la **Práctica 11** (`labs/practica11` / `practica-11-servidor-con-pruebas`).

Implementa la totalidad del contrato de especificación definido en `API_ENTRYPOINTS.md`.

---

## 1. Estructura del Proyecto

```text
backend/
├── Dockerfile                   # Imagen ligera Python 3.12 con uv
├── docker-compose.yml           # Orquestación: db (Postgres), almacen (RustFS S3), api y túnel
├── pyproject.toml               # Dependencias del proyecto y configuración de pytest
├── .env.example                 # Plantilla de variables de entorno
├── .env                         # Configuración local de secretos y claves S3
├── smoke.sh                     # Script de pruebas de humo contra el servidor
├── app/
│   ├── config.py                # Ajustes centralizados con pydantic-settings
│   ├── db.py                    # Engine, DeclarativeBase y sesión SQLAlchemy 2.0
│   ├── security.py              # Argon2id para contraseñas y PyJWT para tokens de sesión
│   ├── deps.py                  # Inyección de dependencias y control de acceso (RBAC)
│   ├── errors.py                # Manejador de errores estandarizado ({error, code, hint})
│   ├── almacen.py               # Almacén de objetos compatible con Amazon S3 / RustFS
│   ├── limpieza.py              # Depuración de archivos multimedia huérfanos
│   ├── models.py                # Modelos ORM relacionales de todas las entidades
│   ├── schemas.py               # Esquemas Pydantic v2 para validación y serialización
│   ├── seed.py                  # Población de datos iniciales (salas, alumnos, usuarios)
│   ├── main.py                  # Instancia FastAPI, middlewares, lifespan y montaje de routers
│   └── routers/
│       ├── auth.py              # /auth (login, register, refresh, logout, me)
│       ├── rooms.py             # /rooms (listado, detalle y alumnos por sala)
│       ├── children.py          # /children y /parents (expedientes, certificados médicos)
│       ├── attendance.py        # /attendance (ingreso secuencial, egreso y registros)
│       ├── bitacora.py          # /children/{id}/timeline, bitacoras y actividades
│       ├── weekly_summaries.py  # /children/{id}/weekly-summaries (resúmenes pedagógicos)
│       ├── weekly_observations.py # /weekly-observations (comentarios de familia y educadora)
│       ├── daily_plans.py       # /daily-plans (planeación y menú diario)
│       ├── daily_logs.py        # /daily-logs (registro diario / hoja oficial en PDF)
│       ├── staff_chats.py       # /staff y /chats/workers (mensajería interna del personal)
│       ├── parent_chats.py      # /chats/bitacora (comunicación directa familia-docente)
│       ├── family_groups.py     # /family-groups (grupos de mensajería con familias)
│       ├── announcements.py     # /announcements (circulares del centro y contador no leídos)
│       ├── suggestions.py       # /suggestions (buzón de quejas, sugerencias y respuestas)
│       ├── lost_objects.py      # /lost-objects (catálogo de objetos perdidos y reclamos)
│       ├── media.py             # /media (subida multipart y streaming con caché ETag)
│       └── ws_notifications.py  # /notifications y /ws/v1/connect (WebSockets en tiempo real)
└── tests/
    ├── conftest.py              # Fixtures compartidas, TestClient y perfiles de sesión
    ├── test_auth.py             # Pruebas de autenticación, credenciales y refresh
    ├── test_permisos.py         # Pruebas de control de acceso por roles (RBAC)
    ├── test_seguridad.py        # Pruebas de tokens expirados, firmas y hashes Argon2
    ├── test_contrato.py         # Pruebas de cumplimiento de contrato de API_ENTRYPOINTS.md
    └── test_modulos_heber.py    # Grupos con familias, observaciones, planeación y registro diario
```

---

## 2. Cuentas de Prueba Preconfiguradas (Seed)

Al iniciar el servidor por primera vez, la base de datos se inicializa automáticamente con las siguientes cuentas de prueba:

| Rol | Correo Electrónico | Contraseña | Identificador |
| :--- | :--- | :--- | :--- |
| **FAMILIAR** | `familiar@sonrisas.edu` | `Password123!` | `usr_familia_garcia` |
| **EDUCADORA** | `educadora@sonrisas.edu` | `Password123!` | `usr_educadora_laura` |
| **DIRECCION** | `direccion@sonrisas.edu` | `Password123!` | `usr_dir_mendez` |

---

## 3. Puesta en Marcha con Docker Compose

Para arrancar el backend completo (Base de datos PostgreSQL, Almacén de objetos S3 y API FastAPI con recarga en vivo):

```bash
cd backend
docker compose up -d
```

- **API REST**: `http://localhost:8000/api/v1`
- **Documentación Swagger UI interactiva**: `http://localhost:8000/docs`
- **Healthcheck**: `http://localhost:8000/api/v1/health`
- **Consola Web de RustFS (S3)**: `http://localhost:9001` (Usuario: `sonrisas-admin`, Clave: `sonrisas-secret-key-12345678`)

Para detener los servicios:
```bash
docker compose down
```

---

## 4. Ejecución de Pruebas Unitarias

### Dentro del contenedor Docker:
```bash
docker compose exec api uv run --no-sync pytest
```

### Directamente en el entorno local (con `uv`):
```bash
cd backend
uv run pytest
```

---

## 5. Pruebas de Humo (Smoke Test)

Se incluye el script `smoke.sh` que comprueba de extremo a extremo el flujo completo contra el servidor levantado:

```bash
cd backend
bash smoke.sh
# O contra otra dirección:
bash smoke.sh http://127.0.0.1:8000/api/v1
```
