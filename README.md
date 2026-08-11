# shoppew

shoppew is an original Vietnamese multi-vendor marketplace under active implementation. The repository is designed to contain the customer storefront, Seller Center, Admin application, Spring Boot modular-monolith backend, native Android client, and a reproducible local service environment.

The status of each deliverable is recorded in [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md). Do not treat planned modules as verified features.

## Requirements

- Git
- Java 21 (the Maven Wrapper downloads Maven)
- Node.js 22 or newer
- pnpm 10 or newer
- Docker Desktop with Docker Compose
- Android Studio/SDK platform tools and a USB-debuggable physical Android device for the native client

All examples below use PowerShell from the repository root. `make setup`, `make dev`, `make test`, `make backend-test`, `make web-test`, and `make reset-db` are convenience aliases for environments that have `make`; the PowerShell commands are the supported Windows path.

## First setup

```powershell
Copy-Item .env.example .env
docker compose config --quiet
corepack pnpm install --frozen-lockfile
```

Or run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup.ps1
```

The setup script creates `.env` when it is missing, validates Compose, and installs the pnpm workspace. Generate the TypeScript API contract only after the backend is running. The credentials in `.env.example` are local-development defaults; never reuse them in a deployed environment.

## Environment

shoppew has three different configuration times. Mixing them is a common source of apparently ignored settings.

| Scope | When read | Important settings |
| --- | --- | --- |
| Docker Compose | When `docker compose` starts | Root `.env`: local ports, PostgreSQL/MinIO credentials, backend settings passed into the backend container |
| Spring Boot backend | At backend process start | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `MAIL_HOST`, `MAIL_PORT`, `S3_ENDPOINT`, `S3_PUBLIC_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`, and `APP_*` runtime policy |
| Web applications | At dev/build start | Storefront `NEXT_PUBLIC_API_URL` and `NEXT_PUBLIC_MEDIA_ORIGIN`; Seller `VITE_API_URL`; Admin `VITE_API_URL` and `VITE_STOREFRONT_URL` |
| Android | At Gradle build time | Debug `SHOPPEW_DEBUG_API_BASE_URL`; release `SHOPPEW_API_BASE_URL` |

Root `.env` is automatically consumed by Docker Compose. A backend started directly with `mvnw.cmd spring-boot:run` does **not** automatically import that file; it uses `application.yml` defaults plus variables exported in that PowerShell process. For example:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:15432/shoppew'
$env:DB_USERNAME = 'shoppew'
$env:DB_PASSWORD = '<local-password-from-your-.env>'
.\backend\mvnw.cmd spring-boot:run
```

Copy a web application's `.env.example` to its `.env.local` only when overriding the local defaults. `NEXT_PUBLIC_*` and `VITE_*` values are public client configuration baked into web output; never put a secret in them. Backend credentials and provider secrets remain runtime-only. Production requirements and the complete variable checklist are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Daily development start

With the unchanged local defaults, terminal 1 starts infrastructure and the backend:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

After readiness is `UP`, terminal 2 refreshes the API contract and starts all three web clients:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm dev
```

`scripts/dev.ps1 -InfrastructureOnly` starts only PostgreSQL, Redis, MinIO, and Mailpit. If `.env` changes database credentials/ports from the example, export the matching backend runtime variables before using the full script; the standalone JVM does not import `.env`.

## Start local infrastructure

```powershell
docker compose up -d postgres redis minio minio-init mailpit
docker compose ps
```

Local services:

| Service | URL / port |
| --- | --- |
| PostgreSQL | `localhost:15432` |
| Redis | `localhost:16379` |
| MinIO API | http://localhost:9000 |
| MinIO console | http://localhost:9001 |
| Mailpit inbox | http://localhost:8025 |

## Run the backend

```powershell
.\backend\mvnw.cmd spring-boot:run
```

When the backend is healthy:

- Readiness: http://localhost:28080/actuator/health/readiness
- Public system contract: http://localhost:28080/api/v1/public/system
- OpenAPI JSON: http://localhost:28080/v3/api-docs
- Swagger UI: http://localhost:28080/swagger-ui.html

Build and test:

```powershell
.\backend\mvnw.cmd clean test package
```

Backend integration tests use Testcontainers and therefore require Docker.

## Run the customer storefront

Start the backend first, generate the TypeScript contract from its live OpenAPI document, then run the Next.js application:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/storefront dev
```

