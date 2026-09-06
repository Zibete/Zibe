from __future__ import annotations

import unittest

from functions.rooms_v2_notification_core import (
    private_room_recipient,
    public_room_recipients,
    safe_text_preview,
)


class RoomsV2NotificationCoreTest(unittest.TestCase):
    def fixture(self) -> dict:
        return {
            "publicRooms": {
                "room-1": {"status": "open"},
            },
            "membershipIndexByUser": {
                "sender": {
                    "room-1": {
                        "active": True,
                        "identityId": "sender-id",
                        "notificationsEnabled": True,
                    }
                },
                "receiver": {
                    "room-1": {
                        "active": True,
                        "identityId": "receiver-id",
                        "notificationsEnabled": True,
                    }
                },
                "muted": {
                    "room-1": {
                        "active": True,
                        "identityId": "muted-id",
                        "notificationsEnabled": False,
                    }
                },
                "former": {
                    "room-1": {
                        "active": False,
                        "identityId": "former-id",
                        "notificationsEnabled": True,
                    }
                },
            },
            "private": {
                "identityOwners": {
                    "room-1": {
                        "sender-id": {"uid": "sender", "active": True},
                        "receiver-id": {"uid": "receiver", "active": True},
                    }
                },
                "bans": {"room-1": {}},
                "visibleThreads": {},
                "conversations": {
                    "conv-1": {
                        "conversationId": "conv-1",
                        "roomId": "room-1",
                        "closed": False,
                        "blockedBy": {},
                        "participants": {
                            "sender": {"identityId": "sender-id"},
                            "receiver": {"identityId": "receiver-id"},
                        },
                    }
                },
                "conversationReaders": {
                    "conv-1": {"sender": True, "receiver": True},
                },
            },
        }

    def test_public_room_excludes_sender_muted_former_and_banned(self):
        state = self.fixture()
        self.assertEqual(
            ["receiver"],
            public_room_recipients(
                state,
                room_id="room-1",
                sender_identity_id="sender-id",
                now_ms=10_000,
                lease_ms=120_000,
            ),
        )
        state["private"]["bans"]["room-1"]["receiver"] = {"createdAt": 1}
        self.assertEqual(
            [],
            public_room_recipients(
                state,
                room_id="room-1",
                sender_identity_id="sender-id",
                now_ms=10_000,
                lease_ms=120_000,
            ),
        )

    def test_public_room_suppresses_fresh_visible_thread_only(self):
        state = self.fixture()
        state["private"]["visibleThreads"]["receiver"] = {
            "roomId": "room-1",
            "updatedAt": 1_000,
        }
        self.assertEqual(
            [],
            public_room_recipients(
                state,
                room_id="room-1",
                sender_identity_id="sender-id",
                now_ms=121_000,
                lease_ms=120_000,
            ),
        )
        self.assertEqual(
            ["receiver"],
            public_room_recipients(
                state,
                room_id="room-1",
                sender_identity_id="sender-id",
                now_ms=121_001,
                lease_ms=120_000,
            ),
        )

    def test_private_room_selects_only_other_contextual_participant(self):
        state = self.fixture()
        self.assertEqual(
            "receiver",
            private_room_recipient(
                state,
                room_id="room-1",
                conversation_id="conv-1",
                sender_identity_id="sender-id",
                now_ms=10_000,
                lease_ms=120_000,
            ),
        )

    def test_private_room_denies_closed_blocked_inactive_banned_or_revoked_reader(self):
        for mutator in (
            lambda state: state["private"]["conversations"]["conv-1"].update(closed=True),
            lambda state: state["private"]["conversations"]["conv-1"]["blockedBy"].update(receiver=True),
            lambda state: state["membershipIndexByUser"]["receiver"]["room-1"].update(active=False),
            lambda state: state["private"]["bans"]["room-1"].update(receiver={"createdAt": 1}),
            lambda state: state["private"]["conversationReaders"]["conv-1"].update(receiver=False),
        ):
            state = self.fixture()
            mutator(state)
            with self.subTest(state=state):
                self.assertIsNone(
                    private_room_recipient(
                        state,
                        room_id="room-1",
                        conversation_id="conv-1",
                        sender_identity_id="sender-id",
                        now_ms=10_000,
                        lease_ms=120_000,
                    )
                )

    def test_private_room_suppresses_fresh_matching_visible_thread(self):
        state = self.fixture()
        state["private"]["visibleThreads"]["receiver"] = {
            "roomId": "room-1",
            "conversationId": "conv-1",
            "updatedAt": 1_000,
        }
        self.assertIsNone(
            private_room_recipient(
                state,
                room_id="room-1",
                conversation_id="conv-1",
                sender_identity_id="sender-id",
                now_ms=121_000,
                lease_ms=120_000,
            )
        )

    def test_payload_preview_is_bounded_and_never_requires_a_media_url(self):
        self.assertEqual("hello world", safe_text_preview(" hello   world ", fallback="message"))
        self.assertEqual("message", safe_text_preview("", fallback="message"))
        self.assertEqual("abc", safe_text_preview("abcdef", fallback="message", limit=3))


if __name__ == "__main__":
    unittest.main()
