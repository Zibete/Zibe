from __future__ import annotations

import importlib
import sys
import types
import unittest
from unittest.mock import MagicMock, patch


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
            self.main._parse_other_uid_from_chat_id("alice_team|bob", "bob"),
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

    def test_safe_id_never_returns_complete_short_identifier(self):
        self.assertEqual("ali...ce", self.main._safe_id("alice"))
        self.assertEqual("***", self.main._safe_id("uid"))

    def test_read_int_rejects_missing_and_invalid_values(self):
        self.assertEqual(3, self.main._read_int("3"))
        self.assertIsNone(self.main._read_int(None))
        self.assertIsNone(self.main._read_int("invalid"))


class RoomMessageContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = _load_main_module()

    @staticmethod
    def _event(*, room_key="room-key", message_id="message-id", data=None):
        return types.SimpleNamespace(
            params={"roomKey": room_key, "messageId": message_id},
            data=data,
        )

    @staticmethod
    def _message(**overrides):
        message = {
            "senderUid": "sender",
            "nameUser": "Visible alias",
            "content": "Hello room",
            "chatType": 100,
        }
        message.update(overrides)
        return message

    def _run_trigger(
        self,
        *,
        members,
        message=None,
        tokens=None,
        active_uids=None,
        send_side_effect=None,
        room_key="room-key",
        message_id="message-id",
    ):
        tokens = tokens or {}
        active_uids = active_uids or set()
        members_ref = MagicMock()
        members_ref.get.return_value = members

        with (
            patch.object(self.main.db, "reference", return_value=members_ref),
            patch.object(
                self.main,
                "_get_user_token",
                side_effect=lambda uid: tokens.get(uid),
            ) as get_token,
            patch.object(
                self.main,
                "_is_receiver_in_active_room",
                side_effect=lambda uid, **_kwargs: uid in active_uids,
            ) as is_active,
            patch.object(
                self.main,
                "_send_push",
                side_effect=send_side_effect,
            ) as send_push,
        ):
            self.main.on_group_message_created(
                self._event(
                    room_key=room_key,
                    message_id=message_id,
                    data=message or self._message(),
                )
            )

        return get_token, is_active, send_push

    def test_recipient_selection_excludes_sender(self):
        recipients = self.main._room_recipient_uids(
            {"sender": {}, "member-a": {}, "member-b": {}},
            "sender",
        )

        self.assertEqual(["member-a", "member-b"], recipients)

    def test_recipient_selection_uses_current_members_only(self):
        recipients = self.main._room_recipient_uids(
            {"sender": {}, "current-member": {}},
            "sender",
        )

        self.assertEqual(["current-member"], recipients)
        self.assertNotIn("former-member", recipients)

    def test_trigger_sends_data_only_to_current_members(self):
        _, _, send_push = self._run_trigger(
            members={"sender": {}, "member-a": {}, "member-b": {}},
            tokens={"member-a": "token-a", "member-b": "token-b"},
        )

        self.assertEqual(2, send_push.call_count)
        self.assertEqual(
            {"token-a", "token-b"},
            {call.kwargs["token"] for call in send_push.call_args_list},
        )
        for call in send_push.call_args_list:
            self.assertFalse(call.kwargs["include_notification"])
            self.assertEqual("room", call.kwargs["data_payload"]["type"])

    def test_active_room_accepts_room_key_or_target_id_with_fresh_lease(self):
        room_key_thread = {
            "nodeType": "room",
            "roomKey": "legacy-key",
            "updatedAt": 1_000,
        }
        room_id_thread = {
            "nodeType": "room",
            "targetId": "stable-room-id",
            "updatedAt": 1_000,
        }

        self.assertTrue(
            self.main._is_active_room_thread(
                room_key_thread,
                room_key="legacy-key",
                now_ms=121_000,
            )
        )
        self.assertTrue(
            self.main._is_active_room_thread(
                room_id_thread,
                room_key="legacy-key",
                room_id="stable-room-id",
                now_ms=121_000,
            )
        )
        self.assertFalse(
            self.main._is_active_room_thread(
                room_id_thread,
                room_key="legacy-key",
                room_id="stable-room-id",
                now_ms=121_001,
            )
        )

    def test_trigger_skips_member_with_active_room(self):
        _, _, send_push = self._run_trigger(
            members={"sender": {}, "active": {}, "inactive": {}},
            tokens={"active": "token-active", "inactive": "token-inactive"},
            active_uids={"active"},
        )

        send_push.assert_called_once()
        self.assertEqual("token-inactive", send_push.call_args.kwargs["token"])

    def test_trigger_skips_member_without_token(self):
        get_token, _, send_push = self._run_trigger(
            members={"sender": {}, "missing-token": {}},
        )

        get_token.assert_called_once_with("missing-token")
        send_push.assert_not_called()

    def test_text_payload_has_explicit_room_contract(self):
        message = self.main._normalize_room_message(
            self._message(roomId="stable-room-id", roomName="Visible room")
        )

        payload = self.main._build_room_payload(
            room_key="legacy-room-key",
            message_id="message-id",
            message=message,
        )

        self.assertEqual(
            {
                "type": "room",
                "roomKey": "legacy-room-key",
                "roomId": "stable-room-id",
                "roomName": "Visible room",
                "messageId": "message-id",
                "senderName": "Visible alias",
                "messageType": 100,
                "preview": "Hello room",
            },
            payload,
        )
        self.assertNotIn("senderUid", payload)

    def test_message_normalization_accepts_current_user_name_key(self):
        message = self._message(nameUser="", userName="Current alias")

        normalized = self.main._normalize_room_message(message)

        self.assertEqual("Current alias", normalized["senderName"])

    def test_media_payload_uses_safe_preview_fallback(self):
        message = self.main._normalize_room_message(
            self._message(
                content="https://storage.example/private-room-image",
                chatType=200,
            )
        )

        payload = self.main._build_room_payload(
            room_key="room-key",
            message_id="message-id",
            message=message,
        )

        self.assertEqual(self.main.ROOM_MEDIA_PREVIEW, payload["preview"])
        self.assertNotIn("https://", payload["preview"])

    def test_invalid_message_shape_is_rejected_before_membership_read(self):
        invalid_messages = (
            self._message(senderUid=""),
            self._message(nameUser=""),
            self._message(content=""),
            self._message(chatType="invalid"),
        )

        for invalid_message in invalid_messages:
            with self.subTest(message=invalid_message):
                with (
                    patch.object(self.main.db, "reference") as reference,
                    patch.object(self.main, "_send_push") as send_push,
                    patch.object(self.main.logger, "warning"),
                ):
                    self.main.on_group_message_created(
                        self._event(data=invalid_message)
                    )

                reference.assert_not_called()
                send_push.assert_not_called()

    def test_fcm_error_is_isolated_per_recipient(self):
        with patch.object(self.main.logger, "exception") as log_exception:
            _, _, send_push = self._run_trigger(
                members={"sender": {}, "member-a": {}, "member-b": {}},
                tokens={"member-a": "token-a", "member-b": "token-b"},
                send_side_effect=(RuntimeError("fcm unavailable"), "message-id"),
            )

        self.assertEqual(2, send_push.call_count)
        log_exception.assert_called_once()

    def test_error_logs_redact_ids_and_never_include_content(self):
        room_key = "room-key-abcdefghijklmnopqrstuvwxyz"
        message_id = "message-id-abcdefghijklmnopqrstuvwxyz"
        receiver_uid = "receiver-uid-abcdefghijklmnopqrstuvwxyz"
        secret_content = "private room content must not be logged"

        with patch.object(self.main.logger, "exception") as log_exception:
            self._run_trigger(
                members={"sender": {}, receiver_uid: {}},
                message=self._message(content=secret_content),
                tokens={receiver_uid: "token"},
                send_side_effect=RuntimeError("fcm unavailable"),
                room_key=room_key,
                message_id=message_id,
            )

        rendered_log_call = str(log_exception.call_args)
        self.assertNotIn(room_key, rendered_log_call)
        self.assertNotIn(message_id, rendered_log_call)
        self.assertNotIn(receiver_uid, rendered_log_call)
        self.assertNotIn(secret_content, rendered_log_call)
        self.assertIn(self.main._safe_id(room_key), rendered_log_call)


if __name__ == "__main__":
    unittest.main()
