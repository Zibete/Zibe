from __future__ import annotations

import time
from typing import Any

from firebase_admin import db
from firebase_functions import db_fn

if __package__:
    from .legacy_main import _get_user_token, _safe_id, _send_push
    from .rooms_v2_notification_core import (
        private_room_recipient,
        public_room_recipients,
        safe_text_preview,
    )
else:
    from legacy_main import _get_user_token, _safe_id, _send_push
    from rooms_v2_notification_core import (
        private_room_recipient,
        public_room_recipients,
        safe_text_preview,
    )

ROOT = "RoomsV2"
PUBLIC_MESSAGE_PATH = "/RoomsV2/publicMessages/{roomId}/{messageId}"
PRIVATE_MESSAGE_PATH = "/RoomsV2/privateMessages/{conversationId}/{messageId}"
VISIBLE_THREAD_LEASE_MS = 120_000
PREVIEW_MAX_CHARS = 160


def _event_data(event: db_fn.Event) -> dict | None:
    value: Any = event.data
    getter = getattr(value, "val", None)
    if callable(getter):
        value = getter()
    return value if isinstance(value, dict) else None


def _read_str(value: object) -> str:
    return str(value or "").strip()


def _read_rooms_state() -> dict:
    value = db.reference(ROOT).get()
    return value if isinstance(value, dict) else {}


def _message_preview(message: dict) -> str:
    kind = _read_str(message.get("kind")).lower() or "text"
    if kind == "text":
        return safe_text_preview(
            message.get("text"),
            fallback="Nuevo mensaje en la sala",
            limit=PREVIEW_MAX_CHARS,
        )
    labels = {
        "image": "Imagen compartida",
        "audio": "Audio compartido",
        "video": "Video compartido",
        "file": "Archivo compartido",
        "event": "Actualización en la sala",
    }
    return labels.get(kind, "Nuevo mensaje en la sala")


def _base_payload(message: dict, *, message_id: str) -> dict[str, str]:
    return {
        "messageId": message_id,
        "senderIdentityId": _read_str(message.get("authorIdentityId")),
        "senderName": _read_str(message.get("authorDisplayName")) or "ZIBE",
        "messageType": _read_str(message.get("kind")) or "text",
        "preview": _message_preview(message),
    }


@db_fn.on_value_created(reference=PUBLIC_MESSAGE_PATH)
def on_room_v2_public_message_created(
    event: db_fn.Event[db_fn.DataSnapshot],
) -> None:
    room_id = _read_str(event.params.get("roomId"))
    message_id = _read_str(event.params.get("messageId"))
    message = _event_data(event)
    if not room_id or not message_id or not isinstance(message, dict):
        return
    sender_identity_id = _read_str(message.get("authorIdentityId"))
    if not sender_identity_id:
        return

    state = _read_rooms_state()
    now_ms = int(time.time() * 1000)
    recipients = public_room_recipients(
        state,
        room_id=room_id,
        sender_identity_id=sender_identity_id,
        now_ms=now_ms,
        lease_ms=VISIBLE_THREAD_LEASE_MS,
    )
    payload = {
        "type": "room_v2",
        "roomId": room_id,
        **_base_payload(message, message_id=message_id),
    }

    for uid in recipients:
        token = _get_user_token(uid)
        if not token:
            continue
        try:
            _send_push(
                token=token,
                data_payload=payload,
                include_notification=False,
            )
        except Exception:
            # Keep a single invalid/unavailable recipient from blocking the others.
            # Identifier logging remains redacted through the legacy helper.
            import logging

            logging.getLogger(__name__).exception(
                "RoomsV2 public push failed roomId=%s messageId=%s receiver=%s",
                _safe_id(room_id),
                _safe_id(message_id),
                _safe_id(uid),
            )


@db_fn.on_value_created(reference=PRIVATE_MESSAGE_PATH)
def on_room_v2_private_message_created(
    event: db_fn.Event[db_fn.DataSnapshot],
) -> None:
    conversation_id = _read_str(event.params.get("conversationId"))
    message_id = _read_str(event.params.get("messageId"))
    message = _event_data(event)
    if not conversation_id or not message_id or not isinstance(message, dict):
        return
    room_id = _read_str(message.get("roomId"))
    sender_identity_id = _read_str(message.get("authorIdentityId"))
    if not room_id or not sender_identity_id:
        return

    state = _read_rooms_state()
    recipient_uid = private_room_recipient(
        state,
        room_id=room_id,
        conversation_id=conversation_id,
        sender_identity_id=sender_identity_id,
        now_ms=int(time.time() * 1000),
        lease_ms=VISIBLE_THREAD_LEASE_MS,
    )
    if not recipient_uid:
        return
    token = _get_user_token(recipient_uid)
    if not token:
        return

    payload = {
        "type": "room_private_v2",
        "roomId": room_id,
        "conversationId": conversation_id,
        **_base_payload(message, message_id=message_id),
    }
    try:
        _send_push(
            token=token,
            data_payload=payload,
            include_notification=False,
        )
    except Exception:
        import logging

        logging.getLogger(__name__).exception(
            "RoomsV2 private push failed roomId=%s conversation=%s messageId=%s receiver=%s",
            _safe_id(room_id),
            _safe_id(conversation_id),
            _safe_id(message_id),
            _safe_id(recipient_uid),
        )
