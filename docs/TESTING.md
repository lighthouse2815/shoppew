# Testing shoppew

No single green command proves the whole marketplace. Use the narrowest gate while developing, then run the release matrix against the exact commit. Docker is required for backend integration tests and real-backend smoke/E2E.

## Command matrix

Run these from the repository root unless the command changes directory explicitly.

| Layer | Command | What it proves |
| --- | --- | --- |
| Normal local gate | `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1` | Backend tests; all web lint, type-check, and unit/component tests |
| Backend release gate | `.\backend\mvnw.cmd clean verify` | Clean compile/package plus unit and Testcontainers integration tests against migrated PostgreSQL/Redis dependencies |
| Web quality | `corepack pnpm lint`; `corepack pnpm typecheck`; `corepack pnpm test`; `corepack pnpm build` | Workspace lint/types/tests and production builds for every web application with a build script |
| API E2E types | `corepack pnpm typecheck:e2e` | Playwright setup/spec/teardown compile against their TypeScript contract |
| Real HTTP vertical slice | `.\scripts\smoke-catalog.ps1` | Backend, PostgreSQL, Redis, MinIO, Mailpit, pricing, inventory, checkout, order, engagement, operations, and adapter state through real HTTP/SQL evidence |
| Browser E2E | `corepack pnpm test:e2e` | Storefront, Seller Center, and Admin behavior against the real local backend |
| Android local gates | See [ANDROID.md](ANDROID.md) | JVM tests, lint, debug build, Android-test compile |
| Android device gate | `connectedDebugAndroidTest` from [ANDROID.md](ANDROID.md) | Instrumented tests on an explicitly connected physical phone |

`scripts/test.ps1` does not run production web builds, Playwright, the catalog smoke, or Android. Run those separately before a release claim.

## Zero-to-test setup

```powershell
Copy-Item .env.example .env
corepack pnpm install --frozen-lockfile
docker compose up -d postgres redis minio minio-init mailpit
.\backend\mvnw.cmd spring-boot:run
```

Wait for `http://localhost:28080/actuator/health/readiness` to report `UP`. In another terminal, generate the client contract and start clients as needed:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm dev
```

The root `.env` configures Compose. A directly launched backend reads process environment plus `application.yml`; it does not import `.env` automatically. See the README environment section before overriding database or provider settings.

## Unit and integration tests

Backend tests keep deterministic business time where required and use Testcontainers for integration boundaries. If they fail before the Spring context starts, confirm Docker Desktop is running and that the current user can reach the Docker daemon. Prefer a focused Maven `-Dtest=...` run during development, then restore `clean verify` for release evidence.

Web Vitest suites cover API/session providers, protected routing, validation, critical forms, loading/error/empty states, and security regressions. Lint, TypeScript, tests, and build are separate gates; passing Vitest does not prove that Next/Vite production output builds.

## OpenAPI contract drift

The running backend's `/v3/api-docs` is authoritative. `pnpm --filter @shoppew/api-client generate` rewrites the committed TypeScript schema, so review its diff. To compare without changing the working tree:

```powershell
$generatedContract = Join-Path $env:TEMP 'shoppew-schema.generated.d.ts'
corepack pnpm --filter @shoppew/api-client exec openapi-typescript http://localhost:28080/v3/api-docs -o $generatedContract
node .\scripts\ci\compare-openapi-contract.mjs .\web\packages\api-client\src\schema.d.ts $generatedContract
```

The CI API-contract job packages a clean backend, starts it against a fresh PostgreSQL/Redis service pair, waits for Flyway/readiness, and performs this comparison.

## Real HTTP catalog and commerce smoke

With Compose dependencies and the backend healthy:

```powershell
.\scripts\smoke-catalog.ps1
```

The script creates uniquely named synthetic admin, seller, shop, product, customer, inventory, checkout, order, notification/review, refund/dispute, finance, and audit data. It verifies object bytes through MinIO and email delivery through SMTP/Mailpit. Push delivery remains explicitly `SKIPPED` in this local smoke because it does not provision a real Firebase project, Application Default Credentials, client configuration, or a registered physical-device FID; API integration tests cover authenticated registration, ownership transfer, encrypted storage, revocation, and retry independently. The script does not delete its data and is not safe to aim at production casually.

Retain the printed `Run: yyyyMMddHHmmss` value when investigating or reusing its accounts in E2E. There are no permanent demo users.

## Browser end-to-end smoke

The repository uses Playwright for a real-backend browser smoke across the customer storefront, Seller Center, and Admin. API interception is not used to mock commerce data.

Prerequisites:

- PostgreSQL, Redis, MinIO, Mailpit, and the backend are running through `docker compose`;
- backend readiness is `UP` at `http://localhost:28080/actuator/health/readiness`;
- Google Chrome is installed. The Playwright config intentionally uses the system Chrome channel and does not require a downloaded browser binary.

