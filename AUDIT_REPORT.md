# OwnVoice — Project Audit & Remediation Report

**Scope:** full repository (`/home/user/OwnVoice`) — Windows/Python desktop app, Kotlin Android app,
two Next.js 15 apps (`web/` and `ownvoice-web-landing-page/`), Python test suite, build/release config.
**Date:** 2026-10-06 · **Branch:** `arena/d6329b28-ownvoice`

Every item below was found by reading the code/config and was **fixed in this branch** unless it
appears under *Known limitations*.

---

## 1. Verification results after the remediation

| Check | Command | Result |
|:---|:---|:---|
| Python E2E suite (Linux + stubs) | `PYTHONPATH=tests/sandbox_stubs python3 tests/run_tests.py` | **223 discovered / 204 passed / 19 failed / 0 errors (~4 s)** — the 19 are Windows-API stub artifacts, documented in `TEST_READY.md` §3.1 |
| Python lint | `python3 -m pyflakes app.py config.py core/*.py ui/*.py ui/views/*.py server/*.py scripts/*.py tests/*.py` | **clean (exit 0)** |
| Python lint (bugs) | `python3 -m ruff check --select F,E9,B app.py config.py core/ ui/ server/ scripts/ tests/` | **All checks passed** (was 30 findings) |
| Web app types | `cd web && npx tsc --noEmit` | **clean** |
| Landing app types | `cd ownvoice-web-landing-page && npx tsc --noEmit` | **clean** |
| Web app build | `cd web && npx next build` | **SUCCESS** (all routes, 103 kB shared JS, includes the new `/releases/[file]`) |
| Web app tests | `cd web && npm test` | **10 tests / 7 pass / 0 fail / 3 skipped** (Prisma integration skips without a DB) |
| Landing tests | `cd ownvoice-web-landing-page && npm test` | **15 tests / 12 pass / 0 fail / 3 skipped** |
| Android config | `android/app/build.gradle.kts`, `AndroidManifest.xml`, `res/xml/network_security_config.xml` | hardened (no Gradle/emulator available offline — see §8) |

Baseline before the audit: 19 test failures in `web/tests/api.test.mjs` failing `npm test`, 30 lint findings,
7 security defects of high severity, and several stale/incorrect docs.

---

## 2. Repository structure (as audited)

```
OwnVoice/
├── app.py, config.py            # desktop coordinator + persistent settings
├── core/                        # audio capture/AGC, AI engine, injector, hotkeys, snippets,
│                                # context detector, offline engine, gist sync, bridge server
├── ui/ (+ ui/views/)            # Tkinter/customtkinter hub, floating capsule, tray, settings
├── tests/                       # 4-tier E2E suite (223 tests) + stress/benchmark scripts
├── android/                     # Kotlin app: IME, floating bubble, meeting service, bridge client
├── server/cloud_relay.py        # optional FastAPI WebSocket relay (phone ⇄ PC cross-network)
├── web/                         # Next.js 15 BYOK workspace (Prisma + SQLite, AES-256-GCM keys)
├── ownvoice-web-landing-page/   # Next.js 15 marketing site + checkout + webhooks + downloads
└── releases/, android/*.apk     # published installers
```

---

## 3. Critical security fixes

### 3.1 Payment webhooks accepted unsigned events → free lifetime licenses
`ownvoice-web-landing-page/app/api/webhooks/{stripe,cashfree}/route.ts` parsed `stripe-signature` /
`x-webhook-signature` but **never verified them**. Any unauthenticated POST with
`{"type":"checkout.session.completed", ...}` created a `COMPLETED` purchase plus an `ACTIVE` lifetime license.

*Fix:* new `lib/webhook-verify.ts` — HMAC-SHA256 verification with `timingSafeEqual`, a 5-minute
replay window for Stripe, the gateway-specific construction for Cashfree (`timestamp + rawBody`),
and **fail-closed** behaviour when the secret is missing. Handlers are now idempotent (gateway retries
no longer duplicate licenses) and reject `GET` with 405. Covered by 5 new tests.

### 3.2 Licenses were minted without any payment
* `web/app/api/licenses/checkout/route.ts` and the landing twin created a `COMPLETED` purchase and an
  `ACTIVE` license for any logged-in user — no gateway involved.
