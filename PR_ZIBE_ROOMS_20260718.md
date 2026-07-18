# 🚪 feat(rooms): implementar Salas de punta a punta

Base: `main` (`bf187`)
Rama: `feature/rooms-end-to-end`

## 🧩 Qué se cambió

- `domain` incorpora contratos y casos de uso para crear, ingresar, cambiar,
  leer, reanudar y salir de una sala con `ZibeResult`.
- `data` centraliza RTDB, Storage y DataStore; usa fan-outs raíz atómicos,
  timestamps/incrementos de servidor y sesión local posterior al éxito remoto.
- `app/ui/groups` reemplaza el placeholder por listado Compose con búsqueda,
  refresh, loading/empty/error, sheets de creación/ingreso y confirmación al
  cambiar de sala.
- `app/ui/groups/host` entrega Chat, Participantes y Privados: texto, imagen,
  eventos, retry, lectura visible, perfil seguro y routing `group_dm`.
- `MainViewModel` consolida unread público y privado en el badge Salas.
- `ZibeFirebaseMessagingService` y `NotificationHelper` abren la sala correcta,
  respetan preferencia/permisos y deduplican por `roomKey + messageId`.
- `database.rules.json` protege metadata, identidad, membresía, mensajes,
  lectura, `activeThread` y fan-out contextual `group_dm`.
- `functions/main.py` notifica sólo a miembros actuales, excluye emisor y sala
  activa, usa payload data-only y evita identificadores completos en logs.
- Se eliminaron adapters/layouts Groups obsoletos reemplazados por Compose.

## 🏗️ Cambios de API/Contratos

- Las salas nuevas usan `roomKey`/`roomId` generado e inmutable; el nombre es
  presentación y `Groups/Names` resuelve unicidad.
- `Groups/Aliases` reserva alias anónimos o una key técnica estable por UID para
  perfil real, sin convertir el nombre visible en identidad técnica.
- Unread público vive en `Users/Data/{uid}/Rooms/{roomKey}` y sólo se limpia con
  Chat visible. `readGroupMessages` queda como fallback legacy.
- Los privados conservan `Chats/group_dm` y `Users/Data/{uid}/group_dm`; sus
  resúmenes modernos agregan `roomKey` y `lastMessageId` vinculados al fan-out.
- `activeThread` admite `{nodeType: room, roomKey, updatedAt}` con lease.
- Salir elimina únicamente membresía, alias, estado de sala y sesión activa; no
  elimina privados ni historiales.

## 🕰️ Compatibilidad legacy

- Se mantienen `Groups/Meta`, `Groups/Users`, `Groups/Chat`,
  `Chats/group_dm` y resúmenes `group_dm` existentes.
- Si falta `roomId`, la key histórica se adapta como identidad de sala.
- Conteo y lectura legacy se derivan sin borrar ni migrar datos remotos.
- Entradas corruptas o ambiguas se omiten de forma segura sin crash ni cleanup.

## ✅ Validación Local

```bash
python scripts/check_architecture.py
python -m py_compile functions/main.py
python -m unittest discover -s functions/tests -p "test_*.py"
npm ci
npm run test:rules
./gradlew help --warning-mode all --no-daemon
./gradlew testDebugUnitTest --no-daemon
./gradlew :app:generateDebugAndroidTestLintModel --no-daemon
./gradlew :app:compileDebugAndroidTestKotlin --no-daemon
./gradlew lintDebug --rerun-tasks --no-daemon
./gradlew :app:assembleDebug --no-daemon
./gradlew :app:compileReleaseKotlin --no-daemon
zipalign -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk
git diff --check
```

- Arquitectura: OK.
- Functions: 19 tests, 0 fallos.
- Rules: 67 tests, 0 fallos.
- Kotlin/JVM: 156 tests, 0 fallos.
- AndroidTest Kotlin, lint, debug APK y release Kotlin: `BUILD SUCCESSFUL`.
- APK 16 KB: `Verification successful`.
- `connectedDebugAndroidTest`: suite construida y empaquetada; ejecución
  bloqueada por `No connected devices!` (`adb devices -l` vacío).
- `npm ci`: OK con warning de engine por Node 24 y auditoría existente de 41
  vulnerabilidades; no se actualizó toolchain fuera de alcance.

## 🧪 Tests agregados

- Unit: validaciones, alias ocupado, creación, ingreso real/anónimo, cambio,
  salida no destructiva, búsqueda, reducers/ViewModels, unread/visibilidad,
  `group_dm`, legacy, errores y cancelación.
- Rules: allow/deny para salas, identidad, membresía, mensajes, cursores,
  concurrencia en el mismo milisegundo, privados y seguridad DM preservada.
- Functions: destinatarios, emisor, exmiembros, sala activa, token ausente,
  texto/multimedia, payload inválido, errores FCM y logs seguros.
- Instrumentation con fakes/Hilt: listado/estados/sheets, host y tabs, back/exit,
  eliminación del placeholder y routing de intents de notificación.

## 📱 Validación manual pendiente

- [ ] Crear una sala con perfil real y otra con alias anónimo usando dos cuentas.
- [ ] Cambiar de sala, confirmar evento de salida/entrada y sesión restaurada.
- [ ] Enviar/recibir texto e imagen con app foreground, background y killed.
- [ ] Confirmar unread público sólo al ver Chat y privado independiente.
- [ ] Abrir perfil/privado desde participante y desde tab Privados.
- [ ] Verificar que un anónimo no exponga su perfil real ni permita self-chat.
- [ ] Salir y confirmar que `group_dm` e historiales siguen disponibles.
- [ ] Validar TalkBack, font scaling, rotación, teclado e insets en dispositivo.
- [ ] Abrir notificación de sala y confirmar supresión con sala activa.

## 🚀 Publicación posterior requerida

No se ejecutó ningún deploy. Después de aprobar el PR y validar físicamente:

```bash
firebase deploy --only database --project zproyecto1
firebase deploy --only functions:on_group_message_created --project zproyecto1
```

## 🚧 Fuera de alcance

- Audio público, reacciones, respuestas, menciones y moderación avanzada.
- Migración destructiva o renombre remoto de salas legacy.
- Upgrades generales de dependencias/toolchain.
- Push, deploy, merge, release o tag desde esta rama.

## 📚 Commits incluidos

- `b7559 feat(rooms): add lifecycle and persistence contracts`
- `67bad feat(rooms-ui): deliver discovery and room host`
- `98310 feat(firebase): secure rooms and notification delivery`
- `165b5 docs(rooms): document contracts and compatibility`
