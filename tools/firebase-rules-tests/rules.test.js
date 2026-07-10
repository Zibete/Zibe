const path = require("node:path");
const { readFileSync } = require("node:fs");
const { before, after, beforeEach, describe, it } = require("node:test");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

const projectId = "demo-zibe";
const rules = readFileSync(
  path.join(__dirname, "..", "..", "database.rules.json"),
  "utf8"
);

const uidA = "user_a";
const uidB = "user_b";
const uidC = "user_c";
const chatId = [uidA, uidB].sort().join("_");

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

const seed = async (path, value) => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.database().ref(path).set(value);
  });
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

  it("allows valid dm message creation with seen 1", async () => {
    await assertSucceeds(authedDb(uidA).ref(messagePath).set(dmMessage()));
  });

  it("allows the current atomic dm fan-out payload", async () => {
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
        [conversationPath]: receiverConversation,
      })
    );
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

  it("allows the existing dm soft-delete type update without changing seen", async () => {
    await seed(messagePath, dmMessage());
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
      authedDb(uidA).ref(conversationPath).set(dmConversation({ seen: 0 }))
    );
    await assertSucceeds(
      authedDb(uidA).ref(conversationPath).set(dmConversation({ seen: 1 }))
    );
  });

  for (const validSeen of [0, 1, 2, 3]) {
    it(`allows dm conversation seen ${validSeen}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertSucceeds(
        authedDb(uidA).ref(`${conversationPath}/seen`).set(validSeen)
      );
    });
  }

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
        authedDb(uidA).ref(`${conversationPath}/unreadCount`).set(validUnread)
      );
    });
  }

  for (const invalidUnread of [-1, 1.5, "1"]) {
    it(`blocks invalid dm unreadCount ${JSON.stringify(invalidUnread)}`, async () => {
      await seed(conversationPath, dmConversation());
      await assertFails(
        authedDb(uidA).ref(`${conversationPath}/unreadCount`).set(invalidUnread)
      );
    });
  }

  it("blocks outsider updates to an otherwise valid dm conversation", async () => {
    await seed(conversationPath, dmConversation());
    await assertFails(
      authedDb(uidC).ref(`${conversationPath}/seen`).set(2)
    );
  });

  it("enforces group membership for chat and meta updates", async () => {
    const groupName = "group_1";
    const groupMeta = {
      name: "Group 1",
      description: "desc",
      creatorUid: uidA,
      type: 1,
      users: 1,
      createdAt: 123,
      totalMessages: 0,
    };

    await assertSucceeds(
      authedDb(uidA).ref(`Groups/Meta/${groupName}`).set(groupMeta)
    );
    await assertFails(
      authedDb(uidB)
        .ref(`Groups/Meta/${groupName}`)
        .update({ description: "hack" })
    );

    const member = {
      userId: uidA,
      userName: "User A",
      type: 1,
      joinedAtMs: 123,
    };

    await assertSucceeds(
      authedDb(uidA).ref(`Groups/Users/${groupName}/${uidA}`).set(member)
    );
    await assertFails(
      authedDb(uidB).ref(`Groups/Users/${groupName}/${uidA}`).set(member)
    );

    const groupMsg = {
      content: "hello",
      timestamp: 123,
      userName: "User A",
      senderUid: uidA,
      chatType: 100,
      userType: 1,
    };

    await assertSucceeds(
      authedDb(uidA).ref(`Groups/Chat/${groupName}`).push(groupMsg)
    );
    await assertFails(
      authedDb(uidB).ref(`Groups/Chat/${groupName}`).push({
        ...groupMsg,
        senderUid: uidB,
        userName: "User B",
      })
    );

    await assertSucceeds(
      authedDb(uidA)
        .ref(`Groups/Meta/${groupName}/totalMessages`)
        .set(1)
    );
    await assertFails(
      authedDb(uidB)
        .ref(`Groups/Meta/${groupName}/totalMessages`)
        .set(2)
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
