from __future__ import annotations

import copy
from typing import Any

if __package__:
    from .rooms_v2_state import create_room_state, join_room_state, leave_room_state
else:
    from rooms_v2_state import create_room_state, join_room_state, leave_room_state


def _peek(root: object, *parts: str) -> Any:
    cursor: Any = root
    for part in parts:
        if not isinstance(cursor, dict):
            return None
        cursor = cursor.get(part)
    return cursor


def _fresh_public_thread(
    root: dict,
    uid: str,
    *,
    room_id: str,
    now: int,
    lease_ms: int,
) -> bool:
    value = _peek(root, "private", "visibleThreads", uid)
    if not isinstance(value, dict):
        return False
    if str(value.get("roomId") or "") != room_id:
        return False
    if str(value.get("conversationId") or ""):
        return False
    updated_at = int(value.get("updatedAt") or 0)
    return 0 <= now - updated_at <= lease_ms


def append_public_room_event_state(
    current: object,
    *,
    room_id: str,
    identity_id: str,
    display_name: str,
    mode: str,
    text: str,
    message_id: str,
    now: int,
    actor_uid: str,
    visible_lease_ms: int,
) -> dict:
    """Append a server-owned informational event without exposing account identity."""
    root = copy.deepcopy(current) if isinstance(current, dict) else {}
    room = _peek(root, "publicRooms", room_id)
    if not isinstance(room, dict):
        return root

    seq = int(room.get("lastSeq") or 0) + 1
    message = {
        "messageId": message_id,
        "roomId": room_id,
        "authorIdentityId": identity_id,
        "authorDisplayName": display_name,
        "authorMode": mode,
        "text": text,
        "sentAt": now,
        "seq": seq,
        "kind": "event",
        "removed": False,
    }
    root.setdefault("publicMessages", {}).setdefault(room_id, {})[message_id] = message
    room["lastSeq"] = seq
    room["updatedAt"] = now

    memberships = _peek(root, "membershipIndexByUser")
    if isinstance(memberships, dict):
        for member_uid, rooms in memberships.items():
            membership = rooms.get(room_id) if isinstance(rooms, dict) else None
            if not isinstance(membership, dict) or membership.get("active") is not True:
                continue
            if _peek(root, "private", "bans", room_id, member_uid) is not None:
                continue
            if member_uid == actor_uid or _fresh_public_thread(
                root,
                member_uid,
                room_id=room_id,
                now=now,
                lease_ms=visible_lease_ms,
            ):
                membership["lastReadSeq"] = seq
                membership["lastReadAt"] = now
                membership["unreadCount"] = 0
            else:
                last_read_seq = int(membership.get("lastReadSeq") or 0)
                membership["unreadCount"] = max(0, seq - last_read_seq)
    return root


def create_room_with_event_state(
    current: object,
    *,
    uid: str,
    display_name: str,
    name: str,
    normalized_name: str,
    name_key: str,
    description: str,
    operation_id: str,
    room_id: str,
    identity_id: str,
    now: int,
    public_profile_id: str,
    event_message_id: str,
    visible_lease_ms: int,
) -> dict:
    already_created = isinstance(
        _peek(current, "private", "createOperations", uid, operation_id),
        dict,
    )
    root = create_room_state(
        current,
        uid=uid,
        display_name=display_name,
        name=name,
        normalized_name=normalized_name,
        name_key=name_key,
        description=description,
        operation_id=operation_id,
        room_id=room_id,
        identity_id=identity_id,
        now=now,
        public_profile_id=public_profile_id,
    )
    if already_created:
        return root
    return append_public_room_event_state(
        root,
        room_id=room_id,
        identity_id=identity_id,
        display_name=display_name,
        mode="real",
        text=f"{display_name} creó la sala",
        message_id=event_message_id,
        now=now,
        actor_uid=uid,
        visible_lease_ms=visible_lease_ms,
    )


def join_room_with_event_state(
    current: object,
    *,
    uid: str,
    room_id: str,
    mode: str,
    display_name: str,
    identity_key: str,
    candidate_identity_id: str,
    now: int,
    public_profile_id: str | None,
    alias_claim_key: str | None,
    event_message_id: str,
    visible_lease_ms: int,
) -> dict:
    before = _peek(current, "membershipIndexByUser", uid, room_id)
    was_active = isinstance(before, dict) and before.get("active") is True
    root = join_room_state(
        current,
        uid=uid,
        room_id=room_id,
        mode=mode,
        display_name=display_name,
        identity_key=identity_key,
        candidate_identity_id=candidate_identity_id,
        now=now,
        public_profile_id=public_profile_id,
        alias_claim_key=alias_claim_key,
    )
    if was_active:
        return root
    membership = _peek(root, "membershipIndexByUser", uid, room_id)
    if not isinstance(membership, dict):
        return root
    return append_public_room_event_state(
        root,
        room_id=room_id,
        identity_id=str(membership.get("identityId") or candidate_identity_id),
        display_name=str(membership.get("displayName") or display_name),
        mode=str(membership.get("mode") or mode),
        text=f"{display_name} se unió a la sala",
        message_id=event_message_id,
        now=now,
        actor_uid=uid,
        visible_lease_ms=visible_lease_ms,
    )


def leave_room_with_event_state(
    current: object,
    *,
    uid: str,
    room_id: str,
    now: int,
    event_message_id: str,
    visible_lease_ms: int,
) -> dict:
    membership = _peek(current, "membershipIndexByUser", uid, room_id)
    identity_id = str(membership.get("identityId") or "") if isinstance(membership, dict) else ""
    display_name = str(membership.get("displayName") or "") if isinstance(membership, dict) else ""
    mode = str(membership.get("mode") or "real") if isinstance(membership, dict) else "real"
    root = leave_room_state(current, uid=uid, room_id=room_id, now=now)
    if not identity_id or not display_name:
        return root
    return append_public_room_event_state(
        root,
        room_id=room_id,
        identity_id=identity_id,
        display_name=display_name,
        mode=mode,
        text=f"{display_name} salió de la sala",
        message_id=event_message_id,
        now=now,
        actor_uid=uid,
        visible_lease_ms=visible_lease_ms,
    )


def should_notify_public_room_message(message: object) -> bool:
    """Presence/lifecycle events belong in the timeline, never in push notifications."""
    if not isinstance(message, dict):
        return False
    return str(message.get("kind") or "text").strip().lower() != "event"
