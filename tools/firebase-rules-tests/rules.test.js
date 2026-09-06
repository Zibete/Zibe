const path = require("node:path");
const assert = require("node:assert/strict");
const { execFileSync } = require("node:child_process");
const { readFileSync } = require("node:fs");
const { before, after, beforeEach, describe, it } = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

const projectId = "demo-zibe";
const repositoryRoot = path.join(__dirname, "..", "..");
const rulesGitRef = process.env.ZIBE_RULES_GIT_REF;
const rules = rulesGitRef
  ? execFileSync(
      "git",
      ["show", `${rulesGitRef}:database.rules.json`],
      { cwd: repositoryRoot, encoding: "utf8" }
    )
  : readFileSync(path.join(repositoryRoot, "database.rules.json"), "utf8");

const uidA = "user_a";
const uidB = "user_b";
const uidC = "user_c";
const chatId = [uidA, uidB].sort().join("|");

let testEnv;

const authedDb = (uid) => testEnv.authenticatedContext(uid).database();
const unauthDb = () => testEnv.unauthenticatedContext().database();

const userAccount = (uid) => ({
  id: uid,
  name: "User",
  birthDate: "2000-01-01",
  createdAt: 123,
  age: 23,
  email: "user@example.com",
  photoUrl: "https://example.com/photo.png",
  isOnline: true,
  description: "desc",
  latitude: 0,
  longitude: 0,
});

const dmMessage = (overrides = {}) => ({
  content: "hi",
  createdAt: 123,
  audioDurationMs: 0,
  senderUid: uidA,
  type: 100,
  seen: 1,
  ...overrides,
});

const dmConversation = (overrides = {}) => ({
  lastContent: "hi",
  lastMessageAt: 123,
  userId: uidA,
  otherId: uidA,
  otherName: "User A",
  otherPhotoUrl: "https://example.com/photo.png",
  state: "dm",
  unreadCount: 1,
  seen: 0,
  ...overrides,
});

const messageId = "message_1";
const messagePath = `Chats/dm/${chatId}/${messageId}`;
const conversationPath = `Users/Data/${uidB}/dm/${uidA}`;

const roomKey = "room_1";
const roomName = "Room One";
const roomNameKey = roomName.toLowerCase();
const entryMessageId = "entry_1";
const seededRoomCreatedAt = 1700000000000;

const roomMeta = (overrides = {}) => ({
  roomId: roomKey,
  name: roomName,
  description: "A public room",
  creatorUid: uidA,
  type: 1,
  users: 1,
  createdAt: Date.now(),
  totalMessages: 1,
  lastMessageAt: Date.now(),
  lastMessageId: entryMessageId,
  ...overrides,
});

const roomMember = (uid, overrides = {}) => ({
  userId: uid,
  userName: uid === uidA ? "User A" : "User B",
  type: 1,
  joinedAtMs: Date.now(),
  photoUrl: "",
  aliasKey: uid === uidA ? "user a" : "user b",
  ...overrides,
});

const roomMessage = (uid, messageId, overrides = {}) => ({
  content: "hello room",
  timestamp: Date.now(),
  nameUser: uid === uidA ? "User A" : "User B",
  senderUid: uid,
  chatType: 100,
  userType: 1,
  clientMessageId: messageId,
  roomId: roomKey,
  roomName,
  ...overrides,
});

const roomReadState = (overrides = {}) => ({
  unreadCount: 0,
  lastReadAt: Date.now(),
  lastReadMessageId: entryMessageId,
  lastUnreadMessageId: "",
  ...overrides,
});

const groupDmMessage = (overrides = {}) => ({
  content: "private room message",
  createdAt: Date.now(),
  audioDurationMs: 0,
  senderUid: uidA,
  type: 100,
  seen: 1,
  ...overrides,
});

const groupDmConversation = (ownerUid, otherUid, overrides = {}) => ({
  lastContent: "private room message",
  lastMessageAt: Date.now(),
  userId: ownerUid,
  otherId: otherUid,
  otherName: otherUid === uidA ? "User A" : "User B",
  otherPhotoUrl: "",
  state: "group_dm",
  unreadCount: 0,
  seen: 1,
  roomKey,
  lastMessageId: "private_1",
  ...overrides,
});

const seed = async (path, value) => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.database().ref(path).set(value);
  });
};

const createRoomFanout = async (uid = uidA, overrides = {}, identityType = 1) => {
  const now = Date.now();
  const aliasKey = identityType === 1
    ? `public-${uid.replaceAll("_", "-")}`
    : "anonymous-founder";
  const message = roomMessage(uid, entryMessageId, {
    content: `${uid} joined`,
    timestamp: now,
    chatType: 111,
    userType: identityType,
    nameUser: uid === uidA ? "User A" : "User B",
  });
  const meta = roomMeta({
    creatorUid: uid,
    createdAt: now,
    lastMessageAt: now,
    ...overrides,
  });
  await authedDb(uid).ref().update({
    [`Groups/Names/${roomNameKey}`]: roomKey,
    [`Groups/Meta/${roomKey}`]: meta,
    [`Groups/Aliases/${roomKey}/${aliasKey}`]: uid,
    [`Groups/Users/${roomKey}/${uid}`]: roomMember(uid, {
      joinedAtMs: now,
      type: identityType,
      aliasKey,
    }),
    [`Groups/Chat/${roomKey}/${entryMessageId}`]: message,
    [`Users/Data/${uid}/Rooms/${roomKey}`]: roomReadState({
      lastReadAt: now,
      lastReadMessageId: entryMessageId,
    }),
  });
};

