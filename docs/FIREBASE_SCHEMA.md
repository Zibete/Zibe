# 🔥 Firebase — Esquema y contratos (RTDB · Storage · FCM)

Este documento define el **contrato de datos** entre la app y Firebase: dónde vive cada dato, quién lo escribe/lee y qué invariantes se esperan (seguridad, estructura, consistencia).

> 📌 Los paths listados deben mantenerse **estables**. Si se renombra un nodo, actualizar este documento + reglas + tests.

---

## 🧭 Convenciones

- RTDB usa `PascalCase` para raíces (`Users`, `Groups`, `Sessions`) y subnodos descriptivos.
- Separación conceptual:

| Área | Descripción |
|---|---|
| Perfil público | Datos presentables (nombre, foto, etc.). |
| Datos privados por usuario | Estado, listas, contadores, vistas activas. |
| Mensajes | Colecciones append-only por chat/grupo. |
| Sesiones | Token FCM e instalación activa (control de dispositivo). |

---

## 🗄️ Realtime Database (RTDB)

## 🌳 Árbol completo actual (RTDB)

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
│        │  ├─ ActiveView
│        │  │  └─ activeThread
│        │  │     ├─ nodeType
│        │  │     ├─ otherUid
│        │  │     └─ updatedAt
│        │  └─ ChatList
│        │     └─ readGroupMessages
│        ├─ ChatList
│        │  └─ readGroupMessages
│        ├─ FavoriteList
│        │  └─ {otherUid}: true
│        ├─ dm
│        │  └─ {otherUid}
│        │     ├─ lastContent
│        │     ├─ lastMessageAt
│        │     ├─ userId
│        │     ├─ otherId
│        │     ├─ otherName
│        │     ├─ otherPhotoUrl
│        │     ├─ state
│        │     ├─ unreadCount
│        │     └─ seen
│        └─ group_dm
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
│  ├─ dm
│  │  └─ {chatId}
│  │     └─ {messageId}
│  │        ├─ content
│  │        ├─ createdAt
│  │        ├─ senderUid
│  │        ├─ type
│  │        ├─ seen
│  │        └─ audioDurationMs? (opcional)
│  └─ group_dm
│     └─ {chatId}
│        └─ {messageId}
│           ├─ content
│           ├─ createdAt
│           ├─ senderUid
│           ├─ type
│           ├─ seen
│           └─ audioDurationMs? (opcional)
├─ Groups
│  ├─ Meta
│  │  └─ {groupName}
│  │     ├─ name
│  │     ├─ description
│  │     ├─ creatorUid
│  │     ├─ type
│  │     ├─ users
│  │     ├─ createdAt
│  │     └─ totalMessages
│  ├─ Users
│  │  └─ {groupName}
│  │     └─ {uid}
│  │        ├─ userId
│  │        ├─ userName
│  │        ├─ type
│  │        └─ joinedAtMs
│  └─ Chat
│     └─ {groupName}
│        └─ {messageId}
│           ├─ content
│           ├─ timestamp
│           ├─ senderUid
│           ├─ chatType
│           ├─ userType
│           └─ userName | nameUser
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

> Fuente de verdad del árbol y validaciones: `database.rules.json`.

### 👤 Usuarios

**Perfil público**

| Path | Propósito | Lectura | Escritura |
|---|---|---|---|
| `Users/Accounts/{uid}` | Perfil visible/consumible por la app. | Frecuente (listas, perfil). | Usuario autenticado (solo su `uid`). |

**Datos privados**

| Path | Propósito | Invariante |
|---|---|---|
| `Users/Data/{uid}/ClientData/Status` | Presencia / última actividad (`lastSeenMs`, `isOnline`). | Actualizaciones frecuentes y livianas — evitar payloads grandes. |
| `Users/Data/{uid}/ClientData/ActiveView` | Vista activa (qué chat/pantalla está mirando). | `activeThread` incluye `updatedAt`; Functions solo lo acepta durante un lease de 120 segundos y la app lo limpia al salir. |

**Listas y contadores**

| Path | Propósito | Invariante |
|---|---|---|
| `Users/Data/{uid}/ChatList` | Estado resumido por conversación (unread / seen / último mensaje). | Índice para la UI — no almacenar histórico completo acá. |
| `Users/Data/{uid}/FavoriteList` | Favoritos del usuario. | Solo el usuario escribe su lista. |

---

### 💬 Conversaciones (resúmenes)

