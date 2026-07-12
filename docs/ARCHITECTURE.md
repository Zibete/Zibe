# Arquitectura de ZIBE

## Dirección de dependencias

ZIBE mantiene una modernización incremental: Compose y Views conviven en `app`,
pero las dependencias entre módulos son estrictas.

```text
app --------------------> data -----------------> domain
 |                         |                        |
 +-------------------------+------------------------+--> core:common
 +-------------------------------------------------> core:designsystem
```

- `domain`: modelos, contratos y casos de uso puros. No conoce Android,
  Firebase, proveedores sociales, Hilt ni implementaciones concretas.
- `data`: implementa contratos de `domain`; es dueño de Firebase Auth, RTDB,
  Storage, DataStore, snapshots, paths y referencias.
- `core:common`: `ZibeResult`, política de cancelación y utilidades puras.
- `core:designsystem`: tokens, tema y componentes Compose compartidos.
- `app`: composición Hilt, Activities/Fragments/Compose, navegación, permisos,
  media y adaptadores legacy.

`scripts/check_architecture.py` protege estas fronteras en local y CI. Si una
feature necesita una operación nueva, se extiende primero el contrato dueño; no
se atraviesa la capa con una referencia Firebase o una implementación concreta.

## Flujo por feature

```text
UI -> acción -> ViewModel -> UseCase/contrato -> Repository -> Firebase/DataStore
UI <- effect/state <- ViewModel <- ZibeResult/Flow <- Repository
```

- Estado durable de pantalla: `StateFlow` con un único estado principal.
- Eventos one-shot: `SharedFlow` sin replay o `Channel`, según el patrón de la
  feature; no se mezclan mecanismos en un mismo ViewModel.
- Operaciones fallibles: el repositorio captura con `zibeCatching` y devuelve
  `ZibeResult<T>`. El ViewModel decide presentación y snackbar.
- Cancelación: `CancellationException` siempre se relanza; no se transforma en
  error visible ni fallback.
- Navegación, `Context`, `Activity`, `Uri`, launchers y media viven en el borde
  Android, nunca en ViewModels.

## Chat directo

`ChatViewModel` expone `ChatUiState` como fuente principal. Los reducers puros
mantienen timeline, selección y soft-delete. `ChatPhotoController` posee camera,
picker, crop y temporales; `ChatActivity` conserva el borde de audio.

`DefaultSendChatMessageUseCase` valida bloqueo y crea el mensaje. Para DM,
`ChatRepository` ejecuta un único fan-out raíz atómico que incluye mensaje y los
dos resúmenes. `ChatRefs` y todos los tipos Firebase quedan privados de `data`.
Groups conserva el path y comportamiento legacy.

Los estados DM son monotónicos:

```text
MSG_DELIVERED (1) -> MSG_RECEIVED (2) -> MSG_SEEN (3)
```

El éxito de `messaging.send()` solo confirma aceptación por FCM. El dispositivo
Android receptor escribe `MSG_RECEIVED`; Functions puede promover a `MSG_SEEN`
solo cuando `activeThread` del receptor coincide y su lease es fresco.

Ver [FIREBASE_SCHEMA.md](FIREBASE_SCHEMA.md) y
[ADR-0002](adr/0002-dm-delivery-contract.md).

## UI, errores y permisos

`BaseEdgeToEdgeActivity` aloja un único snackbar global por Activity. Las
features publican mensajes mediante la política transversal; no crean hosts o
collectors paralelos.

Los permisos se solicitan en contexto. Notificaciones se pide al entrar a
Settings; camera, picker y micrófono se piden al iniciar su acción. Rechazar no
debe bloquear navegación y el rechazo permanente ofrece App Settings.

## Source sets y validación

- `src/test`: reducers, mappers, casos de uso, cancelación y decisiones puras.
- `src/androidTest`: flujos Android/Compose y grafo Hilt.
- `src/debug`: wiring exclusivo de debug, como App Check.
- `functions/tests`: contratos Python de Functions.
- `tools/firebase-rules-tests`: permisos e invariantes RTDB.

La matriz reproducible está en [CI.md](CI.md). Instrumentation requiere
emulador/dispositivo y no reemplaza la validación física de credenciales,
notificaciones, background/killed, cámara y audio.

## Decisiones registradas

- [ADR-0001: límites modulares](adr/0001-module-boundaries.md)
- [ADR-0002: contrato DM delivery/receipt/seen](adr/0002-dm-delivery-contract.md)
