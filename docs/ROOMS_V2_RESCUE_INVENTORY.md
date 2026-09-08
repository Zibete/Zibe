# RoomsV2 clean rescue inventory

> **Estado actual — septiembre 2026:** este documento nació como inventario de rescate de Phase 0. La implementación RoomsV2 ya fue portada y endurecida progresivamente en `feature/rooms-v2-clean`; el contenido histórico de abajo se conserva como registro de decisiones y del enfoque de rescate, no como estado operativo vigente.

## Estado funcional actual

La vertical RoomsV2 es funcional y fue validada manualmente en dispositivo para navegación/entrada a salas y flujo general. El CI de la rama está verde. La feature permanece en desarrollo y este PR no pretende cerrar todo el polish de UX.

### Pendientes conocidos / fuera de alcance de esta iteración

1. Al ingresar a una sala aparece un mensaje de error aunque la sala abre y el flujo continúa.
2. Falta mostrar la cantidad de personas presentes mediante badge sobre `person_icon`.
3. El composer/barra inferior presenta un comportamiento indeseado cuando se expande el teclado.
4. Media de RoomsV2 queda deliberadamente fuera de alcance de esta entrega.
5. Identidad visual: usuarios no incógnitos deben mostrar su avatar real; las iniciales quedan reservadas para identidad anónima/incógnita.
6. Falta restaurar la elección de identidad al entrar a la sala: perfil/avatar propio o identidad anónima con nickname configurable.

## Baseline histórico

- Clean target branch: `feature/rooms-v2-clean`
- Base: `main`
- Donor/reference branch: `feature/rooms-end-to-end`
- The donor branch must remain intact. Do not rewrite, delete, reset, rebase, squash, or force-push it.
- Product rule: preserve existing Zibe UX/components by default. Do not create a new visual component when Zibe already has an equivalent.
- UI completion rule: compile/CI is necessary but not sufficient. Physical confirmation by Matías is required; screenshots are requested only when the implementing AI has a concrete visual ambiguity that cannot be resolved from the existing design system/code.

## Product decisions already fixed

- Leaving a room closes/hides all contextual privates for the leaver without deleting history.
- Re-entering can recover a previous contextual private only when the same contextual identity is recovered.
- Anonymous identity continuity means same Firebase user + same alias in that room. A different alias starts distinct contextual private history.
- Owner can appoint moderators. Moderation includes kick, ban, message removal and report review.
- Private report review must expose only report evidence, not arbitrary private-history browsing.
- Public room trees must never expose Firebase UID/internal ownership identifiers.
- Existing DM delivery/received/seen contract must remain unchanged.

## KEEP — preserve from `main`

These are the clean Zibe baseline and are the reference implementation for UX and architecture. They are not to be replaced by donor equivalents unless a narrowly justified change is required.

### Existing room directory / entry experience

- Existing Salas/Groups directory UX on `main`.
- Existing navigation shell from PR #52.
- Existing Zibe bottom navigation, toolbar/account behavior and search patterns.
- Existing room directory should be adapted to RoomsV2 contracts only where necessary; it is not a redesign target.

### Existing chat/design system

- `app/src/main/java/com/zibete/proyecto1/ui/chat/ChatScreen.kt`
- `app/src/main/java/com/zibete/proyecto1/ui/chat/ChatTimeline.kt`
- `app/src/main/java/com/zibete/proyecto1/ui/chat/message/ChatMessageRow.kt`
- `app/src/main/java/com/zibete/proyecto1/ui/chat/components/ChatInput.kt`
- `app/src/main/java/com/zibete/proyecto1/ui/chat/components/ChatInfoRow.kt`
- `app/src/main/java/com/zibete/proyecto1/ui/chat/components/ChatTopBar.kt`
- Existing photo/audio/recording/chat media components.
- `core/designsystem/src/main/java/com/zibete/proyecto1/ui/theme/Theme.kt`
- `core/designsystem/src/main/java/com/zibete/proyecto1/ui/theme/ZibeColors.kt`
- `core/designsystem/src/main/java/com/zibete/proyecto1/ui/theme/ZibeTextStyles.kt`

Target room-chat visual language: normal Zibe chat, with room-specific author treatment (profile circle/avatar + display name/alias + Zibe bubble) and `ChatInfoRow`-style timeline events such as “Zibe se unió a la sala”.

### Existing DM contract

Preserve without semantic changes:

- `MSG_DELIVERED = 1`
- `MSG_RECEIVED = 2`
- `MSG_SEEN = 3`
- Android receiver acknowledgement semantics.
- Active DM directly seen behavior.
- Chat-list checks/unread semantics.
- Existing DM FCM routing and soft-delete/tombstone rules.

## PORT WITH FIXES — donor implementation worth rescuing

Port semantically in small commits. Do not bulk cherry-pick the 40 donor commits.

### RoomsV2 domain contract

Candidate files:

- `domain/src/main/java/com/zibete/proyecto1/domain/roomsv2/RoomsV2Contracts.kt`
- `domain/src/main/java/com/zibete/proyecto1/domain/roomsv2/RoomV2Exception.kt`
- `domain/src/main/java/com/zibete/proyecto1/domain/roomsv2/RoomsV2UseCases.kt`
- `domain/src/main/java/com/zibete/proyecto1/domain/roomsv2/RoomsV2Validation.kt`
- `domain/src/test/java/com/zibete/proyecto1/domain/roomsv2/RoomsV2ValidationTest.kt`

Review before port:

- Add reaction/reply model deliberately rather than later retrofitting if they affect message schema.
- Keep identity-facing APIs UID-free.
- Keep Android/Firebase dependencies out of domain.

### RoomsV2 Firebase data adapter