const buildJoinRoomUpdates = ({
  uid = uidB,
  identityType = 1,
  displayName = "User B",
  aliasKey = "public-user-b",
  targetRoomKey = roomKey,
  targetRoomName = roomName,
  targetRoomDescription = "A public room",
  previousUsers = 1,
  previousMessages = 1,
  receiverUids = [uidA],
  messageId = "join_user_b",
  createdAt = seededRoomCreatedAt,
} = {}) => {
  const now = Date.now();
  const updates = {
    [`Groups/Meta/${targetRoomKey}`]: roomMeta({
      roomId: targetRoomKey,
      name: targetRoomName,
      description: targetRoomDescription,
      users: previousUsers + 1,
      createdAt,
      totalMessages: previousMessages + 1,
      lastMessageAt: now,
      lastMessageId: messageId,
    }),
    [`Groups/Aliases/${targetRoomKey}/${aliasKey}`]: uid,
    [`Groups/Users/${targetRoomKey}/${uid}`]: roomMember(uid, {
      userName: displayName,
      type: identityType,
      joinedAtMs: now,
      photoUrl: identityType === 1 ? "https://example.com/profile.png" : "",
      aliasKey,
    }),
    [`Groups/Chat/${targetRoomKey}/${messageId}`]: roomMessage(uid, messageId, {
      content: `${displayName} joined`,
      timestamp: now,
      nameUser: displayName,
      userType: identityType,
      chatType: 111,
      roomId: targetRoomKey,
      roomName: targetRoomName,
    }),
    [`Users/Data/${uid}/Rooms/${targetRoomKey}`]: roomReadState({
      lastReadAt: now,
      lastReadMessageId: messageId,
    }),
  };
  for (const receiverUid of receiverUids) {
    updates[`Users/Data/${receiverUid}/Rooms/${targetRoomKey}/unreadCount`] = 1;
    updates[`Users/Data/${receiverUid}/Rooms/${targetRoomKey}/lastUnreadMessageId`] = messageId;
  }
  return updates;
};

const joinRoomFanout = async (options = {}) => {
  const uid = options.uid ?? uidB;
  const authUid = options.authUid ?? uid;
  const updates = buildJoinRoomUpdates(options);
  const database = options.authenticated === false ? unauthDb() : authedDb(authUid);
  return database.ref().update(updates);
};

const joinRoomLikeClient = async (options = {}) => {
  const uid = options.uid ?? uidB;
  const targetRoomKey = options.targetRoomKey ?? roomKey;
  const membership = await authedDb(uid)
    .ref(`Groups/Users/${targetRoomKey}/${uid}`)
    .get();
  if (membership.exists()) return false;
  await joinRoomFanout(options);
  return true;
};

const seedRoom = async ({ withUserB = false } = {}) => {
  const now = Date.now() - 1000;
  await seed("Groups", {
    Names: { [roomNameKey]: roomKey },
    Meta: {
      [roomKey]: roomMeta({
        users: withUserB ? 2 : 1,
        createdAt: seededRoomCreatedAt,
        lastMessageAt: now,
      }),
    },
    Users: {
      [roomKey]: {
        [uidA]: roomMember(uidA, { joinedAtMs: now }),
        ...(withUserB ? { [uidB]: roomMember(uidB, { joinedAtMs: now }) } : {}),
      },
    },
    Chat: {
      [roomKey]: {
        [entryMessageId]: roomMessage(uidA, entryMessageId, {
          timestamp: now,
          chatType: 111,
        }),
      },
    },
  });
  await seed(`Users/Data/${uidA}/Rooms/${roomKey}`, roomReadState({
    lastReadAt: now,
  }));
  if (withUserB) {
    await seed(`Users/Data/${uidB}/Rooms/${roomKey}`, roomReadState({
      unreadCount: 1,
      lastReadAt: now,
      lastReadMessageId: "",
      lastUnreadMessageId: entryMessageId,
    }));
  }
};

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    database: {
      rules,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearDatabase();
});

