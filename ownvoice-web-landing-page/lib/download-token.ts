import crypto from "crypto";

const DEV_DOWNLOAD_SECRET = "ownvoice-insecure-dev-download-secret-change-me";

/**
 * Resolves the HMAC secret used to sign/verify download tokens.
 * Production requires a real secret so signed download links cannot be forged.
 */
function resolveTokenSecret(): string {
  const secret =
    process.env.DOWNLOAD_TOKEN_SECRET || process.env.JWT_SECRET;
  if (secret && secret.length >= 32) return secret;
  if (process.env.NODE_ENV === "production") {
    throw new Error(
      "DOWNLOAD_TOKEN_SECRET (or JWT_SECRET) is not set or shorter than 32 characters. " +
        "Generate one with: node -e \"crypto.randomBytes(32).toString('hex')\""
    );
  }
  return secret || DEV_DOWNLOAD_SECRET;
}

export interface DownloadTokenPayload {
  licenseKey: string;
  email: string;
  os: "windows" | "mac" | "linux" | "any";
  expiresAt: number; // Unix timestamp in milliseconds
  createdAt: number;
}

export interface TokenVerificationResult {
  isValid: boolean;
  payload?: DownloadTokenPayload;
  error?: string;
}

/**
 * Generates a signed, tamper-proof, time-expiring download token for desktop installers.
 * By default, tokens expire in 24 hours (86,400 seconds).
 */
export function generateDownloadToken(params: {
  licenseKey: string;
  email: string;
  os?: "windows" | "mac" | "linux" | "any";
  validHours?: number;
}): string {
  const hours = params.validHours ?? 24;
  const now = Date.now();
  const expiresAt = now + hours * 60 * 60 * 1000;

  const payload: DownloadTokenPayload = {
    licenseKey: params.licenseKey.trim().toUpperCase(),
    email: params.email.trim().toLowerCase(),
    os: params.os || "any",
    expiresAt,
    createdAt: now,
  };

  const payloadEncoded = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const signature = crypto
    .createHmac("sha256", resolveTokenSecret())
    .update(payloadEncoded)
    .digest("base64url");

  return `${payloadEncoded}.${signature}`;
}

/**
 * Verifies a download token, checking signature integrity and expiration time.
 */
export function verifyDownloadToken(token: string): TokenVerificationResult {
  if (!token || typeof token !== "string" || !token.includes(".")) {
    return { isValid: false, error: "Missing or malformed download token." };
  }

  const [payloadEncoded, signature] = token.split(".");
  if (!payloadEncoded || !signature) {
    return { isValid: false, error: "Invalid token format." };
  }

  // Verify HMAC signature in constant time
  const expectedSignature = crypto
    .createHmac("sha256", resolveTokenSecret())
    .update(payloadEncoded)
    .digest("base64url");

  const sigBuf = Buffer.from(signature);
  const expBuf = Buffer.from(expectedSignature);

  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
    return { isValid: false, error: "Invalid or forged download token." };
  }

  try {
    const payload: DownloadTokenPayload = JSON.parse(
      Buffer.from(payloadEncoded, "base64url").toString("utf-8")
    );

    // Verify expiration
    if (Date.now() > payload.expiresAt) {
      return {
        isValid: false,
        payload,
        error: "Download token has expired (valid for 24 hours). Please generate a new link.",
      };
    }

    return { isValid: true, payload };
  } catch {
    return { isValid: false, error: "Corrupted token payload." };
  }
}
