# Pre-Scale Coherence Review

## Estado

Auditoría en ejecución sobre `refactor/pre-scale-coherence-review`, basada en
`origin/main` (`b52d841`). Este documento registra evidencia y no reemplaza los
tests ni la validación final.

## Baseline previo a cambios de producción

Ejecutado el 11 de julio de 2026:

- `npm ci`: completó; npm informó 40 vulnerabilidades transitivas y Node 24 fuera
  del rango declarado por `superstatic`.
- `npm run test:rules`: 42 tests pasaron.
- `gradlew.bat help --warning-mode all`: pasó con flags AGP 9 deprecados.
- `gradlew.bat testDebugUnitTest`: pasó.
- `gradlew.bat :app:generateDebugAndroidTestLintModel`: pasó.
- `gradlew.bat lintDebug`: pasó.
- `gradlew.bat :app:assembleDebug`: pasó.
- `gradlew.bat :app:compileReleaseKotlin`: pasó con warnings preexistentes.
- `gradlew.bat :app:kspDebugAndroidTestKotlin`: pasó.
- `python -m py_compile functions/main.py`: pasó.
- `python -m unittest discover functions/tests`: no aplicaba en el baseline
  porque el directorio todavía no existía; se agrega en la primera etapa.
- `adb devices`: detectó `emulator-5554`.
- `gradlew.bat connectedDebugAndroidTest`: falló en el baseline con 19 de 20
  tests fallidos. Dos fallos de Signup reportaron `Espresso Intents init() must
  be called`; los restantes agotaron el timeout esperando estado Compose. El
  reporte quedó en `app/build/reports/androidTests/connected/debug/index.html`.
  Esta deuda preexistente debe corregirse antes de declarar la matriz final en
  verde.

## Hallazgos verificados

### Arquitectura

- `domain` conoce Android, Firebase, Credentials y Facebook.
- Presentation inyecta implementaciones concretas y accede a referencias
  Firebase en varios ViewModels.
- `ChatRefs` expone `DatabaseReference` y `StorageReference` fuera de `data`.
- No existe un check automático de límites de módulos/imports.

### Chat y contrato distribuido

- `activeThread` no tiene freshness/lease y puede producir falsos `SEEN` si queda
  persistido después de salir o morir el proceso.
- El fan-out es atómico, pero calcula `unreadCount + 1` fuera de una transacción;
  envíos concurrentes pueden perder incrementos.
- El resumen identifica el último mensaje por sender y timestamp, sin
  `lastMessageId`.
- Rules permite mutaciones de bloqueo/soft-delete y lecturas de datos privados
  más amplias que el ownership documentado.
- El formato actual `<uidA>_<uidB>` no puede parsearse si un identificador
  contiene `_`; los characterization tests congelan esta limitación antes de
  corregir el contrato.

### Lifecycle, UI y errores

- `ChatViewModel` mezcla estado, Android/media, `runBlocking`, identidades
  `lateinit` y acciones placeholder.
- `ZibeFirebaseMessagingService` lanza trabajo en un scope no ligado al callback
  ni cancelado por lifecycle.
- Chat mantiene un snackbar host paralelo al host transversal.
- `POST_NOTIFICATIONS` se solicita en Splash sin modelar rechazo permanente.
- Existen caminos que convierten errores o cancelación en `null`, defaults o
  callbacks vacíos.

### Toolchain y dependencias

- Baseline: AGP 9.0.1, Gradle 9.3.1, Kotlin 2.2.10, bytecode 11 y target SDK 34.
- La matriz objetivo verificada es AGP 9.2.x + Gradle 9.4.1 + JDK/bytecode 17,
  compile SDK 36, target SDK 35 y min SDK 26.
- Las versiones están duplicadas y todavía no existe version catalog.
- Picasso, Room, Volley, lifecycle-livedata y glide-transformations no mostraron
  consumidores en el source scan inicial; cada eliminación requiere validar el
  grafo y compilar.

## Etapa de toolchain