| Path | Propósito | Invariante |
|---|---|---|
| `Users/Data/{uid}/dm/{otherUid}` | Metadata de conversación 1:1 (último mensaje, timestamp, flags). | No duplicar mensajes — es metadata para construir la lista rápido. |
| `Users/Data/{uid}/group_dm/{otherUid}` | Resumen de conversaciones grupales/relación. | Estructura consistente con la UI que lo consume. |

> 📌 Documentar en el código cómo se construye `chatId` (si aplica) y qué campos mínimos existen en estos resúmenes.

El owner `{uid}` controla `state` (bloqueo, silencio, ocultamiento). El otro
participante solo escribe campos de entrega permitidos y nunca escribe `state`
ni reemplaza el resumen completo. `unreadCount` exige exactamente el valor
anterior + 1 para el sender y se materializa con `ServerValue.increment(1)`
dentro del mismo fan-out raíz. Rules exige, además, timestamp creciente, `seen =
0` y estado sin cambios para un mensaje nuevo. Receipt solo puede avanzar
`seen` si el resto del resumen permanece idéntico.
El timestamp escrito por el partner no puede superar `now + 5s`, evitando que
un cliente alterado bloquee envíos posteriores con un valor futuro extremo.
Android persiste `createdAt`/`lastMessageAt` del fan-out DM con
`ServerValue.TIMESTAMP`, por lo que el contrato no depende del reloj del equipo.

---

### 📨 Mensajes

| Path | Propósito | Invariante |
|---|---|---|
| `Chats/dm/{chatId}/{messageId}` | Mensajes de conversaciones directas. | Append-only — para "borrar", preferir flags o limpieza controlada. |
| `Chats/group_dm/{chatId}/{messageId}` | Mensajes con estructura de grupo (según implementación actual). | Considerar consolidación con `Groups/Chat/...` a futuro sin romper compatibilidad. |
| `Groups/Chat/{groupName}/{messageId}` | Mensajes de un grupo identificado por `groupName`. | `groupName` debe ser estable — evitar renames que rompan historial. |

#### Contrato de entrega y lectura DM

| Estado | Writer válido | Significado |
|---|---|---|
| `MSG_DELIVERED = 1` | Cliente Android sender al crear el mensaje. | El mensaje fue persistido por el sender. |
| `MSG_RECEIVED = 2` | Cliente Android receptor en `ZibeFirebaseMessagingService.onMessageReceived()`. | El receptor ejecutó código Android y confirmó la recepción del data-message. |
| `MSG_SEEN = 3` | Cliente Android receptor al ver el chat, o `on_dm_message_created` si el receptor ya está en ese DM activo. | El receptor vio o leyó el mensaje. |

Los cambios de estado son monotónicos: `MSG_SEEN` no vuelve a `MSG_RECEIVED` y
`MSG_RECEIVED` no vuelve a `MSG_DELIVERED`. Un resultado exitoso de
`messaging.send()` confirma únicamente que FCM aceptó el envío; no cuenta como
recepción del dispositivo.

Los tres valores no representan lo mismo en todos los nodos:

- `Chats/dm/{chatId}/{messageId}/seen` es el estado monotónico del mensaje y
  controla los checks de la burbuja.
- `Users/Data/{uid}/dm/{otherUid}/seen` es el estado visual del último mensaje
  de esa conversación en chatlist. Puede volver a un valor menor cuando llega
  un mensaje nuevo; no comparte la monotonicidad del mensaje individual.
- `Users/Data/{uid}/dm/{otherUid}/unreadCount` es el badge del receptor. Puede
  incrementarse y debe volver a `0` cuando los mensajes quedan vistos.

Los DM se envían por FCM como data-only con prioridad alta. El cliente receptor
crea la notificación local y confirma `MSG_RECEIVED` después de ejecutar
`ZibeFirebaseMessagingService.onMessageReceived()`. Si el receptor ya publicó
el mismo DM en `activeThread`, `on_dm_message_created` no envía push: avanza el
mensaje directamente a `MSG_SEEN`, sincroniza el resumen si sigue siendo el
último mensaje y limpia el badge.

`chatId` conserva `<sortedUidA>_<sortedUidB>` cuando ambos UIDs no contienen
underscore, manteniendo los paths históricos. Si alguno contiene `_`, usa el
formato no ambiguo `<sortedUidA>|<sortedUidB>`. Android, Functions y Rules
aceptan ambos formatos; Rules no autoriza UIDs con `_` sobre paths legacy
ambiguos. El carácter `|` queda reservado y no se admite dentro de un UID.
Antes de desplegar Rules debe auditarse si existen paths legacy con múltiples
underscores: quedan bloqueados por seguridad y requieren una migración operativa
explícita al formato `|`; esta rama no despliega ni migra datos remotos.

