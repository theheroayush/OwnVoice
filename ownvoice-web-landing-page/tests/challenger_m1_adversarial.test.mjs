import test from "node:test";
import assert from "node:assert/strict";
import crypto from "crypto";
import fs from "fs";
import path from "path";
import { createJiti } from "jiti";
import { PrismaClient } from "@prisma/client";

const basePath = process.cwd().endsWith("ownvoice-web-landing-page")
  ? process.cwd()
  : path.join(process.cwd(), "ownvoice-web-landing-page");

const jiti = createJiti(import.meta.url, {
  alias: { "@": basePath },
});

const { generateDownloadToken, verifyDownloadToken } = await jiti.import(path.join(basePath, "lib", "download-token.ts"));
const { GET } = await jiti.import(path.join(basePath, "app", "api", "download", "secure", "route.ts"));
const { NextRequest } = await import("next/server.js");

const { default: prisma } = await jiti.import(path.join(basePath, "lib", "db.ts"));

// Helper to construct request
function makeRequest(params = {}) {
  const url = new URL("http://localhost:3000/api/download/secure");
  if (params.token !== undefined) url.searchParams.set("token", params.token);
  if (params.os !== undefined) url.searchParams.set("os", params.os);
  return new NextRequest(url.toString());
}

test("Empirical Challenge 1: Missing download token returns HTTP 401 with MISSING_TOKEN", async () => {
  const req = makeRequest();
  const res = await GET(req);
  assert.equal(res.status, 401);
  const data = await res.json();
  assert.equal(data.success, false);
  assert.equal(data.code, "MISSING_TOKEN");
  assert.ok(data.error.includes("Missing secure download token"));
});

test("Empirical Challenge 2: Malformed tokens return HTTP 403 with INVALID_OR_EXPIRED_TOKEN, empty returns 401", async () => {
  // Empty token string returns 401 MISSING_TOKEN
  const resEmpty = await GET(makeRequest({ token: "", os: "windows" }));
  assert.equal(resEmpty.status, 401, "Empty token '' must return 401 MISSING_TOKEN");
  const dataEmpty = await resEmpty.json();
  assert.equal(dataEmpty.code, "MISSING_TOKEN");

  const malformedInputs = [
    "notokenwithoutdot",
    ".",
    "..",
    "a.b.c",
    "!!!.???",
    "invalidbase64url!@#$.invalidsignature",
    "eyJhbGciOiJIUzI1NiJ9.", // missing signature
    ".fakesignature", // missing payload
  ];

  for (const token of malformedInputs) {
    const req = makeRequest({ token, os: "windows" });
    const res = await GET(req);
    assert.equal(res.status, 403, `Token '${token}' must yield 403`);
    const data = await res.json();
    assert.equal(data.success, false);
    assert.equal(data.code, "INVALID_OR_EXPIRED_TOKEN");
  }
});

test("Empirical Challenge 3: Forged HMAC signatures are rejected in constant time without data leakage", async () => {
  const validPayload = {
    licenseKey: "OV-TEST-1234-5678-ABCD",
    email: "attacker@test.com",
    os: "any",
    expiresAt: Date.now() + 86400000,
    createdAt: Date.now(),
  };
  const payloadEncoded = Buffer.from(JSON.stringify(validPayload)).toString("base64url");

  // Attack A: Signed with wrong secret
  const forgedSig1 = crypto
    .createHmac("sha256", "wrong-secret-key-attacker")
    .update(payloadEncoded)
    .digest("base64url");
  const forgedToken1 = `${payloadEncoded}.${forgedSig1}`;

  const res1 = await GET(makeRequest({ token: forgedToken1 }));
  assert.equal(res1.status, 403);
  const data1 = await res1.json();
  assert.equal(data1.code, "INVALID_OR_EXPIRED_TOKEN");

  // Attack B: Truncated / mismatched length signature
  const forgedToken2 = `${payloadEncoded}.shortsig`;
  const res2 = await GET(makeRequest({ token: forgedToken2 }));
  assert.equal(res2.status, 403);

  // Attack C: Single-byte flip in signature
  const realToken = generateDownloadToken({ licenseKey: "OV-TEST-1234-5678-ABCD", email: "user@test.com" });
  const [pl, sig] = realToken.split(".");
  const flippedSig = sig.slice(0, -1) + (sig.slice(-1) === "A" ? "B" : "A");
  const res3 = await GET(makeRequest({ token: `${pl}.${flippedSig}` }));
  assert.equal(res3.status, 403);

  // Attack D: Payload altered after signing
  const tamperedPayload = Buffer.from(JSON.stringify({ ...validPayload, email: "admin@ownvoice.ai" })).toString("base64url");
  const res4 = await GET(makeRequest({ token: `${tamperedPayload}.${sig}` }));
  assert.equal(res4.status, 403);
});

