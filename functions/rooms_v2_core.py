from __future__ import annotations

import hashlib
import re
import unicodedata
from dataclasses import dataclass

ROOT = "RoomsV2"
MAX_ROOM_NAME_CHARS = 60
MIN_ROOM_NAME_CHARS = 3
MAX_DESCRIPTION_CHARS = 280
MIN_ALIAS_CHARS = 3
MAX_ALIAS_CHARS = 24
MAX_MESSAGE_CHARS = 4_000
MAX_CLIENT_MESSAGE_ID_CHARS = 120

_WHITESPACE_RE = re.compile(r"\s+")


class RoomsV2ValidationError(ValueError):
    pass


def collapse_spaces(value: object) -> str:
    return _WHITESPACE_RE.sub(" ", str(value or "").strip())


def normalize_alias(value: object) -> str:
    normalized = unicodedata.normalize("NFKC", collapse_spaces(value)).lower()
    return collapse_spaces(normalized)


def validate_room_name(value: object) -> str:
    name = collapse_spaces(value)
    if len(name) < MIN_ROOM_NAME_CHARS or len(name) > MAX_ROOM_NAME_CHARS:
        raise RoomsV2ValidationError(
            f"Room name must contain {MIN_ROOM_NAME_CHARS}-{MAX_ROOM_NAME_CHARS} characters."
        )
    if any(ord(char) < 32 for char in name):
        raise RoomsV2ValidationError("Room name contains control characters.")
    return name


def validate_description(value: object) -> str:
    description = str(value or "").strip()
    if len(description) > MAX_DESCRIPTION_CHARS:
        raise RoomsV2ValidationError(
            f"Description cannot exceed {MAX_DESCRIPTION_CHARS} characters."
        )
    if any(ord(char) < 32 and char not in "\n\r\t" for char in description):
        raise RoomsV2ValidationError("Description contains control characters.")
    return description


def _is_alias_char_allowed(char: str) -> bool:
    if char in " ._-":
        return True
    category = unicodedata.category(char)
    return category.startswith("L") or category.startswith("N")


def validate_alias(value: object) -> tuple[str, str]:
    display = unicodedata.normalize("NFKC", collapse_spaces(value))
    normalized = normalize_alias(display)
    if len(normalized) < MIN_ALIAS_CHARS or len(normalized) > MAX_ALIAS_CHARS:
        raise RoomsV2ValidationError(
            f"Alias must contain {MIN_ALIAS_CHARS}-{MAX_ALIAS_CHARS} characters."
        )
    if not normalized[0].isalnum() or any(not _is_alias_char_allowed(c) for c in normalized):
        raise RoomsV2ValidationError(
            "Alias can only contain letters, numbers, spaces, dot, hyphen and underscore."
        )
    return display, normalized


def validate_message(value: object) -> str:
    text = str(value or "").strip()
    if not text:
        raise RoomsV2ValidationError("Message cannot be empty.")
    if len(text) > MAX_MESSAGE_CHARS:
        raise RoomsV2ValidationError(f"Message cannot exceed {MAX_MESSAGE_CHARS} characters.")
    return text


def validate_client_message_id(value: object) -> str:
    client_message_id = str(value or "").strip()
    if not client_message_id or len(client_message_id) > MAX_CLIENT_MESSAGE_ID_CHARS:
        raise RoomsV2ValidationError("Invalid clientMessageId.")
    if any(char in ".#$[]/" or ord(char) < 32 for char in client_message_id):
        raise RoomsV2ValidationError("clientMessageId contains invalid Firebase path characters.")
    return client_message_id


def private_alias_key(normalized_alias: str) -> str:
    # This digest is only used under a server-only branch. It is not an anonymity boundary.
    return hashlib.sha256(normalized_alias.encode("utf-8")).hexdigest()


def new_public_id(prefix: str, token: str) -> str:
    cleaned = str(token).replace("=", "").replace("/", "_").replace("+", "-")
    return f"{prefix}_{cleaned}"


def resolve_alias_claim(current: object, *, uid: str, candidate_identity_id: str) -> tuple[dict, bool]:
    if isinstance(current, dict):
        current_uid = str(current.get("uid") or "").strip()
        identity_id = str(current.get("identityId") or "").strip()
        active = current.get("active") is True
        if active and current_uid and current_uid != uid:
            return dict(current), False
        if current_uid == uid and identity_id:
            reused = dict(current)
            reused["active"] = True
            return reused, True
    return {"uid": uid, "identityId": candidate_identity_id, "active": True}, True


def resolve_message_claim(current: object, *, candidate_message_id: str) -> tuple[dict, bool]:
    if isinstance(current, dict):
        message_id = str(current.get("messageId") or "").strip()
        if message_id:
            return dict(current), False
    return {"messageId": candidate_message_id}, True


@dataclass(frozen=True)
class MembershipProjection:
    room_id: str
    identity_id: str
    display_name: str
    mode: str
    role: str
    active: bool
    joined_at: int
    last_read_at: int = 0

    def as_dict(self) -> dict:
        return {
            "roomId": self.room_id,
            "identityId": self.identity_id,
            "displayName": self.display_name,
            "mode": self.mode,
            "role": self.role,
            "active": self.active,
            "joinedAt": self.joined_at,
            "lastReadAt": self.last_read_at,
        }