Run the typed gate and browser suite from the repository root:

```powershell
pnpm typecheck:e2e
pnpm test:e2e
```

Playwright starts the three web development servers when their ports are free, or reuses existing local servers outside CI. The global setup runs `scripts/smoke-catalog.ps1`, or reuses a named successful smoke run, to obtain isolated customer, seller, admin, shop, catalog, and address data from the real Docker services. It clears the buyer cart before the browser flow. The Seller test creates the moderation listing entirely through the Seller Center UI; global teardown clears the cart, drives any test order to a terminal state when possible, and archives that listing on both passing and failing runs.

To rerun against a previously successful catalog smoke without repeating the full seed, provide its 14-digit run ID:

```powershell
$env:SHOPPEW_E2E_RUN_ID = 'yyyyMMddHHmmss'
pnpm test:e2e
```

Optional endpoint overrides are `SHOPPEW_API_URL`, `SHOPPEW_STOREFRONT_URL`, `SHOPPEW_SELLER_URL`, and `SHOPPEW_ADMIN_URL`. `PLAYWRIGHT_CHANNEL` selects another installed browser channel; `PLAYWRIGHT_EXECUTABLE_PATH` selects an explicit installed executable. `SHOPPEW_E2E_PASSWORD` can override the development smoke password when reusing a run.

Failure evidence is written to ignored `test-results/` and `playwright-report/` directories, including traces, screenshots, and retained failure video.

The final Phase 15 run used system Chrome, one worker, and four serial tests against the real backend. It passed `4/4` in 78.8 seconds and covered:

- customer login, browsing, cart, authoritative checkout preview, COD placement, order detail, completion, six-entry history, and immutable item snapshot;
- Seller Center login, full product draft creation with required attributes, a real PNG, variant and inventory, moderation submission, and the complete order process/ready/ship/deliver sequence;
- Admin login, product approval, user/shop inspection, completed-order detail, and audit inspection;
- teardown evidence of an archived test listing, an empty buyer cart, and no non-terminal Playwright lifecycle orders.

`pnpm typecheck:e2e` also passed. The suite uses no commerce API interception. A non-fatal Next development-server reporter warning (`ERR_OUT_OF_RANGE` from ignore-listed frames) appeared after the green run, but Playwright exited `0`, all flow assertions passed, and no application source location was reported.

## Android physical-device verification

Do not install or start an emulator. Confirm a real device with `adb devices -l`; if none is listed, stop and ask the user to connect a phone with USB debugging enabled. Apply `adb reverse` for backend and object-storage ports, then use the exact Gradle commands in [ANDROID.md](ANDROID.md). Record the device model, Android version, connected-test count, and any manual runtime evidence. A build or JVM test alone does not prove device networking, secure session restoration, deep links, notification permission, or Room fallback.

## CI status and evidence

- `.github/workflows/ci.yml` defines backend `clean verify`, web quality/builds, Android JVM/lint/assembly without an emulator, and live OpenAPI contract comparison.
- `.github/workflows/e2e.yml` is manual and creates an isolated Compose project before running real-backend Playwright, then removes its volumes even on failure.
- `.github/workflows/security.yml` runs dependency review, CodeQL for both source families, Trivy source/configuration/secret scanning, High/Critical scans of the backend and all three web runtime images, and four retained CycloneDX SBOMs. Dependabot covers Maven, npm, Gradle, GitHub Actions, and both Docker build roots.
- Hosted CI run `32336008972` and Security run `32336009009` are both green on exact implementation commit `c382139`: five CI jobs, both CodeQL source families, Trivy source/configuration/secret scanning, all four runtime images, and four SBOMs. Dependency review was correctly skipped on the `push` event and remains enforced on pull requests. Future releases must still record both workflows on their own exact release commit.

For every release claim, record the commit, exact command, date, pass/fail counts, relevant device/browser/runtime, and failure artifacts. Do not convert an interrupted, blocked, or skipped gate into `VERIFIED`.

## Failure triage

1. Check `docker compose ps` and backend readiness before blaming a client.
2. Read backend logs with the response `X-Request-Id`; logs intentionally omit secrets and raw internal exception messages.
3. For Playwright, open the failure trace from `playwright-report/` and correlate it with `test-results/e2e-runtime.json` and backend logs.
4. For stale web behavior, stop conflicting dev servers and remove only generated build output for the affected application before rebuilding. Do not delete the workspace or user data.
5. For Android, verify the exact serial, recreate `adb reverse` after USB reconnect, and confirm the debug build used `http://127.0.0.1:28080/`.
6. When a smoke run partially writes data, use its unique run ID for diagnosis. Reset all Compose volumes only through the confirmation-gated `scripts/reset-db.ps1` when losing local data is acceptable.
