from __future__ import annotations

from firebase_functions import https_fn

if __package__:
    from .rooms_v2 import (
        _fail,
        _normalize_room_name,
        _now_ms,
        _payload,
        _public_id,
        _room_name_key,
        _safe_id,
        _transaction,
        _uid,
        _validated,
    )
    from .rooms_v2_core import validate_description, validate_room_name
    from .rooms_v2_state import (
        close_room_state,
        edit_room_state,
        remove_member_state,
        remove_public_message_state,
        report_message_state,
        resolve_report_state,
        set_moderator_state,
        transfer_owner_state,
    )
else:
    from rooms_v2 import (
        _fail,
        _normalize_room_name,
        _now_ms,
        _payload,
        _public_id,
        _room_name_key,
        _safe_id,
        _transaction,
        _uid,
        _validated,
    )
    from rooms_v2_core import validate_description, validate_room_name
    from rooms_v2_state import (
        close_room_state,
        edit_room_state,
        remove_member_state,
        remove_public_message_state,
        report_message_state,
        resolve_report_state,
        set_moderator_state,
        transfer_owner_state,
    )


def _bounded_text(value: object, *, field: str, minimum: int, maximum: int) -> str:
    text = " ".join(str(value or "").strip().split())
    if len(text) < minimum or len(text) > maximum:
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            f"{field} must contain {minimum}-{maximum} characters.",
            room_code="INVALID_INPUT",
        )
    return text


@https_fn.on_call(region="us-central1")
def edit_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    name = _validated(validate_room_name, data.get("name"))
    description = _validated(validate_description, data.get("description"))
    normalized_name = _normalize_room_name(name)
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: edit_room_state(
            current,
            uid=uid,
            room_id=room_id,
            name=name,
            normalized_name=normalized_name,
            new_name_key=_room_name_key(normalized_name),
            description=description,
            now=now,
            audit_id=audit_id,
        )
    )
    return {"ok": True, "roomId": room_id}


@https_fn.on_call(region="us-central1")
def close_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    room_id = _safe_id(_payload(request).get("roomId"), field="roomId")
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: close_room_state(
            current,
            uid=uid,
            room_id=room_id,
            now=now,
            audit_id=audit_id,
        )
    )
    return {"ok": True, "roomId": room_id, "status": "closed"}


@https_fn.on_call(region="us-central1")
def transfer_room_owner_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    target_identity_id = _safe_id(
        data.get("targetIdentityId"),
        field="targetIdentityId",
    )
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: transfer_owner_state(
            current,
            uid=uid,
            room_id=room_id,
            target_identity_id=target_identity_id,
            now=now,
            audit_id=audit_id,
        )
    )
    return {
        "ok": True,
        "roomId": room_id,
        "ownerIdentityId": target_identity_id,
    }


@https_fn.on_call(region="us-central1")
def set_room_moderator_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    target_identity_id = _safe_id(
        data.get("targetIdentityId"),
        field="targetIdentityId",
    )
    enabled = data.get("enabled")
    if not isinstance(enabled, bool):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "enabled must be boolean.",
            room_code="INVALID_INPUT",
        )
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: set_moderator_state(
            current,
            uid=uid,
            room_id=room_id,
            target_identity_id=target_identity_id,
            enabled=enabled,
            now=now,
            audit_id=audit_id,
        )
    )
    return {
        "ok": True,
        "roomId": room_id,
        "targetIdentityId": target_identity_id,
        "role": "moderator" if enabled else "member",
    }


def _remove_member(
    request: https_fn.CallableRequest,
    *,
    ban: bool,
) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    target_identity_id = _safe_id(
        data.get("targetIdentityId"),
        field="targetIdentityId",
    )
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: remove_member_state(
            current,
            uid=uid,
            room_id=room_id,
            target_identity_id=target_identity_id,
            ban=ban,
            now=now,
            audit_id=audit_id,
        )
    )
    return {
        "ok": True,
        "roomId": room_id,
        "targetIdentityId": target_identity_id,
        "banned": ban,
    }


@https_fn.on_call(region="us-central1")
def kick_room_member_v2(request: https_fn.CallableRequest) -> dict:
    return _remove_member(request, ban=False)


@https_fn.on_call(region="us-central1")
def ban_room_member_v2(request: https_fn.CallableRequest) -> dict:
    return _remove_member(request, ban=True)


@https_fn.on_call(region="us-central1")
def remove_room_message_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    message_id = _safe_id(data.get("messageId"), field="messageId")
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: remove_public_message_state(
            current,
            uid=uid,
            room_id=room_id,
            message_id=message_id,
            now=now,
            audit_id=audit_id,
        )
    )
    return {"ok": True, "roomId": room_id, "messageId": message_id}


@https_fn.on_call(region="us-central1")
def report_room_message_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    message_id = _safe_id(data.get("messageId"), field="messageId")
    conversation_value = data.get("conversationId")
    conversation_id = (
        _safe_id(conversation_value, field="conversationId")
        if conversation_value not in (None, "")
        else None
    )
    reason = _bounded_text(
        data.get("reason"),
        field="reason",
        minimum=3,
        maximum=500,
    )
    report_id = _public_id("report")
    now = _now_ms()
    _transaction(
        lambda current: report_message_state(
            current,
            uid=uid,
            room_id=room_id,
            message_id=message_id,
            conversation_id=conversation_id,
            reason=reason,
            report_id=report_id,
            now=now,
        )
    )
    return {"ok": True, "roomId": room_id, "reportId": report_id}


@https_fn.on_call(region="us-central1")
def resolve_room_report_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    report_id = _safe_id(data.get("reportId"), field="reportId")
    resolution = _bounded_text(
        data.get("resolution"),
        field="resolution",
        minimum=1,
        maximum=500,
    )
    now = _now_ms()
    audit_id = _public_id("audit")
    _transaction(
        lambda current: resolve_report_state(
            current,
            uid=uid,
            room_id=room_id,
            report_id=report_id,
            resolution=resolution,
            now=now,
            audit_id=audit_id,
        )
    )
    return {"ok": True, "roomId": room_id, "reportId": report_id}
