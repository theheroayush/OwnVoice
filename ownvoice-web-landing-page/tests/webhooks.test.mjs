import test from "node:test";
import assert from "node:assert/strict";
import crypto from "crypto";

// Node strips TypeScript type annotations, so the real implementations can be
// imported directly — this test verifies the actual code, not a copy of it.
import {
  verifyStripeSignature,
  verifyCashfreeSignature,
} from "../lib/webhook-verify.ts";

const STRIPE_SECRET = "whsec_test_secret_value";
const CASHFREE_SECRET = "cashfree_test_secret_value";

function stripeHeader(body, secret, timestamp = Math.floor(Date.now() / 1000)) {
  const signature = crypto
    .createHmac("sha256", secret)
    .update(`${timestamp}.${body}`, "utf8")
    .digest("hex");
  return `t=${timestamp},v1=${signature}`;
}

function cashfreeSignature(body, secret, timestamp) {
  return crypto
    .createHmac("sha256", secret)
    .update(`${timestamp}${body}`, "utf8")
    .digest("base64");
}

test("Stripe webhook: accepts a correctly signed payload", () => {
  const body = JSON.stringify({ type: "checkout.session.completed", id: "evt_1" });
  const result = verifyStripeSignature(body, stripeHeader(body, STRIPE_SECRET), STRIPE_SECRET);
  assert.equal(result.ok, true);
});

test("Stripe webhook: rejects a tampered body (free-license minting attempt)", () => {
  const body = JSON.stringify({ type: "checkout.session.completed", amount_total: 4900 });
  const header = stripeHeader(body, STRIPE_SECRET);
  const forged = JSON.stringify({ type: "checkout.session.completed", amount_total: 0 });
  const result = verifyStripeSignature(forged, header, STRIPE_SECRET);
  assert.equal(result.ok, false);
});

test("Stripe webhook: rejects a wrong secret and a stale timestamp", () => {
  const body = "{}";
  assert.equal(
    verifyStripeSignature(body, stripeHeader(body, "attacker-secret"), STRIPE_SECRET).ok,
    false
  );

  const staleTimestamp = Math.floor(Date.now() / 1000) - 3600;
  assert.equal(
    verifyStripeSignature(body, stripeHeader(body, STRIPE_SECRET, staleTimestamp), STRIPE_SECRET).ok,
    false
  );
});

test("Stripe webhook: fails closed when headers or secret are missing", () => {
  assert.equal(verifyStripeSignature("{}", null, STRIPE_SECRET).ok, false);
  assert.equal(verifyStripeSignature("{}", "garbage", STRIPE_SECRET).ok, false);
  assert.equal(verifyStripeSignature("{}", stripeHeader("{}", STRIPE_SECRET), undefined).ok, false);
});

test("Cashfree webhook: accepts a valid signature and rejects forgeries", () => {
  const body = JSON.stringify({ type: "PAYMENT_SUCCESS_WEBHOOK" });
  const timestamp = String(Date.now());

  assert.equal(
    verifyCashfreeSignature(body, cashfreeSignature(body, CASHFREE_SECRET, timestamp), timestamp, CASHFREE_SECRET).ok,
    true
  );

  // Signature computed with a different secret -> rejected
  assert.equal(
    verifyCashfreeSignature(body, cashfreeSignature(body, "wrong-secret", timestamp), timestamp, CASHFREE_SECRET).ok,
    false
  );

  // Missing headers / missing secret -> rejected (fail closed)
  assert.equal(verifyCashfreeSignature(body, null, timestamp, CASHFREE_SECRET).ok, false);
  assert.equal(verifyCashfreeSignature(body, cashfreeSignature(body, CASHFREE_SECRET, timestamp), null, CASHFREE_SECRET).ok, false);
  assert.equal(verifyCashfreeSignature(body, cashfreeSignature(body, CASHFREE_SECRET, timestamp), timestamp, undefined).ok, false);
});
