# Grupos de mensajes con familias

Responsable: Heberto Camilo Doria Gutiérrez.
Rama: `heber-grupos-padres` (creada desde `main` en `70f3af9`).

## Qué hace

Funciona como un grupo de WhatsApp entre la educadora y las familias que ella elige.

- **Educadora**: en Mensajes → modo *Familiar*, el botón de grupo (arriba a la izquierda) abre
  "Nuevo grupo con familias". Escribe un nombre y marca las familias que quiere agregar
  (por ejemplo, solo Sofía y Mateo). El grupo aparece en la sección "Grupos con familias".
- **Familia**: en su inicio aparece el acceso rápido **Grupos**. Ahí ve solo los grupos donde
  está alguno de sus hijos y puede escribir. Las familias no crean grupos.
- Todos los participantes pueden enviar texto, tomar una foto o adjuntar un archivo.
- Cada mensaje muestra quién lo envió; los propios aparecen a la derecha.
- El grupo con el mensaje más reciente sube al inicio de la lista.
- Viene un grupo de ejemplo, "Paseo al parque", con las familias de Mateo, Sofía y Lucas.

Los avisos generales siguen en el apartado de **Anuncios**; los grupos son para conversar.

## Cómo está construido

| Capa | Archivo |
| --- | --- |
| Modelo | `model/FamilyGroups.kt` |
| Repositorio (interfaz) | `data/repository/FamilyGroupsRepository.kt` |
| Repositorio simulado | `data/repository/impl/MockFamilyGroupsRepositoryImpl.kt` |
| Reglas de presentación | `ui/state/FamilyGroupsPresenter.kt` |
| ViewModel | `ui/state/FamilyGroupsViewModel.kt` |
| Diálogo para crear grupo | `ui/components/FamilyGroupComponents.kt` |
| Pantalla de la familia | `ui/screens/ParentGroupsScreen.kt` |

Cambios pequeños en archivos existentes: `MessagesScreen.kt` (sección de grupos y botón),
`ParentHomeScreen.kt` (acceso rápido "Grupos"), `SonrisasNavHost.kt` y `Route.kt` (ruta
`parent_groups`), `AppViewModelProvider.kt` y `SonrisasApplication.kt` (dependencias).

La creación de grupos y el envío de mensajes pasan por el ViewModel y el repositorio, no por
la pantalla. La conversación reutiliza `WorkerChatConversationDialog`, que ya tenía fotos y
archivos, para que se vea igual que los grupos de trabajadores.

## Reglas

- Nombre obligatorio, máximo 40 caracteres.
- Al menos una familia.
- No se envían mensajes vacíos (texto o adjunto son obligatorios).
- Una familia solo puede escribir en grupos donde está su hijo o hija.
- Solo la educadora puede crear grupos.

## Pruebas

- `FamilyGroupsRepositoryTest` (9 pruebas): creación, validaciones, envío con adjunto,
  rechazo de mensajes vacíos y orden por actividad.
- `FamilyGroupsPresenterTest` (8 pruebas): qué grupos ve cada perfil, mensajes propios,
  resumen del grupo, vista previa de fotos y archivos, búsqueda.

## Limitaciones

- Datos simulados en memoria: los grupos y mensajes se pierden al cerrar la app.
- El perfil Familiar de la app corresponde siempre a la familia García (Mateo y Lucía).
- No hay contador de mensajes no leídos en los grupos.
- No se puede editar el grupo, agregar o quitar familias después de crearlo, ni salir de él.
- Los archivos adjuntos se guardan como referencia (URI); no se suben a ningún servidor.
