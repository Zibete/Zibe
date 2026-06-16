# functions/main.py

from __future__ import annotations

import logging

from firebase_functions import db_fn
from firebase_admin import initialize_app, messaging, db

# ============================================================
# CONFIG / CONSTANTS
# ============================================================

# --- Sessions ---
NODE_SESSIONS = "Sessions"
KEY_FCM_TOKEN = "fcmToken"

# --- Users / Chats / Groups ---
NODE_USERS_ACCOUNTS = "Users/Accounts"
NODE_DM = "dm"
PATH_DM_CHAT = "/Chats/dm/{chatId}/{messageId}"
PATH_GROUP_CHAT = "/Groups/Chat/{groupName}/{messageId}"

# --- Active thread contract ---
ACTIVE_THREAD_NODE_TYPE = "nodeType"
ACTIVE_THREAD_OTHER_UID = "otherUid"

# --- DM payload contract ---
PAYLOAD_KEY_TYPE = "type"
PAYLOAD_KEY_CHAT_ID = "chatId"
PAYLOAD_KEY_MESSAGE_ID = "messageId"
PAYLOAD_KEY_SENDER_UID = "senderUid"
PAYLOAD_KEY_SENDER_NAME = "senderName"
PAYLOAD_KEY_CONTENT = "content"

# --- Current RTDB message keys used by app models ---
MSG_KEY_SENDER_UID = "senderUid"
MSG_KEY_CONTENT = "content"
MSG_KEY_TYPE = "type"
GROUP_MSG_KEY_SENDER_NAME = "nameUser"
GROUP_MSG_KEY_CONTENT = "content"
DM_MSG_TYPE_TEXT = 100
DM_VISIBLE_CONTENT_FALLBACK = "Abri ZIBE para ver el mensaje"
DM_SENDER_NAME_FALLBACK = "ZIBE"

# --- Legacy group contract, do not reuse for DM ---
LEGACY_GROUP_PAYLOAD_KEY_OTHER_ID = "id_user"
LEGACY_GROUP_PAYLOAD_KEY_OTHER_NAME = "user"
LEGACY_GROUP_PAYLOAD_KEY_CONTENT = "msg"
LEGACY_GROUP_PAYLOAD_KEY_UNREAD = "novistos"

# ============================================================
# INIT ADMIN SDK
# ============================================================

initialize_app()
logger = logging.getLogger(__name__)

# ============================================================
# HELPERS
# ============================================================

def _read_str(data: dict | None, key: str) -> str:
    if not isinstance(data, dict):
        return ""
    value = data.get(key)
    return str(value).strip() if value is not None else ""


def _get_user_token(uid: str) -> str | None:
    """Reads /Sessions/<uid>/fcmToken."""
    if not uid:
        return None

    value = db.reference(f"{NODE_SESSIONS}/{uid}/{KEY_FCM_TOKEN}").get()
    token = str(value).strip() if value is not None else ""
    return token or None


def _get_sender_name(sender_uid: str) -> str:
    if not sender_uid:
        return DM_SENDER_NAME_FALLBACK

    value = db.reference(f"{NODE_USERS_ACCOUNTS}/{sender_uid}/name").get()
    sender_name = str(value).strip() if value is not None else ""
    return sender_name or DM_SENDER_NAME_FALLBACK


def _get_visible_dm_content(data: dict) -> str:
    content = _read_str(data, MSG_KEY_CONTENT)
    message_type = data.get(MSG_KEY_TYPE)

    try:
        is_text = int(message_type) == DM_MSG_TYPE_TEXT
    except (TypeError, ValueError):
        is_text = False

    if is_text and content:
        return content

    return DM_VISIBLE_CONTENT_FALLBACK


def _parse_other_uid_from_chat_id(chat_id: str, sender_uid: str) -> str | None:
    """
    chatId contract in app:
      ChatIdGenerator.getChatId(uidA, uidB) -> "<sortedUidA>_<sortedUidB>"
    """
    if not chat_id or not sender_uid or "_" not in chat_id:
        return None

    parts = [part.strip() for part in chat_id.split("_") if part and part.strip()]
    if len(parts) != 2:
        return None

    uid_a, uid_b = parts
    if sender_uid == uid_a:
        return uid_b
    if sender_uid == uid_b:
        return uid_a
    return None


def _get_active_thread(uid: str) -> dict | None:
    """
    Reads:
      /Users/Data/{uid}/ClientData/ActiveView/activeThread
    """
    if not uid:
        return None

    value = db.reference(
        f"Users/Data/{uid}/ClientData/ActiveView/activeThread"
    ).get()

    return value if isinstance(value, dict) else None


def _is_receiver_in_active_dm(receiver_uid: str, other_uid: str) -> bool:
    """
    Skip push if receiver is already viewing this same DM:
      activeThread.nodeType == "dm"
      activeThread.otherUid == <other participant uid>
    """
    active_thread = _get_active_thread(receiver_uid)
    if not isinstance(active_thread, dict):
        return False

    node_type = _read_str(active_thread, ACTIVE_THREAD_NODE_TYPE)
    active_other_uid = _read_str(active_thread, ACTIVE_THREAD_OTHER_UID)

    return node_type == NODE_DM and active_other_uid == other_uid


def _send_push(
    *,
    token: str,
    data_payload: dict,
    include_notification: bool,
    title: str | None = None,
    body: str | None = None,
) -> str | None:
    """
    Sends FCM.
    - DM uses data + notification for reliable background display
    - Group keeps current legacy behavior
    """
    if not token:
        return None

    safe_data = {str(k): str(v) for k, v in data_payload.items() if v is not None}

    message_kwargs = {
        "token": token,
        "data": safe_data,
        "android": messaging.AndroidConfig(priority="high"),
    }

    if include_notification:
        message_kwargs["notification"] = messaging.Notification(
            title=title or "",
            body=body or "",
        )

    fcm_message = messaging.Message(**message_kwargs)
    return messaging.send(fcm_message)

