from __future__ import annotations

from typing import Any

from firebase_admin import db
from firebase_functions import https_fn

if __package__:
    from .rooms_v2 import (
        MODE_REAL,
        ROLE_MEMBER,
        ROLE_OWNER,
        ROOT,
        _active_membership,
        _fanout,
        _fail,
        _increment_member_count,
        _now_ms,
        _payload,
        _room,
        _uid,
    )
    from .rooms_v2_core import can_moderate_target
else:
    from rooms_v2 import (
        MODE_REAL,
        ROLE_MEMBER,
        ROLE_OWNER,
        ROOT,
        _active_membership,
        _fanout,
        _fail,
        _increment_member_count,
        _now_ms,
        _payload,
        _room,
        _uid,
    )
    from rooms_v2_core import can_moderate_target

ROLE_MODERATOR = "moderator"


def _public_member(room_id: str, identity_id: str) -> dict:
    value = db.reference(f"{ROOT}/publicMembers/{room_id}/{identity_id}").get()
    if not isinstance(value, dict):
        _fail(https_fn.FunctionsErrorCode.NOT_FOUND, "Member identity not found.")
    return value


def _identity_owner(room_id: str, identity_id: str) -> dict:
    value = db.reference(f"{ROOT}/private/identityOwners/{room_id}/{identity_id}").get()
    if not isinstance(value, dict) or not str(value.get("uid") or "").strip():
        _fail(https_fn.FunctionsErrorCode.NOT_FOUND, "Member ownership mapping not found.")
    return value


def _require_role(membership: dict, *allowed: str) -> str:
    role = str(membership.get("role") or "").lower()
    if role not in allowed:
        _fail(https_fn.FunctionsErrorCode.PERMISSION_DENIED, "Insufficient room role.")
    return role


def _identity_id(value: object) -> str:
    identity_id = str(value or "").strip()
    if not identity_id or len(identity_id) > 120 or any(char in ".#$[]/" for char in identity_id):
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Invalid identityId.")
    return identity_id


def _deactivate_member(
    *,
    actor_identity_id: str,
    target_uid: str,
    room_id: str,
    target_identity_id: str,
    target_membership: dict,
    target_public: dict,
    target_owner: dict,
    ban: bool,
) -> None:
    now = _now_ms()
    identity_key = str(target_owner.get("identityKey") or "").strip()
    inactive_membership = dict(target_membership)
    inactive_membership["active"] = False
    inactive_public = dict(target_public)
    inactive_public["active"] = False
    updates: dict[str, Any] = {
        f"membershipIndexByUser/{target_uid}/{room_id}": inactive_membership,
        f"publicMembers/{room_id}/{target_identity_id}": inactive_public,
        f"private/identityOwners/{room_id}/{target_identity_id}/active": False,
        f"publicRooms/{room_id}/updatedAt": now,
    }
    if identity_key:
        updates[f"private/userIdentity/{target_uid}/{room_id}/{identity_key}/active"] = False
        if identity_key.startswith("anon_"):
            alias_hash = identity_key.removeprefix("anon_")
            claim = db.reference(f"{ROOT}/private/aliasClaims/{room_id}/{alias_hash}").get()
            if (
                isinstance(claim, dict)
                and claim.get("uid") == target_uid
                and claim.get("identityId") == target_identity_id
            ):
                updates[f"private/aliasClaims/{room_id}/{alias_hash}/active"] = False
    if ban:
        updates[f"private/bans/{room_id}/{target_uid}"] = {
            "byIdentityId": actor_identity_id,
            "createdAt": now,
        }
    _fanout(updates)
    _increment_member_count(room_id, -1)


@https_fn.on_call(region="us-central1")
def close_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    room_id = str(_payload(request).get("roomId") or "").strip()
    _require_role(_active_membership(uid, room_id), ROLE_OWNER)
    room = _room(room_id)
    if str(room.get("status") or "").lower() == "closed":
        return {"ok": True, "roomId": room_id, "status": "closed"}
    now = _now_ms()
    _fanout(
        {
            f"publicRooms/{room_id}/status": "closed",
            f"publicRooms/{room_id}/closedAt": now,
            f"publicRooms/{room_id}/updatedAt": now,
        }
    )
    return {"ok": True, "roomId": room_id, "status": "closed"}


