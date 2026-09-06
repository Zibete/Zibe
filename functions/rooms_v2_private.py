from __future__ import annotations

from firebase_functions import https_fn

if __package__:
    from .rooms_v2 import (
        VISIBLE_THREAD_LEASE_MS,
        _fail,
        _now_ms,
        _payload,
        _peek,
        _public_id,
        _safe_id,
        _transaction,
        _uid,
        _validated,
    )
    from .rooms_v2_core import validate_client_message_id, validate_message
    from .rooms_v2_state import (
        active_membership,
        block_private_state,
        conversation_id_for,
        open_private_state,
        send_private_text_state,
    )
else:
    from rooms_v2 import (
        VISIBLE_THREAD_LEASE_MS,
        _fail,
        _now_ms,
        _payload,
        _peek,
        _public_id,
        _safe_id,
        _transaction,
        _uid,
        _validated,
    )
    from rooms_v2_core import validate_client_message_id, validate_message
    from rooms_v2_state import (
        active_membership,
        block_private_state,
        conversation_id_for,
        open_private_state,
        send_private_text_state,
    )


@https_fn.on_call(region="us-central1")
def open_room_private_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    target_identity_id = _safe_id(
        data.get("targetIdentityId"),
        field="targetIdentityId",
    )
    now = _now_ms()

    def mutate(current: object) -> dict:
        root = current if isinstance(current, dict) else {}
        actor = active_membership(root, uid, room_id)
        actor_identity_id = str(actor.get("identityId") or "")
        conversation_id = conversation_id_for(
            room_id,
            actor_identity_id,
            target_identity_id,
        )
        return open_private_state(
            current,
            uid=uid,
            room_id=room_id,
            target_identity_id=target_identity_id,
            conversation_id=conversation_id,
            now=now,
        )

    state = _transaction(mutate)
    actor = _peek(state, "membershipIndexByUser", uid, room_id)
    if not isinstance(actor, dict):
        _fail(
            https_fn.FunctionsErrorCode.INTERNAL,
            "Membership missing after private open.",
            room_code="INTERNAL",
        )
    conversation_id = conversation_id_for(
        room_id,
        str(actor.get("identityId") or ""),
        target_identity_id,
    )
    return {"conversationId": conversation_id, "roomId": room_id}


@https_fn.on_call(region="us-central1")
def send_room_private_v2_text(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    conversation_id = _safe_id(
        data.get("conversationId"),
        field="conversationId",
    )
    text = _validated(validate_message, data.get("text"))
    client_message_id = _validated(
        validate_client_message_id,
        data.get("clientMessageId"),
    )
    candidate_message_id = _public_id("msg")
    now = _now_ms()

    state = _transaction(
        lambda current: send_private_text_state(
            current,
            uid=uid,
            room_id=room_id,
            conversation_id=conversation_id,
            text=text,
            client_message_id=client_message_id,
            candidate_message_id=candidate_message_id,
            now=now,
            visible_lease_ms=VISIBLE_THREAD_LEASE_MS,
        )
    )
    claim = _peek(
        state,
        "private",
        "messageClaims",
        uid,
        f"private_{conversation_id}",
        client_message_id,
    )
    message_id = (
        str(claim.get("messageId") or "")
        if isinstance(claim, dict)
        else candidate_message_id
    )
    message = _peek(state, "privateMessages", conversation_id, message_id)
    if not isinstance(message, dict):
        _fail(
            https_fn.FunctionsErrorCode.INTERNAL,
            "Private message missing after transaction.",
            room_code="INTERNAL",
        )
    return message


@https_fn.on_call(region="us-central1")
def block_room_private_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    conversation_id = _safe_id(
        data.get("conversationId"),
        field="conversationId",
    )
    blocked = data.get("blocked")
    if not isinstance(blocked, bool):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "blocked must be boolean.",
            room_code="INVALID_INPUT",
        )
    now = _now_ms()
    _transaction(
        lambda current: block_private_state(
            current,
            uid=uid,
            room_id=room_id,
            conversation_id=conversation_id,
            blocked=blocked,
            now=now,
        )
    )
    return {
        "ok": True,
        "roomId": room_id,
        "conversationId": conversation_id,
        "blocked": blocked,
    }
