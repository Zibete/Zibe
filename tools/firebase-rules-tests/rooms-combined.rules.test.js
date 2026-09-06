const path = require("node:path");
const { readFileSync } = require("node:fs");
const { before, after, beforeEach, describe, it } = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

const projectId = "demo-zibe-rooms";
const repositoryRoot = path.join(__dirname, "..", "..");
const rulesPath = path.join(
  repositoryRoot,
  "build",
  "generated",
  "firebase",
  "database.rooms-combined.rules.json",
);
const rules = readFileSync(rulesPath, "utf8");

const uidOwner = "owner_uid";
const uidMember = "member_uid";
const uidModerator = "moderator_uid";
const uidFormer = "former_uid";
const uidOutsider = "outsider_uid";
const roomId = "room_public_1";
const ownerIdentityId = "id_owner_opaque";
const memberIdentityId = "anon_member_opaque";
const moderatorIdentityId = "id_moderator_opaque";
const conversationId = "conv_contextual_1";
let testEnv;

const authedDb = (uid) => testEnv.authenticatedContext(uid).database();
const unauthDb = () => testEnv.unauthenticatedContext().database();

const membership = (identityId, displayName, mode, role, active = true) => ({
  roomId,
  identityId,
  displayName,
  mode,
  role,
  active,
  joinedAt: 1,
  lastReadAt: 1,
  lastReadSeq: 0,
  unreadCount: 1,
  notificationsEnabled: true,
});

const publicIdentity = (identityId, displayName, mode, role, active = true) => ({
  identityId,
  displayName,
  mode,
  role,
  active,
  joinedAt: 1,
});

const message = {
  messageId: "msg_1",
  roomId,
  authorIdentityId: memberIdentityId,
  authorDisplayName: "Ghost",
  authorMode: "anonymous",
  text: "hello",
  sentAt: 2,
  seq: 1,
  kind: "text",
  removed: false,
};

const seedFixture = async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.database().ref().set({
      Users: {
        Accounts: {
          [uidOwner]: {
            id: uidOwner,
            name: "Owner",
            birthDate: "2000-01-01",
            createdAt: 1,
            age: 26,
            email: "owner@example.test",
            photoUrl: "",
            isOnline: true,
            description: "",
            latitude: 0,
            longitude: 0,
          },
        },
      },
      RoomsV2: {
        publicRooms: {
          [roomId]: {
            roomId,
            name: "Sala segura",
            normalizedName: "sala segura",
            description: "fixture",
            status: "open",
            ownerIdentityId,
            memberCount: 3,
            pendingCount: 0,
            createdAt: 1,
            updatedAt: 2,
            lastSeq: 1,
          },
        },
        publicMembers: {
          [roomId]: {
            [ownerIdentityId]: publicIdentity(ownerIdentityId, "Owner", "real", "owner"),
            [memberIdentityId]: publicIdentity(memberIdentityId, "Ghost", "anonymous", "member"),
            [moderatorIdentityId]: publicIdentity(moderatorIdentityId, "Moderator", "real", "moderator"),
          },
        },
        publicMessages: {
          [roomId]: { msg_1: message },
        },
        membershipIndexByUser: {
          [uidOwner]: {
            [roomId]: membership(ownerIdentityId, "Owner", "real", "owner"),
          },
          [uidMember]: {
            [roomId]: membership(memberIdentityId, "Ghost", "anonymous", "member"),
          },
          [uidModerator]: {
            [roomId]: membership(moderatorIdentityId, "Moderator", "real", "moderator"),
          },
          [uidFormer]: {
            [roomId]: membership("id_former", "Former", "real", "member", false),
          },
        },
        conversationIndexByUser: {
          [uidOwner]: {
            [roomId]: {
              [conversationId]: {
                conversationId,
                roomId,
                otherIdentity: publicIdentity(memberIdentityId, "Ghost", "anonymous", "member"),
                closed: false,
                blocked: false,
                lastText: "private",
                updatedAt: 3,
                unreadCount: 0,
                lastReadSeq: 1,
              },
            },
          },
          [uidMember]: {
            [roomId]: {
              [conversationId]: {
                conversationId,
                roomId,
                otherIdentity: publicIdentity(ownerIdentityId, "Owner", "real", "owner"),
                closed: false,
                blocked: false,
                lastText: "private",
                updatedAt: 3,
                unreadCount: 1,
                lastReadSeq: 0,
              },
            },
          },
        },
        privateMessages: {
          [conversationId]: {
            private_msg_1: {
              ...message,
              messageId: "private_msg_1",
              conversationId,
              text: "private evidence",
            },
          },
        },
        reports: {
          [roomId]: {
            report_1: {
              reportId: "report_1",
              roomId,
              reason: "spam",
              status: "open",
              evidence: message,
              createdAt: 4,
              resolution: "",
              reporterIdentityId: memberIdentityId,
            },
          },
        },
        private: {
          identityOwners: {
            [roomId]: {
              [memberIdentityId]: {
                uid: uidMember,
                identityKey: "anon_hash",
                active: true,
              },
            },
          },
          aliasClaims: {
            [roomId]: {
              digest: { uid: uidMember, identityId: memberIdentityId, active: true },
            },
          },
          bans: {
            [roomId]: { banned_uid: { createdAt: 1 } },
          },
          conversationReaders: {
            [conversationId]: {
              [uidOwner]: true,
              [uidMember]: true,
              [uidFormer]: false,
            },
          },
        },
      },
    });
  });
};

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    database: { rules },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearDatabase();
  await seedFixture();
});

