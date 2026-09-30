# Bitácoras independientes por alumno

La bitácora del prototipo ahora pertenece al alumno que se abre. Antes todos los chats leían la lista de Mateo y las asistencias de otros alumnos no aparecían en su bitácora.

## Aportación

- Mensajes, archivos adjuntos, actividades e ingresos/egresos se registran con el identificador del alumno seleccionado.
- Cambiar de alumno y volver conserva los registros de cada uno durante la sesión.
- El borrador y la selección de archivos se limpian al cambiar de alumno para evitar enviarlos al destinatario equivocado.
- Mateo conserva sus seis registros iniciales de ejemplo. Los demás muestran un estado vacío con instrucciones para empezar.
- La observación de alimentación de ejemplo usa el nombre del alumno abierto.

## Recorrido para presentar

1. Iniciar sesión, abrir Sala 1A y seleccionar Sofía López.
2. Enviar un mensaje y registrar una actividad con el botón +.
3. Abrir asistencia, completar los campos y ambas firmas, guardar y aceptar la confirmación. El ingreso aparece en su bitácora.
4. Volver a la sala y abrir Lucas: no debe mostrar los registros de Sofía.
5. Volver a Sofía: sus registros siguen allí. Mateo conserva sus propios ejemplos.

## Integración

`BitacoraRepository.timelineForChild(childId)` entrega el historial de un alumno. Las operaciones `addChatMessage`, `addActivityCard` y `addEventChip`, así como los métodos correspondientes de los ViewModels, requieren `childId`. Los consumidores de la app se actualizaron juntos; usar siempre el identificador del alumno de la pantalla.

La implementación sigue siendo simulada y en memoria: recrear el contenedor de la app o terminar el proceso reinicia los datos. No agrega servidor, almacenamiento permanente ni sincronización. Las otras pantallas con datos de ejemplo, como resúmenes e historiales, mantienen su funcionamiento previo.

## Pruebas

`ChildTimelineRepositoryTest` comprueba datos iniciales, mensajes y adjuntos, mensajes vacíos, actividades, ingresos y egresos, identificadores únicos y reinicio de sesión. `ChildTimelineTest` comprueba envío, cambio de alumno y borradores en la interfaz. `AppTimelineFlowTest` recorre inicio de sesión, sala, actividad, firmas, asistencia y regreso a la bitácora mediante la navegación real.

Desde la raíz del proyecto, con Java y Android SDK configurados y un emulador/dispositivo conectado:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --no-configuration-cache --console=plain
```

El APK de prueba se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Validación del 30 de septiembre de 2026: compilación debug correcta; 8 pruebas unitarias y 4 pruebas instrumentadas aprobadas. Las 4 instrumentadas también aprobaron en el emulador Android 17/API 37 con pantalla configurada a 1600 × 2560 y densidad 240 para comprobar el recorrido en tamaño de tablet. Esto no sustituye probar el APK en la tablet física del socio formador. De las pruebas anteriores, 7 unitarias y 3 instrumentadas se añadieron para esta aportación; las otras 2 ya estaban en el proyecto.
