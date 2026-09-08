from __future__ import annotations

import copy
from typing import Any

ROLE_OWNER = "owner"
ROLE_MODERATOR = "moderator"
ROLE_MEMBER = "member"
MODE_REAL = "real"
MODE_ANONYMOUS = "anonymous"
STATUS_OPEN = "open"
STATUS_CLOSED = "closed"


class RoomsV2StateError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def _fail(code: str, message: str) -> None:
    raise RoomsV2StateError(code, message)


def _dict(value: object) -> dict:
    return value if isinstance(value, dict) else {}


def _root(current: object) -> dict:
    return copy.deepcopy(current) if isinstance(current, dict) else {}


def _node(root: dict, *parts: str) -> dict:
    cursor = root
    for part in parts:
        value = cursor.get(part)
        if not isinstance(value, dict):
            value = {}
            cursor[part] = value
        cursor = value
    return cursor


def _peek(root: dict, *parts: str) -> Any:
    cursor: Any = root
    for part in parts:
        if not isinstance(cursor, dict):
            return None
        cursor = cursor.get(part)
    return cursor


def _delete(root: dict, *parts: str) -> None:
    if not parts:
        return
    parent = _peek(root, *parts[:-1]) if len(parts) > 1 else root
    if isinstance(parent, dict):
        parent.pop(parts[-1], None)


def active_membership(root: dict, uid: str, room_id: str) -> dict:
    membership = _peek(root, "membershipIndexByUser", uid, room_id)
    if not isinstance(membership, dict) or membership.get("active") is not True:
        _fail("PERMISSION_DENIED", "Active membership is required.")
    if _peek(root, "private", "bans", room_id, uid) is not None:
        _fail("PERMISSION_DENIED", "This account is banned.")
    return membership


def open_room(root: dict, room_id: str) -> dict:
    room = _peek(root, "publicRooms", room_id)
    if not isinstance(room, dict):
        _fail("NOT_FOUND", "Room not found.")
    if str(room.get("status") or "").lower() != STATUS_OPEN:
        _fail("ROOM_CLOSED", "Room is closed.")
    return room


def require_role(membership: dict, *roles: str) -> str:
    role = str(membership.get("role") or "").lower()
    if role not in roles:
        _fail("PERMISSION_DENIED", "Insufficient room role.")
    return role


def can_moderate(actor_role: str, target_role: str) -> bool:
    actor = str(actor_role or "").lower()
    target = str(target_role or "").lower()
    if target == ROLE_OWNER:
        return False
    if actor == ROLE_OWNER:
        return True
    return actor == ROLE_MODERATOR and target == ROLE_MEMBER


def membership_projection(
    *,
    room_id: str,
    identity_id: str,
    display_name: str,
    mode: str,
    role: str,
    joined_at: int,
    active: bool,
    last_read_at: int,
    last_read_seq: int,
    unread_count: int,
    notifications_enabled: bool,
    public_profile_id: str | None = None,
) -> dict:
    result = {
        "roomId": room_id,
        "identityId": identity_id,
        "displayName": display_name,
        "mode": mode,
        "role": role,
        "active": active,
        "joinedAt": int(joined_at),
        "lastReadAt": int(last_read_at),
        "lastReadSeq": max(0, int(last_read_seq)),
        "unreadCount": max(0, int(unread_count)),
        "notificationsEnabled": bool(notifications_enabled),
    }
    if public_profile_id:
        result["publicProfileId"] = public_profile_id
    return result


def public_identity(projection: dict) -> dict:
    """Return only room-safe contextual identity fields.

    Firebase UID/profile ownership stays in server-only/self-only projections. Realtime
    Database rules are not filters, so a readable public member object must never contain
    a sensitive child such as ``publicProfileId``.
    """
    return {
        "identityId": projection["identityId"],
        "displayName": projection["displayName"],
        "mode": projection["mode"],
        "role": projection["role"],
        "active": projection["active"],
        "joinedAt": projection["joinedAt"],
    }


def _identity_owner(root: dict, room_id: str, identity_id: str) -> dict:
    owner = _peek(root, "private", "identityOwners", room_id, identity_id)
    if not isinstance(owner, dict) or not str(owner.get("uid") or "").strip():
        _fail("NOT_FOUND", "Identity ownership mapping not found.")
    return owner


def _public_member(root: dict, room_id: str, identity_id: str) -> dict:
    member = _peek(root, "publicMembers", room_id, identity_id)
    if not isinstance(member, dict):
        _fail("NOT_FOUND", "Member identity not found.")
    return member