- Matriz aplicada: AGP 9.2.1, Gradle 9.4.1, Kotlin integrado 2.3.10, KSP 2.3.9,
  JDK/bytecode 17, compile SDK 36, target SDK 35 y min SDK 26.
- `gradle/libs.versions.toml` centraliza plugins, BOMs y librerías versionadas.
- Se eliminaron los opt-outs `android.builtInKotlin=false`,
  `android.newDsl=false` y `android.uniquePackageNames=false`.
- Se eliminó el `force` global de Material y se alineó Navigation en 2.9.6.
- Se evaluó retirar Jetifier. Sin Jetifier, `:app:processDebugResources` falla al
  enlazar el estilo legacy `Widget.Support.CoordinatorLayout` con un color raw;
  por eso se conserva temporalmente y de forma documentada hasta retirar o
  reemplazar la dependencia legacy que aporta ese recurso.
- Validación dirigida posterior: `help --warning-mode all`, unit tests, modelo
  lint de androidTest, lint, assemble debug, compile release y KSP/Hilt de
  androidTest pasaron. El único warning de configuración restante es Jetifier,
  conservado por la incompatibilidad reproducida.

## Invariantes protegidas

- Receipt de mensaje DM: delivered `1`, received `2`, seen `3`, sin downgrade.
- El éxito del envío FCM no equivale a recepción del dispositivo.
- Mensaje y dos resúmenes se escriben all-or-nothing.
- Timeline ordenada, visibilidad participant-specific y soft-delete actual.
- Compatibilidad de Groups/group chat legacy.
- Sin deploy, merge, release ni tag durante esta revisión.

## Etapa de límites arquitectónicos y DI

- `domain` ya no declara dependencias de Firebase Auth/Database, Facebook,
  Credentials, Hilt Android ni Jakarta Inject, y sus contratos de auth, sesión
  y perfil exponen modelos y valores propios (`AuthUser`,
  `AuthCredentialRequest`, `String` para URI y subscriptions cancelables).
- Logout delega la limpieza de sesiones Android/sociales a
  `ExternalSessionCleaner`; la implementación con Credential Manager y Facebook
  vive en `app`.
- Presentation de users, favorites, groups y main depende de contratos. Las
  lecturas Firebase que estaban en ViewModels se movieron a `data`, y
  `MainViewModel` inyecta `LogoutUseCase` en lugar de `DefaultLogoutUseCase`.
- `scripts/check_architecture.py` bloquea imports Android/Firebase/sociales en
  `domain` y dependencias concretas/Firebase nuevas en presentation. CI lo
  ejecuta antes de los tests de Rules.
- Excepción temporal explícita: `ChatViewModel`, `ChatListViewModel`,
  `ChatListFragment` y `ProfileViewModel` conservan dependencias de chat
  concretas hasta las etapas 4 y 5. El check no permite extender esa deuda a
  otros archivos.
- Validación dirigida: compilación de main/unit/androidTest, grafo Hilt,
  `testDebugUnitTest`, check arquitectónico y `git diff --check` pasaron.

## Etapa de dominio y persistencia de chat

- `ChatRefs`, `DatabaseReference`, `StorageReference`, snapshots y paths quedan
  encapsulados en `ChatRepository`; app y domain operan con `ChatThread` y
  `ChatRepositoryContract`.
- `ChatListViewModel` consume `observeConversations()` como `Flow` y ya no crea
  ni retiene listeners Firebase. Chat, chat list y profile dejaron de inyectar
  repositorios concretos, por lo que se eliminaron todas las excepciones del
  check arquitectónico.
- `DefaultSendChatMessageUseCase` concentra bloqueo por receptor, creación del
  mensaje `MSG_DELIVERED`, resúmenes, incremento unread y elección entre fan-out
  DM atómico o persistencia group legacy.
- `ZibeFirebaseMessagingService` depende de
  `DirectMessageReceiptAcknowledger`; el callback completa el trabajo suspendido
  dentro de su ventana de ejecución y mantiene receipt separado del éxito FCM.
- Tests de domain verifican que DM use una sola operación de fan-out, conserve
  delivered/unread y no escriba cuando el receptor bloqueó al emisor.