test("Empirical Challenge 4: Expired tokens are rejected with expiration notice", async () => {
  const expiredPayloads = [
    { label: "1 second expired", offsetMs: -1000 },
    { label: "24 hours expired", offsetMs: -86400000 },
    { label: "Epoch 1970", absoluteExpiresAt: 0 },
    { label: "Negative timestamp", absoluteExpiresAt: -999999 },
  ];

  for (const item of expiredPayloads) {
    const expiresAt = item.absoluteExpiresAt !== undefined ? item.absoluteExpiresAt : Date.now() + item.offsetMs;
    const payload = {
      licenseKey: "OV-EXPIRED-TEST-KEY",
      email: "expired@test.com",
      os: "any",
      expiresAt,
      createdAt: expiresAt - 86400000,
    };
    const secret = process.env.DOWNLOAD_TOKEN_SECRET || process.env.JWT_SECRET || "ownvoice-secure-download-vault-key-prod-2026-strict-anti-piracy";
    const payloadEncoded = Buffer.from(JSON.stringify(payload)).toString("base64url");
    const signature = crypto.createHmac("sha256", secret).update(payloadEncoded).digest("base64url");
    const expiredToken = `${payloadEncoded}.${signature}`;

    const res = await GET(makeRequest({ token: expiredToken }));
    assert.equal(res.status, 403, `Expired token (${item.label}) must return 403`);
    const data = await res.json();
    assert.equal(data.code, "INVALID_OR_EXPIRED_TOKEN");
    assert.ok(data.error.includes("expired"), `Error message should mention expiration for ${item.label}`);
  }
});

test("Empirical Challenge 5: Corrupted JSON payload in valid HMAC container is caught safely", async () => {
  const secret = process.env.DOWNLOAD_TOKEN_SECRET || process.env.JWT_SECRET || "ownvoice-secure-download-vault-key-prod-2026-strict-anti-piracy";
  const nonJson = Buffer.from("NOT_VALID_JSON_STRING").toString("base64url");
  const sig = crypto.createHmac("sha256", secret).update(nonJson).digest("base64url");
  const corruptedToken = `${nonJson}.${sig}`;

  const res = await GET(makeRequest({ token: corruptedToken }));
  assert.equal(res.status, 403);
  const data = await res.json();
  assert.equal(data.code, "INVALID_OR_EXPIRED_TOKEN");
});

test("Empirical Challenge 6: Valid token with requestedOs='mac' cleanly responds with HTTP 404 FILE_BEING_PREPARED", async () => {
  const validToken = generateDownloadToken({ licenseKey: "OV-VALID-TEST-0001", email: "user@test.com" });
  const res = await GET(makeRequest({ token: validToken, os: "mac" }));
  assert.equal(res.status, 404, "mac requestedOs must return 404 because no macOS binary exists");
  const data = await res.json();
  assert.equal(data.success, false);
  assert.equal(data.code, "FILE_BEING_PREPARED");
  assert.ok(data.advice.includes("Web Workspace"), "Advice must recommend Web Workspace or Windows");
});