Candidate files:

- `data/src/main/java/com/zibete/proyecto1/data/roomsv2/FirebaseRoomsV2Repository.kt`
- `app/src/main/java/com/zibete/proyecto1/di/RoomsV2Module.kt`

Required fixes before acceptance:

- Map `FirebaseFunctionsException` details using backend key `roomV2Code` instead of collapsing failures to `INTERNAL`.
- Preserve coroutine cancellation.
- Verify callable payload/result contracts against final backend.
- Do not expose UID through public identity models.

### RoomsV2 Functions/state engine

Candidate files:

- `functions/rooms_v2.py`
- `functions/rooms_v2_core.py`
- `functions/rooms_v2_state.py`
- `functions/rooms_v2_private.py`
- `functions/rooms_v2_moderation.py`
- `functions/rooms_v2_notification_core.py`
- `functions/rooms_v2_notifications.py`
- RoomsV2 unit tests under `functions/tests/`.

Required fixes before acceptance:

- `publicMembers`/public identity projections must never contain `publicProfileId` when that value is a Firebase UID.
- Add regression test proving UID strings do not enter public room/member/message trees.
- Re-audit owner/moderator hierarchy and report evidence boundaries.
- Preserve contextual-private restore/close rules already agreed.
- Extend message model deliberately for reactions/replies before declaring message contract stable.

### RTDB Rules / test harness

Candidate files:

- `database.roomsv2.rules.json`
- `tools/firebase-rules-tests/merge_rooms_rules.py`
- `tools/firebase-rules-tests/rooms-v2.rules.test.js`
- `tools/firebase-rules-tests/rooms-combined.rules.test.js`
- relevant `package.json` scripts.

Required fixes/review:

- Canonical deploy must use combined base + RoomsV2 rules.
- Rules are not filters; sensitive children cannot live beneath readable public parents.
- Re-run base, V2 and combined suites after every schema change.

### Firebase Functions entrypoint

Candidate concept from donor:

- split legacy Functions into `legacy_main.py` and export RoomsV2 through `functions/main.py`.

Port only after verifying that Firebase discovery/deploy behavior remains deterministic and existing function names stay unchanged.

### RoomsV2 notification routing

Candidate donor work:

- `app/src/main/java/com/zibete/proyecto1/ZibeFirebaseMessagingService.kt`
- `app/src/main/java/com/zibete/proyecto1/notifications/NotificationHelper.kt`
- RoomsV2 payload contract.

Required fixes/review:

- Explicitly distinguish `room_v2` and `room_private_v2`; never derive a legacy room key from the notification type.
- Public push routes to room; private push routes to exact contextual conversation.
- Do not alter DM notification semantics.
- Route through a session-safe bootstrap; stale post-logout notification must not bypass auth/session checks.

### Local emulator/test tooling

Potentially useful but optional:

- `firebase.local.json`
- `firebase.roomsv2.local.json`
- `firebase.rooms-combined.local.json`
- local Functions/Android probe tooling.

Port only what materially improves reproducible testing. Avoid making the production app depend on local-test infrastructure.

## DISCARD — do not port as implementation

Keep available only as donor/reference history.

### Generic RoomsV2 UI introduced in donor branch

- `app/src/main/java/com/zibete/proyecto1/ui/groups/RoomsV2Screen.kt` as a replacement redesign of the already-good directory.
- `app/src/main/java/com/zibete/proyecto1/ui/groups/host/RoomV2HostScreen.kt`.
- Its large administrative `RoomHeader`, permanent `Chat/Personas/Privados/Reportes` tab model, full-width message cards and `OutlinedTextField + Enviar` composer.
- `RoomV2HostFragment/ViewModel/UiState` must not be copied wholesale. Business/state ideas may be reimplemented against the clean room-chat UX.

### Parallel/experimental room UI generations

Do not port wholesale:

- experimental `RoomsScreen.kt` donor version;
- donor rewrites of `GroupHostRoute`, `GroupChatTab`, `GroupPrivateChatsTab`, `GroupUsersTab` merely to preserve the abandoned UI architecture;
- giant donor `GroupsViewModel` / `GroupRepository` rewrites unless an individual behavior is proven necessary for RoomsV2.

### Legacy deletions/refactors that are not required for RoomsV2

Do not reproduce deletions merely because the donor branch removed them:

- legacy group adapters/layout removals;
- unrelated Auth/session/UI changes;
- build/test changes with no direct RoomsV2 requirement.

Any cleanup happens later as an independent, justified refactor after the new implementation supersedes legacy behavior.

## Clean implementation sequence after Phase 0

1. Port and harden RoomsV2 domain/backend/rules only; app should still look and behave like clean `main`.
2. Adapt the existing Salas directory to the new contract without redesigning it.
3. Build the public room chat from existing Zibe chat primitives/components.
4. Add participant surface and contextual identity behavior.
5. Add contextual private chats using the normal Zibe DM visual language.
6. Add reactions + replies as explicit product features and backend contracts.
7. Reuse existing photo/audio/recording UX with RoomsV2-safe Storage paths/rules.
8. Expose moderation through secondary menus/sheets, not as primary chat navigation.
9. Add RoomsV2 FCM/badges/session-safe routing.
10. Validate CI + real-device compile/runtime checkpoints. Request screenshots only for a concrete visual ambiguity that code/design tokens cannot resolve.

## Phase 0 exit criterion histórico

Phase 0 se consideró completa cuando:

- `feature/rooms-v2-clean` quedó creada desde `main`;
- la rama donante permaneció intacta y disponible como referencia;
- este inventario quedó versionado;
- todavía no se había copiado implementación RoomsV2 a la rama limpia.
