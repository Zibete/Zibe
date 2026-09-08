from __future__ import annotations

import hashlib
import secrets
import time
import unicodedata
from typing import Any, Callable

from firebase_admin import db
from firebase_functions import https_fn

if __package__:
    from .rooms_v2_core import (
        RoomsV2ValidationError,
        private_alias_key,
        validate_alias,
        validate_client_message_id,
        validate_description,
        validate_message,
        validate_room_name,
    )
    from .rooms_v2_events import (
        create_room_with_event_state,
        join_room_with_event_state,
        leave_room_with_event_state,
    )
    from .rooms_v2_reply_state import send_public_text_with_reply_state
    from .rooms_v2_state import (
        MODE_ANONYMOUS,
        MODE_REAL,
        RoomsV2StateError,
        mark_read_state,
        set_notifications_state,
        set_visible_thread_state,
    )
else:
    from rooms_v2_core import (
        RoomsV2ValidationError,
        private_alias_key,
        validate_alias,
        validate_client_message_id,
        validate_description,
        validate_message,
        validate_room_name,
    )
    from rooms_v2_events import (
        create_room_with_event_state,
        join_room_with_event_state,
        leave_room_with_event_state,
    )
    from rooms_v2_reply_state import send_public_text_with_reply_state
    from rooms_v2_state import (
        MODE_ANONYMOUS,
        MODE_REAL,
        RoomsV2StateError,
        mark_read_state,
        set_notifications_state,
        set_visible_thread_state,
    )

ROOT = "RoomsV2"
VISIBLE_THREAD_LEASE_MS = 120_000
MAX_ID_CHARS = 120


def _now_ms() -> int:
    return int(time.time() * 1000)


def _fail(
    code: https_fn.FunctionsErrorCode,
    message: str,
    *,
    room_code: str | None = None,
) -> None:
    details = {"roomV2Code": room_code} if room_code else None
    raise https_fn.HttpsError(code, message, details)


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


def _validated(fn: Callable, value: object):
    try:
        return fn(value)
    except RoomsV2ValidationError as exc:
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            str(exc),
            room_code="INVALID_INPUT",
        )


def _safe_id(value: object, *, field: str = "id") -> str:
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


def _profile_name(uid: str) -> str:
    value = db.reference(f"Users/Accounts/{uid}/name").get()
    name = str(value or "").strip()
    if not name:
        _fail(
            https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            "A real public profile is required.",
            room_code="CONFLICT",
        )
    return name[:80]


def _normalize_room_name(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    without_marks = "".join(c for c in decomposed if not unicodedata.combining(c))
    return " ".join(without_marks.lower().split())


def _room_name_key(normalized_name: str) -> str:
    return hashlib.sha256(normalized_name.encode("utf-8")).hexdigest()


def _public_id(prefix: str) -> str:
    token = secrets.token_urlsafe(16).replace("=", "").replace("/", "_").replace("+", "-")
    return f"{prefix}_{token}"


def _state_error(exc: RoomsV2StateError) -> None:
    mapping = {
        "UNAUTHENTICATED": https_fn.FunctionsErrorCode.UNAUTHENTICATED,
        "PERMISSION_DENIED": https_fn.FunctionsErrorCode.PERMISSION_DENIED,
        "ALIAS_TAKEN": https_fn.FunctionsErrorCode.ALREADY_EXISTS,
        "ROOM_NAME_TAKEN": https_fn.FunctionsErrorCode.ALREADY_EXISTS,
        "ROOM_CLOSED": https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
        "IDENTITY_CHANGE_REQUIRED": https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
        "OWNER_ACTION_REQUIRED": https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
        "NOT_FOUND": https_fn.FunctionsErrorCode.NOT_FOUND,
        "INVALID_INPUT": https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
        "CONFLICT": https_fn.FunctionsErrorCode.ABORTED,
    }
    _fail(
        mapping.get(exc.code, https_fn.FunctionsErrorCode.INTERNAL),
        str(exc),
        room_code=exc.code,
    )


def _transaction(mutator: Callable[[object], dict]) -> dict:
    try:
        value = db.reference(ROOT).transaction(mutator)
    except RoomsV2StateError as exc:
        _state_error(exc)
    if not isinstance(value, dict):
        _fail(
            https_fn.FunctionsErrorCode.INTERNAL,
            "RoomsV2 transaction returned an invalid state.",
            room_code="INTERNAL",
        )
    return value


def _peek(root: dict, *parts: str) -> Any:
    cursor: Any = root
    for part in parts:
        if not isinstance(cursor, dict):
            return None
        cursor = cursor.get(part)
    return cursor


def _membership(root: dict, uid: str, room_id: str) -> dict:
    value = _peek(root, "membershipIndexByUser", uid, room_id)
    if not isinstance(value, dict):
        _fail(
            https_fn.FunctionsErrorCode.INTERNAL,
            "Membership projection missing after transaction.",
            room_code="INTERNAL",
        )
    return value


@https_fn.on_call(region="us-central1")
def create_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    name = _validated(validate_room_name, data.get("name"))
    description = _validated(validate_description, data.get("description"))
    operation_id = _validated(validate_client_message_id, data.get("operationId"))
    display_name = _profile_name(uid)
    normalized_name = _normalize_room_name(name)
    name_key = _room_name_key(normalized_name)
    room_id = _public_id("room")
    identity_id = _public_id("id")
    event_message_id = _public_id("event")
    now = _now_ms()

    state = _transaction(
        lambda current: create_room_with_event_state(
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
            public_profile_id=uid,
            event_message_id=event_message_id,
            visible_lease_ms=VISIBLE_THREAD_LEASE_MS,
        )
    )
    operation = _peek(state, "private", "createOperations", uid, operation_id)
    committed_room_id = (
        str(operation.get("roomId") or "")
        if isinstance(operation, dict)
        else room_id
    )
    return {"membership": _membership(state, uid, committed_room_id)}


@https_fn.on_call(region="us-central1")
def join_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    mode = str(data.get("mode") or MODE_REAL).strip().lower()
    now = _now_ms()

    if mode == MODE_REAL:
        display_name = _profile_name(uid)
        identity_key = "real"
        public_profile_id: str | None = uid
        alias_claim_key: str | None = None
    elif mode == MODE_ANONYMOUS:
        display_name, normalized_alias = _validated(validate_alias, data.get("alias"))
        alias_claim_key = private_alias_key(normalized_alias)
        identity_key = f"anon_{alias_claim_key}"
        public_profile_id = None
    else:
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "Unknown identity mode.",
            room_code="INVALID_INPUT",
        )

    candidate_identity_id = _public_id("anon" if mode == MODE_ANONYMOUS else "id")
    event_message_id = _public_id("event")
    state = _transaction(
        lambda current: join_room_with_event_state(
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
            event_message_id=event_message_id,
            visible_lease_ms=VISIBLE_THREAD_LEASE_MS,
        )
    )
    return {"membership": _membership(state, uid, room_id)}


