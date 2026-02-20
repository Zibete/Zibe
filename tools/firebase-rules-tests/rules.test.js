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

  it("enforces dm chat participants and message validation", async () => {
    const msgRef = authedDb(uidA).ref(`Chats/dm/${chatId}`).push();
    await assertSucceeds(
      msgRef.set({
        content: "hi",
        createdAt: 123,
        audioDurationMs: 0,
        senderUid: uidA,
        type: 100,
        seen: 1,
      })
    );

    await assertSucceeds(authedDb(uidB).ref(`Chats/dm/${chatId}`).get());
    await assertFails(authedDb(uidC).ref(`Chats/dm/${chatId}`).get());

    await assertSucceeds(
      authedDb(uidB).ref(`Chats/dm/${chatId}/${msgRef.key}`).update({ seen: 2 })
    );
    await assertFails(
      authedDb(uidB)
        .ref(`Chats/dm/${chatId}/${msgRef.key}`)
        .update({ content: "hack" })
    );
  });

  it("allows participants to update conversations and blocks outsiders", async () => {
    const conversation = {
      lastContent: "hi",
      lastMessageAt: 123,
      userId: uidA,
      otherId: uidA,
      otherName: "User A",
      otherPhotoUrl: "https://example.com/photo.png",
      state: "dm",
      unreadCount: 1,
      seen: 1,
    };

    await assertSucceeds(
      authedDb(uidA)
        .ref(`Users/Data/${uidB}/dm/${uidA}`)
        .set(conversation)
    );
    await assertFails(
      authedDb(uidC)
        .ref(`Users/Data/${uidB}/dm/${uidA}`)
        .set(conversation)
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
