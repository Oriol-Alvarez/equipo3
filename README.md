# Sonrisas de Cristal — Equipo 3

App Android (Kotlin + Jetpack Compose) para la estancia infantil, con su backend en FastAPI.

| Carpeta | Qué es |
| --- | --- |
| `app/` | App Android |
| `backend/` | Servidor (FastAPI + PostgreSQL + RustFS), ver `backend/README.md` |
| `docs/` | Notas de cada módulo |
| `API_ENTRYPOINTS.md` | Contrato de la API |

## 1. Levantar el backend

```bash
cd backend
cp .env.example .env              # solo la primera vez
docker compose up -d --build
docker compose exec api uv run --no-sync pytest    # pruebas del backend
```

Al arrancar se crean las salas, los niños y estas cuentas (contraseña `Password123!`):

| Cuenta | Rol |
| --- | --- |
| `educadora@sonrisas.edu` | Educadora (Sala 1A) |
| `direccion@sonrisas.edu` | Dirección |
| `familiar@sonrisas.edu` | Familia (mamá de Mateo) |

Después de 5 contraseñas equivocadas seguidas la cuenta se bloquea 1 minuto.

> Si `uv.lock` cambia al construir es normal: se agregó `pyjwt[crypto]` (lo usan las notificaciones push).

## 2. Correr la app

La dirección del servidor está fija en `app/src/main/java/com/example/etapa1/data/remote/NetworkClient.kt`:

```kotlin
const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1/"
```

Para usar otro servidor (por ejemplo un túnel de Cloudflare), se cambia esa línea.

Luego en Android Studio se corre la app. `10.0.2.2` es la computadora vista desde el emulador.

Para probar sin servidor, poner `AppContainer.USE_MOCKS = true` en `SonrisasApplication.kt` (datos de ejemplo).

Pruebas de la app:

```powershell
.\gradlew.bat testDebugUnitTest
# contra el servidor real (con el backend corriendo en localhost:8000):
$env:SONRISAS_IT="1"; .\gradlew.bat testDebugUnitTest --tests "*BackendIntegrationTest"
```

## 3. Notificaciones push (opcional)

Sin estos dos archivos todo funciona igual, solo no llegan notificaciones. Ninguno se sube a git.

1. **App:** en la consola de Firebase, agregar la app Android `com.example.etapa1`, descargar
   `google-services.json` y ponerlo en `app/google-services.json`.
2. **Servidor:** en Firebase > Configuración del proyecto > Cuentas de servicio > *Generar nueva
   clave privada*. Guardar el archivo como `backend/secretos/firebase.json` y en `backend/.env` poner:
   ```
   FIREBASE_CREDENTIALS=/code/secretos/firebase.json
   ```
   y reiniciar con `docker compose up -d`.

Se avisa por push de: mensajes de la bitácora, actividades nuevas, anuncios, respuestas a
sugerencias y mensajes en grupos con familias. El teléfono se registra al iniciar sesión y se da
de baja al cerrar sesión.

## 4. Cómo trabajamos con git

- `main`: lo estable. `integracion`: donde se juntan los cambios de todos antes de pasar a `main`.
- Antes de seguir trabajando, traer lo último: `git pull origin integracion` (o hacer merge de `integracion` en tu rama).
- No subir `local.properties`, `app/google-services.json` ni `backend/secretos/`.
