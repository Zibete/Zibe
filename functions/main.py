# functions/main.py

from __future__ import annotations

from firebase_functions import db_fn
from firebase_admin import initialize_app, messaging, db

# ============================================================
# CONFIG / CONSTANTES (EDITÁ TODO ACÁ)
# ============================================================

# --- Sesiones (tokens) ---
NODE_SESSIONS = "sessions"
KEY_FCM_TOKEN = "fcmToken"

# --- Nodos de chat bajo /Chats/<chatType>/... ---
# (TU DB LEGACY ACTUAL)
CHAT_TYPE_1_1 = "ChatWith"     # chat 1-1
CHAT_TYPE_UNKNOWN = "Unknown"  # legacy/raro: lo usabas como "chat dentro de grupo" o similar

# --- Triggers RTDB ---
# 1) Mensaje creado en cualquier chat bajo /Chats/<chatType>/<chatId>/<messageId>
PATH_CHAT_ANY = "/Chats/{chatType}/{chatId}/{messageId}"

# 2) Mensaje creado en chat de grupo bajo /Groups/Chat/<groupName>/<messageId>
# (esto lo mantenemos igual que veníamos usando para grupos)
PATH_GROUP_CHAT = "/Groups/Chat/{groupName}/{messageId}"

# --- Payload contract con tu app (FCM data payload) ---
# Regla que venís usando:
# - Si payload.type == "ChatWith" => es 1-1
# - Si no, payload.type = nombre del grupo (para grupo)
PAYLOAD_TYPE_1_1 = CHAT_TYPE_1_1

# --- Campos esperados en tus mensajes (ajustá si tu JSON difiere) ---
MSG_KEY_TEXT = "msg"
MSG_KEY_SENDER_UID = "envia"     # sender uid
MSG_KEY_RECEIVER_UID = "recibe"  # si no lo tenés en DB, lo resolvemos desde chatId
MSG_KEY_SENDER_NAME = "user"     # o "userName" según tu modelo viejo

# ============================================================
# INIT ADMIN SDK
# ============================================================

initialize_app()

# ============================================================
# HELPERS
# ============================================================

def _get_user_token(uid: str) -> str | None:
    """Lee /sessions/<uid>/fcmToken. Devuelve None si no existe."""
    value = db.reference(f"{NODE_SESSIONS}/{uid}/{KEY_FCM_TOKEN}").get()
    return value if isinstance(value, str) and value.strip() else None


def _send_push(token: str, title: str, data_payload: dict) -> None:
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
            body=safe_data.get("msg", "")
        ),
        data=safe_data
    )
    messaging.send(msg)


def _parse_receiver_from_chat_id(chat_id: str, sender_uid: str) -> str | None:
    """
    Fallback: si no existe 'recibe' en el mensaje, intentamos inferirlo desde chatId.
    Tu app suele armar chatId como "uidA_uidB" (ordenado o no).
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
# TRIGGER 1: /Chats/{chatType}/{chatId}/{messageId}
# ============================================================

@db_fn.on_value_created(reference=PATH_CHAT_ANY)
def on_chat_message_created(event: db_fn.Event[db_fn.DataSnapshot]) -> None:
    """
    Se dispara para cualquier mensaje nuevo debajo de /Chats/<chatType>/...
    - Si chatType == ChatWith => tratamos como 1-1 y mandamos push al receptor
    - Si chatType == Unknown  => hoy lo ignoramos (o lo podés mapear a grupo si aplica)
    """
    chat_type = (event.params.get("chatType") or "").strip()
    chat_id = (event.params.get("chatId") or "").strip()

    data = event.data.val()
    if not isinstance(data, dict):
        return

    # Solo notificamos 1-1 por este trigger
    if chat_type != CHAT_TYPE_1_1:
        # Si en tu DB "Unknown" en realidad también requiere notificar, lo definimos después.
        return

    sender_uid = _read_str(data, MSG_KEY_SENDER_UID)
    msg_text = _read_str(data, MSG_KEY_TEXT)
    if not sender_uid or not msg_text:
        return

    # Receptor: primero 'recibe', si no existe inferimos desde chatId
    receiver_uid = _read_str(data, MSG_KEY_RECEIVER_UID)
    if not receiver_uid:
        receiver_uid = _parse_receiver_from_chat_id(chat_id, sender_uid) or ""

    if not receiver_uid:
        return

    token = _get_user_token(receiver_uid)
    if not token:
        return

    sender_name = _read_str(data, MSG_KEY_SENDER_NAME) or "Alguien"

    # Contract con tu app: type == "ChatWith" => 1-1
    payload = {
        "type": PAYLOAD_TYPE_1_1,
        "id_user": sender_uid,
        "user": sender_name,
        "msg": msg_text,
        # si tenés contador real lo podés agregar; si no, tu app ya recalcula por Firebase
        "novistos": data.get("novistos", "")
    }

    _send_push(
        token=token,
        title=f"Nuevo mensaje de {sender_name}",
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

    sender_uid = _read_str(data, MSG_KEY_SENDER_UID)
    msg_text = _read_str(data, MSG_KEY_TEXT)
    if not msg_text:
        return

    sender_name = _read_str(data, MSG_KEY_SENDER_NAME) or group_name

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
            data_payload=payload
        )