@https_fn.on_call(region="us-central1")
def leave_room_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    room_id = _safe_id(_payload(request).get("roomId"), field="roomId")
    now = _now_ms()
    event_message_id = _public_id("event")
    _transaction(
        lambda current: leave_room_with_event_state(
            current,
            uid=uid,
            room_id=room_id,
            now=now,
            event_message_id=event_message_id,
            visible_lease_ms=VISIBLE_THREAD_LEASE_MS,
        )
    )
    return {"ok": True, "roomId": room_id}


@https_fn.on_call(region="us-central1")
def set_room_notifications_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    enabled = data.get("enabled")
    if not isinstance(enabled, bool):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "enabled must be boolean.",
            room_code="INVALID_INPUT",
        )
    _transaction(
        lambda current: set_notifications_state(
            current,
            uid=uid,
            room_id=room_id,
            enabled=enabled,
        )
    )
    return {"ok": True, "roomId": room_id, "enabled": enabled}


@https_fn.on_call(region="us-central1")
def send_room_v2_text(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    text = _validated(validate_message, data.get("text"))
    client_message_id = _validated(
        validate_client_message_id,
        data.get("clientMessageId"),
    )
    reply_value = data.get("replyToMessageId")
    reply_to_message_id = (
        _safe_id(reply_value, field="replyToMessageId")
        if reply_value not in (None, "")
        else None
    )
    candidate_message_id = _public_id("msg")
    now = _now_ms()

    state = _transaction(
        lambda current: send_public_text_with_reply_state(
            current,
            uid=uid,
            room_id=room_id,
            text=text,
            client_message_id=client_message_id,
            candidate_message_id=candidate_message_id,
            reply_to_message_id=reply_to_message_id,
            now=now,
            visible_lease_ms=VISIBLE_THREAD_LEASE_MS,
        )
    )
    claim = _peek(
        state,
        "private",
        "messageClaims",
        uid,
        f"room_{room_id}",
        client_message_id,
    )
    message_id = (
        str(claim.get("messageId") or "")
        if isinstance(claim, dict)
        else candidate_message_id
    )
    message = _peek(state, "publicMessages", room_id, message_id)
    if not isinstance(message, dict):
        _fail(
            https_fn.FunctionsErrorCode.INTERNAL,
            "Message missing after transaction.",
            room_code="INTERNAL",
        )
    return message


@https_fn.on_call(region="us-central1")
def mark_room_thread_read_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    conversation_value = data.get("conversationId")
    conversation_id = (
        _safe_id(conversation_value, field="conversationId")
        if conversation_value not in (None, "")
        else None
    )
    try:
        visible_seq = int(data.get("visibleSeq") or 0)
    except (TypeError, ValueError):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "visibleSeq must be an integer.",
            room_code="INVALID_INPUT",
        )
    now = _now_ms()
    _transaction(
        lambda current: mark_read_state(
            current,
            uid=uid,
            room_id=room_id,
            conversation_id=conversation_id,
            visible_seq=visible_seq,
            now=now,
        )
    )
    return {"ok": True, "roomId": room_id, "visibleSeq": visible_seq}


@https_fn.on_call(region="us-central1")
def set_room_visible_thread_v2(request: https_fn.CallableRequest) -> dict:
    uid = _uid(request)
    data = _payload(request)
    room_id = _safe_id(data.get("roomId"), field="roomId")
    conversation_value = data.get("conversationId")
    conversation_id = (
        _safe_id(conversation_value, field="conversationId")
        if conversation_value not in (None, "")
        else None
    )
    visible = data.get("visible")
    if not isinstance(visible, bool):
        _fail(
            https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            "visible must be boolean.",
            room_code="INVALID_INPUT",
        )
    now = _now_ms()
    _transaction(
        lambda current: set_visible_thread_state(
            current,
            uid=uid,
            room_id=room_id,
            conversation_id=conversation_id,
            visible=visible,
            now=now,
        )
    )
    return {
        "ok": True,
        "roomId": room_id,
        "conversationId": conversation_id,
        "visible": visible,
    }