@https_fn.on_call(region="us-central1")
def transfer_room_owner_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    target_identity_id = _identity_id(data.get("targetIdentityId"))
    actor = _active_membership(uid, room_id)
    _require_role(actor, ROLE_OWNER)
    actor_identity_id = str(actor.get("identityId") or "").strip()
    if target_identity_id == actor_identity_id:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Owner is already this identity.")

    target_public = _public_member(room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, "Target member is inactive.")
    if str(target_public.get("mode") or "").lower() != MODE_REAL:
        _fail(
            https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            "Ownership can only be transferred to an active real-profile identity.",
        )
    target_owner = _identity_owner(room_id, target_identity_id)
    target_uid = str(target_owner["uid"])
    if target_uid == uid:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Target belongs to the current owner account.")
    target_membership = _active_membership(target_uid, room_id)
    actor_updated = dict(actor)
    actor_updated["role"] = ROLE_MEMBER
    target_updated = dict(target_membership)
    target_updated["role"] = ROLE_OWNER
    _fanout(
        {
            f"membershipIndexByUser/{uid}/{room_id}": actor_updated,
            f"membershipIndexByUser/{target_uid}/{room_id}": target_updated,
            f"publicMembers/{room_id}/{actor_identity_id}/role": ROLE_MEMBER,
            f"publicMembers/{room_id}/{target_identity_id}/role": ROLE_OWNER,
            f"publicRooms/{room_id}/ownerIdentityId": target_identity_id,
            f"publicRooms/{room_id}/updatedAt": _now_ms(),
        }
    )
    return {"ok": True, "roomId": room_id, "ownerIdentityId": target_identity_id}


@https_fn.on_call(region="us-central1")
def set_room_moderator_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    target_identity_id = _identity_id(data.get("targetIdentityId"))
    enabled = data.get("enabled")
    if not isinstance(enabled, bool):
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "enabled must be boolean.")
    _require_role(_active_membership(uid, room_id), ROLE_OWNER)
    target_public = _public_member(room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, "Target member is inactive.")
    if str(target_public.get("role") or "").lower() == ROLE_OWNER:
        _fail(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, "Owner role cannot be changed here.")
    target_uid = str(_identity_owner(room_id, target_identity_id)["uid"])
    target_membership = _active_membership(target_uid, room_id)
    new_role = ROLE_MODERATOR if enabled else ROLE_MEMBER
    updated = dict(target_membership)
    updated["role"] = new_role
    _fanout(
        {
            f"membershipIndexByUser/{target_uid}/{room_id}": updated,
            f"publicMembers/{room_id}/{target_identity_id}/role": new_role,
            f"publicRooms/{room_id}/updatedAt": _now_ms(),
        }
    )
    return {"ok": True, "roomId": room_id, "targetIdentityId": target_identity_id, "role": new_role}


def _remove_room_member(request: https_fn.CallableRequest, *, ban: bool) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = str(data.get("roomId") or "").strip()
    target_identity_id = _identity_id(data.get("targetIdentityId"))
    actor = _active_membership(uid, room_id)
    actor_role = _require_role(actor, ROLE_OWNER, ROLE_MODERATOR)
    actor_identity_id = str(actor.get("identityId") or "").strip()
    if target_identity_id == actor_identity_id:
        _fail(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, "Use leave for the current identity.")
    target_public = _public_member(room_id, target_identity_id)
    if target_public.get("active") is not True:
        _fail(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, "Target member is inactive.")
    if not can_moderate_target(actor_role, str(target_public.get("role") or "")):
        _fail(https_fn.FunctionsErrorCode.PERMISSION_DENIED, "Role hierarchy forbids this action.")
    target_owner = _identity_owner(room_id, target_identity_id)
    target_uid = str(target_owner["uid"])
    _deactivate_member(
        actor_identity_id=actor_identity_id,
        target_uid=target_uid,
        room_id=room_id,
        target_identity_id=target_identity_id,
        target_membership=_active_membership(target_uid, room_id),
        target_public=target_public,
        target_owner=target_owner,
        ban=ban,
    )
    return {
        "ok": True,
        "roomId": room_id,
        "targetIdentityId": target_identity_id,
        "banned": ban,
    }


@https_fn.on_call(region="us-central1")
def kick_room_member_v2(request: https_fn.CallableRequest) -> dict:
    return _remove_room_member(request, ban=False)


@https_fn.on_call(region="us-central1")
def ban_room_member_v2(request: https_fn.CallableRequest) -> dict:
    return _remove_room_member(request, ban=True)
