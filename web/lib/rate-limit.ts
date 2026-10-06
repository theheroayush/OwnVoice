/**
 * Minimal in-process rate limiter for authentication endpoints.
 *
 * The failing-credential endpoints (login, register) are the classic
 * brute-force targets. A single Next.js server instance keeps the counters in
 * memory, which is enough to stop scripted attacks; for multi-instance
 * deployments swap `buckets` for Redis/Upstash while keeping this interface.
 */

interface Bucket {
  failures: number[];
}

const buckets = new Map<string, Bucket>();

export interface RateLimitResult {
  allowed: boolean;
  retryAfterSeconds: number;
  remaining: number;
}

/**
 * Records a failed attempt for `key` and reports whether the caller is now
 * locked out.
 *
 * @param key            usually `${route}:${ip}:${email}`
 * @param maxAttempts    failures allowed inside `windowSeconds`
 * @param windowSeconds  sliding window length
 * @param lockoutSeconds how long to block once the limit is hit
 */
export function recordFailure(
  key: string,
  maxAttempts = 5,
  windowSeconds = 300,
  lockoutSeconds = 300
): RateLimitResult {
  const now = Date.now();
  const windowMs = windowSeconds * 1000;
  const bucket = buckets.get(key) ?? { failures: [] };
  bucket.failures = bucket.failures.filter((t) => now - t < windowMs);

  if (bucket.failures.length >= maxAttempts) {
    const oldest = bucket.failures[0];
    buckets.set(key, bucket);
    return {
      allowed: false,
      retryAfterSeconds: Math.ceil((oldest + windowMs - now) / 1000) || lockoutSeconds,
      remaining: 0,
    };
  }

  bucket.failures.push(now);
  buckets.set(key, bucket);
  return {
    allowed: true,
    retryAfterSeconds: 0,
    remaining: Math.max(0, maxAttempts - bucket.failures.length),
  };
}

/** True when `key` is currently locked out (without recording a new attempt). */
export function isLockedOut(key: string, maxAttempts = 5, windowSeconds = 300): RateLimitResult {
  const now = Date.now();
  const windowMs = windowSeconds * 1000;
  const bucket = buckets.get(key);
  if (!bucket) return { allowed: true, retryAfterSeconds: 0, remaining: maxAttempts };

  bucket.failures = bucket.failures.filter((t) => now - t < windowMs);
  if (bucket.failures.length >= maxAttempts) {
    return {
      allowed: false,
      retryAfterSeconds: Math.ceil((bucket.failures[0] + windowMs - now) / 1000),
      remaining: 0,
    };
  }
  return { allowed: true, retryAfterSeconds: 0, remaining: maxAttempts - bucket.failures.length };
}

/** Clears the counter after a successful authentication. */
export function clearFailures(key: string): void {
  buckets.delete(key);
}

/** Best-effort client IP from the usual proxy headers. */
export function clientIp(req: Request): string {
  const forwarded = req.headers.get("x-forwarded-for");
  if (forwarded) return forwarded.split(",")[0].trim();
  return req.headers.get("x-real-ip") ?? "unknown";
}
