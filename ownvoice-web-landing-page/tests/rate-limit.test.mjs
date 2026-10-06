import test from "node:test";
import assert from "node:assert/strict";

import { clearFailures, isLockedOut, recordFailure } from "../lib/rate-limit.ts";

test("recordFailure allows attempts up to the limit then locks out", () => {
  const key = `test-limit-${Date.now()}`;

  for (let i = 0; i < 5; i++) {
    const result = recordFailure(key, 5, 300);
    assert.equal(result.allowed, true, `attempt ${i + 1} should be allowed`);
  }

  const sixth = recordFailure(key, 5, 300);
  assert.equal(sixth.allowed, false);
  assert.ok(sixth.retryAfterSeconds > 0);

  // A locked-out key stays locked until the window elapses.
  assert.equal(isLockedOut(key, 5, 300).allowed, false);
});

test("clearFailures unlocks a key after a successful login", () => {
  const key = `test-clear-${Date.now()}`;
  for (let i = 0; i < 5; i++) recordFailure(key, 5, 300);
  assert.equal(isLockedOut(key, 5, 300).allowed, false);

  clearFailures(key);
  assert.equal(isLockedOut(key, 5, 300).allowed, true);
});

test("expired failures fall outside the sliding window", () => {
  const key = `test-window-${Date.now()}`;
  // windowSeconds = 0 means every previous failure is already stale.
  recordFailure(key, 1, 0);
  const next = recordFailure(key, 1, 0);
  assert.equal(next.allowed, true);
});
