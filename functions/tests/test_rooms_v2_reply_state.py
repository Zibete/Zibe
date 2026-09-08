from __future__ import annotations

import unittest

from functions.rooms_v2_reply_state import (
    send_private_text_with_reply_state,
    send_public_text_with_reply_state,
)
from functions.rooms_v2_state import (
    MODE_REAL,
    RoomsV2StateError,
    conversation_id_for,
    create_room_state,
    join_room_state,
    open_private_state,
    send_private_text_state,
    send_public_text_state,
)


LEASE_MS = 120_000


class RoomsV2ReplyStateTest(unittest.TestCase):
    def room_with_member(self):
        root = create_room_state(
            {},
            uid="owner",
            display_name="Owner",
            name="Sala Uno",
            normalized_name="sala uno",
            name_key="room-name-key",
            description="fixture",
            operation_id="create-op",
            room_id="room-1",
            identity_id="owner-id",
            now=1,
            public_profile_id="owner",
        )
        return join_room_state(
            root,
            uid="member",
            room_id="room-1",
            mode=MODE_REAL,
            display_name="Member",
            identity_key="real",
            candidate_identity_id="member-id",
            now=2,
            public_profile_id="member",
            alias_claim_key=None,
        )

    def test_public_reply_persists_target(self):
        root = self.room_with_member()
        root = send_public_text_state(
            root,
            uid="owner",
            room_id="room-1",
            text="first",
            client_message_id="client-first",
            candidate_message_id="msg-first",
            now=3,
            visible_lease_ms=LEASE_MS,
        )
        root = send_public_text_with_reply_state(
            root,
            uid="member",
            room_id="room-1",
            text="reply",
            client_message_id="client-reply",
            candidate_message_id="msg-reply",
            reply_to_message_id="msg-first",
            now=4,
            visible_lease_ms=LEASE_MS,
        )

        self.assertEqual(
            "msg-first",
            root["publicMessages"]["room-1"]["msg-reply"]["replyToMessageId"],
        )

    def test_private_reply_is_scoped_to_same_conversation(self):
        root = self.room_with_member()
        conversation_id = conversation_id_for("room-1", "owner-id", "member-id")
        root = open_private_state(
            root,
            uid="owner",
            room_id="room-1",
            target_identity_id="member-id",
            conversation_id=conversation_id,
            now=3,
        )
        root = send_private_text_state(
            root,
            uid="owner",
            room_id="room-1",
            conversation_id=conversation_id,
            text="secret",
            client_message_id="private-first",
            candidate_message_id="private-msg-first",
            now=4,
            visible_lease_ms=LEASE_MS,
        )
        root = send_private_text_with_reply_state(
            root,
            uid="member",
            room_id="room-1",
            conversation_id=conversation_id,
            text="private reply",
            client_message_id="private-reply",
            candidate_message_id="private-msg-reply",
            reply_to_message_id="private-msg-first",
            now=5,
            visible_lease_ms=LEASE_MS,
        )

        self.assertEqual(
            "private-msg-first",
            root["privateMessages"][conversation_id]["private-msg-reply"][
                "replyToMessageId"
            ],
        )

        with self.assertRaises(RoomsV2StateError) as error:
            send_private_text_with_reply_state(
                root,
                uid="member",
                room_id="room-1",
                conversation_id=conversation_id,
                text="bad target",
                client_message_id="private-cross-thread",
                candidate_message_id="private-msg-cross-thread",
                reply_to_message_id="msg-from-public-thread",
                now=6,
                visible_lease_ms=LEASE_MS,
            )
        self.assertEqual("NOT_FOUND", error.exception.code)

    def test_missing_or_removed_reply_target_is_rejected(self):
        root = self.room_with_member()
        with self.assertRaises(RoomsV2StateError) as missing:
            send_public_text_with_reply_state(
                root,
                uid="owner",
                room_id="room-1",
                text="reply",
                client_message_id="client-missing",
                candidate_message_id="msg-missing",
                reply_to_message_id="does-not-exist",
                now=3,
                visible_lease_ms=LEASE_MS,
            )
        self.assertEqual("NOT_FOUND", missing.exception.code)

        root = send_public_text_state(
            root,
            uid="owner",
            room_id="room-1",
            text="first",
            client_message_id="client-first",
            candidate_message_id="msg-first",
            now=4,
            visible_lease_ms=LEASE_MS,
        )
        root["publicMessages"]["room-1"]["msg-first"]["removed"] = True
        with self.assertRaises(RoomsV2StateError) as removed:
            send_public_text_with_reply_state(
                root,
                uid="member",
                room_id="room-1",
                text="reply",
                client_message_id="client-removed",
                candidate_message_id="msg-removed",
                reply_to_message_id="msg-first",
                now=5,
                visible_lease_ms=LEASE_MS,
            )
        self.assertEqual("CONFLICT", removed.exception.code)

    def test_idempotent_retry_keeps_original_reply_target(self):
        root = self.room_with_member()
        for index in (1, 2):
            root = send_public_text_state(
                root,
                uid="owner",
                room_id="room-1",
                text=f"target {index}",
                client_message_id=f"target-{index}",
                candidate_message_id=f"msg-target-{index}",
                now=2 + index,
                visible_lease_ms=LEASE_MS,
            )

        root = send_public_text_with_reply_state(
            root,
            uid="member",
            room_id="room-1",
            text="reply",
            client_message_id="stable-client-id",
            candidate_message_id="msg-reply",
            reply_to_message_id="msg-target-1",
            now=5,
            visible_lease_ms=LEASE_MS,
        )
        retry = send_public_text_with_reply_state(
            root,
            uid="member",
            room_id="room-1",
            text="changed retry",
            client_message_id="stable-client-id",
            candidate_message_id="msg-duplicate",
            reply_to_message_id="msg-target-2",
            now=6,
            visible_lease_ms=LEASE_MS,
        )

        self.assertEqual(
            {"msg-target-1", "msg-target-2", "msg-reply"},
            set(retry["publicMessages"]["room-1"]),
        )
        self.assertEqual(
            "msg-target-1",
            retry["publicMessages"]["room-1"]["msg-reply"]["replyToMessageId"],
        )


if __name__ == "__main__":
    unittest.main()
