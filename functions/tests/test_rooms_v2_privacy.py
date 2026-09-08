from __future__ import annotations

import unittest

from functions.rooms_v2_state import MODE_REAL, create_room_state, join_room_state


class RoomsV2PrivacyTest(unittest.TestCase):
    def test_real_owner_uid_never_enters_public_member_projection(self):
        uid = "firebase-owner-uid"
        root = create_room_state(
            {},
            uid=uid,
            display_name="Owner",
            name="Sala privada",
            normalized_name="sala privada",
            name_key="claim",
            description="",
            operation_id="op-create",
            room_id="room-opaque",
            identity_id="identity-opaque-owner",
            now=1,
            public_profile_id=uid,
        )

        public_member = root["publicMembers"]["room-opaque"]["identity-opaque-owner"]
        self.assertNotIn("publicProfileId", public_member)
        self.assertNotIn(uid, str(public_member))
        self.assertEqual(
            uid,
            root["private"]["identityOwners"]["room-opaque"]["identity-opaque-owner"]["uid"],
        )

    def test_real_joiner_uid_never_enters_public_member_projection(self):
        owner_uid = "firebase-owner-uid"
        member_uid = "firebase-member-uid"
        root = create_room_state(
            {},
            uid=owner_uid,
            display_name="Owner",
            name="Sala privada",
            normalized_name="sala privada",
            name_key="claim",
            description="",
            operation_id="op-create",
            room_id="room-opaque",
            identity_id="identity-opaque-owner",
            now=1,
            public_profile_id=owner_uid,
        )
        root = join_room_state(
            root,
            uid=member_uid,
            room_id="room-opaque",
            mode=MODE_REAL,
            display_name="Member",
            identity_key="real",
            candidate_identity_id="identity-opaque-member",
            now=2,
            public_profile_id=member_uid,
            alias_claim_key=None,
        )

        public_member = root["publicMembers"]["room-opaque"]["identity-opaque-member"]
        self.assertNotIn("publicProfileId", public_member)
        self.assertNotIn(member_uid, str(public_member))
        self.assertEqual(
            member_uid,
            root["private"]["identityOwners"]["room-opaque"]["identity-opaque-member"]["uid"],
        )


if __name__ == "__main__":
    unittest.main()
