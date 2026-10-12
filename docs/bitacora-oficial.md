# Bitácora oficial en PDF

Responsable: Heberto Camilo Doria Gutiérrez. Rama: `heber`.

## Qué hace

- **Educadora**: en la ficha del alumno aparece un icono de PDF arriba a la derecha (junto al chat).
  Abre la pantalla "Bitácora oficial (PDF)", donde:
  1. Elige la semana: esta semana, la pasada, hace 2 o hace 3 semanas.
  2. Ve qué días tienen registro (ingreso, estadía, egreso) y cuáles quedarán en blanco.
  3. Toca **Generar PDF**. Se arma la "Bitácora de filtro diario" con el mismo orden y apartados
     que la hoja oficial de Canvas, en tamaño carta horizontal.
  4. Puede **Ver** el PDF, **Compartir**lo (correo, WhatsApp, Drive…) o **Guardar en Descargas**.
- **Familia**: no ve el botón.
- La hora de entrada y salida que se captura en "Pasar asistencia" también se guarda en este registro.
- Vienen precargadas 4 semanas de ejemplo para todos los niños, para que la demo siempre tenga datos.

## Cómo queda la hoja

El PDF copia la hoja oficial "Bitácora de filtro diario": mismos títulos, apartados, preguntas
(con el mismo texto) y opciones. Va en tamaño carta horizontal, en 2 páginas, con una columna por día.

| Hoja oficial | En el PDF |
| --- | --- |
| Encabezado | "Apoyos para la inclusión en la primera infancia / Prestación de servicios…" |
| I. Datos Generales | Estancia Infantil, nombre completo de la niña o niño y Grupo L / M / P (marcado) |
| Fecha y L M M J V | "Fecha: dd/mm/aaaa" en cada columna, con la letra del día marcada |
| II. Registro de Ingreso | Todas las preguntas de la hoja, incluidos los "Nombre y firma" |
| IV. Registro de egreso | Todas las preguntas de la hoja, incluidos los "Nombre y firma" |
| III. Registro de estadía (2.ª página) | Alimentación, Descanso, Funciones Excretoras, Estado de ánimo, Accidentes y Salud |

Reglas al llenar:

- Las preguntas de opción se imprimen con todas sus opciones y una casilla para cada una
  (Sí / No; Sí / No / No aplica; Nada / Poco / Todo / Pidió más; Feliz / Triste / Enojado;
  No aplica / Avisó: Sí / Avisó: No). La respuesta se marca con una casilla negra con "x".
- Un día sin registro o una pregunta sin contestar queda con las casillas vacías, para llenarla a mano.
- "Nombre y firma": se imprime el nombre y queda un renglón para firmar a mano.
- Sin lesiones se escribe "NA", como en la hoja de ejemplo. Si no está enferma(o), el medicamento
  se marca "No aplica".
- Si un texto es muy largo, la tabla sigue en otra página con "(continuación)".
- Pie de página: "Generado con la app Sonrisas de Cristal el … · Página n de N".
- No se incluyen los logotipos del gobierno que trae la hoja de Canvas; se pueden agregar si la
  estancia nos da los archivos y el permiso para usarlos.

## Cómo está construido

| Capa | Archivo |
| --- | --- |
| Modelo del registro diario | `model/DailyLog.kt` |
| Datos de ejemplo | `model/DailyLogSampleData.kt` |
| Validación | `domain/DailyLogValidator.kt` |
| Repositorio | `data/repository/DailyLogRepository.kt`, `impl/MockDailyLogRepositoryImpl.kt` |
| Texto de la hoja | `domain/LogSheet.kt` (`LogSheetFormatter`) |
| Dibujo de la hoja (sin Android) | `domain/LogSheetLayout.kt` |
| Generar el archivo PDF | `data/export/LogSheetExporter.kt`, `data/export/AndroidLogSheetExporter.kt` |
| Reglas de la pantalla | `ui/state/ExportLogPresenter.kt` |
| ViewModel | `ui/state/ExportLogViewModel.kt` |
| Pantalla | `ui/screens/ExportLogScreen.kt` (ruta `export_log`) |

Cambios pequeños en `ChildDetailScreen.kt` (icono PDF), `AttendanceViewModel.kt` (guarda la hora),
`SonrisasNavHost.kt`, `Route.kt`, `AppViewModelProvider.kt`, `SonrisasApplication.kt` y
`domain/SchoolWeek.kt` (semana de una fecha).

