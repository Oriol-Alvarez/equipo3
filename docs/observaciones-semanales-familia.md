# Observaciones de la familia en el resumen semanal

Responsable: Heberto Camilo Doria Gutiérrez. Rama: `heber`.

## Qué hace

- **Trabajadores / Educadoras**: en la ficha del niño → pestaña **Resumen Semanal**, disponen del
  apartado **"Observaciones de los trabajadores"**. En la **semana actual** pueden escribir observaciones
  (hasta 500 caracteres) y guardarlas para que las familias las lean. También pueden consultar las
  observaciones que dejó la familia en su respectivo apartado.
- **Familia**: en su inicio → pestaña **Resumen Semanal**, ve el apartado **"Observaciones de los trabajadores"**
  (solo lectura) y el apartado **"Observaciones de la familia"**, donde en la **semana actual** puede escribir
  una observación (hasta 500 caracteres) y guardarla. En semanas pasadas ambos apartados quedan como histórico de consulta.
- Se eliminó la tarjeta de confirmación de lectura y reacciones emoji del resumen semanal para enfocar
  la interacción directamente en la retroalimentación bidireccional mediante observaciones.
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
