# 🔥 Firebase — contrato vigente (RTDB · Functions · FCM)

Este documento describe el contrato Firebase que usa actualmente ZIBE en `feature/rooms-v2-clean`.

> Los cambios de paths, permisos o payloads deben mantenerse alineados entre Android, Functions, Rules y tests. Esta rama no despliega ni migra datos remotos por sí sola.

---

## Fuentes de verdad

El ruleset que se despliega **no** es `database.rules.json` de forma aislada.

- `database.rules.json`: contrato base de ZIBE fuera de RoomsV2.
- `database.roomsv2.rules.json`: contrato exclusivo de RoomsV2.
- `tools/firebase-rules-tests/merge_rooms_rules.py`: combina ambos árboles.
- `build/generated/firebase/database.rooms-combined.rules.json`: ruleset generado.
- `firebase.json`: referencia el ruleset combinado para Realtime Database y ejecuta el merge antes del deploy.

Por lo tanto, cualquier cambio de esquema debe preservar la compatibilidad de las suites base, RoomsV2 y combined.

---

## Realtime Database

### Árbol base actual

```text
/
├─ Users
│  ├─ Accounts
│  │  └─ {uid}
│  │     ├─ id
│  │     ├─ name
│  │     ├─ birthDate
│  │     ├─ createdAt
│  │     ├─ age
│  │     ├─ email
│  │     ├─ photoUrl
│  │     ├─ isOnline
│  │     ├─ description
│  │     ├─ latitude
│  │     └─ longitude
│  └─ Data
│     └─ {uid}
│        ├─ ClientData
│        │  ├─ Status
│        │  │  ├─ status
│        │  │  └─ lastSeenMs
│        │  └─ ActiveView
│        │     └─ activeThread
│        │        ├─ nodeType
│        │        ├─ otherUid
│        │        └─ updatedAt
│        ├─ FavoriteList
│        │  └─ {otherUid}: true
│        └─ dm
│           └─ {otherUid}
│              ├─ lastContent
│              ├─ lastMessageAt
│              ├─ userId
│              ├─ otherId
│              ├─ otherName
│              ├─ otherPhotoUrl
│              ├─ state
│              ├─ unreadCount
│              └─ seen
├─ Chats
│  └─ dm
│     └─ {chatId}
│        └─ {messageId}
│           ├─ content
│           ├─ createdAt
│           ├─ senderUid
│           ├─ type
│           ├─ seen
│           └─ audioDurationMs? (opcional)
├─ Sessions
│  └─ {uid}
│     ├─ activeInstallId
│     └─ fcmToken
└─ Feedback
   └─ {screen}
      └─ {feedbackId}
         ├─ id
         ├─ name
         ├─ email
         ├─ feedback
         ├─ device
         ├─ appVersion
         └─ createdAt
```

### RoomsV2

RoomsV2 vive bajo una raíz independiente:

```text
/RoomsV2
├─ publicRooms
├─ publicMembers
├─ publicMessages
├─ privateMessages
├─ membershipIndexByUser
├─ visibleStateByUser
├─ conversationIndexByUser
├─ roomNameIndex
├─ aliasIndex
├─ roomInternal
├─ identityOwners
├─ conversationParticipants
├─ reportEvidence
├─ reports
├─ bans
├─ roomOperations
├─ messageRequests
└─ privateMessageRequests
```

La separación entre proyecciones públicas, índices privados e información interna es parte del contrato de seguridad. Un dato sensible no debe colocarse debajo de un padre legible públicamente esperando que Rules actúe como filtro.

#### Proyecciones públicas

- `RoomsV2/publicRooms/{roomId}`: directorio de salas.
- `RoomsV2/publicMembers/{roomId}/{identityId}`: identidad contextual visible dentro de la sala.
- `RoomsV2/publicMessages/{roomId}/{messageId}`: timeline público de la sala.

