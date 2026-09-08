# functions/main.py

from __future__ import annotations

import logging
import time

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
ACTIVE_THREAD_UPDATED_AT = "updatedAt"
ACTIVE_THREAD_LEASE_MS = 120_000

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
MSG_KEY_CREATED_AT = "createdAt"
MSG_KEY_TYPE = "type"
MSG_KEY_SEEN = "seen"
MSG_SEEN = 3
CONVERSATION_KEY_LAST_MESSAGE_AT = "lastMessageAt"
CONVERSATION_KEY_USER_ID = "userId"
CONVERSATION_KEY_UNREAD_COUNT = "unreadCount"
CONVERSATION_KEY_SEEN = "seen"
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


def _event_data_as_dict(event: db_fn.Event) -> dict | None:
    data = event.data
    val = getattr(data, "val", None)
    if callable(val):
        data = val()
    return data if isinstance(data, dict) else None


def _safe_id(value: str | None, *, head: int = 6, tail: int = 4) -> str:
    """
    Keeps production diagnostics useful without writing full UIDs/chat IDs/message IDs.
    """
    if not value:
        return ""
    text = str(value)
    if len(text) <= head + tail + 3:
        return text
    return f"{text[:head]}...{text[-tail:]}"


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
    if not chat_id or not sender_uid:
        return None

    delimiter = "|" if "|" in chat_id else "_"

    prefix = f"{sender_uid}{delimiter}"
    suffix = f"{delimiter}{sender_uid}"
    if chat_id.startswith(prefix):
        return chat_id[len(prefix):] or None
    if chat_id.endswith(suffix):
        return chat_id[:-len(suffix)] or None
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


def _is_receiver_in_active_dm(
    receiver_uid: str,
    other_uid: str,
    now_ms: int | None = None,
) -> bool:
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
    updated_at = _read_int(active_thread.get(ACTIVE_THREAD_UPDATED_AT))
    current_time_ms = now_ms if now_ms is not None else int(time.time() * 1000)
    is_fresh = (
        updated_at is not None
        and 0 <= current_time_ms - updated_at <= ACTIVE_THREAD_LEASE_MS
    )

    return node_type == NODE_DM and active_other_uid == other_uid and is_fresh


def _read_int(value: object) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _set_dm_message_seen_if_below(
    chat_id: str,
    message_id: str,
    target_seen: int,
) -> None:
    if not chat_id or not message_id:
        return

    seen_ref = db.reference(f"Chats/dm/{chat_id}/{message_id}/{MSG_KEY_SEEN}")

    def update(current_seen: object) -> int | object:
        numeric_seen = _read_int(current_seen)
        if numeric_seen is not None and numeric_seen >= target_seen:
            return current_seen
        return target_seen

    seen_ref.transaction(update)


def _sync_dm_conversation_if_latest(
    *,
    owner_uid: str,
    other_uid: str,
    sender_uid: str,
    message_created_at: int,
    target_seen: int,
    clear_unread: bool,
) -> None:
    conversation_ref = db.reference(f"Users/Data/{owner_uid}/dm/{other_uid}")

    def update(current: object) -> object:
        if not isinstance(current, dict):
            return current

        current_sender_uid = _read_str(current, CONVERSATION_KEY_USER_ID)
        current_last_message_at = _read_int(
            current.get(CONVERSATION_KEY_LAST_MESSAGE_AT)
        )
        if (
            current_sender_uid != sender_uid
            or current_last_message_at != message_created_at
        ):
            return current

        updated = dict(current)
        current_seen = _read_int(current.get(CONVERSATION_KEY_SEEN))
        if current_seen is None or current_seen < target_seen:
            updated[CONVERSATION_KEY_SEEN] = target_seen
        if clear_unread:
            updated[CONVERSATION_KEY_UNREAD_COUNT] = 0
        return updated

    conversation_ref.transaction(update)


