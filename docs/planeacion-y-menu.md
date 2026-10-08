# Planeación y menú diario

Responsable: Heberto Camilo Doria Gutiérrez. Rama: `heber`.

## Qué hace

- **Educadora**: en la sala aparece la tarjeta "Planeación y menú · Hoy" con la comida y la
  primera actividad. Al tocarla se abre la semana (lunes a viernes). Puede elegir un día,
  tocar el lápiz (o "Publicar planeación" si el día está vacío) y capturar:
  - Menú: desayuno, colación, comida y avisos de cocina (opcional, p. ej. menús por alergia).
  - Actividades: hora (24 h, "09:30") y nombre; puede agregar o quitar filas (máximo 10).
  Al publicar, las actividades se ordenan por hora.
- **Familia**: en el inicio de su hijo aparece la misma tarjeta; abre la semana en modo consulta.
- En fin de semana se muestra la semana siguiente.
- Viene precargada la semana actual de ejemplo para que la demo siempre tenga datos.

## Reglas

- Desayuno, colación y comida son obligatorios; cada campo hasta 120 caracteres.
- Al menos una actividad; cada una con nombre y hora válida en formato 24 h.
- Las filas de actividad totalmente vacías se ignoran.
- Solo la educadora puede editar.
- Mientras edita, el borrador vive en el ViewModel (no se pierde al girar la pantalla).

## Cómo está construido

| Capa | Archivo |
| --- | --- |
| Semana escolar | `domain/SchoolWeek.kt` |
| Modelo y datos de ejemplo | `model/DailyPlan.kt` |
| Repositorio | `data/repository/DailyPlanRepository.kt`, `impl/MockDailyPlanRepositoryImpl.kt` |
| Reglas y validación | `ui/state/DailyPlanPresenter.kt` |
| ViewModel | `ui/state/DailyPlanViewModel.kt` |
| Componentes | `ui/components/DailyPlanComponents.kt` |
| Pantalla | `ui/screens/DailyPlanScreen.kt` (ruta `daily_plan`) |

Cambios pequeños en `RoomDashboardScreen.kt`, `ParentHomeScreen.kt`, `SonrisasNavHost.kt`,
`Route.kt`, `AppViewModelProvider.kt` y `SonrisasApplication.kt`.

## Pruebas

- `SchoolWeekTest` (4): lunes a viernes, fin de semana, cambio de mes, etiquetas.
- `DailyPlanPresenterTest` (9): validaciones, orden por hora, edición de un plan existente,
  títulos de día y guardado en el repositorio.

## Limitaciones

- Datos en memoria: lo publicado se pierde al cerrar la app.
- Una sola planeación para la Sala 1A (no hay planeación por sala).
- No hay aviso a las familias cuando se publica o cambia la planeación.