* `app/api/checkout/session/route.ts` fell back to "test mode", granting a free license whenever
  `STRIPE_SECRET_KEY` / `CASHFREE_*` were unset (the default in `.env.example`) — silently giving the
  product away in a misconfigured production deployment.

*Fix:* direct issuance now returns **503** unless `NODE_ENV !== "production"` **and**
`OWNVOICE_ENABLE_TEST_CHECKOUT="true"`; the landing checkout returns a clear `GATEWAY_NOT_CONFIGURED`
error instead of a free license. Licenses are only minted by verified webhooks.

### 3.3 Seeded admin account with a public password + public license key
`scripts/seed.mjs` (both apps) created `admin@ownvoice.ai` / `AdminPassword123!` and license
`OV-ADM1-NIST-RATO-R001`; the login page shipped a button that auto-filled those credentials.

*Fix:* the password comes from `SEED_ADMIN_PASSWORD` or is generated (`crypto.randomBytes(18)`) and
printed once; `SEED_SKIP_ADMIN=true` seeds prompts only; the license key is random per install; the
login quick-fill only renders outside production and only from `NEXT_PUBLIC_DEMO_ADMIN_*`; `e2e_probe.mjs`
takes credentials from the environment.

### 3.4 Hardcoded fallback secrets for JWT / AES / download tokens
`lib/auth.ts`, `lib/crypto.ts`, `lib/download-token.ts` (landing) fell back to secrets committed in
source, so deployed sessions and BYOK keys were forgeable/decryptable by anyone with the repo.

*Fix:* `resolveJwtSecret()` / `resolveEncryptionSecret()` / `resolveTokenSecret()` require ≥32 chars in
production and throw with generation instructions (`node -e "crypto.randomBytes(32).toString('hex')"`),
while development keeps clearly-labelled throwaway values. `.env.example` files scrubbed.

### 3.5 Android app shipped a working Google API key
`core/SecureConfig.kt` had `DEFAULT_API_KEY = "AQ.Ab8RN6JVp6KVzuryq8BuUZLBYBuUJCNqwECacgdSMUegi5EblA"`
used as the default for every install, plus a "Reset Default" button that restored it.

*Fix:* the constant is deleted; `apiKey` defaults to empty, `isDefaultOrBlankApiKey` → `needsApiKey`,
and the button now clears the key. OwnVoice is BYOK: the phone receives a key by pairing with the desktop.

### 3.6 Desktop bridge exposed its secrets to the whole LAN
`core/bridge_server.py`:

| Defect | Fix |
|:---|:---|
| `GET /status` returned `token`, `pin`, `api_key` and clipboard text to any caller | only returned to `is_loopback_client()`; remote devices must `POST /pair` |
| UDP discovery beacon broadcast `api_key` | removed from the payload (PIN/token stay, they are the pairing channel) |
| `POST /clipboard`, `/note/create`, `/inject`, `/meeting` authorized when the token was *empty* (`not client_token`) or the peer was on any private IP | centralized `is_authorized_request()`; an empty token is never authorization |
| `/inject` and `/mouse` (arbitrary keystrokes/mouse control) accepted bare LAN membership | require the pairing token or current PIN (loopback still allowed for the desktop UI) |
| `is_private_ip()` used `startswith("172.")`, treating *public* 172.32+ addresses as local | uses `ipaddress` (loopback, link-local, RFC1918, IPv4-mapped IPv6) |
| 6-digit PIN could be brute-forced online | 5 failures per 5 minutes → HTTP 429 lockout; the expected PIN is no longer written to logs |

### 3.7 Android transport & backup hardening
* `android:usesCleartextTraffic="true"` with a blanket user-CA trust anchor let any local malware/MITM
  read HTTPS API traffic → `network_security_config.xml` now trusts **system CAs only** (cleartext kept
  for the LAN bridge, with an explanatory comment) and `allowBackup="false"` prevents `adb`/cloud backup
  extraction of the BYOK key, bridge token and gist token from `ownvoice_secure_prefs`.

### 3.8 Release signing key was committed and release config was unsafe
`android/app/build.gradle.kts` baked `storePassword`/`keyAlias` credentials into the build file and
debug builds reused the release keystore; `android/app/release.keystore` was tracked.

*Fix:* credentials come from `OWNVOICE_KEYSTORE_*` env vars or the git-ignored `android/keystore.properties`
(`keystore.properties.example` added), assemble/bundle fail fast with a `GradleException` when they are
missing, debug uses Gradle's debug keystore, and the keystore is no longer tracked.

