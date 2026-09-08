from __future__ import annotations

from typing import Any

from firebase_admin import db
from firebase_functions import https_fn

if __package__:
    from .rooms_v2_profile_core import (
        RoomsV2ProfileError,
        build_context_profile,
        resolve_context_profile_owner_uid,
        validate_context_profile_access,
    )
else:
    from rooms_v2_profile_core import (
        RoomsV2ProfileError,
        build_context_profile,
        resolve_context_profile_owner_uid,
        validate_context_profile_access,
    )

ROOT = "RoomsV2"
MAX_ID_CHARS = 120


def _fail(
    code: https_fn.FunctionsErrorCode,
    message: str,
    *,
    room_code: str,
) -> None:
    raise https_fn.HttpsError(code, message, {"roomV2Code": room_code})


def _uid(request: https_fn.CallableRequest) -> str:
    if request.auth is None or not request.auth.uid:
        _fail(
            https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            "Authentication is required.",
            room_code="UNAUTHENTICATED",
        )
    return request.auth.uid


def _payload(request: https_fn.CallableRequest) -> dict[str, Any]:
    if request.data is None:
        return {}
    if not isinstance(request.data, dict):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "Expected an object payload.",
            room_code="INVALID_INPUT",
        )
    return request.data


def _safe_id(value: object, *, field: str) -> str:
    text = str(value or "").strip()
    if (
        not text
        or len(text) > MAX_ID_CHARS
        or any(char in ".#$[]/" or ord(char) < 32 for char in text)
    ):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            f"Invalid {field}.",
            room_code="INVALID_INPUT",
        )
    return text


def _profile_error(exc: RoomsV2ProfileError) -> None:
    mapping = {
        "PERMISSION_DENIED": https_fn.FunctionsErrorCode.PERMISSION_DENIED,
        "NOT_FOUND": https_fn.FunctionsErrorCode.NOT_FOUND,
        "INTERNAL": https_fn.FunctionsErrorCode.INTERNAL,
    }
    _fail(
        mapping.get(exc.code, https_fn.FunctionsErrorCode.INTERNAL),
        str(exc),
        room_code=exc.code,
    )


@https_fn.on_call(region="us-central1")
def resolve_room_identity_profile_v2(request: https_fn.CallableRequest) -> dict:
    viewer_uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    target_identity_id = _safe_id(
        data.get("identityId"),
        field="identityId",
    )

    viewer_membership = db.reference(
        f"{ROOT}/membershipIndexByUser/{viewer_uid}/{room_id}"
    ).get()
    target_identity = db.reference(
        f"{ROOT}/publicMembers/{room_id}/{target_identity_id}"
    ).get()

    try:
        target_identity = validate_context_profile_access(
            viewer_membership,
            target_identity,
        )
    except RoomsV2ProfileError as exc:
        _profile_error(exc)

    owner_mapping = db.reference(
        f"{ROOT}/private/identityOwners/{room_id}/{target_identity_id}"
    ).get()
    try:
        target_uid = resolve_context_profile_owner_uid(owner_mapping)
    except RoomsV2ProfileError as exc:
        _profile_error(exc)

    account = db.reference(f"Users/Accounts/{target_uid}").get()
    try:
        profile = build_context_profile(target_identity, account)
    except RoomsV2ProfileError as exc:
        _profile_error(exc)

    return {"profile": profile}
