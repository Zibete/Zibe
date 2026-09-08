from __future__ import annotations

import unittest

from functions.rooms_v2_notification_core import room_notification_name


class RoomsV2NotificationNameTests(unittest.TestCase):
    def test_returns_room_name_without_exposing_other_room_data(self) -> None:
        state = {
            "publicRooms": {
                "room_1": {
                    "roomId": "room_1",
                    "name": "  Sala   Kotlin  ",
                    "description": "private-to-notification-payload",
                    "ownerIdentityId": "identity_1",
                }
            },
            "private": {
                "identityOwners": {
                    "room_1": {"identity_1": {"uid": "secret_uid"}}
                }
            },
        }

        self.assertEqual(
            "Sala Kotlin",
            room_notification_name(state, room_id="room_1"),
        )

    def test_falls_back_when_room_or_name_is_missing(self) -> None:
        self.assertEqual(
            "Sala",
            room_notification_name({}, room_id="missing"),
        )
        self.assertEqual(
            "Sala",
            room_notification_name(
                {"publicRooms": {"room_1": {"name": "   "}}},
                room_id="room_1",
            ),
        )

    def test_caps_notification_room_name(self) -> None:
        self.assertEqual(
            "x" * 80,
            room_notification_name(
                {"publicRooms": {"room_1": {"name": "x" * 120}}},
                room_id="room_1",
            ),
        )


if __name__ == "__main__":
    unittest.main()