# ============================================================
# TRIGGER 1: /Chats/dm/{chatId}/{messageId}
# ============================================================

@db_fn.on_value_created(reference=PATH_DM_CHAT)
def on_dm_message_created(event: db_fn.Event[db_fn.DataSnapshot]) -> None:
    """
    Current DM contract:
    - trigger path: /Chats/dm/{chatId}/{messageId}
    - receiver token: /Sessions/{uid}/fcmToken
    - skip push if receiver is already in the same activeThread
    - payload data: { type, chatId, messageId, senderUid, senderName, content }
    - FCM mode: data + notification
    """
    chat_id = (event.params.get("chatId") or "").strip()
    message_id = (event.params.get("messageId") or "").strip()
    logger.info("DM trigger started chatId=%s messageId=%s", chat_id, message_id)

    if not chat_id or not message_id:
        logger.warning("DM trigger missing params chatId=%s messageId=%s", chat_id, message_id)
        return

    data = event.data.val()
    if not isinstance(data, dict):
        logger.warning("DM trigger invalid data chatId=%s messageId=%s", chat_id, message_id)
        return

    sender_uid = _read_str(data, MSG_KEY_SENDER_UID)
    if not sender_uid:
        logger.warning("DM trigger missing senderUid chatId=%s messageId=%s", chat_id, message_id)
        return
    logger.info(
        "DM trigger sender resolved chatId=%s messageId=%s senderUid=%s",
        chat_id,
        message_id,
        sender_uid,
    )

    receiver_uid = _parse_other_uid_from_chat_id(chat_id, sender_uid)
    if not receiver_uid:
        logger.warning(
            "DM trigger receiver not resolved chatId=%s messageId=%s senderUid=%s",
            chat_id,
            message_id,
            sender_uid,
        )
        return

    if receiver_uid == sender_uid:
        logger.warning(
            "DM trigger skipped receiver equals sender chatId=%s messageId=%s senderUid=%s",
            chat_id,
            message_id,
            sender_uid,
        )
        return

    logger.info(
        "DM trigger receiver resolved chatId=%s messageId=%s receiverUid=%s",
        chat_id,
        message_id,
        receiver_uid,
    )

    if _is_receiver_in_active_dm(receiver_uid, sender_uid):
        logger.info(
            "DM trigger skipped active DM chatId=%s messageId=%s receiverUid=%s",
            chat_id,
            message_id,
            receiver_uid,
        )
        return

    token = _get_user_token(receiver_uid)
    if not token:
        logger.warning(
            "DM trigger missing receiver token chatId=%s messageId=%s receiverUid=%s hasToken=False",
            chat_id,
            message_id,
            receiver_uid,
        )
        return

    payload = {
        PAYLOAD_KEY_TYPE: NODE_DM,
        PAYLOAD_KEY_CHAT_ID: chat_id,
        PAYLOAD_KEY_MESSAGE_ID: message_id,
        PAYLOAD_KEY_SENDER_UID: sender_uid,
        PAYLOAD_KEY_SENDER_NAME: _get_sender_name(sender_uid),
        PAYLOAD_KEY_CONTENT: _get_visible_dm_content(data),
    }

    logger.info(
        "DM trigger sending FCM chatId=%s messageId=%s receiverUid=%s hasToken=True",
        chat_id,
        message_id,
        receiver_uid,
    )

    try:
        fcm_message_id = _send_push(
            token=token,
            data_payload=payload,
            include_notification=True,
            title=f"Nuevo mensaje de {payload[PAYLOAD_KEY_SENDER_NAME]}",
            body=payload[PAYLOAD_KEY_CONTENT],
        )
        logger.info(
            "DM trigger FCM sent chatId=%s messageId=%s fcmMessageId=%s",
            chat_id,
            message_id,
            fcm_message_id,
        )
    except Exception:
        logger.exception(
            "DM trigger FCM send failed chatId=%s messageId=%s receiverUid=%s",
            chat_id,
            message_id,
            receiver_uid,
        )
        raise

# ============================================================
# TRIGGER 2: /Groups/Chat/{groupName}/{messageId}
# ============================================================
# Left intentionally close to current legacy behavior.
# Out of scope for this iteration.

@db_fn.on_value_created(reference=PATH_GROUP_CHAT)
def on_group_message_created(event: db_fn.Event[db_fn.DataSnapshot]) -> None:
    group_name = (event.params.get("groupName") or "").strip()
    if not group_name:
        return

    data = event.data.val()
    if not isinstance(data, dict):
        return

    sender_uid = _read_str(data, MSG_KEY_SENDER_UID)
    sender_name = _read_str(data, GROUP_MSG_KEY_SENDER_NAME) or group_name
    group_message_text = _read_str(data, GROUP_MSG_KEY_CONTENT)

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
            PAYLOAD_KEY_TYPE: group_name,
            LEGACY_GROUP_PAYLOAD_KEY_OTHER_ID: sender_uid,
            LEGACY_GROUP_PAYLOAD_KEY_OTHER_NAME: sender_name,
            LEGACY_GROUP_PAYLOAD_KEY_CONTENT: group_message_text,
            LEGACY_GROUP_PAYLOAD_KEY_UNREAD: data.get(LEGACY_GROUP_PAYLOAD_KEY_UNREAD, ""),
        }

        _send_push(
            token=token,
            data_payload=payload,
            include_notification=True,
            title=f"Nuevo mensaje de {group_name}",
            body=group_message_text or sender_name,
        )
