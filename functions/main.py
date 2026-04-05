# functions/main.py

from __future__ import annotations

from firebase_functions import db_fn
from firebase_admin import initialize_app, messaging, db

# ============================================================
# CONFIG / CONSTANTES (EDITÁ TODO ACÁ)
# ============================================================

# --- Sesiones (tokens) ---
NODE_SESSIONS = "Sessions"
KEY_FCM_TOKEN = "fcmToken"

# --- Active view ---
NODE_USERS = "Users"
NODE_USERS_DATA = "Data"
NODE_CLIENT_DATA = "ClientData"
NODE_ACTIVE_VIEW = "ActiveView"
KEY_ACTIVE_THREAD = "activeThread"
KEY_NODE_TYPE = "nodeType"
KEY_OTHER_UID = "otherUid"

# --- Triggers RTDB ---
# 1) Mensaje creado en DM 1:1 bajo /Chats/dm/<chatId>/<messageId>
NODE_DM = "dm"
PATH_DM_CHAT = "/Chats/dm/{chatId}/{messageId}"

# 2) Mensaje creado en chat de grupo bajo /Groups/Chat/<groupName>/<messageId>
# (esto se mantiene igual en esta tanda; grupos quedan fuera de alcance)
PATH_GROUP_CHAT = "/Groups/Chat/{groupName}/{messageId}"

# --- Payload contract vigente para DM ---
PAYLOAD_KEY_TYPE = "type"
PAYLOAD_KEY_CHAT_ID = "chatId"
PAYLOAD_KEY_MESSAGE_ID = "messageId"

# --- Campos esperados en mensajes DM ---
DM_MSG_KEY_CONTENT = "content"
DM_MSG_KEY_SENDER_UID = "senderUid"

# --- Campos legacy del flujo de grupo actual (fuera de esta tanda) ---
GROUP_MSG_KEY_TEXT = "msg"
GROUP_MSG_KEY_SENDER_UID = "envia"
GROUP_MSG_KEY_SENDER_NAME = "user"

# ============================================================
# INIT ADMIN SDK
# ============================================================

initialize_app()

# ============================================================
# HELPERS
# ============================================================

def _get_user_token(uid: str) -> str | None:
    """Lee /Sessions/<uid>/fcmToken. Devuelve None si no existe."""
    value = db.reference(f"{NODE_SESSIONS}/{uid}/{KEY_FCM_TOKEN}").get()
    return value if isinstance(value, str) and value.strip() else None


def _is_user_in_active_dm(uid: str, other_uid: str) -> bool:
    active_thread = db.reference(
        f"{NODE_USERS}/{NODE_USERS_DATA}/{uid}/{NODE_CLIENT_DATA}/{NODE_ACTIVE_VIEW}/{KEY_ACTIVE_THREAD}"
    ).get()

    if not isinstance(active_thread, dict):
        return False

    return (
        _read_str(active_thread, KEY_NODE_TYPE) == NODE_DM
        and _read_str(active_thread, KEY_OTHER_UID) == other_uid
    )


def _send_push(token: str, title: str, body: str, data_payload: dict) -> None:
    """
    Envía notificación:
    - notification.title/body para UI del sistema
    - data payload para tu lógica en app (siempre string-string)
    """
    safe_data = {k: str(v) for k, v in data_payload.items() if v is not None}

    msg = messaging.Message(
        token=token,
        notification=messaging.Notification(
            title=title,
            body=body
        ),
        data=safe_data
    )
    messaging.send(msg)


def _parse_receiver_from_chat_id(chat_id: str, sender_uid: str) -> str | None:
    """
    chatId en el repo se construye como "uidA_uidB" con ambos uid ordenados.
    A partir de eso inferimos el otro participante del DM.
    """
    if not chat_id or "_" not in chat_id:
        return None
    parts = [p for p in chat_id.split("_") if p]
    if len(parts) != 2:
        return None
    a, b = parts[0], parts[1]
    if sender_uid == a:
        return b
    if sender_uid == b:
        return a
    return None


def _read_str(data: dict, key: str) -> str:
    v = data.get(key, "")
    return str(v).strip() if v is not None else ""


# ============================================================
# TRIGGER 1: /Chats/dm/{chatId}/{messageId}
# ============================================================

@db_fn.on_value_created(reference=PATH_DM_CHAT)
def on_dm_message_created(event: db_fn.Event[db_fn.DataSnapshot]) -> None:
    """
    Se dispara para cada mensaje nuevo en /Chats/dm/<chatId>/<messageId>.
    El payload de datos queda alineado al contrato vigente del repo:
    type / chatId / messageId.
    """
    chat_id = (event.params.get("chatId") or "").strip()
    message_id = (event.params.get("messageId") or "").strip()
    if not chat_id or not message_id:
        return

    data = event.data.val()
    if not isinstance(data, dict):
        return

    sender_uid = _read_str(data, DM_MSG_KEY_SENDER_UID)
    if not sender_uid:
        return

    receiver_uid = _parse_receiver_from_chat_id(chat_id, sender_uid) or ""
    if not receiver_uid:
        return

    if _is_user_in_active_dm(receiver_uid, sender_uid):
        return

    token = _get_user_token(receiver_uid)
    if not token:
        return

    payload = {
        PAYLOAD_KEY_TYPE: NODE_DM,
        PAYLOAD_KEY_CHAT_ID: chat_id,
        PAYLOAD_KEY_MESSAGE_ID: message_id,
    }

    _send_push(
        token=token,
        title="Nuevo mensaje",
        body=_read_str(data, DM_MSG_KEY_CONTENT) or "Abrí ZIBE para ver el mensaje",
        data_payload=payload
    )


# ============================================================
# TRIGGER 2: /Groups/Chat/{groupName}/{messageId}
# ============================================================

@db_fn.on_value_created(reference=PATH_GROUP_CHAT)
def on_group_message_created(event: db_fn.Event[db_fn.DataSnapshot]) -> None:
    """
    Mensaje nuevo en un grupo.
    Regla de tu app:
      payload.type != "ChatWith" => se interpreta como groupName.
    Este trigger debe:
      - leer miembros del grupo
      - mandar push a todos menos al sender
    """
    group_name = (event.params.get("groupName") or "").strip()
    if not group_name:
        return

    data = event.data.val()
    if not isinstance(data, dict):
        return

    sender_uid = _read_str(data, GROUP_MSG_KEY_SENDER_UID)
    msg_text = _read_str(data, GROUP_MSG_KEY_TEXT)
    if not msg_text:
        return

    sender_name = _read_str(data, GROUP_MSG_KEY_SENDER_NAME) or group_name

    # === IMPORTANTE: ajustá este path si tu DB difiere ===
    # Asumimos: /Groups/Users/<groupName>/<uid> = userGroup
    members = db.reference(f"Groups/Users/{group_name}").get()

    if not isinstance(members, dict):
        return

    for uid in members.keys():
        uid = str(uid).strip()
        if not uid:
            continue
        if sender_uid and uid == sender_uid:
            continue

        token = _get_user_token(uid)
        if not token:
            continue

        payload = {
            "type": group_name,     # <= clave para tu app: "si no es ChatWith => es group"
            "id_user": sender_uid,  # sender uid
            "user": sender_name,
            "msg": msg_text,
            "novistos": data.get("novistos", "")
        }

        _send_push(
            token=token,
            title=f"Nuevo mensaje de {group_name}",
            body=msg_text,
            data_payload=payload
        )
