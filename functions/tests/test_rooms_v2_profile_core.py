from __future__ import annotations

import unittest

from functions.rooms_v2_profile_core import (
    RoomsV2ProfileError,
    build_context_profile,
    resolve_context_profile_owner_uid,
    validate_context_profile_access,
)


class RoomsV2ProfileCoreTest(unittest.TestCase):
    def test_real_profile_access_requires_active_viewer_membership(self):
        target = {
            "identityId": "id_target",
            "displayName": "Ada",
            "mode": "real",
            "active": True,
        }
        with self.assertRaises(RoomsV2ProfileError) as raised:
            validate_context_profile_access({"active": False}, target)
        self.assertEqual("PERMISSION_DENIED", raised.exception.code)

    def test_anonymous_identity_never_exposes_profile(self):
        target = {
            "identityId": "anon_target",
            "displayName": "Niebla",
            "mode": "anonymous",
            "active": True,
        }
        with self.assertRaises(RoomsV2ProfileError) as raised:
            validate_context_profile_access({"active": True}, target)
        self.assertEqual("PERMISSION_DENIED", raised.exception.code)

    def test_owner_mapping_must_be_active(self):
        with self.assertRaises(RoomsV2ProfileError) as raised:
            resolve_context_profile_owner_uid({"uid": "secret_uid", "active": False})
        self.assertEqual("NOT_FOUND", raised.exception.code)

    def test_profile_snapshot_is_strictly_whitelisted(self):
        target = {
            "identityId": "id_target",
            "displayName": "Ada anterior",
            "mode": "real",
            "active": True,
        }
        account = {
            "id": "secret_uid",
            "name": "Ada",
            "age": 31,
            "description": "Construyendo cosas.",
            "email": "ada@example.com",
            "birthDate": "1995-01-01",
            "photoUrl": "https://storage.example/Users/secret_uid/ProfilePhotos/profile.jpg",
            "latitude": -34.0,
            "longitude": -58.0,
            "isOnline": True,
        }

        profile = build_context_profile(target, account)

        self.assertEqual(
            {"identityId", "displayName", "age", "description"},
            set(profile.keys()),
        )
        self.assertEqual("id_target", profile["identityId"])
        self.assertEqual("Ada", profile["displayName"])
        self.assertEqual(31, profile["age"])
        self.assertEqual("Construyendo cosas.", profile["description"])
        serialized = repr(profile)
        self.assertNotIn("secret_uid", serialized)
        self.assertNotIn("ada@example.com", serialized)
        self.assertNotIn("storage.example", serialized)

    def test_invalid_age_is_omitted_as_zero_without_deriving_birth_date(self):
        target = {
            "identityId": "id_target",
            "displayName": "Ada",
            "mode": "real",
            "active": True,
        }
        profile = build_context_profile(
            target,
            {
                "name": "Ada",
                "age": "unknown",
                "birthDate": "1995-01-01",
            },
        )
        self.assertEqual(0, profile["age"])


if __name__ == "__main__":
    unittest.main()
