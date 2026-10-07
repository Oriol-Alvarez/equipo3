# Avisos de cumpleaños

Responsable: Heberto Camilo Doria Gutiérrez. Rama: `heber`.

## Qué hace

- **Educadora**: al entrar a la sala, arriba de la lista de niños aparece una tarjeta con los
  cumpleaños de **hoy y de los próximos 7 días**: nombre, años que cumple, fecha y "Hoy",
  "Mañana" o "En N días". Tocar un niño abre su bitácora (por ejemplo, para felicitar a la familia).
  Si no hay cumpleaños cercanos, la tarjeta no aparece.
- **Familia**: en el inicio de su hijo o hija aparece una felicitación el día del cumpleaños
  ("¡Feliz cumpleaños, Mateo! 🎉") o una cuenta regresiva en los 7 días anteriores.

Son avisos dentro de la app. No son notificaciones del sistema (las que llegan con la app cerrada).

## Datos

La fecha sale de la ficha del niño (`ChildFullProfile.birthDate`, formato `dd/MM/yyyy`).
Se agregaron fechas de nacimiento para los niños que no tenían una propia (antes todos usaban
`10/05/2024`), aproximadas a la edad de cada ficha. En octubre cumplen Mia Ramírez (9) y
Bruno Benítez (11).

Para probar la vista familiar, Mateo cumple el 14 de abril y Lucía el 22 de julio: se puede
cambiar la fecha del emulador (Ajustes → Sistema → Fecha y hora) a unos días antes.

## Cómo está construido

| Capa | Archivo |
| --- | --- |
| Reglas de fechas | `domain/Birthdays.kt` (`SimpleDate`, `BirthdayCalculator`) |
| ViewModel | `ui/state/BirthdaysViewModel.kt` |
| Tarjeta y felicitación | `ui/components/BirthdayComponents.kt` |

Cambios pequeños en `RoomDashboardScreen.kt`, `ParentHomeScreen.kt`, `AppViewModelProvider.kt`
y `Models.kt` (fechas de nacimiento).

No se usa `java.time` porque la app soporta desde Android 7.0 (API 24), donde no está disponible
sin configuración extra. El 29 de febrero se celebra el 28 en años no bisiestos.

## Pruebas

`BirthdayCalculatorTest` (9): lectura de fechas, días que faltan, cumpleaños de hoy, cambio de año,
29 de febrero, ventana de 7 días, textos en español y que todos los niños de la sala tengan una
fecha válida.

## Limitaciones

- La fecha de hoy se toma al abrir la pantalla.
- La edad que aparece en la ficha (`ageText`) es texto fijo y no se recalcula con la fecha de nacimiento.