def _audit(root: dict, room_id: str, audit_id: str, *, action: str, actor_identity_id: str, created_at: int, target_identity_id: str = "", target_id: str = "") -> None:
    entry = {"auditId": audit_id, "roomId": room_id, "action": action, "actorIdentityId": actor_identity_id, "createdAt": created_at}
    if target_identity_id:
        entry["targetIdentityId"] = target_identity_id
    if target_id:
        entry["targetId"] = target_id
    _node(root, "private", "audit", room_id)[audit_id] = entry


def create_room_state(current: object, *, uid: str, display_name: str, name: str, normalized_name: str, name_key: str, description: str, operation_id: str, room_id: str, identity_id: str, now: int, public_profile_id: str) -> dict:
    root = _root(current)
    operation = _peek(root, "private", "createOperations", uid, operation_id)
    if isinstance(operation, dict):
        existing_room_id = str(operation.get("roomId") or "")
        existing = _peek(root, "membershipIndexByUser", uid, existing_room_id)
        if isinstance(existing, dict):
            return root
    existing_claim = _peek(root, "private", "roomNameClaims", name_key)
    if isinstance(existing_claim, dict):
        _fail("ROOM_NAME_TAKEN", "Room name is already in use.")
    projection = membership_projection(room_id=room_id, identity_id=identity_id, display_name=display_name, mode=MODE_REAL, role=ROLE_OWNER, joined_at=now, active=True, last_read_at=now, last_read_seq=0, unread_count=0, notifications_enabled=True, public_profile_id=public_profile_id)
    _node(root, "publicRooms")[room_id] = {"roomId": room_id, "name": name, "normalizedName": normalized_name, "description": description, "status": STATUS_OPEN, "ownerIdentityId": identity_id, "memberCount": 1, "pendingCount": 0, "createdAt": now, "updatedAt": now, "lastSeq": 0}
    _node(root, "publicMembers", room_id)[identity_id] = public_identity(projection)
    _node(root, "membershipIndexByUser", uid)[room_id] = projection
    _node(root, "private", "userIdentity", uid, room_id)["real"] = {"identityId": identity_id, "mode": MODE_REAL, "displayName": display_name, "active": True, "createdAt": now, "publicProfileId": public_profile_id}
    _node(root, "private", "identityOwners", room_id)[identity_id] = {"uid": uid, "identityKey": "real", "active": True}
    _node(root, "private", "roomNameClaims")[name_key] = {"roomId": room_id, "normalizedName": normalized_name}
    _node(root, "private", "createOperations", uid)[operation_id] = {"roomId": room_id, "identityId": identity_id, "createdAt": now}
    return root