Las identidades públicas son deliberadamente **UID-free**. Los Firebase Auth UIDs o referencias internas de ownership no deben filtrarse a `publicRooms`, `publicMembers` ni `publicMessages`.

#### Estado por usuario

- `RoomsV2/membershipIndexByUser/{uid}`: membresías del usuario.
- `RoomsV2/visibleStateByUser/{uid}`: estado visible/read del usuario.
- `RoomsV2/conversationIndexByUser/{uid}/{roomId}`: privados contextuales visibles para ese usuario.

#### Privados contextuales

- `RoomsV2/privateMessages/{conversationId}` contiene el historial del privado contextual.
- El acceso se resuelve con los índices/participantes internos de RoomsV2; no reutiliza `Chats/group_dm` ni `Users/Data/{uid}/group_dm`.
- Abandonar una sala cierra/oculta sus privados contextuales para quien sale, sin borrar el historial.
- Al reingresar, un privado anterior solo puede recuperarse cuando se recupera la misma identidad contextual.
- Para identidad anónima, continuidad significa mismo usuario Firebase + mismo alias dentro de esa sala. Otro alias inicia identidad e historial contextual distintos.

#### Mutaciones RoomsV2

El cliente observa las proyecciones que las Rules permiten leer, pero las mutaciones sensibles de RoomsV2 se realizan mediante Functions/callables. El backend mantiene los índices, ownership, moderación, secuencias, reportes y fan-out coherentes.

---

## Paths legacy retirados

La rama limpia ya no ofrece contrato de runtime ni permisos específicos para:

```text
/Groups/*
/Chats/group_dm/*
/Users/Data/{uid}/group_dm/*
/Users/Data/{uid}/ChatList/readGroupMessages
/Users/Data/{uid}/ClientData/ChatList/readGroupMessages
```

También fue retirado el trigger legacy de Functions asociado a `/Groups/Chat/*`.

La eliminación del soporte en código/Rules **no borra datos que pudieran existir actualmente en Firebase**. Cualquier limpieza o migración de datos remotos debe tratarse como una operación independiente, explícitamente autorizada y validada antes de ejecutarse.

---

## Usuarios y presencia

### Perfil

`Users/Accounts/{uid}` contiene el perfil consumible por la app. Las Rules permiten lectura autenticada y escritura del propio usuario.

### Estado privado

- `Users/Data/{uid}/ClientData/Status`: presencia y `lastSeenMs`.
- `Users/Data/{uid}/ClientData/ActiveView/activeThread`: thread activo que Android publica mientras una conversación está visible.

`activeThread` contiene:

```text
nodeType
otherUid
updatedAt
```

Functions considera ese estado activo solo dentro de un lease de 120 segundos. Android lo actualiza al entrar y lo limpia al salir.

### Favoritos

`Users/Data/{uid}/FavoriteList/{otherUid}` es propiedad del usuario `{uid}`.

---

## DM — contrato de mensajes y chatlist

### Mensajes

Path:

```text
Chats/dm/{chatId}/{messageId}
```

Estados de entrega/lectura:

| Estado | Writer válido | Significado |
|---|---|---|
| `MSG_DELIVERED = 1` | Sender Android al crear el mensaje | Mensaje persistido por el sender. |
| `MSG_RECEIVED = 2` | Receptor Android | El dispositivo receptor ejecutó el flujo de recepción. |
| `MSG_SEEN = 3` | Receptor Android o backend cuando el DM ya está activo | El receptor vio/leyó el mensaje. |

`Chats/dm/{chatId}/{messageId}/seen` es monotónico: solo admite enteros `1..3`, la creación parte en `1` y no se permiten downgrades.

Durante actualizaciones de estado permanecen inmutables `senderUid`, `content`, `createdAt` y `audioDurationMs`. El borrado visual sigue usando los tipos participant-specific existentes; no se elimina físicamente el mensaje desde el cliente.

### Resumen por conversación