---

## 4. Correctness & reliability fixes

| Area | Defect | Fix |
|:---|:---|:---|
| `core/audio_recorder.py` | Fallback device candidate could request more channels than the endpoint supports (mono Bluetooth) | clamp to `max(1, min(requested, max_in))` |
| `core/injector.py` | Non-Windows branch of `update_target_hwnd()` returned success without storing the HWND | stores `last_target_hwnd` |
| `ui/settings_window.py` | `lambda v=view: v.search_entry.focus_set()` captured a loop variable; VU meter updated Tk widgets from worker threads | default-arg binding fixed; VU monitor converted to `window.after(...)` ticks |
| `core/offline_engine.py` | Whisper token output was cast with `str(outputs[0])` (leaked object repr into transcripts) | token list handling with an explicit docstring |
| `core/note_engine.py`, `app.py` | Empty transcripts still produced/saved an empty note and an error toast | early return + guard in the note branch |
| `config.py` | `sound_effects` defaulted to `True`, contradicting the documented silent-operation requirement | default `False` (opt-in), matching `app.py` and the settings view |
| `server/cloud_relay.py` | Unvalidated room ids, unbounded connections and frame sizes, malformed rooms kept sockets open | room-id pattern `[A-Za-z0-9_-]{8,64}`, ≤4 clients per role, 64 kB frame cap, close-before-accept for bad ids |
| `web/app/(marketing)/download/page.tsx` | Linked `/releases/OwnVoice-*.zip`, which Next.js cannot serve (files are outside `public/`) and which named a version that predates the shipped builds → 404 | new `web/app/releases/[file]/route.ts` streaming route with filename allow-list and path-traversal guard; link updated to a bundle that exists |
| `core/__init__.py`, `ui/__init__.py` | Eager `from .audio_recorder import AudioRecorder` style imports meant that touching *any* `core.*` submodule loaded ~374 modules, including numpy and requests — so `import core.context_detector` (pure ctypes) cost ~24 MB and directly threatened the F15 idle-memory budget | both packages now resolve their exports lazily via PEP 562 `__getattr__`; `core.context_detector` now pulls **41 modules / 0.57 MB** and `from core import AudioRecorder` still works |
| `web/prisma/schema.prisma` (+ landing) | Deprecated `engineType = "binary"` (out-of-process engine, unsupported on serverless); no indexes on foreign keys / hot filters (SQLite does not index FKs) | generator uses the default library engine; `@@index` added for `License`, `Purchase`, `Recording`, `Generation`, `GenerationVersion`, `CustomInstruction`, `Device`, `UsageEvent`, `AuditLog` |
| `ownvoice-web-landing-page/app/api/demo/transform/route.ts` | Reported `latencyMs: Math.floor(Math.random()*80)+120` as if measured | measures real elapsed time |

### Data honesty (fabricated telemetry)
`config.py::get_productivity_stats()` floored the dashboard at **1284 words / 42 dictations / 18 minutes /
3.2 hours** when the user had no data, and `ui/views/home_view.py` rendered decorative `↑ 12%` / `↑ 8%`
trend badges with no data behind them. Both are gone: stats are computed from the real history/notes,
the fallback is zeros, the fake trend chips are removed, and `tests/test_helpers.py` now mirrors the
production calculation. `config.py` also no longer ships the author's personal snippets as defaults and
writes `config.json` / `.env` with `chmod 600`.

---

## 5. Web/API hardening

* **Brute-force protection** — new `lib/rate-limit.ts` (sliding window + lockout) applied to
  `POST /api/auth/login` (5 failures / 5 min per IP+email → HTTP 429 with `Retry-After`) and
  `POST /api/auth/register` (10 per 10 min per IP), in both apps. 3 new tests.
* **PII removed from analytics** — `register` logged `{ email }` into `UsageEvent.metadata`; now only the
  user id is recorded (the user id joins to the email when needed).
* **License key RNG** — `generateLicenseKey()` in both apps used `randomBytes[i] % 32`, biasing 8 of the
  32 alphabet characters ~1.5×; replaced with rejection sampling.
* **Test-suite contract** — `web/tests/api.test.mjs` probes Prisma/DB and **skips** the 3 integration
  tests with an actionable message instead of failing `npm test` on a fresh checkout; it also detects the
  offline runtime stub. `npm test` is green in both apps again.

