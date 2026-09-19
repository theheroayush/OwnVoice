import assert from "node:assert/strict";

const BASE_URL = "http://localhost:3000";

async function runProbes() {
  console.log("=== RUNNING LIVE HTTP ROUTE PROBES ===");

  // 1. Probe Public Pages
  const pages = ["/", "/features", "/how-it-works", "/pricing", "/security", "/faq", "/download", "/docs", "/docs/getting-started", "/login", "/signup"];
  for (const page of pages) {
    const res = await fetch(`${BASE_URL}${page}`);
    assert.equal(res.status, 200, `Page ${page} must return 200 OK`);
    const text = await res.text();
    assert.ok(text.includes("Own Voice"), `Page ${page} must include brand text`);
    console.log(`✓ Probe ${page} -> 200 OK`);
  }

  // 2. Probe Login API with Seeded Admin
  console.log("Probing /api/auth/login...");
  const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: "admin@ownvoice.ai",
      password: "AdminPassword123!",
    }),
  });

  assert.equal(loginRes.status, 200, "Login must return 200");
  const loginData = await loginRes.json();
  assert.equal(loginData.success, true);
  assert.equal(loginData.user.email, "admin@ownvoice.ai");
  console.log("✓ /api/auth/login authenticated successfully");

  // Extract session cookie
  const cookieHeader = loginRes.headers.get("set-cookie");
  assert.ok(cookieHeader, "Session cookie must be set");
  const sessionCookie = cookieHeader.split(";")[0];
  console.log(`✓ Received session cookie: ${sessionCookie.substring(0, 25)}...`);

  // 3. Probe /api/auth/me with Cookie
  console.log("Probing /api/auth/me...");
  const meRes = await fetch(`${BASE_URL}/api/auth/me`, {
    headers: { Cookie: sessionCookie },
  });
  assert.equal(meRes.status, 200);
  const meData = await meRes.json();
  assert.equal(meData.success, true);
  assert.equal(meData.user.email, "admin@ownvoice.ai");
  assert.equal(meData.user.role, "ADMIN");
  console.log("✓ /api/auth/me returned authenticated user profile");

  // 4. Probe /api/admin/stats as Admin
  console.log("Probing /api/admin/stats...");
  const statsRes = await fetch(`${BASE_URL}/api/admin/stats`, {
    headers: { Cookie: sessionCookie },
  });
  assert.equal(statsRes.status, 200);
  const statsData = await statsRes.json();
  assert.equal(statsData.success, true);
  assert.ok(statsData.stats.totalUsers >= 1);
  console.log(`✓ /api/admin/stats returned metrics (Total Users: ${statsData.stats.totalUsers}, Active Licenses: ${statsData.stats.activeLicenses})`);

  // 5. Probe Purchase Checkout & License Generation
  console.log("Probing /api/licenses/checkout...");
  const checkoutRes = await fetch(`${BASE_URL}/api/licenses/checkout`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Cookie: sessionCookie,
    },
    body: JSON.stringify({ currency: "USD", provider: "stripe" }),
  });
  assert.equal(checkoutRes.status, 200);
  const checkoutData = await checkoutRes.json();
  assert.equal(checkoutData.success, true);
  assert.match(checkoutData.licenseKey, /^OV-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/);
  console.log(`✓ /api/licenses/checkout issued lifetime key: ${checkoutData.licenseKey}`);

  // 6. Probe License Verification API
  console.log("Probing /api/licenses/verify...");
  const verifyRes = await fetch(`${BASE_URL}/api/licenses/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ licenseKey: checkoutData.licenseKey }),
  });
  assert.equal(verifyRes.status, 200);
  const verifyData = await verifyRes.json();
  assert.equal(verifyData.success, true);
  assert.equal(verifyData.status, "ACTIVE");
  console.log(`✓ /api/licenses/verify verified key ${checkoutData.licenseKey} as ACTIVE`);

  console.log("\n ALL END-TO-END HTTP PROBES PASSED WITH ZERO ERRORS!");
}

runProbes().catch((err) => {
  console.error("Probe failure:", err);
  process.exit(1);
});