describe("Combined legacy + RoomsV2 boundary", () => {
  it("keeps the legacy authenticated account read while unauthenticated clients stay denied", async () => {
    await assertSucceeds(authedDb(uidMember).ref(`Users/Accounts/${uidOwner}`).get());
    await assertFails(unauthDb().ref(`Users/Accounts/${uidOwner}`).get());
  });

  it("allows authenticated room discovery but denies it to unauthenticated clients", async () => {
    await assertSucceeds(authedDb(uidOutsider).ref("RoomsV2/publicRooms").get());
    await assertFails(unauthDb().ref("RoomsV2/publicRooms").get());
  });

  it("allows each account to read only its own membership projection", async () => {
    await assertSucceeds(authedDb(uidMember).ref(`RoomsV2/membershipIndexByUser/${uidMember}`).get());
    await assertFails(authedDb(uidOwner).ref(`RoomsV2/membershipIndexByUser/${uidMember}`).get());
  });

  it("allows active members to read room participants and public messages", async () => {
    await assertSucceeds(authedDb(uidMember).ref(`RoomsV2/publicMembers/${roomId}`).get());
    await assertSucceeds(authedDb(uidMember).ref(`RoomsV2/publicMessages/${roomId}`).get());
  });

  it("denies public room content to inactive members and outsiders", async () => {
    await assertFails(authedDb(uidFormer).ref(`RoomsV2/publicMessages/${roomId}`).get());
    await assertFails(authedDb(uidOutsider).ref(`RoomsV2/publicMembers/${roomId}`).get());
  });

  it("never exposes server-owned identity, ban, alias or reader mappings", async () => {
    for (const uid of [uidOwner, uidMember, uidModerator]) {
      await assertFails(authedDb(uid).ref(`RoomsV2/private/identityOwners/${roomId}/${memberIdentityId}`).get());
      await assertFails(authedDb(uid).ref(`RoomsV2/private/aliasClaims/${roomId}/digest`).get());
      await assertFails(authedDb(uid).ref(`RoomsV2/private/bans/${roomId}`).get());
      await assertFails(authedDb(uid).ref(`RoomsV2/private/conversationReaders/${conversationId}`).get());
    }
  });

  it("allows contextual private history only to current server-authorized readers", async () => {
    await assertSucceeds(authedDb(uidOwner).ref(`RoomsV2/privateMessages/${conversationId}`).get());
    await assertSucceeds(authedDb(uidMember).ref(`RoomsV2/privateMessages/${conversationId}`).get());
    await assertFails(authedDb(uidFormer).ref(`RoomsV2/privateMessages/${conversationId}`).get());
    await assertFails(authedDb(uidModerator).ref(`RoomsV2/privateMessages/${conversationId}`).get());
  });

  it("does not grant moderators arbitrary private-chat access", async () => {
    await assertSucceeds(authedDb(uidModerator).ref(`RoomsV2/reports/${roomId}`).get());
    await assertFails(authedDb(uidModerator).ref(`RoomsV2/privateMessages/${conversationId}`).get());
  });

  it("restricts reports to active owner and moderator roles", async () => {
    await assertSucceeds(authedDb(uidOwner).ref(`RoomsV2/reports/${roomId}`).get());
    await assertSucceeds(authedDb(uidModerator).ref(`RoomsV2/reports/${roomId}`).get());
    await assertFails(authedDb(uidMember).ref(`RoomsV2/reports/${roomId}`).get());
    await assertFails(authedDb(uidFormer).ref(`RoomsV2/reports/${roomId}`).get());
  });

  it("allows a user to read only their own active contextual-conversation index", async () => {
    await assertSucceeds(authedDb(uidMember).ref(`RoomsV2/conversationIndexByUser/${uidMember}/${roomId}`).get());
    await assertFails(authedDb(uidOwner).ref(`RoomsV2/conversationIndexByUser/${uidMember}/${roomId}`).get());
    await assertFails(authedDb(uidFormer).ref(`RoomsV2/conversationIndexByUser/${uidFormer}/${roomId}`).get());
  });

  it("denies every direct client mutation of server-owned RoomsV2 state", async () => {
    const db = authedDb(uidOwner);
    await assertFails(db.ref(`RoomsV2/publicMessages/${roomId}/msg_2`).set({ text: "bypass" }));
    await assertFails(db.ref(`RoomsV2/publicRooms/${roomId}/ownerIdentityId`).set(memberIdentityId));
    await assertFails(db.ref(`RoomsV2/publicRooms/${roomId}/memberCount`).set(999));
    await assertFails(db.ref(`RoomsV2/membershipIndexByUser/${uidOwner}/${roomId}/role`).set("member"));
    await assertFails(db.ref(`RoomsV2/membershipIndexByUser/${uidOwner}/${roomId}/lastReadSeq`).set(999));
    await assertFails(db.ref(`RoomsV2/conversationIndexByUser/${uidOwner}/${roomId}/${conversationId}/unreadCount`).set(0));
    await assertFails(db.ref(`RoomsV2/private/bans/${roomId}/${uidMember}`).set(true));
    await assertFails(db.ref(`RoomsV2/reports/${roomId}/report_1/resolution`).set("forged"));
  });
});
