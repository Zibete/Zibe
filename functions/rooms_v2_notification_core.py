from __future__ import annotations

from typing import Any


def _peek(root: dict, *parts: str) -> Any:
    cursor: Any = root
    for part in parts:
        if not isinstance(cursor, dict):
            return None
        cursor = cursor.get(part)
    return cursor


def _fresh_visible_thread(
    state: dict,
    uid: str,
    *,
    room_id: str,
    conversation_id: str | None,
    now_ms: int,
    lease_ms: int,
) -> bool:
    thread = _peek(state, "private", "visibleThreads", uid)
    if not isinstance(thread, dict):
        return False
    if str(thread.get("roomId") or "") != room_id:
        return False
    active_conversation = str(thread.get("conversationId") or "") or None
    if active_conversation != conversation_id:
        return False
    try:
        updated_at = int(thread.get("updatedAt") or 0)
    except (TypeError, ValueError):
        return False
    return 0 <= now_ms - updated_at <= lease_ms


def public_room_recipients(
    state: dict,
    *,
    room_id: str,
    sender_identity_id: str,
    now_ms: int,
    lease_ms: int,
) -> list[str]:
    room = _peek(state, "publicRooms", room_id)
    if not isinstance(room, dict) or str(room.get("status") or "").lower() != "open":
        return []

    sender_owner = _peek(
        state,
        "private",
        "identityOwners",
        room_id,
        sender_identity_id,
    )
    sender_uid = (
        str(sender_owner.get("uid") or "").strip()
        if isinstance(sender_owner, dict)
        else ""
    )
    if not sender_uid:
        return []

    recipients: list[str] = []
    membership_index = _peek(state, "membershipIndexByUser")
    if not isinstance(membership_index, dict):
        return recipients

    for candidate_uid, rooms in membership_index.items():
        uid = str(candidate_uid or "").strip()
        if not uid or uid == sender_uid or not isinstance(rooms, dict):
            continue
        membership = rooms.get(room_id)
        if not isinstance(membership, dict) or membership.get("active") is not True:
            continue
        if membership.get("notificationsEnabled") is False:
            continue
        if _peek(state, "private", "bans", room_id, uid) is not None:
            continue
        if _fresh_visible_thread(
            state,
            uid,
            room_id=room_id,
            conversation_id=None,
            now_ms=now_ms,
            lease_ms=lease_ms,
        ):
            continue
        recipients.append(uid)
    return sorted(set(recipients))


def private_room_recipient(
    state: dict,
    *,
    room_id: str,
    conversation_id: str,
    sender_identity_id: str,
    now_ms: int,
    lease_ms: int,
) -> str | None:
    room = _peek(state, "publicRooms", room_id)
    if not isinstance(room, dict) or str(room.get("status") or "").lower() != "open":
        return None

    conversation = _peek(state, "private", "conversations", conversation_id)
    if (
        not isinstance(conversation, dict)
        or str(conversation.get("roomId") or "") != room_id
        or conversation.get("closed") is True
    ):
        return None
    blocked_by = conversation.get("blockedBy")
    if isinstance(blocked_by, dict) and any(value is True for value in blocked_by.values()):
        return None

    participants = conversation.get("participants")
    if not isinstance(participants, dict) or len(participants) != 2:
        return None

    sender_uid = ""
    receiver_uid = ""
    receiver_identity_id = ""
    for uid_value, participant in participants.items():
        uid = str(uid_value or "").strip()
        if not uid or not isinstance(participant, dict):
            return None
        identity_id = str(participant.get("identityId") or "").strip()
        if identity_id == sender_identity_id:
            sender_uid = uid
        else:
            receiver_uid = uid
            receiver_identity_id = identity_id

    if not sender_uid or not receiver_uid or not receiver_identity_id:
        return None

    membership = _peek(state, "membershipIndexByUser", receiver_uid, room_id)
    if (
        not isinstance(membership, dict)
        or membership.get("active") is not True
        or str(membership.get("identityId") or "") != receiver_identity_id
        or membership.get("notificationsEnabled") is False
    ):
        return None
    if _peek(state, "private", "bans", room_id, receiver_uid) is not None:
        return None
    readers = _peek(state, "private", "conversationReaders", conversation_id)
    if not isinstance(readers, dict) or readers.get(receiver_uid) is not True:
        return None
    if _fresh_visible_thread(
        state,
        receiver_uid,
        room_id=room_id,
        conversation_id=conversation_id,
        now_ms=now_ms,
        lease_ms=lease_ms,
    ):
        return None
    return receiver_uid


def safe_text_preview(value: object, *, fallback: str, limit: int = 160) -> str:
    text = " ".join(str(value or "").strip().split())
    if not text:
        return fallback
    return text[: max(1, limit)]
