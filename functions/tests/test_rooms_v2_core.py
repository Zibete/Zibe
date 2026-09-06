import unittest

from functions.rooms_v2_core import (
    RoomsV2ValidationError,
    normalize_alias,
    private_alias_key,
    resolve_alias_claim,
    resolve_message_claim,
    validate_alias,
    validate_client_message_id,
    validate_message,
    validate_room_name,
)


class RoomsV2CoreTest(unittest.TestCase):
    def test_alias_normalization_is_stable(self):
        display, normalized = validate_alias("  ÁlIaS   Uno  ")
        self.assertEqual("ÁlIaS Uno", display)
        self.assertEqual("álias uno", normalized)
        self.assertEqual(normalized, normalize_alias("ÁlIaS Uno"))

    def test_alias_rejects_path_characters(self):
        with self.assertRaises(RoomsV2ValidationError):
            validate_alias("alias/profile")

    def test_alias_digest_is_path_safe_and_deterministic(self):
        first = private_alias_key("álias uno")
        second = private_alias_key("álias uno")
        self.assertEqual(first, second)
        self.assertEqual(64, len(first))
        self.assertNotRegex(first, r"[.#$\[\]/]")

    def test_room_and_message_limits(self):
        self.assertEqual("Sala Norte", validate_room_name("  Sala   Norte "))
        self.assertEqual("hola", validate_message(" hola "))
        with self.assertRaises(RoomsV2ValidationError):
            validate_room_name("ab")
        with self.assertRaises(RoomsV2ValidationError):
            validate_message(" ")

    def test_alias_claim_reuses_same_identity_and_rejects_other_account(self):
        candidate, allowed = resolve_alias_claim(None, uid="user-a", candidate_identity_id="anon-one")
        self.assertTrue(allowed)
        reused, allowed = resolve_alias_claim(candidate, uid="user-a", candidate_identity_id="anon-two")
        self.assertTrue(allowed)
        self.assertEqual("anon-one", reused["identityId"])
        occupied, allowed = resolve_alias_claim(candidate, uid="user-b", candidate_identity_id="anon-three")
        self.assertFalse(allowed)
        self.assertEqual("user-a", occupied["uid"])

    def test_message_claim_is_idempotent(self):
        first, created = resolve_message_claim(None, candidate_message_id="msg-one")
        self.assertTrue(created)
        second, created = resolve_message_claim(first, candidate_message_id="msg-two")
        self.assertFalse(created)
        self.assertEqual("msg-one", second["messageId"])

    def test_client_message_id_is_firebase_path_safe(self):
        self.assertEqual("android-001", validate_client_message_id("android-001"))
        with self.assertRaises(RoomsV2ValidationError):
            validate_client_message_id("bad/id")


if __name__ == "__main__":
    unittest.main()