def _close_conversations_for_identity(root: dict, *, room_id: str, identity_id: str, leaving_uid: str, now: int) -> None:
    conversation_ids = _peek(root, "private", "conversationsByIdentity", room_id, identity_id)
    if not isinstance(conversation_ids, dict):
        return
    for conversation_id in list(conversation_ids.keys()):
        conversation = _peek(root, "private", "conversations", conversation_id)
        if not isinstance(conversation, dict):
            continue
        conversation["closed"] = True
        conversation["updatedAt"] = now
        participants = conversation.get("participants")
        if not isinstance(participants, dict):
            continue
        for participant_uid, participant in participants.items():
            if not isinstance(participant, dict):
                continue
            if participant_uid == leaving_uid:
                _delete(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
                _node(root, "private", "conversationReaders", conversation_id)[participant_uid] = False
            else:
                projection = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
                if isinstance(projection, dict):
                    projection["closed"] = True
                    projection["updatedAt"] = now


def _conversation_is_blocked(conversation: dict) -> bool:
    blocked_by = conversation.get("blockedBy")
    return isinstance(blocked_by, dict) and any(value is True for value in blocked_by.values())


def _reopen_conversations_for_identity(root: dict, *, room_id: str, identity_id: str, uid: str, now: int) -> None:
    conversation_ids = _peek(root, "private", "conversationsByIdentity", room_id, identity_id)
    if not isinstance(conversation_ids, dict):
        return
    for conversation_id in list(conversation_ids.keys()):
        conversation = _peek(root, "private", "conversations", conversation_id)
        if not isinstance(conversation, dict) or _conversation_is_blocked(conversation):
            continue
        participants = conversation.get("participants")
        if not isinstance(participants, dict) or uid not in participants:
            continue
        eligible = True
        for participant_uid, participant in participants.items():
            if not isinstance(participant, dict):
                eligible = False
                break
            participant_identity = str(participant.get("identityId") or "")
            membership = _peek(root, "membershipIndexByUser", participant_uid, room_id)
            if not isinstance(membership, dict) or membership.get("active") is not True or str(membership.get("identityId") or "") != participant_identity or _peek(root, "private", "bans", room_id, participant_uid) is not None:
                eligible = False
                break
        if not eligible:
            continue
        conversation["closed"] = False
        conversation["updatedAt"] = now
        last_seq = int(conversation.get("lastSeq") or 0)
        for participant_uid, participant in participants.items():
            other_uid = next(k for k in participants.keys() if k != participant_uid)
            other_identity_id = str(participants[other_uid].get("identityId") or "")
            other_public = _public_member(root, room_id, other_identity_id)
            existing = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
            last_read_seq = int(existing.get("lastReadSeq") or 0) if isinstance(existing, dict) else last_seq
            _node(root, "conversationIndexByUser", participant_uid, room_id)[conversation_id] = {"conversationId": conversation_id, "roomId": room_id, "otherIdentity": copy.deepcopy(other_public), "closed": False, "blocked": False, "lastText": str(conversation.get("lastText") or ""), "updatedAt": int(conversation.get("updatedAt") or now), "unreadCount": max(0, last_seq - last_read_seq), "lastReadSeq": last_read_seq}
            _node(root, "private", "conversationReaders", conversation_id)[participant_uid] = True


def join_room_state(current: object, *, uid: str, room_id: str, mode: str, display_name: str, identity_key: str, candidate_identity_id: str, now: int, public_profile_id: str | None, alias_claim_key: str | None) -> dict:
    root = _root(current)
    room = open_room(root, room_id)
    if _peek(root, "private", "bans", room_id, uid) is not None:
        _fail("PERMISSION_DENIED", "This account cannot join the room.")
    current_membership = _peek(root, "membershipIndexByUser", uid, room_id)
    if isinstance(current_membership, dict) and current_membership.get("active") is True:
        current_identity = str(current_membership.get("identityId") or "")
        owner = _identity_owner(root, room_id, current_identity)
        current_key = str(owner.get("identityKey") or "")
        if str(current_membership.get("mode") or "") == mode and current_key == identity_key:
            return root
        _fail("IDENTITY_CHANGE_REQUIRED", "Leave the room before changing the contextual identity.")
    existing_identity = _peek(root, "private", "userIdentity", uid, room_id, identity_key)
    existing_identity_id = str(existing_identity.get("identityId") or "") if isinstance(existing_identity, dict) else ""
    identity_id = existing_identity_id or candidate_identity_id
    if alias_claim_key:
        claim = _peek(root, "private", "aliasClaims", room_id, alias_claim_key)
        if isinstance(claim, dict):
            claim_uid = str(claim.get("uid") or "")
            if claim.get("active") is True and claim_uid and claim_uid != uid:
                _fail("ALIAS_TAKEN", "Alias is already active in this room.")
        _node(root, "private", "aliasClaims", room_id)[alias_claim_key] = {"uid": uid, "identityId": identity_id, "active": True}
    last_seq = int(room.get("lastSeq") or 0)
    joined_at = int(existing_identity.get("createdAt") or now) if isinstance(existing_identity, dict) else now
    projection = membership_projection(room_id=room_id, identity_id=identity_id, display_name=display_name, mode=mode, role=ROLE_MEMBER, joined_at=joined_at, active=True, last_read_at=now, last_read_seq=last_seq, unread_count=0, notifications_enabled=True, public_profile_id=public_profile_id if mode == MODE_REAL else None)
    _node(root, "membershipIndexByUser", uid)[room_id] = projection
    _node(root, "publicMembers", room_id)[identity_id] = public_identity(projection)
    private_identity = {"identityId": identity_id, "mode": mode, "displayName": display_name, "active": True, "createdAt": joined_at}
    if mode == MODE_REAL and public_profile_id:
        private_identity["publicProfileId"] = public_profile_id
    _node(root, "private", "userIdentity", uid, room_id)[identity_key] = private_identity
    _node(root, "private", "identityOwners", room_id)[identity_id] = {"uid": uid, "identityKey": identity_key, "active": True}
    room["memberCount"] = max(0, int(room.get("memberCount") or 0)) + 1
    room["updatedAt"] = now
    _reopen_conversations_for_identity(root, room_id=room_id, identity_id=identity_id, uid=uid, now=now)
    return root


def leave_room_state(current: object, *, uid: str, room_id: str, now: int) -> dict:
    root = _root(current)
    membership = active_membership(root, uid, room_id)
    if str(membership.get("role") or "") == ROLE_OWNER:
        _fail("OWNER_ACTION_REQUIRED", "Owner must transfer ownership or close the room before leaving.")
    identity_id = str(membership.get("identityId") or "")
    owner = _identity_owner(root, room_id, identity_id)
    identity_key = str(owner.get("identityKey") or "")
    membership["active"] = False
    public_member = _public_member(root, room_id, identity_id)
    public_member["active"] = False
    owner["active"] = False
    private_identity = _peek(root, "private", "userIdentity", uid, room_id, identity_key)
    if isinstance(private_identity, dict):
        private_identity["active"] = False
    if identity_key.startswith("anon_"):
        alias_key = identity_key.removeprefix("anon_")
        claim = _peek(root, "private", "aliasClaims", room_id, alias_key)
        if isinstance(claim, dict) and claim.get("uid") == uid and claim.get("identityId") == identity_id:
            claim["active"] = False
    room = _peek(root, "publicRooms", room_id)
    if isinstance(room, dict):
        room["memberCount"] = max(0, int(room.get("memberCount") or 0) - 1)
        room["updatedAt"] = now
    _close_conversations_for_identity(root, room_id=room_id, identity_id=identity_id, leaving_uid=uid, now=now)
    return root


def set_notifications_state(current: object, *, uid: str, room_id: str, enabled: bool) -> dict:
    root = _root(current)
    membership = active_membership(root, uid, room_id)
    membership["notificationsEnabled"] = bool(enabled)
    return root


def _fresh_visible_thread(root: dict, uid: str, *, room_id: str, conversation_id: str | None, now: int, lease_ms: int) -> bool:
    value = _peek(root, "private", "visibleThreads", uid)
    if not isinstance(value, dict) or str(value.get("roomId") or "") != room_id:
        return False
    actual_conversation = str(value.get("conversationId") or "") or None
    if actual_conversation != conversation_id:
        return False
    updated_at = int(value.get("updatedAt") or 0)
    return 0 <= now - updated_at <= lease_ms


def set_visible_thread_state(current: object, *, uid: str, room_id: str, conversation_id: str | None, visible: bool, now: int) -> dict:
    root = _root(current)
    active_membership(root, uid, room_id)
    if conversation_id:
        readers = _peek(root, "private", "conversationReaders", conversation_id)
        if not isinstance(readers, dict) or readers.get(uid) is not True:
            _fail("PERMISSION_DENIED", "Conversation access denied.")
    if visible:
        value = {"roomId": room_id, "updatedAt": now}
        if conversation_id:
            value["conversationId"] = conversation_id
        _node(root, "private", "visibleThreads")[uid] = value
    else:
        existing = _peek(root, "private", "visibleThreads", uid)
        if isinstance(existing, dict):
            same_room = str(existing.get("roomId") or "") == room_id
            same_conversation = (str(existing.get("conversationId") or "") or None) == conversation_id
            if same_room and same_conversation:
                _delete(root, "private", "visibleThreads", uid)
    return root


def send_public_text_state(current: object, *, uid: str, room_id: str, text: str, client_message_id: str, candidate_message_id: str, now: int, visible_lease_ms: int) -> dict:
    root = _root(current)
    room = open_room(root, room_id)
    sender = active_membership(root, uid, room_id)
    claims = _node(root, "private", "messageClaims", uid, f"room_{room_id}")
    existing_claim = claims.get(client_message_id)
    if isinstance(existing_claim, dict):
        existing_id = str(existing_claim.get("messageId") or "")
        existing = _peek(root, "publicMessages", room_id, existing_id)
        if isinstance(existing, dict):
            return root
    seq = int(room.get("lastSeq") or 0) + 1
    message_id = (str(existing_claim.get("messageId") or "") if isinstance(existing_claim, dict) else "") or candidate_message_id
    message = {"messageId": message_id, "roomId": room_id, "authorIdentityId": sender["identityId"], "authorDisplayName": sender["displayName"], "authorMode": sender["mode"], "text": text, "sentAt": now, "seq": seq, "kind": "text", "removed": False}
    _node(root, "publicMessages", room_id)[message_id] = message
    claims[client_message_id] = {"messageId": message_id}
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
            if member_uid == uid or _fresh_visible_thread(root, member_uid, room_id=room_id, conversation_id=None, now=now, lease_ms=visible_lease_ms):
                membership["lastReadSeq"] = seq
                membership["lastReadAt"] = now
                membership["unreadCount"] = 0
            else:
                last_read_seq = int(membership.get("lastReadSeq") or 0)
                membership["unreadCount"] = max(0, seq - last_read_seq)
    return root


def mark_read_state(current: object, *, uid: str, room_id: str, conversation_id: str | None, visible_seq: int, now: int) -> dict:
    root = _root(current)
    active_membership(root, uid, room_id)
    if conversation_id is None:
        room = open_room(root, room_id)
        membership = _peek(root, "membershipIndexByUser", uid, room_id)
        last_seq = int(room.get("lastSeq") or 0)
        target = min(max(0, int(visible_seq)), last_seq)
        previous = int(membership.get("lastReadSeq") or 0)
        if target >= previous:
            membership["lastReadSeq"] = target
            membership["lastReadAt"] = now
            membership["unreadCount"] = max(0, last_seq - target)
        return root
    conversation = _peek(root, "private", "conversations", conversation_id)
    readers = _peek(root, "private", "conversationReaders", conversation_id)
    if not isinstance(conversation, dict) or not isinstance(readers, dict) or readers.get(uid) is not True or str(conversation.get("roomId") or "") != room_id:
        _fail("PERMISSION_DENIED", "Conversation access denied.")
    projection = _peek(root, "conversationIndexByUser", uid, room_id, conversation_id)
    if not isinstance(projection, dict):
        _fail("NOT_FOUND", "Conversation projection not found.")
    last_seq = int(conversation.get("lastSeq") or 0)
    target = min(max(0, int(visible_seq)), last_seq)
    previous = int(projection.get("lastReadSeq") or 0)
    if target >= previous:
        projection["lastReadSeq"] = target
        projection["unreadCount"] = max(0, last_seq - target)
    return root


def conversation_id_for(room_id: str, first_identity_id: str, second_identity_id: str) -> str:
    import hashlib
    identities = sorted([first_identity_id, second_identity_id])
    raw = f"{room_id}|{identities[0]}|{identities[1]}".encode("utf-8")
    return "conv_" + hashlib.sha256(raw).hexdigest()[:40]


def open_private_state(current: object, *, uid: str, room_id: str, target_identity_id: str, conversation_id: str, now: int) -> dict:
    root = _root(current)
    open_room(root, room_id)
    actor = active_membership(root, uid, room_id)
    actor_identity_id = str(actor.get("identityId") or "")
    if actor_identity_id == target_identity_id:
        _fail("INVALID_INPUT", "Cannot open a private conversation with self.")
    target_public = _public_member(root, room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail("CONFLICT", "Target identity is inactive.")
    target_owner = _identity_owner(root, room_id, target_identity_id)
    target_uid = str(target_owner.get("uid") or "")
    target_membership = active_membership(root, target_uid, room_id)
    if str(target_membership.get("identityId") or "") != target_identity_id:
        _fail("CONFLICT", "Target contextual identity changed.")
    conversation = _peek(root, "private", "conversations", conversation_id)
    if isinstance(conversation, dict):
        if _conversation_is_blocked(conversation):
            _fail("CONFLICT", "Conversation is blocked.")
        participants = conversation.get("participants")
        if not isinstance(participants, dict) or set(participants) != {uid, target_uid}:
            _fail("CONFLICT", "Conversation identity mismatch.")
    else:
        conversation = {"conversationId": conversation_id, "roomId": room_id, "participants": {uid: {"identityId": actor_identity_id}, target_uid: {"identityId": target_identity_id}}, "blockedBy": {}, "closed": False, "createdAt": now, "updatedAt": now, "lastSeq": 0, "lastText": ""}
        _node(root, "private", "conversations")[conversation_id] = conversation
        _node(root, "private", "conversationsByIdentity", room_id, actor_identity_id)[conversation_id] = True
        _node(root, "private", "conversationsByIdentity", room_id, target_identity_id)[conversation_id] = True
    conversation["closed"] = False
    conversation["updatedAt"] = now
    participants = conversation["participants"]
    last_seq = int(conversation.get("lastSeq") or 0)
    for participant_uid, participant in participants.items():
        other_uid = next(k for k in participants if k != participant_uid)
        other_identity_id = str(participants[other_uid].get("identityId") or "")
        other_public = _public_member(root, room_id, other_identity_id)
        existing = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
        last_read_seq = int(existing.get("lastReadSeq") or 0) if isinstance(existing, dict) else last_seq
        _node(root, "conversationIndexByUser", participant_uid, room_id)[conversation_id] = {"conversationId": conversation_id, "roomId": room_id, "otherIdentity": copy.deepcopy(other_public), "closed": False, "blocked": False, "lastText": str(conversation.get("lastText") or ""), "updatedAt": int(conversation.get("updatedAt") or now), "unreadCount": max(0, last_seq - last_read_seq), "lastReadSeq": last_read_seq}
        _node(root, "private", "conversationReaders", conversation_id)[participant_uid] = True
    return root


def send_private_text_state(current: object, *, uid: str, room_id: str, conversation_id: str, text: str, client_message_id: str, candidate_message_id: str, now: int, visible_lease_ms: int) -> dict:
    root = _root(current)
    open_room(root, room_id)
    actor = active_membership(root, uid, room_id)
    conversation = _peek(root, "private", "conversations", conversation_id)
    if not isinstance(conversation, dict) or str(conversation.get("roomId") or "") != room_id:
        _fail("NOT_FOUND", "Conversation not found.")
    if conversation.get("closed") is True:
        _fail("CONFLICT", "Conversation is closed.")
    if _conversation_is_blocked(conversation):
        _fail("CONFLICT", "Conversation is blocked.")
    participants = conversation.get("participants")
    if not isinstance(participants, dict) or uid not in participants:
        _fail("PERMISSION_DENIED", "Conversation access denied.")
    if str(participants[uid].get("identityId") or "") != str(actor.get("identityId") or ""):
        _fail("PERMISSION_DENIED", "Contextual identity mismatch.")
    for participant_uid, participant in participants.items():
        membership = active_membership(root, participant_uid, room_id)
        if str(membership.get("identityId") or "") != str(participant.get("identityId") or ""):
            _fail("CONFLICT", "Participant identity is no longer active.")
    claims = _node(root, "private", "messageClaims", uid, f"private_{conversation_id}")
    existing_claim = claims.get(client_message_id)
    if isinstance(existing_claim, dict):
        existing_id = str(existing_claim.get("messageId") or "")
        existing = _peek(root, "privateMessages", conversation_id, existing_id)
        if isinstance(existing, dict):
            return root
    seq = int(conversation.get("lastSeq") or 0) + 1
    message_id = (str(existing_claim.get("messageId") or "") if isinstance(existing_claim, dict) else "") or candidate_message_id
    message = {"messageId": message_id, "roomId": room_id, "conversationId": conversation_id, "authorIdentityId": actor["identityId"], "authorDisplayName": actor["displayName"], "authorMode": actor["mode"], "text": text, "sentAt": now, "seq": seq, "kind": "text", "removed": False}
    _node(root, "privateMessages", conversation_id)[message_id] = message
    claims[client_message_id] = {"messageId": message_id}
    conversation["lastSeq"] = seq
    conversation["lastText"] = text
    conversation["updatedAt"] = now
    for participant_uid, participant in participants.items():
        projection = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
        if not isinstance(projection, dict):
            continue
        projection["lastText"] = text
        projection["updatedAt"] = now
        if participant_uid == uid or _fresh_visible_thread(root, participant_uid, room_id=room_id, conversation_id=conversation_id, now=now, lease_ms=visible_lease_ms):
            projection["lastReadSeq"] = seq
            projection["unreadCount"] = 0
        else:
            last_read_seq = int(projection.get("lastReadSeq") or 0)
            projection["unreadCount"] = max(0, seq - last_read_seq)
    return root


def block_private_state(current: object, *, uid: str, room_id: str, conversation_id: str, blocked: bool, now: int) -> dict:
    root = _root(current)
    active_membership(root, uid, room_id)
    conversation = _peek(root, "private", "conversations", conversation_id)
    if not isinstance(conversation, dict) or str(conversation.get("roomId") or "") != room_id:
        _fail("NOT_FOUND", "Conversation not found.")
    participants = conversation.get("participants")
    if not isinstance(participants, dict) or uid not in participants:
        _fail("PERMISSION_DENIED", "Conversation access denied.")
    blocked_by = _node(conversation, "blockedBy")
    if blocked:
        blocked_by[uid] = True
    else:
        blocked_by.pop(uid, None)
    is_blocked = _conversation_is_blocked(conversation)
    conversation["updatedAt"] = now
    for participant_uid in participants:
        projection = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
        if isinstance(projection, dict):
            projection["blocked"] = is_blocked
            projection["updatedAt"] = now
    return root


def edit_room_state(current: object, *, uid: str, room_id: str, name: str, normalized_name: str, new_name_key: str, description: str, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER)
    room = open_room(root, room_id)
    old_name_key = None
    claims = _peek(root, "private", "roomNameClaims")
    if isinstance(claims, dict):
        for key, claim in claims.items():
            if isinstance(claim, dict) and claim.get("roomId") == room_id:
                old_name_key = key
                break
    existing = _peek(root, "private", "roomNameClaims", new_name_key)
    if isinstance(existing, dict) and existing.get("roomId") != room_id:
        _fail("ROOM_NAME_TAKEN", "Room name is already in use.")
    if old_name_key and old_name_key != new_name_key:
        _delete(root, "private", "roomNameClaims", old_name_key)
    _node(root, "private", "roomNameClaims")[new_name_key] = {"roomId": room_id, "normalizedName": normalized_name}
    room["name"] = name
    room["normalizedName"] = normalized_name
    room["description"] = description
    room["updatedAt"] = now
    _audit(root, room_id, audit_id, action="edit_room", actor_identity_id=str(actor.get("identityId") or ""), created_at=now)
    return root


def close_room_state(current: object, *, uid: str, room_id: str, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER)
    room = _peek(root, "publicRooms", room_id)
    if not isinstance(room, dict):
        _fail("NOT_FOUND", "Room not found.")
    room["status"] = STATUS_CLOSED
    room["closedAt"] = int(room.get("closedAt") or now)
    room["updatedAt"] = now
    conversations = _peek(root, "private", "conversations")
    if isinstance(conversations, dict):
        for conversation_id, conversation in conversations.items():
            if not isinstance(conversation, dict) or conversation.get("roomId") != room_id:
                continue
            conversation["closed"] = True
            conversation["updatedAt"] = now
            participants = conversation.get("participants")
            if isinstance(participants, dict):
                for participant_uid in participants:
                    projection = _peek(root, "conversationIndexByUser", participant_uid, room_id, conversation_id)
                    if isinstance(projection, dict):
                        projection["closed"] = True
                        projection["updatedAt"] = now
    _audit(root, room_id, audit_id, action="close_room", actor_identity_id=str(actor.get("identityId") or ""), created_at=now)
    return root


def transfer_owner_state(current: object, *, uid: str, room_id: str, target_identity_id: str, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER)
    actor_identity_id = str(actor.get("identityId") or "")
    if actor_identity_id == target_identity_id:
        _fail("INVALID_INPUT", "Owner already has this identity.")
    target_public = _public_member(root, room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail("CONFLICT", "Target identity is inactive.")
    if str(target_public.get("mode") or "") != MODE_REAL:
        _fail("CONFLICT", "Ownership requires a real-profile identity.")
    target_owner = _identity_owner(root, room_id, target_identity_id)
    target_uid = str(target_owner.get("uid") or "")
    if target_uid == uid:
        _fail("INVALID_INPUT", "Target belongs to current owner.")
    target_membership = active_membership(root, target_uid, room_id)
    actor["role"] = ROLE_MEMBER
    target_membership["role"] = ROLE_OWNER
    _public_member(root, room_id, actor_identity_id)["role"] = ROLE_MEMBER
    target_public["role"] = ROLE_OWNER
    room = open_room(root, room_id)
    room["ownerIdentityId"] = target_identity_id
    room["updatedAt"] = now
    _audit(root, room_id, audit_id, action="transfer_owner", actor_identity_id=actor_identity_id, target_identity_id=target_identity_id, created_at=now)
    return root


def set_moderator_state(current: object, *, uid: str, room_id: str, target_identity_id: str, enabled: bool, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER)
    target_public = _public_member(root, room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail("CONFLICT", "Target identity is inactive.")
    if str(target_public.get("role") or "") == ROLE_OWNER:
        _fail("CONFLICT", "Owner role cannot be changed here.")
    target_uid = str(_identity_owner(root, room_id, target_identity_id).get("uid") or "")
    target_membership = active_membership(root, target_uid, room_id)
    new_role = ROLE_MODERATOR if enabled else ROLE_MEMBER
    target_membership["role"] = new_role
    target_public["role"] = new_role
    room = open_room(root, room_id)
    room["updatedAt"] = now
    _audit(root, room_id, audit_id, action="grant_moderator" if enabled else "revoke_moderator", actor_identity_id=str(actor.get("identityId") or ""), target_identity_id=target_identity_id, created_at=now)
    return root


def remove_member_state(current: object, *, uid: str, room_id: str, target_identity_id: str, ban: bool, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    actor_role = require_role(actor, ROLE_OWNER, ROLE_MODERATOR)
    actor_identity_id = str(actor.get("identityId") or "")
    if actor_identity_id == target_identity_id:
        _fail("INVALID_INPUT", "Use leave for current identity.")
    target_public = _public_member(root, room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail("CONFLICT", "Target identity is inactive.")
    target_role = str(target_public.get("role") or "")
    if not can_moderate(actor_role, target_role):
        _fail("PERMISSION_DENIED", "Role hierarchy forbids this action.")
    owner = _identity_owner(root, room_id, target_identity_id)
    target_uid = str(owner.get("uid") or "")
    target_membership = active_membership(root, target_uid, room_id)
    identity_key = str(owner.get("identityKey") or "")
    target_membership["active"] = False
    target_membership["role"] = ROLE_MEMBER
    target_public["active"] = False
    target_public["role"] = ROLE_MEMBER
    owner["active"] = False
    private_identity = _peek(root, "private", "userIdentity", target_uid, room_id, identity_key)
    if isinstance(private_identity, dict):
        private_identity["active"] = False
    if identity_key.startswith("anon_"):
        alias_key = identity_key.removeprefix("anon_")
        claim = _peek(root, "private", "aliasClaims", room_id, alias_key)
        if isinstance(claim, dict) and claim.get("uid") == target_uid and claim.get("identityId") == target_identity_id:
            claim["active"] = False
    if ban:
        _node(root, "private", "bans", room_id)[target_uid] = {"byIdentityId": actor_identity_id, "createdAt": now}
    room = open_room(root, room_id)
    room["memberCount"] = max(0, int(room.get("memberCount") or 0) - 1)
    room["updatedAt"] = now
    _close_conversations_for_identity(root, room_id=room_id, identity_id=target_identity_id, leaving_uid=target_uid, now=now)
    _audit(root, room_id, audit_id, action="ban_member" if ban else "kick_member", actor_identity_id=actor_identity_id, target_identity_id=target_identity_id, created_at=now)
    return root


def remove_public_message_state(current: object, *, uid: str, room_id: str, message_id: str, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER, ROLE_MODERATOR)
    message = _peek(root, "publicMessages", room_id, message_id)
    if not isinstance(message, dict):
        _fail("NOT_FOUND", "Message not found.")
    if message.get("removed") is not True:
        message["removed"] = True
        message["text"] = ""
        message.pop("attachment", None)
        message["removedAt"] = now
        message["removedByRole"] = str(actor.get("role") or "")
    _audit(root, room_id, audit_id, action="remove_message", actor_identity_id=str(actor.get("identityId") or ""), target_id=message_id, created_at=now)
    return root


def _evidence_from_message(message: dict) -> dict:
    allowed = ("messageId", "roomId", "conversationId", "authorIdentityId", "authorDisplayName", "authorMode", "text", "sentAt", "seq", "kind", "removed", "attachment")
    return {key: copy.deepcopy(message[key]) for key in allowed if key in message}


def report_message_state(current: object, *, uid: str, room_id: str, message_id: str, conversation_id: str | None, reason: str, report_id: str, now: int) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    if conversation_id:
        readers = _peek(root, "private", "conversationReaders", conversation_id)
        conversation = _peek(root, "private", "conversations", conversation_id)
        if not isinstance(readers, dict) or readers.get(uid) is not True or not isinstance(conversation, dict) or conversation.get("roomId") != room_id:
            _fail("PERMISSION_DENIED", "Conversation access denied.")
        message = _peek(root, "privateMessages", conversation_id, message_id)
        claim_scope = f"private_{conversation_id}"
    else:
        message = _peek(root, "publicMessages", room_id, message_id)
        claim_scope = f"room_{room_id}"
    if not isinstance(message, dict):
        _fail("NOT_FOUND", "Message not found.")
    claims = _node(root, "private", "reportClaims", uid, claim_scope)
    existing_id = str(claims.get(message_id) or "")
    if existing_id and isinstance(_peek(root, "reports", room_id, existing_id), dict):
        return root
    claims[message_id] = report_id
    _node(root, "reports", room_id)[report_id] = {"reportId": report_id, "roomId": room_id, "reason": reason, "status": "open", "evidence": _evidence_from_message(message), "createdAt": now, "resolution": "", "reporterIdentityId": str(actor.get("identityId") or "")}
    return root


def resolve_report_state(current: object, *, uid: str, room_id: str, report_id: str, resolution: str, now: int, audit_id: str) -> dict:
    root = _root(current)
    actor = active_membership(root, uid, room_id)
    require_role(actor, ROLE_OWNER, ROLE_MODERATOR)
    report = _peek(root, "reports", room_id, report_id)
    if not isinstance(report, dict):
        _fail("NOT_FOUND", "Report not found.")
    report["status"] = "resolved"
    report["resolution"] = resolution
    report["resolvedAt"] = now
    report["resolvedByIdentityId"] = str(actor.get("identityId") or "")
    _audit(root, room_id, audit_id, action="resolve_report", actor_identity_id=str(actor.get("identityId") or ""), target_id=report_id, created_at=now)
    return root
