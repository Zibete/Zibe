from __future__ import annotations

import importlib
import sys
import types
import unittest
from unittest.mock import MagicMock


def _load_main_module():
    firebase_functions = types.ModuleType("firebase_functions")
    db_fn = types.SimpleNamespace()

    class Event:
        @classmethod
        def __class_getitem__(cls, _item):
            return cls

    class DataSnapshot:
        pass

    db_fn.Event = Event
    db_fn.DataSnapshot = DataSnapshot
    db_fn.on_value_created = lambda **_kwargs: lambda function: function
    firebase_functions.db_fn = db_fn

    firebase_admin = types.ModuleType("firebase_admin")
    firebase_admin.initialize_app = MagicMock()
    firebase_admin.messaging = MagicMock()
    firebase_admin.db = MagicMock()

    with unittest.mock.patch.dict(
        sys.modules,
        {
            "firebase_functions": firebase_functions,
            "firebase_admin": firebase_admin,
        },
    ):
        sys.modules.pop("functions.main", None)
        return importlib.import_module("functions.main")


class DirectMessageContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = _load_main_module()

    def test_parse_other_uid_resolves_both_participants(self):
        self.assertEqual(
            "bob",
            self.main._parse_other_uid_from_chat_id("alice_bob", "alice"),
        )
        self.assertEqual(
            "alice",
            self.main._parse_other_uid_from_chat_id("alice_bob", "bob"),
        )

    def test_parse_other_uid_supports_underscores(self):
        self.assertEqual(
            "alice_team",
            self.main._parse_other_uid_from_chat_id("alice_team_bob", "bob"),
        )

    def test_active_dm_requires_fresh_lease(self):
        self.main._get_active_thread = MagicMock(
            return_value={
                "nodeType": "dm",
                "otherUid": "alice",
                "updatedAt": 1_000,
            }
        )

        self.assertTrue(
            self.main._is_receiver_in_active_dm("bob", "alice", now_ms=121_000)
        )
        self.assertFalse(
            self.main._is_receiver_in_active_dm("bob", "alice", now_ms=121_001)
        )

    def test_visible_content_uses_text_and_hides_media_url(self):
        self.assertEqual(
            "hello",
            self.main._get_visible_dm_content({"type": 100, "content": "hello"}),
        )
        self.assertEqual(
            self.main.DM_VISIBLE_CONTENT_FALLBACK,
            self.main._get_visible_dm_content(
                {"type": 200, "content": "https://storage.example/private"}
            ),
        )

    def test_safe_id_redacts_long_identifiers(self):
        value = "abcdefghijklmnopqrstuvwxyz"

        safe_value = self.main._safe_id(value)

        self.assertNotEqual(value, safe_value)
        self.assertEqual("abcdef...wxyz", safe_value)

    def test_read_int_rejects_missing_and_invalid_values(self):
        self.assertEqual(3, self.main._read_int("3"))
        self.assertIsNone(self.main._read_int(None))
        self.assertIsNone(self.main._read_int("invalid"))


if __name__ == "__main__":
    unittest.main()
