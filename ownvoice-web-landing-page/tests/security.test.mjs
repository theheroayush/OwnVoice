import test from "node:test";
import assert from "node:assert/strict";
import crypto from "crypto";

// Test AES-256-GCM encryption & decryption
const ENCRYPTION_SECRET = "ownvoice-aes-key-for-byok-tokens-32-chars-long";
function getDerivedKey() {
  return crypto.createHash("sha256").update(ENCRYPTION_SECRET).digest();
}

function encryptApiKey(plainTextKey) {
  const key = getDerivedKey();
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  let encrypted = cipher.update(plainTextKey, "utf8", "hex");
  encrypted += cipher.final("hex");
  const authTag = cipher.getAuthTag().toString("hex");
  return `${iv.toString("hex")}:${authTag}:${encrypted}`;
}

function decryptApiKey(encryptedPayload) {
  const [ivHex, authTagHex, cipherTextHex] = encryptedPayload.split(":");
  const key = getDerivedKey();
  const decipher = crypto.createDecipheriv("aes-256-gcm", key, Buffer.from(ivHex, "hex"));
  decipher.setAuthTag(Buffer.from(authTagHex, "hex"));
  let decrypted = decipher.update(cipherTextHex, "hex", "utf8");
  decrypted += decipher.final("utf8");
  return decrypted;
}

function maskApiKey(rawKey) {
  if (!rawKey) return "••••••••••••";
  const trimmed = rawKey.trim();
  if (trimmed.length <= 6) return "••••" + trimmed.slice(-2);
  return "••••••••" + trimmed.slice(-4);
}

function generateLicenseKey() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const segments = [];
  for (let s = 0; s < 4; s++) {
    let segment = "";
    const randomBytes = crypto.randomBytes(4);
    for (let i = 0; i < 4; i++) {
      segment += chars[randomBytes[i] % chars.length];
    }
    segments.push(segment);
  }
  return `OV-${segments.join("-")}`;
}

test("AES-256-GCM encrypts and decrypts accurately with authentication tag", () => {
  const secretKey = "sk-proj-test123456789abcdef9999";
  const ciphertext = encryptApiKey(secretKey);

  assert.notEqual(ciphertext, secretKey);
  assert.equal(ciphertext.split(":").length, 3);

  const decrypted = decryptApiKey(ciphertext);
  assert.equal(decrypted, secretKey);
});

test("AES-256-GCM fails decryption when ciphertext or tag is tampered", () => {
  const secretKey = "sk-proj-authentic-key-12345";
  const ciphertext = encryptApiKey(secretKey);
  const parts = ciphertext.split(":");
  // Tamper with tag
  parts[1] = "00".repeat(16);
  const tampered = parts.join(":");

  assert.throws(() => {
    decryptApiKey(tampered);
  });
});

test("maskApiKey never exposes full key and displays clean suffix hint", () => {
  assert.equal(maskApiKey("sk-proj-9876543210ABCDEF"), "••••••••CDEF");
  assert.equal(maskApiKey("gsk_12345678"), "••••••••5678");
  assert.equal(maskApiKey(""), "••••••••••••");
});

test("generateLicenseKey formats correctly as OV-XXXX-XXXX-XXXX-XXXX", () => {
  const key = generateLicenseKey();
  assert.match(key, /^OV-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/);
});
