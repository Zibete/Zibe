from __future__ import annotations

import unittest

from functions.rooms_v2_state import (
    MODE_ANONYMOUS,
    MODE_REAL,
    RoomsV2StateError,
    block_private_state,
    close_room_state,
    conversation_id_for,
    create_room_state,
    join_room_state,
    leave_room_state,
    open_private_state,
    remove_member_state,
    remove_public_message_state,
    report_message_state,
    resolve_report_state,
    send_private_text_state,
    send_public_text_state,
    set_moderator_state,
    transfer_owner_state,
)


class RoomsV2StateTest(unittest.TestCase):
    def owner_room(self):
        return create_room_state(
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

    def add_member(
        self,
        root,
        *,
        uid="member",
        identity_id="member-id",
        mode=MODE_REAL,
        display_name="Member",
        identity_key="real",
        alias_claim_key=None,
        now=2,
    ):
        return join_room_state(
            root,
            uid=uid,
            room_id="room-1",
            mode=mode,
            display_name=display_name,
            identity_key=identity_key,
            candidate_identity_id=identity_id,
            now=now,
            public_profile_id=uid if mode == MODE_REAL else None,
            alias_claim_key=alias_claim_key,
        )

    def test_create_is_idempotent_and_name_is_unique(self):
        root = self.owner_room()
        retry = create_room_state(
            root,
            uid="owner",
            display_name="Owner",
            name="Sala Uno",
            normalized_name="sala uno",
            name_key="room-name-key",
            description="fixture",
            operation_id="create-op",
            room_id="other-room",
            identity_id="other-id",
            now=2,
            public_profile_id="owner",
        )
        self.assertEqual({"room-1"}, set(retry["publicRooms"]))

        with self.assertRaises(RoomsV2StateError) as error:
            create_room_state(
                retry,
                uid="other",
                display_name="Other",
                name="Sala Uno",
                normalized_name="sala uno",
                name_key="room-name-key",
                description="",
                operation_id="other-op",
                room_id="room-2",
                identity_id="other-id",
                now=3,
                public_profile_id="other",
            )
        self.assertEqual("ROOM_NAME_TAKEN", error.exception.code)

    def test_active_identity_cannot_be_changed_without_leave(self):
        root = self.add_member(self.owner_room())
        with self.assertRaises(RoomsV2StateError) as error:
            join_room_state(
                root,
                uid="member",
                room_id="room-1",
                mode=MODE_ANONYMOUS,
                display_name="Ghost",
                identity_key="anon_hash",
                candidate_identity_id="anon-id",
                now=3,
                public_profile_id=None,
                alias_claim_key="hash",
            )
        self.assertEqual("IDENTITY_CHANGE_REQUIRED", error.exception.code)

    def test_same_account_same_alias_restores_identity_and_private(self):
        root = self.add_member(
            self.owner_room(),
            uid="member",
            identity_id="anon-id",
            mode=MODE_ANONYMOUS,
            display_name="Ghost",
            identity_key="anon_hash",
            alias_claim_key="hash",
        )
        conversation_id = conversation_id_for("room-1", "owner-id", "anon-id")
        root = open_private_state(
            root,
            uid="owner",
            room_id="room-1",
            target_identity_id="anon-id",
            conversation_id=conversation_id,
            now=3,
        )
        root = leave_room_state(root, uid="member", room_id="room-1", now=4)
        self.assertNotIn(
            conversation_id,
            root["conversationIndexByUser"]["member"]["room-1"],
        )
        self.assertFalse(
            root["private"]["conversationReaders"][conversation_id]["member"]
        )

        root = join_room_state(
            root,
            uid="member",
            room_id="room-1",
            mode=MODE_ANONYMOUS,
            display_name="Ghost",
            identity_key="anon_hash",
            candidate_identity_id="replacement-id",
            now=5,
            public_profile_id=None,
            alias_claim_key="hash",
        )
        self.assertEqual(
            "anon-id",
            root["membershipIndexByUser"]["member"]["room-1"]["identityId"],
        )
        self.assertTrue(
            root["private"]["conversationReaders"][conversation_id]["member"]
        )
        self.assertFalse(
            root["conversationIndexByUser"]["member"]["room-1"][conversation_id][
                "closed"
            ]
        )

    def test_active_alias_owned_by_other_account_is_denied(self):
        root = self.add_member(
            self.owner_room(),
            uid="first",
            identity_id="anon-first",
            mode=MODE_ANONYMOUS,
            display_name="Ghost",
            identity_key="anon_hash",
            alias_claim_key="hash",
        )
        with self.assertRaises(RoomsV2StateError) as error:
            self.add_member(
                root,
                uid="second",
                identity_id="anon-second",
                mode=MODE_ANONYMOUS,
                display_name="Ghost",
                identity_key="anon_hash",
                alias_claim_key="hash",
                now=3,
            )
        self.assertEqual("ALIAS_TAKEN", error.exception.code)

    def test_released_alias_can_be_claimed_without_inheriting_identity(self):
        root = self.add_member(
            self.owner_room(),
            uid="first",
            identity_id="anon-first",
            mode=MODE_ANONYMOUS,
            display_name="Ghost",
            identity_key="anon_hash",
            alias_claim_key="hash",
        )
        root = leave_room_state(root, uid="first", room_id="room-1", now=3)
        root = self.add_member(
            root,
            uid="second",
            identity_id="anon-second",
            mode=MODE_ANONYMOUS,
            display_name="Ghost",
            identity_key="anon_hash",
            alias_claim_key="hash",
            now=4,
        )
        self.assertEqual(
            "anon-second",
            root["membershipIndexByUser"]["second"]["room-1"]["identityId"],
        )

    def test_public_message_is_idempotent_and_unread_is_monotonic(self):
        root = self.add_member(self.owner_room())
        root = send_public_text_state(
            root,
            uid="owner",
            room_id="room-1",
            text="hello",
            client_message_id="client-1",
            candidate_message_id="msg-1",
            now=3,
            visible_lease_ms=120_000,
        )
        root = send_public_text_state(
            root,
            uid="owner",
            room_id="room-1",
            text="hello",
            client_message_id="client-1",
            candidate_message_id="msg-duplicate",
            now=4,
            visible_lease_ms=120_000,
        )
        self.assertEqual(1, root["publicRooms"]["room-1"]["lastSeq"])
        self.assertEqual(
            {"msg-1"},
            set(root["publicMessages"]["room-1"]),
        )
        self.assertEqual(
            1,
            root["membershipIndexByUser"]["member"]["room-1"]["unreadCount"],
        )

    def test_private_messages_are_contextual_and_leave_revokes_reader(self):
        root = self.add_member(self.owner_room())
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
            client_message_id="private-1",
            candidate_message_id="private-msg-1",
            now=4,
            visible_lease_ms=120_000,
        )
        self.assertEqual(
            1,
            root["conversationIndexByUser"]["member"]["room-1"][conversation_id][
                "unreadCount"
            ],
        )
        root = leave_room_state(root, uid="member", room_id="room-1", now=5)
        self.assertFalse(
            root["private"]["conversationReaders"][conversation_id]["member"]
        )
        self.assertIn(
            "private-msg-1",
            root["privateMessages"][conversation_id],
        )

    def test_blocked_private_cannot_be_reopened(self):
        root = self.add_member(self.owner_room())
        conversation_id = conversation_id_for("room-1", "owner-id", "member-id")
        root = open_private_state(
            root,
            uid="owner",
            room_id="room-1",
            target_identity_id="member-id",
            conversation_id=conversation_id,
            now=3,
        )
        root = block_private_state(
            root,
            uid="member",
            room_id="room-1",
            conversation_id=conversation_id,
            blocked=True,
            now=4,
        )
        with self.assertRaises(RoomsV2StateError) as error:
            open_private_state(
                root,
                uid="owner",
                room_id="room-1",
                target_identity_id="member-id",
                conversation_id=conversation_id,
                now=5,
            )
        self.assertEqual("CONFLICT", error.exception.code)

    def test_moderator_cannot_moderate_owner_or_moderator(self):
        root = self.add_member(self.owner_room(), uid="moderator")
        root = set_moderator_state(
            root,
            uid="owner",
            room_id="room-1",
            target_identity_id="member-id",
            enabled=True,
            now=3,
            audit_id="audit-grant",
        )
        root = self.add_member(
            root,
            uid="member",
            identity_id="other-member-id",
            now=4,
        )
        with self.assertRaises(RoomsV2StateError) as owner_error:
            remove_member_state(
                root,
                uid="moderator",
                room_id="room-1",
                target_identity_id="owner-id",
                ban=True,
                now=5,
                audit_id="audit-ban-owner",
            )
        self.assertEqual("PERMISSION_DENIED", owner_error.exception.code)

    def test_transfer_owner_is_atomic_and_requires_real_identity(self):
        root = self.add_member(self.owner_room())
        root = transfer_owner_state(
            root,
            uid="owner",
            room_id="room-1",
            target_identity_id="member-id",
            now=3,
            audit_id="audit-transfer",
        )
        self.assertEqual(
            "member-id",
            root["publicRooms"]["room-1"]["ownerIdentityId"],
        )
        self.assertEqual(
            "owner",
            root["membershipIndexByUser"]["member"]["room-1"]["role"],
        )
        self.assertEqual(
            "member",
            root["membershipIndexByUser"]["owner"]["room-1"]["role"],
        )

    def test_message_removal_is_tombstone_and_audited(self):
        root = self.add_member(self.owner_room())
        root = send_public_text_state(
            root,
            uid="member",
            room_id="room-1",
            text="bad",
            client_message_id="client-1",
            candidate_message_id="msg-1",
            now=3,
            visible_lease_ms=120_000,
        )
        root = remove_public_message_state(
            root,
            uid="owner",
            room_id="room-1",
            message_id="msg-1",
            now=4,
            audit_id="audit-remove",
        )
        message = root["publicMessages"]["room-1"]["msg-1"]
        self.assertTrue(message["removed"])
        self.assertEqual("", message["text"])
        self.assertEqual(
            "remove_message",
            root["private"]["audit"]["room-1"]["audit-remove"]["action"],
        )

    def test_report_contains_only_explicit_evidence_and_can_be_resolved(self):
        root = self.add_member(self.owner_room())
        root = send_public_text_state(
            root,
            uid="member",
            room_id="room-1",
            text="report me",
            client_message_id="client-1",
            candidate_message_id="msg-1",
            now=3,
            visible_lease_ms=120_000,
        )
        root = report_message_state(
            root,
            uid="owner",
            room_id="room-1",
            message_id="msg-1",
            conversation_id=None,
            reason="spam",
            report_id="report-1",
            now=4,
        )
        report = root["reports"]["room-1"]["report-1"]
        self.assertNotIn("uid", str(report["evidence"]).lower())
        root = resolve_report_state(
            root,
            uid="owner",
            room_id="room-1",
            report_id="report-1",
            resolution="removed",
            now=5,
            audit_id="audit-resolve",
        )
        self.assertEqual(
            "resolved",
            root["reports"]["room-1"]["report-1"]["status"],
        )

    def test_close_room_blocks_join(self):
        root = close_room_state(
            self.owner_room(),
            uid="owner",
            room_id="room-1",
            now=2,
            audit_id="audit-close",
        )
        with self.assertRaises(RoomsV2StateError) as error:
            self.add_member(root)
        self.assertEqual("ROOM_CLOSED", error.exception.code)


if __name__ == "__main__":
    unittest.main()