The storefront is available at http://localhost:3000 and calls the backend at http://localhost:28080. Its access token stays in memory; the rotating refresh credential remains in the backend-issued HttpOnly cookie.

For deployment, set `NEXT_PUBLIC_SITE_URL` to the canonical Storefront origin. Set `NEXT_PUBLIC_MEDIA_ORIGIN` to the same public S3/CDN origin or path prefix used by backend `S3_PUBLIC_ENDPOINT`. Private-network image optimization is enabled only in local development.

Verify the frontend:

```powershell
corepack pnpm --filter @shoppew/storefront lint
corepack pnpm --filter @shoppew/storefront typecheck
corepack pnpm --filter @shoppew/storefront test
corepack pnpm --filter @shoppew/storefront build
```

## Run the Seller Center

Start the backend first, generate the current API contract, then run the Vite application:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/seller dev
```

The Seller Center is available at http://localhost:3001. It uses in-memory access credentials plus the backend's rotating HttpOnly refresh cookie and verifies every shop-scoped action again on the server.

Verify it with:

```powershell
corepack pnpm --filter @shoppew/seller lint
corepack pnpm --filter @shoppew/seller typecheck
corepack pnpm --filter @shoppew/seller test
corepack pnpm --filter @shoppew/seller build
```

## Run the Admin application

Start the backend first, regenerate the API contract, then run the protected Vite application:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/admin dev
```

Admin is available at http://localhost:3002. Effective system settings are read-only; all moderation and operations use backend-protected commands. Verify it with:

```powershell
corepack pnpm --filter @shoppew/admin lint
corepack pnpm --filter @shoppew/admin typecheck
corepack pnpm --filter @shoppew/admin test
corepack pnpm --filter @shoppew/admin build
```

## Run the Android application on a physical device

The verified workflow does not install or use an emulator. Connect a real Android phone with USB debugging enabled, confirm it with `adb devices -l`, and forward the local backend/object-storage ports before building:

```powershell
adb devices -l
adb -s <serial> reverse tcp:28080 tcp:28080
adb -s <serial> reverse tcp:9000 tcp:9000

Set-Location .\mobile\android
.\gradlew.bat assembleDebug -PSHOPPEW_DEBUG_API_BASE_URL=http://127.0.0.1:28080/
adb -s <serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Do not continue with device tests when no physical device is listed; ask the user to connect one. See [docs/ANDROID.md](docs/ANDROID.md) for the complete gate commands, secure-session design, and the explicit production-push gap.

## Run the local service stack with Docker

```powershell
docker compose up --build
```

Compose starts PostgreSQL, Redis, MinIO, Mailpit, and the backend. The backend waits for its dependencies and runs Flyway migrations before readiness becomes `UP`. The three web applications are not containerized by this local Compose file; run them from source with `corepack pnpm dev` after generating the API client. The native Android app runs on the connected phone.

## Database reset

This removes all local shoppew Compose volumes. The script requires confirmation unless `-Force` is supplied deliberately.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\reset-db.ps1
```

Flyway is the only schema owner and applies migrations when the backend starts. Hibernate uses schema validation, not create/update. There is no permanent seed account. See [docs/DATABASE.md](docs/DATABASE.md) for the model, constraints, transaction boundaries, and indexes.

## Tests and E2E