El PDF se hace con `PdfDocument`, que ya viene en Android (no se agregó ninguna librería).
Se guarda en la caché de la app y se comparte con el `FileProvider` que ya existía.

## Para conectar el formulario (Oriol)

El PDF lee todo de `DailyLogRepository`. Mientras los formularios no lo llamen, solo se ven las
horas de asistencia y los datos de ejemplo. Para conectarlo, en el ViewModel del formulario se
recibe `container.dailyLogRepository` (ya está en `AppContainer`) y se llama así:

```kotlin
val hoy = SimpleDate.today()

// Ingreso (al terminar las preguntas de entrada)
dailyLogRepository.saveIngreso(
    child.id, hoy,
    IngresoLog(
        time = "08:10",
        assistantName = "Laura Gómez",
        deliveredBy = "María López",
        receivedBy = "Laura Gómez",
        isSick = false,
        hasSymptoms = false,
        arrivedClean = true,
        backpackComplete = true,
        goodPhysicalCondition = true
    )
).onFailure { error -> /* mostrar error.message en el pop-up */ }

// Estadía: se puede ir llenando por partes sin borrar lo anterior
dailyLogRepository.updateEstadia(child.id, hoy) { it.copy(breakfast = FoodConsumptionOption.TODO) }
dailyLogRepository.updateEstadia(child.id, hoy) { it.copy(slept = true, napMinutes = 60) }
dailyLogRepository.updateEstadia(child.id, hoy) {
    it.copy(
        sphincterControl = SphincterControl.AVISO_SI,
        sphincterPeeCount = 2, sphincterPoopCount = 1, // Control de esfínteres → Número de veces
        peeCount = 3, poopCount = 1                    // Pipí y/o Popó → Número de veces
    )
}

// Egreso
dailyLogRepository.saveEgreso(
    child.id, hoy,
    EgresoLog(time = "15:05", goodPhysicalCondition = true, collectedBy = "Rosa Fernández",
        deliveredClean = true, belongingsComplete = true, deliveredByStaff = "Lucía Martín")
)
```

- Todas las funciones devuelven `Result`: si algo no es válido regresa un mensaje en español
  listo para mostrar (hora no válida, salida antes de la entrada, texto de más de 300 caracteres…).
- La hoja tiene dos "Número de veces" en Funciones Excretoras: uno bajo "Control de esfínteres"
  (`sphincterPeeCount` / `sphincterPoopCount`, solo si no es "No aplica") y otro bajo
  "Pipí y/o Popó" (`peeCount` / `poopCount`).
- La hora acepta "8:05", "08:15 AM" o "02:10 PM" y se guarda en formato 24 h.
- Las preguntas tipo Sí/No son `Boolean?`: `null` significa "no contestada" y sale "—" en el PDF.
- Si el niño no está enfermo, no hace falta mandar el medicamento; si no hubo accidente, no hace
  falta la descripción (el validador limpia los detalles que no aplican).

## Limitaciones

- Los registros viven en memoria (repositorio simulado, igual que el resto de la app): se pierden
  al cerrar la app. Cuando haya base de datos solo cambia `MockDailyLogRepositoryImpl`.
- Las firmas no se capturan en la app; se firman a mano en la hoja impresa.
- Solo la educadora puede exportar.
- "Guardar en Descargas" funciona desde Android 10. En versiones anteriores se usa Compartir.
- Para "Ver" se necesita una app que abra PDF en el teléfono; si no hay, aparece un aviso.

## Pruebas

- `domain/DailyLogValidatorTest.kt`: horas, campos obligatorios, salida antes de la entrada, límites.
- `data/repository/DailyLogRepositoryTest.kt`: guardar por partes, asistencia, datos de ejemplo válidos.
- `domain/LogSheetFormatterTest.kt`: cada respuesta cae en su fila y día, días en blanco, grupo L/M/P.
- `domain/LogSheetLayoutTest.kt`: número de páginas, continuación con textos largos, nada fuera del margen.
- `ui/state/ExportLogPresenterTest.kt`: semanas disponibles, estado de cada día, nombre del archivo.

Correr con `.\gradlew testDebugUnitTest`.

Prueba manual: entrar como educadora → abrir un niño → icono PDF → Generar PDF → Ver / Compartir /
Guardar en Descargas. Pasar asistencia a un niño y volver a generar: la hora nueva aparece en el día de hoy.
