from __future__ import annotations

from typing import Any

if __package__:
    from .rooms_v2_state import (
        RoomsV2StateError,
        send_private_text_state,
        send_public_text_state,
    )
else:
    from rooms_v2_state import (
        RoomsV2StateError,
        send_private_text_state,
        send_public_text_state,
    )


def _peek(root: object, *parts: str) -> Any:
    cursor: Any = root
    for part in parts:
        if not isinstance(cursor, dict):
            return None
        cursor = cursor.get(part)
    return cursor


def _reply_target(root: object, *parts: str) -> dict:
    message = _peek(root, *parts)
    if not isinstance(message, dict):
        raise RoomsV2StateError("NOT_FOUND", "Reply target not found.")
    if message.get("removed") is True:
        raise RoomsV2StateError("CONFLICT", "Cannot reply to a removed message.")
    if str(message.get("kind") or "").lower() == "event":
        raise RoomsV2StateError("CONFLICT", "Cannot reply to a room event.")
    return message


def _existing_claimed_message(
    root: object,
    *,
    uid: str,
    scope: str,
    client_message_id: str,
    message_path: tuple[str, ...],
) -> dict | None:
    claim = _peek(root, "private", "messageClaims", uid, scope, client_message_id)
    if not isinstance(claim, dict):
        return None
    message_id = str(claim.get("messageId") or "")
    if not message_id:
        return None
    message = _peek(root, *message_path, message_id)
    return message if isinstance(message, dict) else None


def send_public_text_with_reply_state(
    current: object,
    *,
    uid: str,
    room_id: str,
    text: str,
    client_message_id: str,
    candidate_message_id: str,
    reply_to_message_id: str | None,
    now: int,
    visible_lease_ms: int,
) -> dict:
    scope = f"room_{room_id}"
    existing = _existing_claimed_message(
        current,
        uid=uid,
        scope=scope,
        client_message_id=client_message_id,
        message_path=("publicMessages", room_id),
    )
    if existing is None and reply_to_message_id:
        _reply_target(current, "publicMessages", room_id, reply_to_message_id)

    root = send_public_text_state(
        current,
        uid=uid,
        room_id=room_id,
        text=text,
        client_message_id=client_message_id,
        candidate_message_id=candidate_message_id,
        now=now,
        visible_lease_ms=visible_lease_ms,
    )
    if existing is not None or not reply_to_message_id:
        return root

    claim = _peek(root, "private", "messageClaims", uid, scope, client_message_id)
    message_id = str(claim.get("messageId") or "") if isinstance(claim, dict) else ""
    message = _peek(root, "publicMessages", room_id, message_id)
    if not isinstance(message, dict):
        raise RoomsV2StateError("INTERNAL", "Reply message missing after send.")
    message["replyToMessageId"] = reply_to_message_id
    return root


def send_private_text_with_reply_state(
    current: object,
    *,
    uid: str,
    room_id: str,
    conversation_id: str,
    text: str,
    client_message_id: str,
    candidate_message_id: str,
    reply_to_message_id: str | None,
    now: int,
    visible_lease_ms: int,
) -> dict:
    scope = f"private_{conversation_id}"
    existing = _existing_claimed_message(
        current,
        uid=uid,
        scope=scope,
        client_message_id=client_message_id,
        message_path=("privateMessages", conversation_id),
    )
    if existing is None and reply_to_message_id:
        _reply_target(current, "privateMessages", conversation_id, reply_to_message_id)

    root = send_private_text_state(
        current,
        uid=uid,
        room_id=room_id,
        conversation_id=conversation_id,
        text=text,
        client_message_id=client_message_id,
        candidate_message_id=candidate_message_id,
        now=now,
        visible_lease_ms=visible_lease_ms,
    )
    if existing is not None or not reply_to_message_id:
        return root

    claim = _peek(root, "private", "messageClaims", uid, scope, client_message_id)
    message_id = str(claim.get("messageId") or "") if isinstance(claim, dict) else ""
    message = _peek(root, "privateMessages", conversation_id, message_id)
    if not isinstance(message, dict):
        raise RoomsV2StateError("INTERNAL", "Private reply message missing after send.")
    message["replyToMessageId"] = reply_to_message_id
    return root
