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

## Mejoras recientes completadas

1. **Manejo Global de Expiración de Sesión (401 Unauthorized):**
   - Intercepción de errores 401 en `AuthInterceptor` y notificación en `TokenManager`.
   - Limpieza automática de tokens y navegación segura a `LoginScreen` en `SonrisasApp`.
2. **Unificación de Asistencia (`/attendance`) y Registro Diario (`/daily-logs`):**
   - Al registrar ingreso o egreso en `/attendance`, el backend sincroniza automáticamente los datos con la bitácora oficial (`DailyLog`).
3. **Indicador de Conectividad y Resiliencia en UI:**
   - La app muestra un chip visual interactivo (`ChipConexion`) en la barra superior cuando el WebSocket o servidor se encuentran desconectados o reconectando.
4. **Soporte Offline Persistente (`OfflineStore`):**
   - Almacenamiento local en caché para mantener datos disponibles inmediatamente en la app incluso sin conexión a internet.
5. **Pruebas Instrumentadas de UI:**
   - Pruebas UI en `androidTest` (`LoginScreenTest`, `NavigationTest`) para verificar inicio de sesión y navegación por roles.

## Cómo probarlo

```bash
cd backend
cp .env.example .env        # solo la primera vez
docker compose up -d --build
docker compose exec api uv run --no-sync pytest     # 47 pruebas del backend
bash smoke.sh                                       # pruebas de humo
```

En Android Studio abre el proyecto y corre la app en el emulador (usa `http://10.0.2.2:8000` o el túnel activo de Cloudflare).
Cuentas: `educadora@sonrisas.edu`, `familiar@sonrisas.edu`, contraseña `Password123!`.

Prueba de la app contra el servidor real:

```powershell
$env:SONRISAS_IT="1"; .\gradlew.bat testDebugUnitTest --tests "*BackendIntegrationTest"
```

## Qué se verificó

- 47 pruebas del backend (`pytest` en contenedor Docker).
- `assembleDebug` y 107 pruebas unitarias e instrumentadas en Kotlin.
- Túnel Cloudflare activo e integración probada.
