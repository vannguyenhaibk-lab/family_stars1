const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_parent_123";
const BOB_UID = "bob_parent_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read tasks or children", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("children").get());
});

test("Authenticated user: cannot access another user's family data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      id: "task_1",
      userId: ALICE_UID,
      childId: "child_1",
      title: "Clean Room",
      stars: 3,
      status: "ASSIGNED",
      createdByRole: "PARENT",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
});

test("Authenticated user: can create and read their own child and tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Create child
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("children").doc("child_1").set({
      id: "child_1",
      userId: ALICE_UID,
      name: "Bé An",
      starsBalance: 10,
      totalStarsEarned: 10,
      age: 9,
      createdAt: now,
    })
  );

  // Read child
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("children").doc("child_1").get()
  );

  // Create task
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      id: "task_1",
      userId: ALICE_UID,
      childId: "child_1",
      title: "Đọc sách 20 phút",
      stars: 2,
      status: "ASSIGNED",
      createdByRole: "PARENT",
      createdAt: now,
    })
  );

  // Read task list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
});

test("Validation: rejects invalid star counts (e.g. 0 or negative)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      id: "task_bad",
      userId: ALICE_UID,
      childId: "child_1",
      title: "Invalid Task",
      stars: 0, // Must be >= 1
      status: "ASSIGNED",
      createdByRole: "PARENT",
      createdAt: now,
    })
  );
});
