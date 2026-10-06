import test from "node:test";
import assert from "node:assert/strict";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";

// These are integration tests: they need a generated Prisma client and a
// reachable database. When that is unavailable (fresh checkout, CI without a
// DB, missing `prisma generate`) they skip with an actionable message instead
// of failing the whole `npm test` run.
let prisma = null;
let unavailableReason = null;

try {
  const { PrismaClient } = await import("@prisma/client");
  prisma = new PrismaClient();
  // Probe a real table so a generated-but-unmigrated database is detected too.
  await prisma.user.count();
  // Offline sandboxes may install web/tools/prisma-runtime-stub.js, which
  // answers every query with empty data. Running these tests against it would
  // assert nothing, so report it as "unavailable" and skip.
  if (globalThis.__OWNVOICE_PRISMA_STUB__) {
    throw new Error("placeholder Prisma runtime stub installed (no real database)");
  }
} catch (error) {
  unavailableReason =
    `Prisma client/database unavailable (${String(error.message).split("\n")[0]}). ` +
    "Run `npx prisma generate && npx prisma db push` and set DATABASE_URL to enable these tests.";
  prisma = null;
}

const integrationTest = unavailableReason ? test.skip : test;

if (unavailableReason) {
  console.warn(`[api.test] ${unavailableReason}`);
}

const JWT_SECRET = process.env.JWT_SECRET || "ownvoice-insecure-dev-jwt-secret-change-me";

integrationTest("Database connectivity and user creation", async () => {
  const testEmail = `test_${Date.now()}@example.com`;
  const passwordHash = await bcrypt.hash("Password123!", 10);

  const user = await prisma.user.create({
    data: {
      email: testEmail,
      passwordHash,
      name: "Test User",
      settings: {
        create: {
          defaultProvider: "openai",
          defaultModel: "gpt-4o",
          defaultTone: "NATURAL",
        },
      },
    },
    include: {
      settings: true,
    },
  });

  assert.equal(user.email, testEmail);
  assert.equal(user.settings?.defaultProvider, "openai");

  // Verify password comparison
  const isValid = await bcrypt.compare("Password123!", user.passwordHash);
  assert.equal(isValid, true);

  // Verify JWT signing and decoding
  const token = jwt.sign({ sub: user.id, email: user.email, role: user.role }, JWT_SECRET);
  const decoded = jwt.verify(token, JWT_SECRET);
  assert.equal(decoded.sub, user.id);
});

integrationTest("License creation and verification flow", async () => {
  const user = await prisma.user.findFirst();
  assert.ok(user, "User must exist");

  const licenseKey = `OV-TEST-${Date.now().toString().slice(-4)}-AAAA-BBBB`;
  const license = await prisma.license.create({
    data: {
      userId: user.id,
      licenseKey,
      status: "ACTIVE",
    },
  });

  assert.equal(license.status, "ACTIVE");
  assert.equal(license.licenseKey, licenseKey);

  const fetched = await prisma.license.findUnique({
    where: { licenseKey },
  });
  assert.equal(fetched?.id, license.id);
});

integrationTest("Generation and version history flow", async () => {
  const user = await prisma.user.findFirst();
  assert.ok(user, "User must exist");

  const gen = await prisma.generation.create({
    data: {
      userId: user.id,
      title: "Client Reschedule",
      intent: "MESSAGE",
      targetTone: "NATURAL",
      rawInput: "tell rahul meeting moves to tomorrow afternoon",
      outputText: "Hey Rahul, can we move our meeting to tomorrow afternoon? Something came up today. Let me know what works for you.",
      wordCount: 22,
      providerUsed: "openai",
      modelUsed: "gpt-4o",
      versions: {
        create: {
          actionTaken: "INITIAL",
          outputText: "Hey Rahul, can we move our meeting to tomorrow afternoon? Something came up today. Let me know what works for you.",
        },
      },
    },
    include: {
      versions: true,
    },
  });

  assert.equal(gen.versions.length, 1);
  assert.equal(gen.versions[0].actionTaken, "INITIAL");

  // Search generation
  const searchResults = await prisma.generation.findMany({
    where: {
      userId: user.id,
      OR: [
        { title: { contains: "Client" } },
        { outputText: { contains: "Rahul" } },
      ],
    },
  });

  assert.ok(searchResults.length > 0);
  assert.ok(searchResults.some((s) => s.id === gen.id));
});

test.after(async () => {
  if (prisma) {
    await prisma.$disconnect();
  }
});