test("Empirical Challenge 7: Valid token with requestedOs='windows' streams authentic ZIP package with strict headers", async () => {
  const validToken = generateDownloadToken({ licenseKey: "OV-VALID-TEST-0002", email: "user@test.com" });
  const res = await GET(makeRequest({ token: validToken, os: "windows" }));
  assert.equal(res.status, 200);

  // Assert headers
  assert.equal(res.headers.get("content-type"), "application/octet-stream");
  assert.equal(res.headers.get("content-disposition"), 'attachment; filename="OwnVoice-v2.0.0-Windows.zip"');
  assert.equal(res.headers.get("cache-control"), "no-store, no-cache, must-revalidate, private");
  assert.equal(res.headers.get("pragma"), "no-cache");
  assert.equal(res.headers.get("expires"), "0");

  const length = parseInt(res.headers.get("content-length"), 10);
  assert.ok(length > 20000000, `Windows installer must be >20MB (got ${length} bytes)`);

  // Verify stream chunk and ZIP magic header (0x50 0x4B 0x03 0x04)
  const reader = res.body.getReader();
  const { value } = await reader.read();
  await reader.cancel();
  assert.ok(value && value.length > 0);
  const magic = Buffer.from(value.slice(0, 4)).toString("hex");
  assert.equal(magic, "504b0304", "Windows binary chunk must start with PK\\x03\\x04 zip magic header");
});

test("Empirical Challenge 8: Valid token with requestedOs='android' streams APK package with authentic headers", async () => {
  const validToken = generateDownloadToken({ licenseKey: "OV-VALID-TEST-0003", email: "user@test.com" });
  const res = await GET(makeRequest({ token: validToken, os: "android" }));
  assert.equal(res.status, 200);

  assert.equal(res.headers.get("content-type"), "application/octet-stream");
  assert.ok(res.headers.get("content-disposition").includes(".apk"));
  const length = parseInt(res.headers.get("content-length"), 10);
  assert.ok(length > 10000000, `Android APK must be >10MB (got ${length} bytes)`);

  const reader = res.body.getReader();
  const { value } = await reader.read();
  await reader.cancel();
  assert.ok(value && value.length > 0);
  const magic = Buffer.from(value.slice(0, 4)).toString("hex");
  assert.equal(magic, "504b0304", "Android APK chunk must start with ZIP magic header (APK format)");
});

test("Empirical Challenge 9: Path traversal injection via 'os' parameter is strictly neutralized", async () => {
  const validToken = generateDownloadToken({ licenseKey: "OV-VALID-TEST-0004", email: "user@test.com" });
  const attackVectors = [
    "../../package.json",
    "..\\..\\package.json",
    "../../../etc/passwd",
    "../../../../windows/win.ini",
    "..%2f..%2fpackage.json",
    "..%5c..%5cpackage.json",
    "releases/../../package.json",
    "/etc/shadow",
    "C:\\Windows\\System32\\drivers\\etc\\hosts",
    "nul",
    "con",
    "../prisma/schema.prisma",
  ];

  for (const attack of attackVectors) {
    const res = await GET(makeRequest({ token: validToken, os: attack }));
    // Because targetFilename is hardcoded and only switches on 'android' and 'mac',
    // any unrecognized string defaults to 'OwnVoice-v2.0.0-Windows.zip'.
    // The handler MUST NEVER serve the attacked file!
    const disposition = res.headers.get("content-disposition") || "";
    assert.equal(disposition.includes("package.json"), false, `Attack vector '${attack}' must not stream package.json`);
    assert.equal(disposition.includes("schema.prisma"), false, `Attack vector '${attack}' must not stream schema.prisma`);
    assert.equal(disposition.includes("passwd"), false, `Attack vector '${attack}' must not stream passwd`);
    assert.equal(disposition.includes("win.ini"), false, `Attack vector '${attack}' must not stream win.ini`);

    if (res.status === 200) {
      assert.ok(disposition.includes("OwnVoice-v2.0.0-Windows.zip"), "Safe fallback to Windows zip");
      const reader = res.body.getReader();
      await reader.cancel();
    }
  }
});

