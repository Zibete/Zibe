const path = require("node:path");
const { readFileSync } = require("node:fs");
const { before, after, beforeEach, describe, it } = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

const projectId = "demo-zibe-rooms-v2-rules";
const repositoryRoot = path.join(__dirname, "..", "..");
const rules = readFileSync(path.join(repositoryRoot, "database.roomsv2.rules.json"), "utf8");
const uidA = "user_a";
const uidB = "user_b";
const uidC = "user_c";
const roomId = "room_public_1";
const identityId = "id_opaque_a";
let testEnv;

const authedDb = (uid) => testEnv.authenticatedContext(uid).database();
const unauthDb = () => testEnv.unauthenticatedContext().database();

const seedFixture = async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.database().ref("RoomsV2").set({
      publicRooms: {
        [roomId]: {
          roomId,
          name: "Sala segura",
          description: "fixture",
          status: "open",
          ownerIdentityId: identityId,
          memberCount: 1,
          pendingCount: 0,
          createdAt: 1,
          updatedAt: 1,
        },
      },
      publicMembers: {
        [roomId]: {
          [identityId]: {
            identityId,
            displayName: "Alias público",
            mode: "anonymous",
            role: "member",
            active: true,
            joinedAt: 1,
          },
        },
      },
      publicMessages: {
        [roomId]: {
          msg_1: {
            messageId: "msg_1",
            roomId,
            authorIdentityId: identityId,
            authorDisplayName: "Alias público",
            authorMode: "anonymous",
            text: "hola",
            sentAt: 1,
          },
        },
      },
      membershipIndexByUser: {
        [uidA]: {
          [roomId]: {
            roomId,
            identityId,
            displayName: "Alias público",
            mode: "anonymous",
            role: "member",
            active: true,
            joinedAt: 1,
            lastReadAt: 0,
          },
        },
        [uidB]: {
          [roomId]: {
            roomId,
            identityId: "id_opaque_b",
            displayName: "B",
            mode: "real",
            role: "member",
            active: false,
            joinedAt: 1,
            lastReadAt: 0,
          },
        },
      },
      private: {
        identityOwners: {
          [roomId]: {
            [identityId]: { uid: uidA, identityKey: "anon_secret", active: true },
          },
        },
        aliasClaims: {
          [roomId]: {
            digest: { uid: uidA, identityId, active: true },
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

describe("RoomsV2 public/private boundary", () => {
  it("denies the public directory to unauthenticated clients", async () => {
    await assertFails(unauthDb().ref("RoomsV2/publicRooms").get());
  });

  it("allows authenticated directory discovery without exposing private mappings", async () => {
    await assertSucceeds(authedDb(uidC).ref("RoomsV2/publicRooms").get());
    await assertFails(authedDb(uidA).ref(`RoomsV2/private/identityOwners/${roomId}/${identityId}`).get());
    await assertFails(authedDb(uidA).ref(`RoomsV2/private/aliasClaims/${roomId}/digest`).get());
  });

  it("allows a user to read only their own membership index", async () => {
    await assertSucceeds(authedDb(uidA).ref(`RoomsV2/membershipIndexByUser/${uidA}`).get());
    await assertFails(authedDb(uidB).ref(`RoomsV2/membershipIndexByUser/${uidA}`).get());
  });

  it("allows active members to read room identities and messages", async () => {
    await assertSucceeds(authedDb(uidA).ref(`RoomsV2/publicMembers/${roomId}`).get());
    await assertSucceeds(authedDb(uidA).ref(`RoomsV2/publicMessages/${roomId}`).get());
  });

  it("denies inactive members and nonmembers room content", async () => {
    await assertFails(authedDb(uidB).ref(`RoomsV2/publicMessages/${roomId}`).get());
    await assertFails(authedDb(uidC).ref(`RoomsV2/publicMembers/${roomId}`).get());
  });

  it("denies all direct client writes so privileged fanout stays in Functions", async () => {
    await assertFails(authedDb(uidA).ref(`RoomsV2/publicMessages/${roomId}/msg_2`).set({ text: "bypass" }));
    await assertFails(authedDb(uidA).ref(`RoomsV2/membershipIndexByUser/${uidA}/${roomId}/role`).set("owner"));
    await assertFails(authedDb(uidA).ref(`RoomsV2/private/bans/${roomId}/${uidB}`).set(true));
  });
});
