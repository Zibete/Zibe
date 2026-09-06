from __future__ import annotations

import secrets
import time
from typing import Any

from firebase_admin import db
from firebase_functions import https_fn

if __package__:
    from .rooms_v2_core import (
        MembershipProjection,
        RoomsV2ValidationError,
        new_public_id,
        private_alias_key,
        resolve_alias_claim,
        resolve_message_claim,
        validate_alias,
        validate_client_message_id,
        validate_description,
        validate_message,
        validate_room_name,
    )
else:
    from rooms_v2_core import (
        MembershipProjection,
        RoomsV2ValidationError,
        new_public_id,
        private_alias_key,
        resolve_alias_claim,
        resolve_message_claim,
        validate_alias,
        validate_client_message_id,
        validate_description,
        validate_message,
        validate_room_name,
    )

ROOT = "RoomsV2"
STATUS_OPEN = "open"
MODE_REAL = "real"
MODE_ANONYMOUS = "anonymous"
ROLE_OWNER = "owner"
ROLE_MEMBER = "member"


def _now_ms() -> int:
    return int(time.time() * 1000)


def _fail(code: https_fn.FunctionsErrorCode, message: str) -> None:
    raise https_fn.HttpsError(code, message)


def _uid(request: https_fn.CallableRequest) -> str:
    if request.auth is None or not request.auth.uid:
        _fail(https_fn.FunctionsErrorCode.UNAUTHENTICATED, "Authentication is required.")
    return request.auth.uid


def _payload(request: https_fn.CallableRequest) -> dict[str, Any]:
    if request.data is None:
        return {}
    if not isinstance(request.data, dict):
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Expected an object payload.")
    return request.data


def _validated(fn, value: object):
    try:
        return fn(value)
    except RoomsV2ValidationError as exc:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, str(exc))


def _profile_name(uid: str) -> str:
    value = db.reference(f"Users/Accounts/{uid}/name").get()
    name = str(value or "").strip()
    if not name:
        _fail(
            https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            "A real public profile is required.",
        )
    return name[:80]


def _room(room_id: str) -> dict:
    value = db.reference(f"{ROOT}/publicRooms/{room_id}").get()
    if not isinstance(value, dict):
        _fail(https_fn.FunctionsErrorCode.NOT_FOUND, "Room not found.")
    return value


def _active_membership(uid: str, room_id: str) -> dict:
    value = db.reference(f"{ROOT}/membershipIndexByUser/{uid}/{room_id}").get()
    if not isinstance(value, dict) or value.get("active") is not True:
        _fail(https_fn.FunctionsErrorCode.PERMISSION_DENIED, "Active membership is required.")
    return value


def _ensure_room_open(room: dict) -> None:
    if str(room.get("status") or "").lower() != STATUS_OPEN:
        _fail(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, "Room is closed.")


def _ensure_not_banned(uid: str, room_id: str) -> None:
    if db.reference(f"{ROOT}/private/bans/{room_id}/{uid}").get() is not None:
        _fail(https_fn.FunctionsErrorCode.PERMISSION_DENIED, "This account cannot join the room.")


def _membership_projection(
    *,
    room_id: str,
    identity_id: str,
    display_name: str,
    mode: str,
    role: str,
    joined_at: int,
    active: bool = True,
    last_read_at: int = 0,
) -> dict:
    return MembershipProjection(
        room_id=room_id,
        identity_id=identity_id,
        display_name=display_name,
        mode=mode,
        role=role,
        active=active,
        joined_at=joined_at,
        last_read_at=last_read_at,
    ).as_dict()


def _public_member(projection: dict) -> dict:
    return {
        "identityId": projection["identityId"],
        "displayName": projection["displayName"],
        "mode": projection["mode"],
        "role": projection["role"],
        "active": projection["active"],
        "joinedAt": projection["joinedAt"],
    }


def _fanout(updates: dict[str, Any]) -> None:
    # Admin SDK multi-path update is atomic within RTDB.
    db.reference(ROOT).update(updates)


def _increment_member_count(room_id: str, delta: int) -> None:
    ref = db.reference(f"{ROOT}/publicRooms/{room_id}/memberCount")

    def update(current: object) -> int:
        try:
            value = int(current or 0)
        except (TypeError, ValueError):
            value = 0
        return max(0, value + delta)

    ref.transaction(update)