def _mark_active_dm_seen(
    *,
    chat_id: str,
    message_id: str,
    sender_uid: str,
    receiver_uid: str,
    message_created_at: int,
) -> None:
    _set_dm_message_seen_if_below(chat_id, message_id, MSG_SEEN)
    _sync_dm_conversation_if_latest(
        owner_uid=sender_uid,
        other_uid=receiver_uid,
        sender_uid=sender_uid,
        message_created_at=message_created_at,
        target_seen=MSG_SEEN,
        clear_unread=False,
    )
    _sync_dm_conversation_if_latest(
        owner_uid=receiver_uid,
        other_uid=sender_uid,
        sender_uid=sender_uid,
        message_created_at=message_created_at,
        target_seen=MSG_SEEN,
        clear_unread=True,
    )


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
    - DM uses data-only so Android handles receipt and local notification
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
    - FCM mode: data-only so Android executes onMessageReceived
    """
    chat_id = (event.params.get("chatId") or "").strip()
    message_id = (event.params.get("messageId") or "").strip()
    if not chat_id or not message_id:
        logger.warning(
            "DM trigger missing params chatId=%s messageId=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
        )
        return

    data = _event_data_as_dict(event)
    if not isinstance(data, dict):
        logger.warning(
            "DM trigger invalid data chatId=%s messageId=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
        )
        return

    sender_uid = _read_str(data, MSG_KEY_SENDER_UID)
    if not sender_uid:
        logger.warning(
            "DM trigger missing senderUid chatId=%s messageId=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
        )
        return
    receiver_uid = _parse_other_uid_from_chat_id(chat_id, sender_uid)
    if not receiver_uid:
        logger.warning(
            "DM trigger receiver not resolved chatId=%s messageId=%s senderUid=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
            _safe_id(sender_uid),
        )
        return
    if receiver_uid == sender_uid:
        logger.warning(
            "DM trigger skipped receiver equals sender chatId=%s messageId=%s senderUid=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
            _safe_id(sender_uid),
        )
        return
    if _is_receiver_in_active_dm(receiver_uid, sender_uid):
        message_created_at = _read_int(data.get(MSG_KEY_CREATED_AT))
        if message_created_at is None:
            message_created_at = _read_int(
                db.reference(
                    f"Chats/dm/{chat_id}/{message_id}/{MSG_KEY_CREATED_AT}"
                ).get()
            )
        if message_created_at is None:
            _set_dm_message_seen_if_below(chat_id, message_id, MSG_SEEN)
            logger.warning(
                "DM trigger active summary sync skipped missing createdAt chatId=%s messageId=%s",
                _safe_id(chat_id),
                _safe_id(message_id),
            )
            return

        _mark_active_dm_seen(
            chat_id=chat_id,
            message_id=message_id,
            sender_uid=sender_uid,
            receiver_uid=receiver_uid,
            message_created_at=message_created_at,
        )
        logger.info(
            "DM trigger synchronized seen state and skipped active DM chatId=%s messageId=%s receiverUid=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
            _safe_id(receiver_uid),
        )
        return
    token = _get_user_token(receiver_uid)
    if not token:
        logger.warning(
            "DM trigger missing receiver token chatId=%s messageId=%s receiverUid=%s hasToken=False",
            _safe_id(chat_id),
            _safe_id(message_id),
            _safe_id(receiver_uid),
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

    try:
        _send_push(
            token=token,
            data_payload=payload,
            include_notification=False,
        )
    except Exception:
        logger.exception(
            "DM trigger FCM send failed chatId=%s messageId=%s receiverUid=%s",
            _safe_id(chat_id),
            _safe_id(message_id),
            _safe_id(receiver_uid),
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

    data = _event_data_as_dict(event)
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


# ============================================================
# ROOMSV2 EXPORTS
# ============================================================
# Existing unit tests intentionally stub only db_fn. In that synthetic environment
# https_fn is absent, so skip only the new callable/trigger registration there.
try:
    from firebase_functions import https_fn as _https_fn  # noqa: F401
except ImportError:
    pass
else:
    try:
        from .rooms_v2 import (
            create_room_v2,
            join_room_v2,
            leave_room_v2,
            mark_room_thread_read_v2,
            send_room_v2_text,
            set_room_notifications_v2,
            set_room_visible_thread_v2,
        )
        from .rooms_v2_moderation import (
            ban_room_member_v2,
            close_room_v2,
            edit_room_v2,
            kick_room_member_v2,
            remove_room_message_v2,
            report_room_message_v2,
            resolve_room_report_v2,
            set_room_moderator_v2,
            transfer_room_owner_v2,
        )
        from .rooms_v2_notifications import (
            on_room_v2_private_message_created,
            on_room_v2_public_message_created,
        )
        from .rooms_v2_private import (
            block_room_private_v2,
            open_room_private_v2,
            send_room_private_v2_text,
        )
        from .rooms_v2_profile import resolve_room_identity_profile_v2
    except ImportError:
        from rooms_v2 import (
            create_room_v2,
            join_room_v2,
            leave_room_v2,
            mark_room_thread_read_v2,
            send_room_v2_text,
            set_room_notifications_v2,
            set_room_visible_thread_v2,
        )
        from rooms_v2_moderation import (
            ban_room_member_v2,
            close_room_v2,
            edit_room_v2,
            kick_room_member_v2,
            remove_room_message_v2,
            report_room_message_v2,
            resolve_room_report_v2,
            set_room_moderator_v2,
            transfer_room_owner_v2,
        )
        from rooms_v2_notifications import (
            on_room_v2_private_message_created,
            on_room_v2_public_message_created,
        )
        from rooms_v2_private import (
            block_room_private_v2,
            open_room_private_v2,
            send_room_private_v2_text,
        )
        from rooms_v2_profile import resolve_room_identity_profile_v2