Run the normal backend and web checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1
corepack pnpm build
```

The script runs backend tests plus web lint, type-check, and unit/component tests. `pnpm build` is separate. Backend integration tests use Testcontainers, so Docker must be available.

With the full local service stack healthy, run the real HTTP smoke and browser E2E:

```powershell
.\scripts\smoke-catalog.ps1
corepack pnpm typecheck:e2e
corepack pnpm test:e2e
```

The Playwright suite uses the real backend and system Chrome; it does not intercept the commerce API with mock data. Android JVM/lint/build gates and connected tests are documented in [docs/ANDROID.md](docs/ANDROID.md). The complete command matrix, generated evidence, and rerun options are in [docs/TESTING.md](docs/TESTING.md).

GitHub Actions definitions exist for backend/web/Android/API-contract checks and a manually dispatched full E2E run. They are new repository definitions and do not yet constitute verified GitHub-hosted runner evidence.

## Demo users

There are no static customer, seller, or admin credentials and no production seed. `scripts/smoke-catalog.ps1` creates isolated synthetic accounts named with its printed 14-digit run ID:

```text
catalog-buyer-<runId>@example.test
catalog-seller-<runId>@example.test
catalog-admin-<runId>@example.test
```

The E2E global setup writes the exact local-only emails and password for its run to ignored `test-results/e2e-runtime.json`. Use that generated file for manual QA; do not commit it or reuse those credentials outside the disposable local environment. The smoke intentionally leaves its uniquely named commerce data in the local database so it can be inspected.

## Swagger and OpenAPI

Start the backend, then open:

- Swagger UI: http://localhost:28080/swagger-ui.html
- OpenAPI JSON: http://localhost:28080/v3/api-docs

Use `POST /api/v1/auth/login` to obtain the short-lived access token, then choose **Authorize** and provide the token to call protected customer, seller, or admin operations. Refresh credentials are HttpOnly cookies rather than JSON fields. The live document is the source for `@shoppew/api-client`:

```powershell
corepack pnpm --filter @shoppew/api-client generate
```

Review the generated contract change before committing it. Production disables OpenAPI and Swagger by default; enabling `APP_OPENAPI_ENABLED=true` does not add network authentication, so expose it only behind an independently protected operator boundary. See [docs/API.md](docs/API.md) for envelopes, pagination, errors, and endpoint groups.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Backend never becomes ready | Run `docker compose ps` and `docker compose logs backend postgres redis minio-init mailpit`; verify no local port collision and that Docker Desktop is running. |
| Backend tests cannot start containers | Testcontainers needs a reachable Docker daemon. Start Docker Desktop before `mvnw.cmd test/verify`. |
| API client generation fails | The generator reads the live `http://localhost:28080/v3/api-docs`; confirm readiness and that the local profile has OpenAPI enabled. |
| Browser login/CORS fails | Use the exact configured origins (`localhost`, not a different hostname such as `127.0.0.1`) or update `APP_CORS_ALLOWED_ORIGINS` before starting the backend. Clear stale cookies after changing hosts. |
| Local product images do not load | Confirm MinIO is healthy on port 9000, `S3_PUBLIC_ENDPOINT` matches the browser-visible origin, and Storefront `NEXT_PUBLIC_MEDIA_ORIGIN` matches it. Restart/rebuild the Storefront after changing public variables. |
| Refresh works locally but not after deployment | Production requires HTTPS, `APP_SECURE_COOKIES=true`, exact HTTPS CORS origins, correct forwarded headers, and a proxy that preserves `Set-Cookie`. |
| Android cannot reach the backend | Do not start an emulator. Confirm a real phone in `adb devices -l`, then recreate `adb reverse` for ports 28080 and 9000 after reconnecting USB. |
| Local data is inconsistent | Stop active work, then use the confirmation-gated `scripts/reset-db.ps1`; it deletes all local Compose volumes. |

For production limitations, provider credentials, TLS/proxy requirements, backup/restore, and rollout gates, use [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Database](docs/DATABASE.md)
- [HTTP API and OpenAPI](docs/API.md)
- [Authentication](docs/AUTHENTICATION.md)
- [Security](docs/SECURITY.md)
- [Testing and E2E](docs/TESTING.md)
- [Performance baseline](docs/PERFORMANCE.md)
- [Deployment checklist](docs/DEPLOYMENT.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [Customer storefront](docs/STOREFRONT.md)
- [Seller Center](docs/SELLER_CENTER.md)
- [Admin application](web/admin/README.md)
- [Native Android client](docs/ANDROID.md)
- [Search, recommendations, and chat](docs/DISCOVERY_CHAT.md)
- [Implementation status](docs/IMPLEMENTATION_STATUS.md)

Implementation and verification remain separate: do not infer production deployment, provider readiness, or runner verification from the presence of a runbook.