Firebase Rules protege el `seen` de mensaje como entero `1..3`, exige
`MSG_DELIVERED` en la creación y bloquea downgrades, eliminación o cambios de
participantes no autorizados. También mantiene inmutables `senderUid`, `content`,
`createdAt` y `audioDurationMs` durante actualizaciones. En conversaciones,
`seen` es un entero `0..3` y `unreadCount` un entero no negativo, sin máximo
arbitrario.

El soft-delete es participant-specific: solo el sender aplica tipos
`*_SENDER_DLT` y solo el receptor `*_RECEIVER_DLT`. Cuando ambos borraron, el
mensaje queda como tombstone `*_BOTH_DLT`; el cliente no lo elimina físicamente.
Una limpieza definitiva requiere un proceso backend confiable.

El código local no demuestra qué revisión de Functions está desplegada. Para
publicar explícitamente este handler, el responsable operativo debe ejecutar:

```bash
firebase deploy --only functions:on_dm_message_created --project zproyecto1
```

La verificación operativa posterior debe cubrir el data-message en
background/chatlist y el caso con ambos usuarios en el mismo DM activo. Para
inspeccionar Functions Gen2 o Cloud Logging puede usarse Cloud Shell si el
entorno local no dispone de las herramientas necesarias.

---

### 👥 Grupos

| Path | Propósito | Invariante |
|---|---|---|
| `Groups/Meta/{groupName}` | Título, foto, owner, settings del grupo. | Cambios moderados — no alta frecuencia. |
| `Groups/Users/{groupName}/{uid}` | Membresía / rol / estado del usuario en el grupo. | Escrituras restringidas a owner/admin o lógica definida. |

---

### 🔔 Sesiones y notificaciones

| Path | Propósito | Invariante |
|---|---|---|
| `Sessions/{uid}/fcmToken` | Token FCM actual del usuario. | Se actualiza al refrescar token / iniciar sesión; se limpia en logout si corresponde. |
| `Sessions/{uid}/activeInstallId` | Instalación/dispositivo activo (control de sesión). | Si se detecta conflicto, la app debe manejar cierre/control según el flujo de sesión. |

---

### 🗣️ Feedback

| Path | Propósito | Invariante |
|---|---|---|
| `Feedback/{screen}/{feedbackId}` | Feedback autenticado y trazable por pantalla/flujo. | Escribir solo autenticado — evitar incluir datos sensibles. |

---

## 🗃️ Storage

| Path | Propósito | Recomendación |
|---|---|---|
| `profile_photos/` | Fotos de perfil. | Nombres por `uid` + timestamp o hash (evitar colisiones). |
| `photos/` | Fotos compartidas en chats. | Segmentar por chat/grupo si la regla lo requiere: `photos/{chatId}/...` |
| `audios/` | Audios de chat. | Mismo criterio que `photos/`. |

> 📌 Regla de oro: Storage debe asegurar que solo participantes/members puedan leer/crear objetos asociados.

---

## 📬 FCM + Functions (backend)

- Tokens almacenados en `Sessions/{uid}/fcmToken`.
- El backend en `functions/main.py` envía push a partir de eventos en RTDB (triggers).

**Contrato recomendado:**
- No enviar push si el receptor está en `ActiveView` del chat correspondiente.
- Persistir payload mínimo y estable (`type` / `chatId` / `groupName` / `messageId`).

---

## 🧪 Reglas + Emulator + Tests

| Recurso | Ubicación |
|---|---|
| Reglas RTDB | `database.rules.json` — filosofía: **deny-by-default**, permitir por nodo/condición. |
| Emulator config | `firebase.json` |
| Tests de reglas | `tools/firebase-rules-tests/` |

```bash
npm ci
npm run test:rules
```

> ✅ Cada cambio de reglas debe venir acompañado de tests (casos permitidos y denegados).

---

## 🛡️ Nota portfolio (repo público)

- Cada persona debe crear su propio proyecto Firebase para ejecución completa.
- El repo no incluye credenciales reales (`google-services.json` / `local.properties`).
- Para CI/build público se usan plantillas `.example` para compilar sin conectar a un backend real.
