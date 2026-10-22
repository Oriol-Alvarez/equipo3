# Sonrisas de Cristal - Equipo 3

App Android (Kotlin + Jetpack Compose) para la estancia infantil Sonrisas de Cristal, con su
servidor en FastAPI. Sustituye el control en papel y los grupos de WhatsApp: asistencia, bitácora
diaria de cada niño, mensajes con las familias, avisos, resumen semanal y panel de Dirección.

| Carpeta | Qué es |
| --- | --- |
| `app/` | App Android (una sola app para Educadora, Familia y Dirección) |
| `backend/` | Servidor: FastAPI + PostgreSQL + RustFS (fotos), todo en Docker |
| `docs/` | Notas de cada módulo y `entrega-final.md` (cambios y evidencia de la entrega final) |
| `API_ENTRYPOINTS.md` | Contrato de la API |

## Requisitos

| Para | Se necesita |
| --- | --- |
| Servidor | Docker Desktop (Windows/macOS) o Docker Engine con Compose (Linux). Nada más: Python, la base de datos y el almacén de fotos corren en contenedores. |
| Compilar la app | Android Studio (con su JDK incluido) o solo el SDK de Android y `gradlew`. |
| Usar la app | Teléfono con Android 7.0 o superior. Para el bloqueo con huella, el teléfono debe tener huella, rostro o PIN configurado. |

## 1. Levantar el servidor

```bash
cd backend
cp .env.example .env        # solo la primera vez (en Windows: copy .env.example .env)
docker compose up -d --build
```

Antes de usarlo con datos reales, **cambiar `JWT_SECRET` en `backend/.env`** por un texto largo y
aleatorio (por ejemplo el resultado de `python -c "import secrets; print(secrets.token_urlsafe(48))"`).
Si se cambia, todas las sesiones abiertas se cierran.

La API queda en `http://localhost:8000` (documentación interactiva en `http://localhost:8000/docs`).
Para ver que está bien: `curl http://localhost:8000/health`.

### Cuentas de demostración

Con `DATOS_DEMO=true` (el valor de `.env.example`) se crean salas, niños y estas cuentas. La
contraseña de todas es `Password123!`.

| Cuenta | Rol | Quién es |
| --- | --- | --- |
| `direccion@sonrisas.edu` | Dirección | Laura Méndez |
| `educadora@sonrisas.edu` | Educadora | Laura Gómez, Sala 1A |
| `carlos@sonrisas.edu` | Educadora | Carlos Ruiz, Sala 2B |
| `familiar@sonrisas.edu` | Familia | María López Fernández, mamá de Mateo y Lucía |
| `martinez@sonrisas.edu` | Familia | Roberto Martínez Ruiz, papá de Lucas |
| `lopez@sonrisas.edu` | Familia | Ana López Navarro, mamá de Sofía |

Después de 5 contraseñas equivocadas seguidas la cuenta se bloquea 1 minuto. El código para que una
educadora nueva se registre es el valor de `CODIGO_PROFESOR` en `.env`.

### Poner en marcha en la estancia (sin datos de ejemplo)

Para la estancia real no se cargan los niños ni las cuentas de prueba. En `backend/.env`:

```
DATOS_DEMO=false
DIRECCION_EMAIL=responsable@estancia.mx
DIRECCION_PASSWORD=una-clave-de-al-menos-10-caracteres
DIRECCION_NOMBRE=Responsable de la estancia
SALAS_INICIALES=Sala 1A:Lactantes,Sala 2B:Maternales A
```

Con la base vacía, al arrancar se crea solo esa cuenta de Dirección y esas salas. Desde el panel de
Dirección se dan de alta educadoras, familias y niños. Si la base ya tiene usuarios no se toca nada.
Para empezar de cero (borra **todo**, incluidas las fotos): `docker compose down -v` y volver a levantar.

### Acceso desde teléfonos fuera de la red (túnel HTTPS)

```bash
docker compose --profile tunel up -d tunel
docker compose logs tunel | grep trycloudflare      # en PowerShell: | Select-String trycloudflare
```

