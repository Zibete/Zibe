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
│        │  │     └─ otherUid
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
| `Users/Data/{uid}/ClientData/ActiveView` | Vista activa (qué chat/pantalla está mirando). | Debe limpiarse al salir — evitar estado stale. |

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

---

### 📨 Mensajes

| Path | Propósito | Invariante |
|---|---|---|
| `Chats/dm/{chatId}/{messageId}` | Mensajes de conversaciones directas. | Append-only — para "borrar", preferir flags o limpieza controlada. |
| `Chats/group_dm/{chatId}/{messageId}` | Mensajes con estructura de grupo (según implementación actual). | Considerar consolidación con `Groups/Chat/...` a futuro sin romper compatibilidad. |
| `Groups/Chat/{groupName}/{messageId}` | Mensajes de un grupo identificado por `groupName`. | `groupName` debe ser estable — evitar renames que rompan historial. |

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