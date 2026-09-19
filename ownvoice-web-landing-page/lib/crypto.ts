import crypto from "crypto";

const ENCRYPTION_SECRET = process.env.ENCRYPTION_SECRET || "ownvoice-aes-key-for-byok-tokens-32-chars-long";

// Derive 32-byte key using SHA-256
function getDerivedKey(): Buffer {
  return crypto.createHash("sha256").update(ENCRYPTION_SECRET).digest();
}

/**
 * Encrypts a raw API key using AES-256-GCM.
 * Output format: iv:authTag:encryptedHex
 */
export function encryptApiKey(plainTextKey: string): string {
  if (!plainTextKey || typeof plainTextKey !== "string") {
    throw new Error("Invalid API key provided for encryption.");
  }
  const key = getDerivedKey();
  const iv = crypto.randomBytes(16); // 128-bit IV
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);

  let encrypted = cipher.update(plainTextKey, "utf8", "hex");
  encrypted += cipher.final("hex");

  const authTag = cipher.getAuthTag().toString("hex");

  return `${iv.toString("hex")}:${authTag}:${encrypted}`;
}

/**
 * Decrypts an AES-256-GCM ciphertext payload into plaintext.
 */
export function decryptApiKey(encryptedPayload: string): string {
  if (!encryptedPayload || !encryptedPayload.includes(":")) {
    throw new Error("Malformed encrypted payload.");
  }

  const parts = encryptedPayload.split(":");
  if (parts.length !== 3) {
    throw new Error("Invalid encrypted payload structure.");
  }

  const [ivHex, authTagHex, cipherTextHex] = parts;
  const key = getDerivedKey();
  const iv = Buffer.from(ivHex, "hex");
  const authTag = Buffer.from(authTagHex, "hex");

  const decipher = crypto.createDecipheriv("aes-256-gcm", key, iv);
  decipher.setAuthTag(authTag);

  let decrypted = decipher.update(cipherTextHex, "hex", "utf8");
  decrypted += decipher.final("utf8");

  return decrypted;
}

/**
 * Creates a safe display hint for the frontend (e.g. ••••••••8F21).
 * Never exposes the full raw key.
 */
export function maskApiKey(rawKey: string): string {
  if (!rawKey) return "••••••••••••";
  const trimmed = rawKey.trim();
  if (trimmed.length <= 6) {
    return "••••" + trimmed.slice(-2);
  }
  const lastFour = trimmed.slice(-4);
  return "••••••••" + lastFour;
}

/**
 * Generates a high-entropy random API verification challenge
 */
export function generateRandomToken(bytes: number = 32): string {
  return crypto.randomBytes(bytes).toString("hex");
}
