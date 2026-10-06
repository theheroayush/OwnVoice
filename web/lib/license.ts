import crypto from "crypto";
import prisma from "./db";

/**
 * Generates a high-entropy formatted license key:
 * Format: OV-XXXX-XXXX-XXXX-XXXX (16 chars uppercase alphanumeric)
 */
export function generateLicenseKey(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Base32 without confusing chars (0, O, 1, I)
  const segments: string[] = [];

  // 256 is not divisible by the 32-character alphabet, so `byte % 32` would
  // bias the first 8 letters (each ~1.5x more likely). Reject the (few) bytes
  // that fall outside the largest multiple of the alphabet size instead.
  const limit = 256 - (256 % chars.length);
  const pickChar = (): string => {
    for (;;) {
      const [byte] = crypto.randomBytes(1);
      if (byte < limit) return chars[byte % chars.length];
    }
  };

  for (let s = 0; s < 4; s++) {
    let segment = "";
    for (let i = 0; i < 4; i++) {
      segment += pickChar();
    }
    segments.push(segment);
  }

  return `OV-${segments.join("-")}`;
}

export type LicenseStatus = "ACTIVE" | "REVOKED" | "REFUNDED" | "SUSPENDED";

export interface LicenseVerificationResult {
  isValid: boolean;
  licenseKey: string;
  status: LicenseStatus | "NOT_FOUND";
  userId?: string;
  activatedAt?: Date;
  message: string;
}

export async function verifyLicenseKey(licenseKey: string): Promise<LicenseVerificationResult> {
  const normalizedKey = licenseKey.trim().toUpperCase();

  const license = await prisma.license.findUnique({
    where: { licenseKey: normalizedKey },
    include: { user: true },
  });

  if (!license) {
    return {
      isValid: false,
      licenseKey: normalizedKey,
      status: "NOT_FOUND",
      message: "License key does not exist. Please check your key or purchase a license.",
    };
  }

  if (license.status !== "ACTIVE") {
    return {
      isValid: false,
      licenseKey: normalizedKey,
      status: license.status as LicenseStatus,
      userId: license.userId,
      message: `License is currently ${license.status.toLowerCase()}. Please contact support.`,
    };
  }

  if (license.expiresAt && license.expiresAt < new Date()) {
    return {
      isValid: false,
      licenseKey: normalizedKey,
      status: "SUSPENDED",
      userId: license.userId,
      message: "License has expired.",
    };
  }

  return {
    isValid: true,
    licenseKey: normalizedKey,
    status: "ACTIVE",
    userId: license.userId,
    activatedAt: license.activatedAt,
    message: "License is active and valid.",
  };
}

export async function activateLicenseForUser(
  userId: string,
  providedKey?: string,
  purchaseId?: string
) {
  const keyToUse = providedKey ? providedKey.trim().toUpperCase() : generateLicenseKey();

  const license = await prisma.license.create({
    data: {
      userId,
      licenseKey: keyToUse,
      productId: "ownvoice-lifetime",
      purchaseId,
      status: "ACTIVE",
      activatedAt: new Date(),
    },
  });

  return license;
}