def _resolve_existing_identity(uid: str, room_id: str, identity_key: str) -> dict | None:
    value = db.reference(f"{ROOT}/private/userIdentity/{uid}/{room_id}/{identity_key}").get()
    return value if isinstance(value, dict) else None


@https_fn.on_call(region="us-central1")
def create_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    name = _validated(validate_room_name, data.get("name"))
    description = _validated(validate_description, data.get("description"))
    display_name = _profile_name(uid)
    now = _now_ms()
    room_id = new_public_id("room", secrets.token_urlsafe(12))
    identity_id = new_public_id("id", secrets.token_urlsafe(12))
    projection = _membership_projection(
        room_id=room_id,
        identity_id=identity_id,
        display_name=display_name,
        mode=MODE_REAL,
        role=ROLE_OWNER,
        joined_at=now,
    )
    room = {
        "roomId": room_id,
        "name": name,
        "description": description,
        "status": STATUS_OPEN,
        "ownerIdentityId": identity_id,
        "memberCount": 1,
        "pendingCount": 0,
        "createdAt": now,
        "updatedAt": now,
    }
    private_identity = {
        "identityId": identity_id,
        "mode": MODE_REAL,
        "displayName": display_name,
        "active": True,
        "createdAt": now,
    }
    _fanout(
        {
            f"publicRooms/{room_id}": room,
            f"publicMembers/{room_id}/{identity_id}": _public_member(projection),
            f"membershipIndexByUser/{uid}/{room_id}": projection,
            f"private/userIdentity/{uid}/{room_id}/real": private_identity,
            f"private/identityOwners/{room_id}/{identity_id}": {
                "uid": uid,
                "identityKey": "real",
                "active": True,
            },
        }
    )
    return {"membership": projection}


@https_fn.on_call(region="us-central1")
def join_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    if not room_id or len(room_id) > 120:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Invalid roomId.")
    room = _room(room_id)
    _ensure_room_open(room)
    _ensure_not_banned(uid, room_id)

    current = db.reference(f"{ROOT}/membershipIndexByUser/{uid}/{room_id}").get()
    if isinstance(current, dict) and current.get("active") is True:
        return {"membership": current}

    mode = str(data.get("mode") or MODE_REAL).strip().lower()
    now = _now_ms()

    if mode == MODE_REAL:
        display_name = _profile_name(uid)
        identity_key = "real"
        existing = _resolve_existing_identity(uid, room_id, identity_key)
        identity_id = str((existing or {}).get("identityId") or "").strip()
        if not identity_id:
            identity_id = new_public_id("id", secrets.token_urlsafe(12))
    elif mode == MODE_ANONYMOUS:
        display_name, normalized_alias = _validated(validate_alias, data.get("alias"))
        alias_hash = private_alias_key(normalized_alias)
        identity_key = f"anon_{alias_hash}"
        existing = _resolve_existing_identity(uid, room_id, identity_key)
        candidate_identity_id = str((existing or {}).get("identityId") or "").strip()
        if not candidate_identity_id:
            candidate_identity_id = new_public_id("anon", secrets.token_urlsafe(12))
        claim_ref = db.reference(f"{ROOT}/private/aliasClaims/{room_id}/{alias_hash}")

        def claim_alias(current_claim: object) -> dict:
            resolved, _ = resolve_alias_claim(
                current_claim,
                uid=uid,
                candidate_identity_id=candidate_identity_id,
            )
            return resolved

        claim = claim_ref.transaction(claim_alias)
        if not isinstance(claim, dict) or claim.get("uid") != uid:
            _fail(https_fn.FunctionsErrorCode.ALREADY_EXISTS, "Alias is already active in this room.")
        identity_id = str(claim.get("identityId") or "").strip()
        if not identity_id:
            _fail(https_fn.FunctionsErrorCode.INTERNAL, "Alias claim has no identity.")
    else:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Unknown identity mode.")

    joined_at = int((existing or {}).get("createdAt") or now)
    projection = _membership_projection(
        room_id=room_id,
        identity_id=identity_id,
        display_name=display_name,
        mode=mode,
        role=ROLE_MEMBER,
        joined_at=joined_at,
    )
    private_identity = {
        "identityId": identity_id,
        "mode": mode,
        "displayName": display_name,
        "active": True,
        "createdAt": joined_at,
    }
    _fanout(
        {
            f"publicMembers/{room_id}/{identity_id}": _public_member(projection),
            f"membershipIndexByUser/{uid}/{room_id}": projection,
            f"private/userIdentity/{uid}/{room_id}/{identity_key}": private_identity,
            f"private/identityOwners/{room_id}/{identity_id}": {
                "uid": uid,
                "identityKey": identity_key,
                "active": True,
            },
            f"publicRooms/{room_id}/updatedAt": now,
        }
    )
    _increment_member_count(room_id, 1)
    return {"membership": projection}