## Etapa de presentación y media de chat

- `ChatViewModel` expone `ChatUiState` como estado principal inmutable; header y
  contenido se combinan sin duplicar la lista de mensajes.
- El ViewModel ya no importa `Context`, `Activity`, `Intent`, `Uri`, Activity
  Result, UCrop ni APIs de media, no usa `runBlocking`, no mantiene identidades
  `lateinit` y todas sus acciones públicas son no suspend.
- Se eliminaron acciones placeholder. El reducer puro `ChatState.reduce()`
  mantiene mensajes y selección coherentes ante add/change/remove y soft-delete,
  con regresiones unitarias.
- `ChatPhotoController` es dueño de camera, photo picker, crop, permisos y
  archivos temporales; `ChatActivity` conserva el borde de grabación/audio y
  delega reglas de envío al ViewModel/caso de uso.
- URI de preview y uploads cruzan hacia el ViewModel como `String`; los tipos
  Android se materializan únicamente en Activity/controller/composables.

## Etapa de UI effects y errores

- Chat, profile y splash usan el host global de `BaseEdgeToEdgeActivity`; se
  eliminaron hosts y collectors paralelos. El host compartido acepta offset
  superior adicional y sigue calculando app bar, system bars y bottom nav.
- `ChatRoute`/`ChatScreen` ya no reciben `SnackBarManager`; el manager queda en
  el borde de Activity/handlers y existe un solo collector por host.
- `runCatchingPreservingCancellation` define la política común para operaciones
  suspendidas: relanza `CancellationException` y conserva failures ordinarios.
  Se aplicó en ViewModels, FCM y repositorios que antes podían degradar una
  cancelación a fallback o error visible.
- El servicio FCM dejó de capturar `Throwable`; relanza cancelación y limita el
  manejo a `Exception`.
- Tests de core verifican explícitamente cancellation y failure ordinario.

## Etapa de plataforma, permisos y dependencias visuales

- `POST_NOTIFICATIONS` dejó Splash y se solicita al entrar a Main, después de
  explicar su uso para mensajes. Rechazar no bloquea navegación; rationale
  permite reintentar y el rechazo permanente ofrece abrir App Settings.
- `NotificationPermissionCoordinator` encapsula Activity Result, persistencia
  de intento y Settings. Tests cubren pre-Android 13, primera solicitud y
  rechazo permanente.
- Coil se conserva como loader principal de Compose. Glide se mantiene en Views
  y componentes AndroidView existentes; migrarlos sin valor funcional ampliaría
  el riesgo de esta revisión.
- Se eliminaron Picasso, glide-transformations, Room, Volley y LiveData porque
  el source scan no encontró consumidores. Debug assemble, unit tests y
  compilación de androidTest pasaron sin ellos.
- Checklist manual pendiente de la validación final: permitir/rechazar/bloquear
  notificaciones; camera allow/deny/cancel; gallery y crop cancelado; audio
  allow/deny, mínimo, cancelación y envío.

## Etapa de contrato distribuido Firebase

- Android y Python comparten el parser backward-compatible del `chatId`
  histórico y ahora soportan participantes con `_`.
- `activeThread` incluye `updatedAt` de servidor. Functions exige un lease fresco
  de 120 segundos antes de omitir el push y promover a `MSG_SEEN`, eliminando el
  falso seen provocado por estado stale.
- El fan-out DM mantiene una sola escritura raíz, pero el unread del receptor usa
  `ServerValue.increment(1)`; envíos concurrentes ya no calculan el contador a
  partir de una lectura obsoleta.
- Rules quitó el write heredado sobre todo `Users/Data/{uid}`. El owner controla
  el `state` de su resumen; el otro participante solo puede crear el estado DM
  inicial y actualizar campos de entrega permitidos. ActiveView vuelve a ser
  privado del owner.
- Rules agregó el caso negativo de reemplazo/cambio de estado por el otro
  participante y el caso positivo de fan-out con incremento de servidor: 43/43.
- Python cubre parser, redacción de IDs, payload visible y freshness del lease.
  No se ejecutó deploy.