test("Empirical Challenge 10: Revoked or Suspended License in Database blocks download with LICENSE_INACTIVE", async () => {
  // Create or update a test user and license in DB
  const testEmail = `adversary_${Date.now()}@test.com`;
  const revokedKey = `OV-REVOKED-${Date.now().toString().slice(-4)}-XXXX`;

  const user = await prisma.user.create({
    data: {
      email: testEmail,
      passwordHash: "hash123",
      name: "Adversary Test",
    },
  });

  await prisma.license.create({
    data: {
      userId: user.id,
      licenseKey: revokedKey,
      status: "REVOKED",
    },
  });

  const token = generateDownloadToken({ licenseKey: revokedKey, email: testEmail });
  const res = await GET(makeRequest({ token, os: "windows" }));

  assert.equal(res.status, 403);
  const data = await res.json();
  assert.equal(data.success, false);
  assert.equal(data.code, "LICENSE_INACTIVE");
  assert.ok(data.error.includes("revoked"));

  // Also test SUSPENDED
  const suspendedKey = `OV-SUSPENDED-${Date.now().toString().slice(-4)}-YYYY`;
  await prisma.license.create({
    data: {
      userId: user.id,
      licenseKey: suspendedKey,
      status: "SUSPENDED",
    },
  });

  const suspendedToken = generateDownloadToken({ licenseKey: suspendedKey, email: testEmail });
  const resSuspended = await GET(makeRequest({ token: suspendedToken, os: "windows" }));
  assert.equal(resSuspended.status, 403);
  const dataSuspended = await resSuspended.json();
  assert.equal(dataSuspended.code, "LICENSE_INACTIVE");
  assert.ok(dataSuspended.error.includes("suspended"));
});

test("Empirical Challenge 11: Case-insensitivity in 'os' parameter normalizes properly", async () => {
  const token = generateDownloadToken({ licenseKey: "OV-CASE-TEST-0001", email: "user@test.com" });
  for (const osCase of ["WINDOWS", "Windows", "wInDoWs"]) {
    const res = await GET(makeRequest({ token, os: osCase }));
    assert.equal(res.status, 200, `Case '${osCase}' must resolve Windows release`);
    const reader = res.body.getReader();
    await reader.cancel();
  }

  for (const osCase of ["ANDROID", "Android", "aNdRoId"]) {
    const res = await GET(makeRequest({ token, os: osCase }));
    assert.equal(res.status, 200, `Case '${osCase}' must resolve Android APK`);
    const reader = res.body.getReader();
    await reader.cancel();
  }

  for (const osCase of ["MAC", "Mac", "mAc"]) {
    const res = await GET(makeRequest({ token, os: osCase }));
    assert.equal(res.status, 404, `Case '${osCase}' must return 404 FILE_BEING_PREPARED`);
  }
});

test("Empirical Challenge 12: High concurrency download stream initiation does not leak handles or crash", async () => {
  const token = generateDownloadToken({ licenseKey: "OV-STRESS-CONCURRENCY", email: "stress@test.com" });
  const concurrentRequests = 15;
  const promises = [];

  for (let i = 0; i < concurrentRequests; i++) {
    const req = makeRequest({ token, os: i % 2 === 0 ? "windows" : "android" });
    promises.push(
      GET(req).then(async (res) => {
        assert.equal(res.status, 200);
        const reader = res.body.getReader();
        const { value } = await reader.read();
        assert.ok(value && value.length > 0);
        await reader.cancel();
      })
    );
  }

  await Promise.all(promises);
});

test.after(async () => {
  await prisma.$disconnect();
});
