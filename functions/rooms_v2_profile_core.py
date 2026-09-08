from __future__ import annotations


class RoomsV2ProfileError(ValueError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def _fail(code: str, message: str) -> None:
    raise RoomsV2ProfileError(code, message)


def validate_context_profile_access(
    viewer_membership: object,
    target_identity: object,
) -> dict:
    if not isinstance(viewer_membership, dict) or viewer_membership.get("active") is not True:
        _fail("PERMISSION_DENIED", "Active room membership is required.")

    if not isinstance(target_identity, dict) or target_identity.get("active") is not True:
        _fail("NOT_FOUND", "Target room identity is not active.")

    if str(target_identity.get("mode") or "").strip().lower() != "real":
        _fail("PERMISSION_DENIED", "Anonymous identities do not expose a profile.")

    identity_id = str(target_identity.get("identityId") or "").strip()
    if not identity_id:
        _fail("INTERNAL", "Target identity is missing identityId.")

    return target_identity


def resolve_context_profile_owner_uid(owner_mapping: object) -> str:
    if not isinstance(owner_mapping, dict) or owner_mapping.get("active") is not True:
        _fail("NOT_FOUND", "Identity ownership mapping is not active.")

    uid = str(owner_mapping.get("uid") or "").strip()
    if not uid:
        _fail("INTERNAL", "Identity ownership mapping is missing uid.")
    return uid


def build_context_profile(target_identity: dict, account: object) -> dict:
    if not isinstance(account, dict):
        _fail("NOT_FOUND", "Public profile is unavailable.")

    identity_id = str(target_identity.get("identityId") or "").strip()
    if not identity_id:
        _fail("INTERNAL", "Target identity is missing identityId.")

    display_name = str(account.get("name") or "").strip()[:80]
    if not display_name:
        display_name = str(target_identity.get("displayName") or "").strip()[:80]
    if not display_name:
        _fail("NOT_FOUND", "Public profile has no display name.")

    try:
        age = int(account.get("age") or 0)
    except (TypeError, ValueError):
        age = 0
    if age < 18 or age > 120:
        age = 0

    description = str(account.get("description") or "").strip()[:500]

    # Deliberate whitelist. Never add uid/id, email, birthDate, coordinates or photoUrl here.
    return {
        "identityId": identity_id,
        "displayName": display_name,
        "age": age,
        "description": description,
    }