---

## 6. Repository hygiene

* Untracked generated artifacts (files kept on disk): `android/*.apk` (12), `android/app/release.keystore`,
  `history.json`, `test_loopback_recording.wav`, `ownvoice-web-landing-page/tsconfig.tsbuildinfo`, with
  matching `.gitignore` rules.
* Restored the four side-effect imports in `tests/tier1_features/test_f15_arm64_memory.py` that an
  earlier `ruff --fix` pass had silently deleted — without them the memory-overhead assertions were
  vacuous (they still pass, now for the right reason).
* Removed 30+ dead imports/variables and two `except … as e`/f-string smells; `pyflakes` and
  `ruff --select F,E9,B` are clean across `app.py`, `config.py`, `core/`, `ui/`, `server/`, `scripts/`, `tests/`.
* Persisted the previously ephemeral offline tooling into the repo:
  * `web/tools/prisma-type-shim.py`, `web/tools/prisma-runtime-stub.js`, `web/tools/README.md`
  * `tests/sandbox_stubs/` (+ README) — opt-in Linux shims for `ctypes.windll`, `sounddevice`,
    `pynput`, `pystray`, `pyperclip`, `tkinter`.
* Documentation corrected: `TEST_READY.md` (real totals 108/101/8/6 = **223**, not 218; Linux expectations
  and the exact Windows-only failure groups), `README.md` (structure tree listed `OwnVoice.spec` and
  `dist/OwnVoice/OwnVoice.exe`, neither of which exist; test count "252"; download links pointed at
  v1.0.0 while newer releases ship; documented the removed admin credentials).

---

## 7. Tests added

| Test file | Coverage |
|:---|:---|
| `ownvoice-web-landing-page/tests/webhooks.test.mjs` (5) | Stripe/Cashfree signature acceptance, tampered body, wrong secret, stale timestamp, missing header/secret (imports the real `lib/webhook-verify.ts` — Node strips the types) |
| `web/tests/rate-limit.test.mjs` + landing twin (3 each) | lockout after N failures, unlock after success, sliding-window expiry |

---

## 8. Known limitations / recommended next steps

1. **Windows-only test failures on Linux (19).** Artifacts of the `ctypes.windll` stand-in (returns `1`
   for every symbol), the `INPUT` struct layout, the `pyperclip` stub, and two sub-millisecond latency
   ceilings. Use a Windows host for those; see `TEST_READY.md` §3.1. Do **not** "fix" them in product code.
2. **Real Prisma engine/DB.** `npx prisma generate` / `prisma db push` require `binaries.prisma.sh`, which
   is unreachable from this sandbox, so the 3 integration tests skip. On a networked machine run
   `cd web && npx prisma generate && npx prisma db push && npm test` (the `web/tools` helpers exist only
   for offline type-checking/builds). Landing `next build` likewise needs network for
   `next/font/google` (Caveat / Plus Jakarta Sans); a `next/font/local` migration would make it hermetic.
3. **Android artifacts are stale relative to source.** Everything in `releases/` and `android/` was built
   as `versionCode 17 / versionName 2.5.6` (including the file named `OwnVoice-v3.0.0.apk`), and the
   landing download route promises `OwnVoice-Mobile-v3.0.0.apk`. Source is now `18 / "3.0.0"`; rebuild
   the APK on a machine with the Android SDK to publish a consistent artifact. `isMinifyEnabled` /
   `isShrinkResources` are now on — smoke-test the release APK (IME, accessibility service, overlay
   bubble, bridge client) before shipping.
4. **Secrets at rest on Android.** `ownvoice_secure_prefs` still stores the API key, bridge token and gist
   token in plaintext SharedPreferences; `allowBackup="false"` removes the backup/exfiltration path.
   `EncryptedSharedPreferences` (androidx.security-crypto) is the next step, and would require a fallback
   migration for existing installs.
5. **Repository size.** `releases/` (262 MB) and `android/*.apk` remain in the tree and in `.git` history
   (182 MB). Moving installers to GitHub Releases + Git LFS, and/or rewriting history, is advisable before
   anyone clones the repository at scale.
6. **Landing app duplication.** `web/` and `ownvoice-web-landing-page/` are near-identical copies (same
   Prisma schema, crypto, licence, rate-limit and auth code, patched in both here). Extracting a shared
   package would prevent the two from drifting apart.