Eso da una dirección `https://<algo>.trycloudflare.com`. La dirección cambia cada vez que se reinicia
el túnel; para la estancia conviene un túnel con nombre de Cloudflare o un dominio propio. En la app
se pone en la pantalla de inicio de sesión, botón **Servidor** (no hace falta volver a compilar).

### Pruebas del servidor

```bash
docker compose exec api uv run --no-sync pytest -q
```

> **Ojo:** las pruebas borran y vuelven a crear la base de datos con los datos de demostración.
> No correrlas contra la base de la estancia.

## 2. Compilar e instalar la app

### Versión de prueba (debug)

En Android Studio: abrir la carpeta del proyecto y correr la configuración `app`. Desde terminal:

```powershell
.\gradlew.bat assembleDebug -PapiUrl=https://<algo>.trycloudflare.com/api/v1/
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- `-PapiUrl` es el servidor con el que arranca la app (también se cambia después en **Servidor**).
- Con el emulador y el servidor en la misma computadora: `adb reverse tcp:8000 tcp:8000` y
  `-PapiUrl=http://127.0.0.1:8000/api/v1/`.
- En la versión debug se ven los accesos rápidos de las cuentas de demostración en el inicio de
  sesión. Para poder tomar capturas de pantalla (para manuales) agregar `-PpermitirCapturas=true`.
- Para probar sin servidor: `AppContainer.USE_MOCKS = true` en `SonrisasApplication.kt`.

### Versión para entregar (release firmada)

La firma se lee de `keystore.properties` en la raíz del proyecto. Ese archivo y el `.jks` **nunca se
suben a git** (están en `.gitignore`) y la contraseña solo la tiene el equipo.

```
storeFile=C:/ruta/a/sonrisas-release.jks
storePassword=...
keyAlias=sonrisas
keyPassword=...
```

```powershell
.\gradlew.bat assembleRelease -PapiUrl=https://<servidor>/api/v1/
# comprobar la firma
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<versión>\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

La release no permite capturas de pantalla ni grabar la pantalla (datos de menores), no muestra los
accesos rápidos de prueba y no escribe peticiones en el log.

### Pruebas de la app

```powershell
.\gradlew.bat testDebugUnitTest
# contra el servidor real (con el backend corriendo en localhost:8000):
$env:SONRISAS_IT="1"; .\gradlew.bat testDebugUnitTest --tests "*BackendIntegrationTest"
```

## 3. Cómo se comporta la app

- **Sin conexión:** asistencia, actividades, eventos y mensajes de la bitácora se guardan en el
  teléfono (cifrados) con un reloj de espera y se mandan solos al volver la red, sin duplicarse
  aunque se reintenten. Lo último que se vio (salas, niños, fichas, bitácora) se puede consultar sin red.
- **Hora:** todas las horas se muestran en la hora de la estancia (`America/Mexico_City`), aunque el
  teléfono tenga otra zona configurada.
- **Bloqueo:** si la app se queda en segundo plano más de un minuto o se vuelve a abrir con la sesión
  guardada, pide huella, rostro o el PIN del teléfono. Desde ahí también se puede entrar con otra cuenta.
- **Fotos:** solo las ven las educadoras de la sala, la familia del niño y Dirección. Cada consulta
  queda registrada (panel de Dirección > Logs > Fotos) y la familia puede pedir que se borre una
  foto de su hijo, lo que la elimina del servidor.

## 4. Notificaciones push (opcional)

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

## 5. Respaldo

La información vive en dos volúmenes de Docker: `pgdata` (base de datos) y `objetos` (fotos).

```bash
docker compose exec db pg_dump -U postgres sonrisas > respaldo_$(date +%F).sql
docker run --rm -v backend_objetos:/datos -v "$PWD":/destino alpine tar czf /destino/fotos_$(date +%F).tgz -C /datos .
```

Los respaldos contienen datos de menores: guardarlos cifrados y con acceso solo de Dirección.

## 6. Cómo trabajamos con git

- `main`: lo estable. `integracion`: donde se juntan los cambios de todos antes de pasar a `main`.
- Antes de seguir trabajando, traer lo último: `git pull origin integracion` (o hacer merge de `integracion` en tu rama).
- No subir `local.properties`, `keystore.properties`, `*.jks`, `app/google-services.json`,
  `backend/.env` ni `backend/secretos/`.
