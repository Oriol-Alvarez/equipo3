# Observaciones de la familia en el resumen semanal

Responsable: Heberto Camilo Doria Gutiérrez. Rama: `heber`.

## Qué hace

- **Familia**: en su inicio → pestaña **Resumen Semanal**, debajo del resumen aparece
  "Observaciones de la familia". En la **semana actual** puede escribir una observación
  (hasta 500 caracteres) y guardarla. En semanas pasadas solo puede leer lo que escribió.
- **Educadora**: en la ficha del niño → **Resumen Semanal** ve las observaciones que dejó la
  familia en cada semana. No escribe aquí: sus observaciones son otra tarea (Alex).
- Cada observación muestra quién la escribió, su rol y la fecha y hora.
- Ejemplo incluido: una observación de la mamá de Mateo en la semana del 15 al 19 de septiembre.

## Cómo está construido

| Capa | Archivo |
| --- | --- |
| Modelo | `model/WeeklyObservations.kt` |
| Repositorio (interfaz) | `data/repository/WeeklyObservationsRepository.kt` |
| Repositorio simulado | `data/repository/impl/MockWeeklyObservationsRepositoryImpl.kt` |
| Reglas | `ui/state/WeeklyObservationsPresenter.kt` |
| ViewModel | `ui/state/WeeklyObservationsViewModel.kt` |
| Sección visual | `ui/components/WeeklyObservationsComponents.kt` |

Cambios pequeños en `ParentHomeScreen.kt`, `ChildDetailScreen.kt`, `SonrisasNavHost.kt`,
`AppViewModelProvider.kt` y `SonrisasApplication.kt`. No se modificó `WeeklySummaryContent`,
para no chocar con la tarea de observaciones de la educadora.

El borrador, la validación y el guardado viven en el ViewModel, no en la pantalla.
El modelo guarda el rol del autor (`FAMILIA` o `EDUCADORA`), así que la tarea de la
educadora puede reutilizar el mismo repositorio.

## Pruebas

- `WeeklyObservationsRepositoryTest` (6): guardado, texto vacío, largo máximo, orden y datos de ejemplo.
- `WeeklyObservationsPresenterTest` (5): filtro por niño y semana, quién puede escribir y cuándo,
  mensajes para cada perfil y que cada semana tenga una clave distinta.

## Limitaciones

- Datos en memoria: se pierden al cerrar la app.
- El perfil Familiar siempre es la familia García (Mateo y Lucía).
- No se pueden editar ni borrar observaciones.
- La semana se identifica por su texto (`weekRangeText`); si cambia ese texto, cambia la semana.