@https_fn.on_call(region="us-central1")
def leave_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    membership = _active_membership(uid, room_id)
    if str(membership.get("role") or "").lower() == ROLE_OWNER:
        _fail(
            https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            "The owner must transfer ownership or close the room before leaving.",
        )
    identity_id = str(membership.get("identityId") or "").strip()
    owner_map = db.reference(f"{ROOT}/private/identityOwners/{room_id}/{identity_id}").get()
    identity_key = str((owner_map or {}).get("identityKey") or "").strip()
    now = _now_ms()
    updated_membership = dict(membership)
    updated_membership["active"] = False
    public_member = db.reference(f"{ROOT}/publicMembers/{room_id}/{identity_id}").get()
    updated_public_member = dict(public_member) if isinstance(public_member, dict) else _public_member(updated_membership)
    updated_public_member["active"] = False
    updates: dict[str, Any] = {
        f"membershipIndexByUser/{uid}/{room_id}": updated_membership,
        f"publicMembers/{room_id}/{identity_id}": updated_public_member,
        f"private/identityOwners/{room_id}/{identity_id}/active": False,
        f"publicRooms/{room_id}/updatedAt": now,
    }
    if identity_key:
        updates[f"private/userIdentity/{uid}/{room_id}/{identity_key}/active"] = False
        if identity_key.startswith("anon_"):
            alias_hash = identity_key.removeprefix("anon_")
            claim = db.reference(f"{ROOT}/private/aliasClaims/{room_id}/{alias_hash}").get()
            if isinstance(claim, dict) and claim.get("uid") == uid and claim.get("identityId") == identity_id:
                updates[f"private/aliasClaims/{room_id}/{alias_hash}/active"] = False
    _fanout(updates)
    _increment_member_count(room_id, -1)
    return {"ok": True, "roomId": room_id}


@https_fn.on_call(region="us-central1")
def send_room_v2_text(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    room = _room(room_id)
    _ensure_room_open(room)
    membership = _active_membership(uid, room_id)
    text = _validated(validate_message, data.get("text"))
    client_message_id = _validated(validate_client_message_id, data.get("clientMessageId"))
    candidate_message_id = new_public_id("msg", secrets.token_urlsafe(12))
    claim_ref = db.reference(f"{ROOT}/private/messageClaims/{uid}/{room_id}/{client_message_id}")

    def claim_message(current: object) -> dict:
        resolved, _ = resolve_message_claim(current, candidate_message_id=candidate_message_id)
        return resolved

    claim = claim_ref.transaction(claim_message)
    message_id = str((claim or {}).get("messageId") or "").strip()
    if not message_id:
        _fail(https_fn.FunctionsErrorCode.INTERNAL, "Message claim failed.")
    existing = db.reference(f"{ROOT}/publicMessages/{room_id}/{message_id}").get()
    if isinstance(existing, dict):
        return existing

    sent_at = _now_ms()
    message = {
        "messageId": message_id,
        "roomId": room_id,
        "authorIdentityId": membership["identityId"],
        "authorDisplayName": membership["displayName"],
        "authorMode": membership["mode"],
        "text": text,
        "sentAt": sent_at,
    }
    _fanout(
        {
            f"publicMessages/{room_id}/{message_id}": message,
            f"publicRooms/{room_id}/updatedAt": sent_at,
            f"private/messageClaims/{uid}/{room_id}/{client_message_id}/messageId": message_id,
        }
    )
    return message