Path:

```text
Users/Data/{uid}/dm/{otherUid}
```

- `seen`: estado visual del último mensaje en chatlist; admite `0..3` y puede resetearse cuando llega un mensaje nuevo.
- `unreadCount`: entero no negativo; para un fan-out iniciado por el otro participante, Rules solo permite el incremento esperado del receptor.
- `state`: pertenece al owner `{uid}`; el otro participante no puede reemplazarlo arbitrariamente.
- `lastMessageAt`: el fan-out usa timestamp de servidor y Rules bloquea timestamps extremos a futuro (`now + 5s`).

El mensaje individual y el resumen de chatlist tienen semánticas distintas; no deben tratarse como un único contador/estado.

### DM activo

Si Functions detecta que el receptor mantiene ese mismo DM en `activeThread` dentro del lease válido:

1. no envía push;
2. avanza el mensaje a `MSG_SEEN`;
3. sincroniza el resumen si el mensaje sigue siendo el último;
4. limpia el unread correspondiente del receptor.

### `chatId`

Se conserva el formato histórico `<sortedUidA>_<sortedUidB>` cuando no existe ambigüedad por `_`. Cuando alguno de los UIDs contiene `_`, Android/Functions usan el formato no ambiguo con `|`.

Las Rules aceptan el formato seguro actual y bloquean lecturas ambiguas de paths legacy. Esta rama no ejecuta una migración remota de chat IDs existentes.

---

## Sesiones

```text
Sessions/{uid}/activeInstallId
Sessions/{uid}/fcmToken
```

- `activeInstallId`: visible/escribible únicamente por el usuario dueño.
- `fcmToken`: el usuario dueño lo escribe; Functions lo usa para notificaciones.

El flujo de sesión/dispositivo debe seguir evitando que notificaciones o callbacks posteriores al logout salten el bootstrap de autenticación.

---

## Feedback

```text
Feedback/{screen}/{feedbackId}
```

Solo usuarios autenticados pueden escribir. `id` debe coincidir con el `auth.uid` del writer.

---

## FCM + Functions

El runtime Android soporta explícitamente:

1. DM (`dm`).
2. Notificaciones públicas/privadas de RoomsV2 según `RoomsV2NotificationContract`.

Payloads legacy de Groups/`group_dm` ya no tienen una ruta de producción soportada y son ignorados por el cliente.

Para DM, el backend usa data messages de prioridad alta y Android crea la notificación local después de procesar el payload. Un `messaging.send()` exitoso significa que FCM aceptó el envío; no equivale a `MSG_RECEIVED`.

RoomsV2 mantiene su propio contrato de payload, navegación y privados contextuales; no deriva un path legacy a partir del tipo de notificación.

---

## Rules + Emulator + CI

Suites disponibles:

```bash
npm ci
npm run test:rules
npm run test:rules:v2
npm run test:rules:combined
```

O todas juntas:

```bash
npm run test:rules:all
```

Generación manual del ruleset combinado:

```bash
python tools/firebase-rules-tests/merge_rooms_rules.py
```

Salida:

```text
build/generated/firebase/database.rooms-combined.rules.json
```

El CI ejecuta Functions contract tests y las suites de Rules base + RoomsV2 + combined antes de los checks Android.

---

## Storage

La app continúa usando Firebase Storage para contenido como fotos de perfil y media de chat donde corresponde. Los paths efectivos deben verificarse contra el código que los escribe.

`firebase.json` de esta rama no define actualmente un bloque de deploy de Storage Rules; por lo tanto este documento no declara un ruleset de Storage como fuente de verdad de producción.

---

## Operación / deploy

Este contrato describe el código de la rama, **no confirma qué revisión está desplegada actualmente en Firebase**.

Un deploy de Functions, RTDB Rules o una limpieza/migración remota debe hacerse como una operación separada, después de autorización explícita y validación del entorno/proyecto objetivo.
