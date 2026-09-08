from __future__ import annotations

import json
import unittest

from functions.rooms_v2_events import (
    create_room_with_event_state,
    join_room_with_event_state,
    leave_room_with_event_state,
    should_notify_public_room_message,
)


class RoomsV2EventsTest(unittest.TestCase):
    def create_room(self, *, now: int = 1):
        return create_room_with_event_state(
            {},
            uid="owner-account",
            display_name="Owner",
            name="Sala Uno",
            normalized_name="sala uno",
            name_key="room-name-key",
            description="fixture",
            operation_id="create-op",
            room_id="room-1",
            identity_id="owner-identity",
            now=now,
            public_profile_id="owner-account",
            event_message_id="event-create",
            visible_lease_ms=120_000,
        )

    def join_member(self, root, *, now: int = 2, event_id: str = "event-join"):
        return join_room_with_event_state(
            root,
            uid="member-account",
            room_id="room-1",
            mode="real",
            display_name="Member",
            identity_key="real",
            candidate_identity_id="member-identity",
            now=now,
            public_profile_id="member-account",
            alias_claim_key=None,
            event_message_id=event_id,
            visible_lease_ms=120_000,
        )

    def test_create_event_is_uid_free_and_operation_retry_is_idempotent(self):
        root = self.create_room()
        message = root["publicMessages"]["room-1"]["event-create"]

        self.assertEqual("event", message["kind"])
        self.assertEqual("Owner creó la sala", message["text"])
        self.assertNotIn("owner-account", json.dumps(message))
        self.assertEqual(1, root["publicRooms"]["room-1"]["lastSeq"])
        self.assertEqual(
            1,
            root["membershipIndexByUser"]["owner-account"]["room-1"]["lastReadSeq"],
        )

        retry = create_room_with_event_state(
            root,
            uid="owner-account",
            display_name="Owner",
            name="Sala Uno",
            normalized_name="sala uno",
            name_key="room-name-key",
            description="fixture",
            operation_id="create-op",
            room_id="replacement-room",
            identity_id="replacement-identity",
            now=5,
            public_profile_id="owner-account",
            event_message_id="event-duplicate",
            visible_lease_ms=120_000,
        )
        self.assertEqual(1, retry["publicRooms"]["room-1"]["lastSeq"])
        self.assertNotIn("event-duplicate", retry["publicMessages"]["room-1"])

    def test_join_event_marks_actor_read_and_increments_other_member_unread(self):
        root = self.join_member(self.create_room())
        message = root["publicMessages"]["room-1"]["event-join"]

        self.assertEqual("Member se unió a la sala", message["text"])
        self.assertEqual(2, message["seq"])
        self.assertEqual(
            2,
            root["membershipIndexByUser"]["member-account"]["room-1"]["lastReadSeq"],
        )
        self.assertEqual(
            1,
            root["membershipIndexByUser"]["owner-account"]["room-1"]["unreadCount"],
        )

    def test_join_retry_does_not_duplicate_event(self):
        root = self.join_member(self.create_room())
        retry = self.join_member(root, now=3, event_id="event-join-duplicate")

        self.assertEqual(2, retry["publicRooms"]["room-1"]["lastSeq"])
        self.assertNotIn(
            "event-join-duplicate",
            retry["publicMessages"]["room-1"],
        )

    def test_visible_member_does_not_receive_unread_for_presence_event(self):
        root = self.create_room(now=1)
        root.setdefault("private", {}).setdefault("visibleThreads", {})[
            "owner-account"
        ] = {"roomId": "room-1", "updatedAt": 2}
        root = self.join_member(root, now=2)

        owner = root["membershipIndexByUser"]["owner-account"]["room-1"]
        self.assertEqual(0, owner["unreadCount"])
        self.assertEqual(2, owner["lastReadSeq"])

    def test_leave_event_keeps_history_and_does_not_count_for_leaver(self):
        root = self.join_member(self.create_room())
        root = leave_room_with_event_state(
            root,
            uid="member-account",
            room_id="room-1",
            now=3,
            event_message_id="event-leave",
            visible_lease_ms=120_000,
        )

        message = root["publicMessages"]["room-1"]["event-leave"]
        self.assertEqual("Member salió de la sala", message["text"])
        self.assertEqual(3, message["seq"])
        self.assertFalse(
            root["membershipIndexByUser"]["member-account"]["room-1"]["active"]
        )
        self.assertIn("event-join", root["publicMessages"]["room-1"])

    def test_presence_events_are_never_push_notifiable(self):
        self.assertFalse(should_notify_public_room_message({"kind": "event"}))
        self.assertTrue(should_notify_public_room_message({"kind": "text"}))
        self.assertTrue(should_notify_public_room_message({}))
        self.assertFalse(should_notify_public_room_message(None))


if __name__ == "__main__":
    unittest.main()
