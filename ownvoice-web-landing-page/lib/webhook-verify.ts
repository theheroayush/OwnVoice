import crypto from "crypto";

/**
 * Webhook signature verification for the payment gateways.
 *
 * Both Stripe and Cashfree sign the *raw* request body together with a
 * timestamp. Without this check anyone who knows a user id could POST a fake
 * "payment succeeded" event and mint themselves a lifetime license
 * (see app/api/webhooks/*).
 */

export interface VerificationResult {
  ok: boolean;
  reason?: string;
}

/** Constant-time comparison that never throws on length mismatch. */
function safeEqual(a: string, b: string): boolean {
  const bufA = Buffer.from(a, "utf8");
  const bufB = Buffer.from(b, "utf8");
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * Verifies a Stripe webhook signature header.
 *
 * Header format: `t=<unix seconds>,v1=<hex hmac>[,v1=<...>]`, where the signed
 * payload is `${t}.${rawBody}` and the key is the endpoint's signing secret.
 */
export function verifyStripeSignature(
  rawBody: string,
  signatureHeader: string | null,
  secret: string | undefined,
  toleranceSeconds = 300
): VerificationResult {
  if (!secret) return { ok: false, reason: "STRIPE_WEBHOOK_SECRET is not configured." };
  if (!signatureHeader) return { ok: false, reason: "Missing stripe-signature header." };

  let timestamp = "";
  const signatures: string[] = [];
  for (const part of signatureHeader.split(",")) {
    const [key, value] = part.split("=");
    if (key === "t") timestamp = value ?? "";
    if (key === "v1" && value) signatures.push(value);
  }

  if (!timestamp || signatures.length === 0) {
    return { ok: false, reason: "Malformed stripe-signature header." };
  }

  const age = Math.abs(Math.floor(Date.now() / 1000) - Number(timestamp));
  if (!Number.isFinite(age) || age > toleranceSeconds) {
    return { ok: false, reason: "Stripe webhook timestamp outside the tolerance window (replay?)." };
  }

  const expected = crypto
    .createHmac("sha256", secret)
    .update(`${timestamp}.${rawBody}`, "utf8")
    .digest("hex");

  const matched = signatures.some((sig) => safeEqual(sig, expected));
  return matched ? { ok: true } : { ok: false, reason: "Stripe signature mismatch." };
}

/**
 * Verifies a Cashfree webhook signature.
 *
 * Cashfree sends `x-webhook-timestamp` and `x-webhook-signature`, where the
 * signature is base64(HMAC-SHA256(timestamp + rawBody, CASHFREE_SECRET_KEY)).
 */
export function verifyCashfreeSignature(
  rawBody: string,
  signatureHeader: string | null,
  timestampHeader: string | null,
  secret: string | undefined
): VerificationResult {
  if (!secret) return { ok: false, reason: "CASHFREE_SECRET_KEY is not configured." };
  if (!signatureHeader || !timestampHeader) {
    return {
      ok: false,
      reason: "Missing x-webhook-signature / x-webhook-timestamp headers.",
    };
  }

  const expected = crypto
    .createHmac("sha256", secret)
    .update(`${timestampHeader}${rawBody}`, "utf8")
    .digest("base64");

  return safeEqual(signatureHeader, expected)
    ? { ok: true }
    : { ok: false, reason: "Cashfree signature mismatch." };
}