describe("Realtime Database Rules", () => {
  it("denies unauthenticated reads of accounts", async () => {
    await assertFails(unauthDb().ref("Users/Accounts").get());
    await assertSucceeds(authedDb(uidA).ref("Users/Accounts").get());
  });

  it("allows owner writes to account and denies others", async () => {
    await assertSucceeds(
      authedDb(uidA).ref(`Users/Accounts/${uidA}`).set(userAccount(uidA))
    );
    await assertFails(
      authedDb(uidB)
        .ref(`Users/Accounts/${uidA}`)
        .update({ name: "Nope" })
    );
  });

  it("restricts session fields by rules", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.database().ref(`Sessions/${uidA}`).set({
        activeInstallId: "install_1",
        fcmToken: "token_1",
      });
    });

    await assertSucceeds(
      authedDb(uidB).ref(`Sessions/${uidA}/fcmToken`).get()
    );
    await assertFails(
      authedDb(uidB).ref(`Sessions/${uidA}/activeInstallId`).get()
    );
  });

  it("allows atomic owner session cleanup while denying another account", async () => {
    const session = authedDb(uidA).ref(`Sessions/${uidA}`);
    await assertSucceeds(session.update({ activeInstallId: "local-install", fcmToken: "local-token" }));
    const cleanup = { activeInstallId: null, fcmToken: null };
    await assertFails(authedDb(uidB).ref(`Sessions/${uidA}`).update(cleanup));
    await assertSucceeds(session.update(cleanup));
    await assertSucceeds(session.child("activeInstallId").get()).then((snapshot) => {
      assert.equal(snapshot.exists(), false);
    });
  });

  it("allows valid dm message creation with seen 1", async () => {
    await assertSucceeds(authedDb(uidA).ref(messagePath).set(dmMessage()));
  });

  it("allows atomic dm fan-out with receiver-owned fields and server increment", async () => {
    const senderConversation = dmConversation({
      otherId: uidB,
      unreadCount: 0,
      seen: 1,
    });
    const receiverConversation = dmConversation({
      unreadCount: 1,
      seen: 0,
    });

    await assertSucceeds(
      authedDb(uidA).ref().update({
        [messagePath]: dmMessage(),
        [`Users/Data/${uidA}/dm/${uidB}`]: senderConversation,
        [`${conversationPath}/lastContent`]: receiverConversation.lastContent,
        [`${conversationPath}/lastMessageAt`]: { ".sv": "timestamp" },
        [`${conversationPath}/userId`]: receiverConversation.userId,
        [`${conversationPath}/otherId`]: receiverConversation.otherId,
        [`${conversationPath}/otherName`]: receiverConversation.otherName,
        [`${conversationPath}/otherPhotoUrl`]: receiverConversation.otherPhotoUrl,
        [`${conversationPath}/state`]: receiverConversation.state,
        [`${conversationPath}/unreadCount`]: { ".sv": { "increment": 1 } },
        [`${conversationPath}/seen`]: receiverConversation.seen,
      })
    );
    const persistedState = await authedDb(uidB)
      .ref(`${conversationPath}/state`)
      .get();
    assert.equal(persistedState.val(), "dm");
  });

  it("allows a subsequent dm fan-out and resets latest-message seen", async () => {
    await seed(`Users/Data/${uidA}/dm/${uidB}`, dmConversation({
      userId: uidA,
      otherId: uidB,
      state: "silent",
      unreadCount: 0,
      seen: 3,
    }));
    await seed(conversationPath, dmConversation({
      state: "hide",
      unreadCount: 4,
      seen: 3,
    }));

    await assertSucceeds(
      authedDb(uidA).ref().update({
        [`Chats/dm/${chatId}/message_2`]: dmMessage({ createdAt: 456 }),
        [`Users/Data/${uidA}/dm/${uidB}`]: dmConversation({
          userId: uidA,
          otherId: uidB,
          state: "silent",
          unreadCount: 0,
          seen: 1,
          lastMessageAt: 456,
        }),
        [`${conversationPath}/lastContent`]: "next",
        [`${conversationPath}/lastMessageAt`]: { ".sv": "timestamp" },
        [`${conversationPath}/userId`]: uidA,
        [`${conversationPath}/otherId`]: uidA,
        [`${conversationPath}/otherName`]: "User A",
        [`${conversationPath}/otherPhotoUrl`]: "https://example.com/photo.png",
        [`${conversationPath}/state`]: "hide",
        [`${conversationPath}/unreadCount`]: { ".sv": { "increment": 1 } },
        [`${conversationPath}/seen`]: 0,
      })
    );
  });

  it("blocks physical dm deletion and sender-forged receipt", async () => {
    await seed(messagePath, dmMessage());
    await assertFails(authedDb(uidA).ref(messagePath).remove());
    await assertFails(authedDb(uidA).ref(`${messagePath}/seen`).set(2));
    await assertSucceeds(authedDb(uidB).ref(`${messagePath}/seen`).set(2));
  });

  it("blocks arbitrary receiver unread updates by the sender", async () => {
    await seed(conversationPath, dmConversation({ unreadCount: 4 }));
    await assertFails(
      authedDb(uidA).ref(`${conversationPath}/unreadCount`).set(0)
    );
    await assertFails(
      authedDb(uidA).ref(`${conversationPath}/unreadCount`).set(5)
    );
  });

  it("blocks a future timestamp summary denial of service", async () => {
    await seed(conversationPath, dmConversation({
      lastMessageAt: Date.now(),
      unreadCount: 4,
    }));
    await assertFails(
      authedDb(uidA).ref(conversationPath).update({
        lastContent: "spoof",
        lastMessageAt: Date.now() + 60_000,
        unreadCount: 5,
        seen: 0,
      })
    );
  });

  it("rejects ambiguous legacy chat ids and accepts the unambiguous format", async () => {
    const ambiguousPath = "Chats/dm/alice_team_bob/message_1";
    const unambiguousPath = "Chats/dm/alice_team|bob/message_1";
    await seed(ambiguousPath, dmMessage({ senderUid: "alice_team" }));
    await seed(unambiguousPath, dmMessage({ senderUid: "alice_team" }));

    await assertFails(authedDb("alice").ref(ambiguousPath).get());
    await assertFails(authedDb("alice_team").ref(ambiguousPath).get());
    await assertSucceeds(authedDb("alice_team").ref(unambiguousPath).get());
    await assertSucceeds(authedDb("bob").ref(unambiguousPath).get());
  });

  it("blocks dm message creation with seen above delivered", async () => {
    await assertFails(
      authedDb(uidA).ref(messagePath).set(dmMessage({ seen: 2 }))
    );
  });

  for (const [from, to] of [[1, 2], [2, 3], [1, 3]]) {
    it(`allows dm message seen ${from} -> ${to}`, async () => {
      await seed(messagePath, dmMessage({ seen: from }));
      await assertSucceeds(
        authedDb(uidB).ref(`${messagePath}/seen`).set(to)
      );
    });
  }

  for (const [from, to] of [[3, 2], [3, 1], [2, 1]]) {
    it(`blocks dm message seen downgrade ${from} -> ${to}`, async () => {
      await seed(messagePath, dmMessage({ seen: from }));
      await assertFails(
        authedDb(uidB).ref(`${messagePath}/seen`).set(to)
      );
    });
  }

  for (const invalidSeen of [0, 4, 1.5, "2"]) {
    it(`blocks invalid dm message seen ${JSON.stringify(invalidSeen)}`, async () => {
      await seed(messagePath, dmMessage());
      await assertFails(
        authedDb(uidB).ref(`${messagePath}/seen`).set(invalidSeen)
      );
    });
  }

  it("blocks deleting dm message seen", async () => {
    await seed(messagePath, dmMessage());
    await assertFails(authedDb(uidB).ref(`${messagePath}/seen`).set(null));
  });

  it("enforces participant-owned dm soft-delete transitions", async () => {
    await seed(messagePath, dmMessage());
    await assertFails(authedDb(uidB).ref(messagePath).update({ type: 101 }));
    await assertSucceeds(authedDb(uidB).ref(messagePath).update({ type: 102 }));
    await assertSucceeds(authedDb(uidA).ref(messagePath).update({ type: 103 }));
  });

  it("blocks non-participant dm message seen update", async () => {
    await seed(messagePath, dmMessage());
    await assertFails(authedDb(uidC).ref(`${messagePath}/seen`).set(2));
  });

  for (const [field, value] of [
    ["senderUid", uidB],
    ["content", "changed"],
    ["createdAt", 456],
    ["audioDurationMs", 1000],
  ]) {
    it(`blocks changing dm seen together with immutable ${field}`, async () => {
      await seed(messagePath, dmMessage());
      await assertFails(
        authedDb(uidB).ref(messagePath).update({ seen: 2, [field]: value })
      );
    });
  }

  it("allows dm participants to read and blocks outsiders", async () => {
    await seed(messagePath, dmMessage());
    await assertSucceeds(authedDb(uidB).ref(messagePath).get());
    await assertFails(authedDb(uidC).ref(messagePath).get());
  });

  it("allows current dm conversation payloads with seen 0 and 1", async () => {
    await assertSucceeds(
      authedDb(uidB).ref(conversationPath).set(dmConversation({ seen: 0 }))
    );
    await assertSucceeds(
      authedDb(uidB).ref(conversationPath).set(dmConversation({ seen: 1 }))
    );
  });

  it("blocks the other participant from replacing or changing owned conversation state", async () => {
    await seed(conversationPath, dmConversation());
    await assertFails(
      authedDb(uidA).ref(conversationPath).set(dmConversation({ state: "blocked" }))
    );
    await assertFails(
      authedDb(uidA).ref(`${conversationPath}/state`).set("blocked")
    );
    await assertSucceeds(
      authedDb(uidB).ref(`${conversationPath}/state`).set("blocked")
    );
  });

  for (const validSeen of [0, 1, 2, 3]) {
    it(`allows dm conversation seen ${validSeen}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertSucceeds(
        authedDb(uidB).ref(`${conversationPath}/seen`).set(validSeen)
      );
    });
  }

  it("allows receiver receipt updates only on an unchanged sender summary", async () => {
    const senderSummaryPath = `Users/Data/${uidA}/dm/${uidB}`;
    await seed(senderSummaryPath, dmConversation({
      userId: uidA,
      otherId: uidB,
      unreadCount: 0,
      seen: 1,
    }));
    await assertSucceeds(
      authedDb(uidB).ref(`${senderSummaryPath}/seen`).set(2)
    );
    await assertFails(
      authedDb(uidB).ref(senderSummaryPath).update({
        seen: 3,
        lastContent: "forged",
      })
    );
  });

  for (const invalidSeen of [-1, 4, 1.5, "2"]) {
    it(`blocks invalid dm conversation seen ${JSON.stringify(invalidSeen)}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertFails(
        authedDb(uidA).ref(`${conversationPath}/seen`).set(invalidSeen)
      );
    });
  }

  for (const validUnread of [0, 1, 1000000]) {
    it(`allows non-negative integer dm unreadCount ${validUnread}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertSucceeds(
        authedDb(uidB).ref(`${conversationPath}/unreadCount`).set(validUnread)
      );
    });
  }

  for (const invalidUnread of [-1, 1.5, "1"]) {
    it(`blocks invalid dm unreadCount ${JSON.stringify(invalidUnread)}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertFails(
        authedDb(uidB).ref(`${conversationPath}/unreadCount`).set(invalidUnread)
      );
    });
  }

  it("blocks outsider updates to an otherwise valid dm conversation", async () => {
    await seed(conversationPath, dmConversation());
    await assertFails(
      authedDb(uidC).ref(`${conversationPath}/seen`).set(2)
    );
  });

  it("creates a room only through the complete atomic fan-out", async () => {
    await assertSucceeds(createRoomFanout());

    await assertFails(
      authedDb(uidB).ref(`Groups/Meta/partial_room`).set(
        roomMeta({ roomId: "partial_room", creatorUid: uidB })
      )
    );
    await assertFails(
      unauthDb().ref(`Groups/Meta/${roomKey}`).get()
    );
    await assertSucceeds(
      authedDb(uidC).ref(`Groups/Meta/${roomKey}`).get()
    );
  });

  it("rejects anonymous room creation without leaving partial data", async () => {
    await assertFails(createRoomFanout(uidA, {}, 0));
    const snapshot = await authedDb(uidA).ref(`Groups/Meta/${roomKey}`).get();
    assert.equal(snapshot.exists(), false);
  });

  it("enforces unique room names and immutable technical metadata", async () => {
    await createRoomFanout();
    await assertFails(
      authedDb(uidB).ref(`Groups/Names/${roomNameKey}`).set("another_room")
    );
    for (const [field, value] of [
      ["roomId", "changed"],
      ["name", "Changed"],
      ["creatorUid", uidB],
      ["createdAt", Date.now()],
    ]) {
      await assertFails(
        authedDb(uidA).ref(`Groups/Meta/${roomKey}/${field}`).set(value)
      );
    }
    await assertSucceeds(
      authedDb(uidA).ref(`Groups/Meta/${roomKey}/description`).set("Updated")
    );
    await assertFails(
      authedDb(uidA).ref(`Groups/Meta/${roomKey}`).remove()
    );
  });

  it("keeps valid legacy metadata readable without allowing it to become a new room", async () => {
    await seed("Groups/Meta/legacy_room", {
      name: "Legacy Room",
      description: "legacy",
      creatorUid: uidA,
      type: 1,
      users: 1,
      createdAt: 123,
      totalMessages: 4,
    });
    await assertSucceeds(authedDb(uidB).ref("Groups/Meta/legacy_room").get());
    await assertFails(
      authedDb(uidA).ref("Groups/Meta/legacy_room/creatorUid").set(uidB)
    );
  });

  it("upgrades legacy metadata non-destructively when a current member sends", async () => {
    const legacyKey = "legacy_room";
    const legacyMessageId = "legacy_message_5";
    const now = Date.now();
    await seed(`Groups/Meta/${legacyKey}`, {
      name: "Legacy Room",
      description: "legacy",
      creatorUid: uidA,
      type: 1,
      users: 1,
      createdAt: 123,
      totalMessages: 4,
    });
    await seed(`Groups/Users/${legacyKey}/${uidA}`, roomMember(uidA, {
      joinedAtMs: 123,
    }));
    await assertSucceeds(
      authedDb(uidA).ref().update({
        [`Groups/Meta/${legacyKey}/roomId`]: legacyKey,
        [`Groups/Meta/${legacyKey}/totalMessages`]: 5,
        [`Groups/Meta/${legacyKey}/lastMessageAt`]: now,
        [`Groups/Meta/${legacyKey}/lastMessageId`]: legacyMessageId,
        [`Groups/Chat/${legacyKey}/${legacyMessageId}`]: roomMessage(
          uidA,
          legacyMessageId,
          {
            timestamp: now,
            roomId: legacyKey,
            roomName: "Legacy Room",
          }
        ),
      })
    );
    const description = await authedDb(uidA)
      .ref(`Groups/Meta/${legacyKey}/description`)
      .get();
    assert.equal(description.val(), "legacy");
  });

  it("exposes membership count data to authenticated discovery but restricts chat", async () => {
    await seedRoom();
    await assertSucceeds(authedDb(uidA).ref(`Groups/Users/${roomKey}`).get());
    await assertSucceeds(authedDb(uidA).ref(`Groups/Chat/${roomKey}`).get());
    await assertSucceeds(authedDb(uidB).ref(`Groups/Users/${roomKey}`).get());
    await assertFails(authedDb(uidB).ref(`Groups/Chat/${roomKey}`).get());
  });

  it("joins a room with real identity only through an atomic membership fan-out", async () => {
    await seedRoom();
    const now = Date.now();
    const joinMessageId = "join_user_b";
    await assertSucceeds(
      authedDb(uidB).ref().update({
        [`Groups/Meta/${roomKey}/users`]: 2,
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: now,
        [`Groups/Meta/${roomKey}/lastMessageId`]: joinMessageId,
        [`Groups/Aliases/${roomKey}/user b`]: uidB,
        [`Groups/Users/${roomKey}/${uidB}`]: roomMember(uidB, { joinedAtMs: now }),
        [`Groups/Chat/${roomKey}/${joinMessageId}`]: roomMessage(uidB, joinMessageId, {
          content: "User B joined",
          timestamp: now,
          chatType: 111,
        }),
        [`Users/Data/${uidB}/Rooms/${roomKey}`]: roomReadState({
          lastReadAt: now,
          lastReadMessageId: joinMessageId,
        }),
        [`Users/Data/${uidA}/Rooms/${roomKey}/unreadCount`]: 1,
        [`Users/Data/${uidA}/Rooms/${roomKey}/lastUnreadMessageId`]: joinMessageId,
      })
    );
    await assertFails(
      authedDb(uidC).ref(`Groups/Users/${roomKey}/${uidB}`).set(roomMember(uidB))
    );
    await assertFails(
      authedDb(uidC).ref(`Groups/Users/missing/${uidC}`).set(roomMember(uidC))
    );
  });

  it("accepts the exact modern public join fan-out produced by the client", async () => {
    await seedRoom();
    await assertSucceeds(joinRoomFanout());
    const membership = await authedDb(uidB)
      .ref(`Groups/Users/${roomKey}/${uidB}`)
      .get();
    assert.equal(membership.child("type").val(), 1);
  });

  it("accepts the exact modern anonymous join fan-out produced by the client", async () => {
    await seedRoom();
    await assertSucceeds(joinRoomFanout({
      identityType: 0,
      displayName: "Ghost",
      aliasKey: "ghost",
      messageId: "join_ghost",
    }));
    const membership = await authedDb(uidB)
      .ref(`Groups/Users/${roomKey}/${uidB}`)
      .get();
    assert.equal(membership.child("type").val(), 0);
    assert.equal(membership.child("photoUrl").val(), "");
  });

  it("rejects unauthenticated, spoofed and missing-room join fan-outs", async () => {
    await seedRoom();
    await assertFails(joinRoomFanout({ authenticated: false }));
    await assertFails(joinRoomFanout({ authUid: uidC }));
    await assertFails(joinRoomFanout({
      targetRoomKey: "missing_room",
      targetRoomName: "Missing Room",
    }));
  });

  it("accepts the exact join fan-out for valid legacy room metadata", async () => {
    const legacyKey = "legacy_room";
    const createdAt = Date.now() - 2000;
    await seed(`Groups/Meta/${legacyKey}`, {
      name: "Legacy Room",
      description: "legacy",
      creatorUid: uidA,
      type: 1,
      users: 1,
      createdAt,
      totalMessages: 4,
    });
    await seed(`Groups/Users/${legacyKey}/${uidA}`, roomMember(uidA, {
      joinedAtMs: createdAt,
    }));
    await seed(`Users/Data/${uidA}/Rooms/${legacyKey}`, roomReadState({
      lastReadAt: createdAt,
    }));

    await assertSucceeds(joinRoomFanout({
      targetRoomKey: legacyKey,
      targetRoomName: "Legacy Room",
      targetRoomDescription: "legacy",
      previousMessages: 4,
      messageId: "legacy_join_user_b",
      createdAt,
    }));
    const meta = await authedDb(uidB).ref(`Groups/Meta/${legacyKey}`).get();
    assert.equal(meta.child("roomId").val(), legacyKey);
    assert.equal(meta.child("users").val(), 2);
  });

  it("makes a repeated client join idempotent when membership already exists", async () => {
    await seedRoom();
    assert.equal(await joinRoomLikeClient(), true);
    assert.equal(await joinRoomLikeClient({ messageId: "duplicate_join" }), false);
    const messages = await authedDb(uidB).ref(`Groups/Chat/${roomKey}`).get();
    assert.equal(Object.keys(messages.val()).length, 2);
  });

  it("accepts the exact public leave fan-out produced by the client", async () => {
    const sourceKey = "public_leave_room";
    const sourceName = "Public Leave Room";
    const createdAt = Date.now() - 2000;
    await seed(`Groups/Meta/${sourceKey}`, roomMeta({
      roomId: sourceKey,
      name: sourceName,
      users: 2,
      createdAt,
      lastMessageAt: createdAt,
    }));
    await seed(`Groups/Users/${sourceKey}`, {
      [uidA]: roomMember(uidA, { joinedAtMs: createdAt }),
      [uidB]: roomMember(uidB, {
        joinedAtMs: createdAt,
        aliasKey: "public-user-b",
      }),
    });
    await seed(`Groups/Aliases/${sourceKey}/public-user-b`, uidB);
    await seed(`Users/Data/${uidA}/Rooms/${sourceKey}`, roomReadState({
      lastReadAt: createdAt,
    }));
    await seed(`Users/Data/${uidB}/Rooms/${sourceKey}`, roomReadState({
      lastReadAt: createdAt,
    }));

    const now = Date.now();
    const leaveMessageId = "leave_public_user_b";
    await assertSucceeds(authedDb(uidB).ref().update({
      [`Groups/Meta/${sourceKey}`]: roomMeta({
        roomId: sourceKey,
        name: sourceName,
        users: 1,
        createdAt,
        totalMessages: 2,
        lastMessageAt: now,
        lastMessageId: leaveMessageId,
      }),
      [`Groups/Chat/${sourceKey}/${leaveMessageId}`]: roomMessage(
        uidB,
        leaveMessageId,
        {
          content: "User B left",
          timestamp: now,
          chatType: 111,
          roomId: sourceKey,
          roomName: sourceName,
        }
      ),
      [`Groups/Aliases/${sourceKey}/public-user-b`]: null,
      [`Groups/Users/${sourceKey}/${uidB}`]: null,
      [`Users/Data/${uidB}/Rooms/${sourceKey}`]: null,
      [`Users/Data/${uidB}/ClientData/ActiveView/activeThread`]: null,
      [`Users/Data/${uidA}/Rooms/${sourceKey}/unreadCount`]: 1,
      [`Users/Data/${uidA}/Rooms/${sourceKey}/lastUnreadMessageId`]: leaveMessageId,
    }));
  });

  it("switches rooms through one atomic leave and join fan-out", async () => {
    const sourceKey = "source_room";
    const sourceName = "Source Room";
    const createdAt = Date.now() - 2000;
    await seedRoom();
    await seed(`Groups/Meta/${sourceKey}`, roomMeta({
      roomId: sourceKey,
      name: sourceName,
      users: 2,
      createdAt,
      lastMessageAt: createdAt,
    }));
    await seed(`Groups/Users/${sourceKey}/${uidA}`, roomMember(uidA, {
      joinedAtMs: createdAt,
    }));
    await seed(`Groups/Users/${sourceKey}/${uidB}`, roomMember(uidB, {
      joinedAtMs: createdAt,
      aliasKey: "public-user-b",
    }));
    await seed(`Groups/Aliases/${sourceKey}/public-user-b`, uidB);
    await seed(`Users/Data/${uidA}/Rooms/${sourceKey}`, roomReadState({
      lastReadAt: createdAt,
    }));
    await seed(`Users/Data/${uidB}/Rooms/${sourceKey}`, roomReadState({
      lastReadAt: createdAt,
    }));
    const now = Date.now();
    const leaveMessageId = "leave_source_user_b";
    const updates = {
      ...buildJoinRoomUpdates({ messageId: "join_target_user_b" }),
      [`Groups/Meta/${sourceKey}`]: roomMeta({
        roomId: sourceKey,
        name: sourceName,
        users: 1,
        createdAt,
        totalMessages: 2,
        lastMessageAt: now,
        lastMessageId: leaveMessageId,
      }),
      [`Groups/Chat/${sourceKey}/${leaveMessageId}`]: roomMessage(
        uidB,
        leaveMessageId,
        {
          content: "User B left",
          timestamp: now,
          chatType: 111,
          roomId: sourceKey,
          roomName: sourceName,
        }
      ),
      [`Groups/Aliases/${sourceKey}/public-user-b`]: null,
      [`Groups/Users/${sourceKey}/${uidB}`]: null,
      [`Users/Data/${uidB}/Rooms/${sourceKey}`]: null,
      [`Users/Data/${uidB}/ClientData/ActiveView/activeThread`]: null,
      [`Users/Data/${uidA}/Rooms/${sourceKey}/unreadCount`]: 1,
      [`Users/Data/${uidA}/Rooms/${sourceKey}/lastUnreadMessageId`]: leaveMessageId,
    };

    await assertSucceeds(authedDb(uidB).ref().update(updates));
    assert.equal(
      (await authedDb(uidB).ref(`Groups/Users/${sourceKey}/${uidB}`).get()).exists(),
      false
    );
    assert.equal(
      (await authedDb(uidB).ref(`Groups/Users/${roomKey}/${uidB}`).get()).exists(),
      true
    );
  });

  it("allows public identities with the same display name when their technical aliases differ", async () => {
    await seedRoom();
    const now = Date.now();
    const joinMessageId = "join_same_public_name";
    await assertSucceeds(
      authedDb(uidB).ref().update({
        [`Groups/Meta/${roomKey}/users`]: 2,
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: now,
        [`Groups/Meta/${roomKey}/lastMessageId`]: joinMessageId,
        [`Groups/Aliases/${roomKey}/public-user-b`]: uidB,
        [`Groups/Users/${roomKey}/${uidB}`]: roomMember(uidB, {
          userName: "User A",
          aliasKey: "public-user-b",
          joinedAtMs: now,
        }),
        [`Groups/Chat/${roomKey}/${joinMessageId}`]: roomMessage(uidB, joinMessageId, {
          content: "User A joined",
          nameUser: "User A",
          timestamp: now,
          chatType: 111,
        }),
        [`Users/Data/${uidB}/Rooms/${roomKey}`]: roomReadState({
          lastReadAt: now,
          lastReadMessageId: joinMessageId,
        }),
        [`Users/Data/${uidA}/Rooms/${roomKey}/unreadCount`]: 1,
        [`Users/Data/${uidA}/Rooms/${roomKey}/lastUnreadMessageId`]: joinMessageId,
      })
    );
  });

  it("reserves anonymous aliases atomically and rejects duplicates or spoofed indexes", async () => {
    await seedRoom();
    const now = Date.now();
    await assertSucceeds(
      authedDb(uidB).ref().update({
        [`Groups/Meta/${roomKey}/users`]: 2,
        [`Groups/Aliases/${roomKey}/ghost`]: uidB,
        [`Groups/Users/${roomKey}/${uidB}`]: roomMember(uidB, {
          userName: "Ghost",
          type: 0,
          aliasKey: "ghost",
          joinedAtMs: now,
        }),
        [`Users/Data/${uidB}/Rooms/${roomKey}`]: roomReadState({ lastReadAt: now }),
      })
    );
    await assertFails(
      authedDb(uidC).ref(`Groups/Aliases/${roomKey}/ghost`).set(uidC)
    );
    await assertFails(
      authedDb(uidC).ref().update({
        [`Groups/Meta/${roomKey}/users`]: 3,
        [`Groups/Aliases/${roomKey}/different`]: uidC,
        [`Groups/Users/${roomKey}/${uidC}`]: roomMember(uidC, {
          userName: "Ghost",
          type: 0,
          aliasKey: "ghost",
        }),
        [`Users/Data/${uidC}/Rooms/${roomKey}`]: roomReadState(),
      })
    );
  });

  it("rejects an exact anonymous join fan-out when its alias is already reserved", async () => {
    await seedRoom();
    await seed(`Groups/Aliases/${roomKey}/ghost`, uidC);
    await assertFails(joinRoomFanout({
      identityType: 0,
      displayName: "Ghost",
      aliasKey: "ghost",
      messageId: "duplicate_alias_join",
    }));
    const membership = await authedDb(uidB)
      .ref(`Groups/Users/${roomKey}/${uidB}`)
      .get();
    assert.equal(membership.exists(), false);
  });

  it("sends a public room message with atomic counters and receiver unread", async () => {
    await seedRoom({ withUserB: true });
    const messageId = "message_2";
    const now = Date.now();
    await assertSucceeds(
      authedDb(uidA).ref().update({
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: now,
        [`Groups/Meta/${roomKey}/lastMessageId`]: messageId,
        [`Groups/Chat/${roomKey}/${messageId}`]: roomMessage(uidA, messageId, {
          timestamp: now,
        }),
        [`Users/Data/${uidA}/Rooms/${roomKey}`]: roomReadState({
          lastReadAt: now,
          lastReadMessageId: messageId,
        }),
        [`Users/Data/${uidB}/Rooms/${roomKey}/unreadCount`]: 2,
        [`Users/Data/${uidB}/Rooms/${roomKey}/lastUnreadMessageId`]: messageId,
      })
    );
  });

  it("accepts distinct public messages that share the same server millisecond", async () => {
    await seedRoom({ withUserB: true });
    const messageId = "message_same_millisecond";
    const previousMeta = await authedDb(uidA).ref(`Groups/Meta/${roomKey}`).get();
    const timestamp = previousMeta.child("lastMessageAt").val();
    await assertSucceeds(
      authedDb(uidA).ref().update({
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: timestamp,
        [`Groups/Meta/${roomKey}/lastMessageId`]: messageId,
        [`Groups/Chat/${roomKey}/${messageId}`]: roomMessage(uidA, messageId, {
          timestamp,
        }),
        [`Users/Data/${uidA}/Rooms/${roomKey}`]: roomReadState({
          lastReadAt: timestamp,
          lastReadMessageId: messageId,
        }),
        [`Users/Data/${uidB}/Rooms/${roomKey}/unreadCount`]: 2,
        [`Users/Data/${uidB}/Rooms/${roomKey}/lastUnreadMessageId`]: messageId,
      })
    );
  });

  it("rejects public message forgery, partial sends, counter drift, mutation and deletion", async () => {
    await seedRoom({ withUserB: true });
    const messageId = "message_2";
    await assertFails(
      authedDb(uidA).ref(`Groups/Chat/${roomKey}/${messageId}`).set(
        roomMessage(uidA, messageId)
      )
    );
    await assertFails(
      authedDb(uidA).ref(`Groups/Meta/${roomKey}/totalMessages`).set(2)
    );
    await assertFails(
      authedDb(uidA).ref(`Groups/Chat/${roomKey}/${entryMessageId}/content`).set("edited")
    );
    await assertFails(
      authedDb(uidA).ref(`Groups/Chat/${roomKey}/${entryMessageId}`).remove()
    );
    await assertFails(
      authedDb(uidB).ref().update({
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: Date.now(),
        [`Groups/Meta/${roomKey}/lastMessageId`]: messageId,
        [`Groups/Chat/${roomKey}/${messageId}`]: roomMessage(uidB, messageId, {
          nameUser: "User A",
        }),
      })
    );
  });

  it("allows only monotonic owner room reads and rejects forged unread values", async () => {
    await seedRoom({ withUserB: true });
    const oldState = roomReadState({
      unreadCount: 2,
      lastReadAt: Date.now() - 1000,
      lastReadMessageId: "",
      lastUnreadMessageId: entryMessageId,
    });
    await seed(`Users/Data/${uidB}/Rooms/${roomKey}`, oldState);
    await assertSucceeds(
      authedDb(uidB).ref(`Users/Data/${uidB}/Rooms/${roomKey}`).set({
        ...oldState,
        unreadCount: 0,
        lastReadAt: Date.now(),
        lastReadMessageId: entryMessageId,
      })
    );
    await assertFails(
      authedDb(uidB).ref(`Users/Data/${uidB}/Rooms/${roomKey}/lastReadAt`).set(1)
    );
    await assertFails(
      authedDb(uidB).ref(`Users/Data/${uidB}/Rooms/${roomKey}/unreadCount`).set(-1)
    );
    await assertFails(
      authedDb(uidC).ref(`Users/Data/${uidB}/Rooms/${roomKey}/unreadCount`).set(3)
    );
    await assertFails(
      authedDb(uidB).ref(`Users/Data/${uidB}/Rooms/${roomKey}`).remove()
    );
    await assertFails(
      authedDb(uidB).ref(`Users/Data/${uidB}/Rooms/${roomKey}/lastReadAt`)
        .set(Date.now() + 60_000)
    );
  });

  it("accepts fresh DM and room active-thread shapes and rejects ambiguous or stale leases", async () => {
    await assertSucceeds(
      authedDb(uidA).ref(`Users/Data/${uidA}/ClientData/ActiveView/activeThread`).set({
        nodeType: "dm",
        otherUid: uidB,
        updatedAt: { ".sv": "timestamp" },
      })
    );
    await assertSucceeds(
      authedDb(uidA).ref(`Users/Data/${uidA}/ClientData/ActiveView/activeThread`).set({
        nodeType: "room",
        roomKey,
        updatedAt: { ".sv": "timestamp" },
      })
    );
    await assertFails(
      authedDb(uidA).ref(`Users/Data/${uidA}/ClientData/ActiveView/activeThread`).set({
        nodeType: "room",
        roomKey,
        otherUid: uidB,
        updatedAt: Date.now(),
      })
    );
    await assertFails(
      authedDb(uidA).ref(`Users/Data/${uidA}/ClientData/ActiveView/activeThread`).set({
        nodeType: "room",
        roomKey,
        updatedAt: Date.now() - 121_000,
      })
    );
  });

  it("leaves a room without deleting private group conversations", async () => {
    await seedRoom({ withUserB: true });
    await seed(`Groups/Aliases/${roomKey}/ghost`, uidB);
    await seed(`Groups/Users/${roomKey}/${uidB}`, roomMember(uidB, {
      userName: "Ghost",
      type: 0,
      aliasKey: "ghost",
    }));
    const privatePath = `Chats/group_dm/${chatId}/private_1`;
    const leaveMessageId = "leave_user_b";
    const now = Date.now();
    await seed(privatePath, groupDmMessage());
    await assertSucceeds(
      authedDb(uidB).ref().update({
        [`Groups/Meta/${roomKey}/users`]: 1,
        [`Groups/Meta/${roomKey}/totalMessages`]: 2,
        [`Groups/Meta/${roomKey}/lastMessageAt`]: now,
        [`Groups/Meta/${roomKey}/lastMessageId`]: leaveMessageId,
        [`Groups/Chat/${roomKey}/${leaveMessageId}`]: roomMessage(
          uidB,
          leaveMessageId,
          {
            content: "Ghost left",
            timestamp: now,
            nameUser: "Ghost",
            chatType: 111,
            userType: 0,
          }
        ),
        [`Users/Data/${uidA}/Rooms/${roomKey}/unreadCount`]: 1,
        [`Users/Data/${uidA}/Rooms/${roomKey}/lastUnreadMessageId`]: leaveMessageId,
        [`Groups/Aliases/${roomKey}/ghost`]: null,
        [`Groups/Users/${roomKey}/${uidB}`]: null,
        [`Users/Data/${uidB}/Rooms/${roomKey}`]: null,
      })
    );
    const persistedPrivate = await authedDb(uidB).ref(privatePath).get();
    assert.equal(persistedPrivate.exists(), true);
  });

  it("restricts group_dm reads to participants and blocks global authenticated reads", async () => {
    const path = `Chats/group_dm/${chatId}/private_1`;
    await seed(path, groupDmMessage());
    await assertSucceeds(authedDb(uidA).ref(path).get());
    await assertSucceeds(authedDb(uidB).ref(path).get());
    await assertFails(authedDb(uidC).ref(path).get());
    await assertFails(authedDb(uidC).ref("Chats/group_dm").get());
  });

  it("allows the exact modern group_dm client fan-out and keeps context in summaries", async () => {
    await seedRoom({ withUserB: true });
    const privateMessageId = "private_fanout_1";
    const now = Date.now();
    await assertFails(
      authedDb(uidA).ref(`Users/Data/${uidB}/group_dm/${uidA}`).set(
        groupDmConversation(uidB, uidA, {
          userId: uidA,
          unreadCount: 1,
          seen: 0,
          lastMessageId: privateMessageId,
        })
      )
    );
    await assertSucceeds(
      authedDb(uidA).ref().update({
        [`Chats/group_dm/${chatId}/${privateMessageId}`]: groupDmMessage({ createdAt: now }),
        [`Users/Data/${uidA}/group_dm/${uidB}`]: groupDmConversation(uidA, uidB, {
          lastMessageAt: now,
          lastMessageId: privateMessageId,
        }),
        [`Users/Data/${uidB}/group_dm/${uidA}`]: groupDmConversation(uidB, uidA, {
          lastMessageAt: now,
          userId: uidA,
          unreadCount: 1,
          seen: 0,
          lastMessageId: privateMessageId,
        }),
      })
    );
    await assertFails(
      authedDb(uidA).ref(`Chats/group_dm/${chatId}/private_extra_context`).set(
        groupDmMessage({ roomKey, senderName: "User A" })
      )
    );
  });

  it("enforces append-only group_dm content with monotonic seen and participant soft-delete", async () => {
    const path = `Chats/group_dm/${chatId}/private_1`;
    await assertSucceeds(authedDb(uidA).ref(path).set(groupDmMessage()));
    await assertFails(authedDb(uidA).ref(path).remove());
    await assertFails(authedDb(uidB).ref(`${path}/content`).set("forged"));
    await assertSucceeds(authedDb(uidB).ref(`${path}/seen`).set(2));
    await assertFails(authedDb(uidB).ref(`${path}/seen`).set(1));
    await assertFails(authedDb(uidA).ref(`${path}/seen`).set(3));
    await assertSucceeds(authedDb(uidB).ref(`${path}/type`).set(102));
    await assertFails(authedDb(uidC).ref(`${path}/seen`).set(3));
  });

  it("protects group_dm summary ownership and receiver unread increments", async () => {
    const ownerPath = `Users/Data/${uidB}/group_dm/${uidA}`;
    await seed(ownerPath, groupDmConversation(uidB, uidA, {
      userId: uidA,
      unreadCount: 1,
      seen: 0,
    }));
    await assertFails(
      authedDb(uidA).ref(`${ownerPath}/state`).set("blocked")
    );
    await assertSucceeds(
      authedDb(uidB).ref(`${ownerPath}/state`).set("blocked")
    );
    await assertFails(
      authedDb(uidA).ref(`${ownerPath}/unreadCount`).set(-1)
    );
    await assertFails(
      authedDb(uidC).ref(ownerPath).get()
    );
    await assertFails(
      authedDb(uidA).ref(ownerPath).remove()
    );
    await assertSucceeds(
      authedDb(uidB).ref(ownerPath).remove()
    );
  });

  it("persists immutable room context on modern group_dm summaries", async () => {
    const ownerPath = `Users/Data/${uidB}/group_dm/${uidA}`;
    await seedRoom({ withUserB: true });
    await seed(ownerPath, groupDmConversation(uidB, uidA, {
      userId: uidA,
      unreadCount: 1,
      seen: 0,
    }));
    await assertFails(
      authedDb(uidB).ref(`${ownerPath}/roomKey`).set("another-room")
    );
    await assertFails(
      authedDb(uidB).ref(`${ownerPath}/roomKey`).set("missing-room")
    );
  });

  it("allows authenticated feedback writes", async () => {
    const feedback = {
      id: uidA,
      name: "User A",
      email: "a@example.com",
      feedback: "hello",
      device: "device",
      appVersion: "1.0.0",
      createdAt: 123,
    };

    await assertSucceeds(
      authedDb(uidA).ref("Feedback/app").push(feedback)
    );
  });
});
