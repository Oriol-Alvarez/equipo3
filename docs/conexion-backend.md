# Conexión de la app con el backend (rama de prueba)

Responsable: Heberto Camilo Doria Gutiérrez.
Rama: `heber-backend-test` (creada desde `main` en `c03f800`). Es una rama de prueba para que
Oriol la revise; si algo no cuadra, se descarta sin afectar `main` ni `oriol`.

## Qué tiene esta rama

1. **El backend de Oriol unido con `main`.** La rama `oriol` (commit "primera version backend") se
   había separado antes de los PR #3 a #6. Aquí se juntó con `main`, así que tiene el backend y
   también observaciones semanales, cumpleaños, planeación y menú, y la bitácora en PDF.
   El único choque fue `SonrisasApplication.kt`; se quedó la estructura de Oriol (`USE_MOCKS`,
   repositorios Mock y de red) y se agregaron los repositorios que faltaban.
2. **Backend para los módulos que todavía no tenían servidor:**
   - Observaciones semanales: `/weekly-observations`, `/children/{id}/weekly-observations`.
   - Planeación y menú diario: `/daily-plans`, `/daily-plans/{fecha}`.
   - Registro diario (fuente de la bitácora oficial en PDF): `/daily-logs`,
     `/children/{id}/daily-logs/{fecha}/ingreso|estadia|egreso`.
   - Detalle en `API_ENTRYPOINTS.md`, módulos 15, 16 y 17.
3. **Ajustes a grupos con familias (`/family-groups`):**
   - Una familia ya no puede leer ni escribir en grupos donde no está ninguno de sus hijos (403).
   - Los mensajes salen en orden y los grupos van del más reciente al más antiguo.
   - No se aceptan mensajes vacíos ni grupos con alumnos que no existen.
   - Las horas se guardan en hora de Ciudad de México (antes salían en UTC).
4. **La app conectada al servidor** en esos módulos: grupos con familias (incluye subir fotos y
   archivos a `/media/upload`), observaciones semanales, planeación y menú, y registro diario.
   Antes solo estaban conectados login, salas, niños, asistencia, anuncios, sugerencias y
   objetos perdidos.

## Cómo funciona en la app

Las interfaces de los repositorios siguen siendo síncronas, igual que antes, para no tocar las
pantallas. Cada repositorio de red:

- guarda los datos en un `StateFlow` y lo actualiza al momento cuando el usuario hace un cambio;
- manda el cambio al servidor en segundo plano, **en orden y uno por uno** (`RemoteSync.SerialQueue`),
  para que el ingreso, la estadía y el egreso del mismo día no choquen;
- al responder el servidor, reemplaza el dato provisional por el del servidor; si el servidor lo
  rechaza, lo quita;
- se vuelve a cargar del servidor al abrir la pantalla (`refresh()` en el ViewModel).

Con `AppContainer.USE_MOCKS = true` todo vuelve a funcionar con los datos de ejemplo, sin servidor.

En grupos con familias, la app ya usa el id y nombre reales de la sesión (se guarda `userId` en
`TokenManager` al iniciar sesión) para saber qué mensajes son "míos", y la lista de familias para
crear un grupo viene de `/family-groups/candidates`.

| Capa | Archivo |
| --- | --- |
| DTOs nuevos | `data/remote/ModulosDtos.kt` |
| Endpoints nuevos | `data/remote/SonrisasApi.kt` (al final) |
| Utilidades de sincronización | `data/remote/RemoteSync.kt` |
| Repositorios de red | `data/repository/impl/NetworkFamilyGroupsRepositoryImpl.kt`, `NetworkWeeklyObservationsRepositoryImpl.kt`, `NetworkDailyPlanRepositoryImpl.kt`, `NetworkDailyLogRepositoryImpl.kt` |
| Backend | `backend/app/routers/weekly_observations.py`, `daily_plans.py`, `daily_logs.py`, `backend/app/acceso.py`, `backend/app/horario.py` |

## Cómo probarlo

```bash
cd backend
cp .env.example .env        # solo la primera vez
docker compose up -d --build
docker compose exec api uv run --no-sync pytest     # 43 pruebas del backend
bash smoke.sh                                       # pruebas de humo de Oriol
```

En Android Studio abre el proyecto y corre la app en el emulador (usa `http://10.0.2.2:8000`).
Cuentas: `educadora@sonrisas.edu`, `familiar@sonrisas.edu`, contraseña `Password123!`.

Prueba de la app contra el servidor real (crea datos de prueba, por eso hay que pedirla):

```powershell
$env:SONRISAS_IT="1"; .\gradlew.bat testDebugUnitTest --tests "*BackendIntegrationTest"
```

## Qué se verificó

- 43 pruebas del backend (las de Oriol y 17 nuevas en `tests/test_modulos_heber.py`).
- `smoke.sh` completo.
- La app compila (`assembleDebug`) y pasan las 104 pruebas unitarias, incluidas las 4 de
  integración que usan los repositorios de red contra el servidor en Docker.
- **No se probó en el emulador** (no arrancó en la compu donde se hizo). Falta revisar a mano
  las pantallas con el servidor corriendo.

## Pendiente / para platicar con Oriol

- Chats de trabajadores y chat bitácora (`MessagesRepository`, `BitacoraRepository`) siguen con
  datos de ejemplo; el backend ya tiene las rutas.
- El ingreso y egreso que captura "Pasar asistencia" se guardan dos veces: en `/attendance`
  (de Oriol) y en `/daily-logs` (hoja oficial). Conviene decidir si se juntan.
- Las pruebas del backend escriben en la misma base que usa la app en desarrollo.
- `API_ENTRYPOINTS.pdf` no se actualizó; los módulos nuevos solo están en el `.md`.
