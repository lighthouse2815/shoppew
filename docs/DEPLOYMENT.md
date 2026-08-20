# Deploying shoppew

This is a production-readiness checklist, not a claim that shoppew is deployed. The repository contains non-root backend and web images, Flyway migrations, health endpoints, security headers for every web runtime, and local Compose. It does **not** contain a production Compose stack, TLS proxy, cloud infrastructure, managed secret configuration, or a completed external-provider rollout.

## Supported deployment shape

- Run the Spring Boot modular monolith as one or more stateless backend replicas behind an HTTPS reverse proxy or load balancer.
- Use managed PostgreSQL as commerce authority, managed Redis for acceleration/rate limiting, and S3-compatible object storage with a public media origin or CDN.
- Run the Next.js Storefront as a Node server. Publish Seller Center and Admin `dist/` output as static single-page applications with fallback routing to `index.html`.
- Keep Storefront, Seller Center, Admin, API, and media on explicit HTTPS origins. Put only those exact web origins in backend CORS configuration.
- Build Android separately with a release API URL and an external release-signing process.

Kubernetes and microservices are not requirements. The local `docker-compose.yml` is a developer stack and must not be promoted unchanged: it publishes local credentials, Mailpit, an anonymously readable local MinIO bucket, and the mock payment/shipping path.

## Build immutable artifacts

Run the release gates before building artifacts:

```powershell
.\backend\mvnw.cmd clean verify
corepack pnpm install --frozen-lockfile
corepack pnpm lint
corepack pnpm typecheck
corepack pnpm typecheck:e2e
corepack pnpm test
corepack pnpm build
docker build --tag shoppew-backend:<immutable-version> .\backend
docker build --file .\deploy\docker\Dockerfile.storefront --build-arg NEXT_PUBLIC_API_URL=https://<api-origin> --build-arg NEXT_PUBLIC_MEDIA_ORIGIN=https://<media-origin> --build-arg NEXT_PUBLIC_SITE_URL=https://<storefront-origin> --tag shoppew-storefront:<immutable-version> .
docker build --file .\deploy\docker\Dockerfile.seller --build-arg VITE_API_URL=https://<api-origin> --tag shoppew-seller:<immutable-version> .
docker build --file .\deploy\docker\Dockerfile.admin --build-arg VITE_API_URL=https://<api-origin> --build-arg VITE_STOREFRONT_URL=https://<storefront-origin> --tag shoppew-admin:<immutable-version> .
```

The backend image runs as UID `10001`. The Storefront image uses Next.js standalone output and runs as the image's non-root `node` user on port `3000`. Seller Center and Admin run as UID `101` on the official unprivileged NGINX image, listen on port `8080`, expose `/healthz`, and return `index.html` for client-side routes. All three web Docker builds reject missing, non-HTTPS, credential-bearing, or path-bearing production origins before compiling a public bundle.

The image build arguments are public client configuration, not runtime secrets. Changing an API, media, or Storefront origin requires a new immutable web image. The checked-in NGINX template receives the same compiled API origin as `SHOPPEW_API_ORIGIN` so its `connect-src` policy matches the bundle; do not override that variable independently at runtime.

Before a release, generate the TypeScript contract from the intended backend version and confirm it matches the committed `web/packages/api-client/src/schema.d.ts`:

```powershell
corepack pnpm --filter @shoppew/api-client generate
git diff -- web/packages/api-client/src/schema.d.ts
```

Do not generate from an untrusted or differently versioned API.

## Backend runtime configuration

Set `SPRING_PROFILES_ACTIVE=prod`. The production profile keeps Hibernate on `validate`, runs Flyway, defaults secure cookies/email verification on, defaults mock payment off, hides health details, and disables OpenAPI/Swagger unless deliberately enabled.

The production validator refuses to boot with known development/default secrets, short secrets, insecure cookies, disabled email verification/rate limiting, mock payment enabled, localhost/non-HTTPS public URLs, or non-HTTPS CORS origins.

| Group | Required production settings |
| --- | --- |
| Process | `SPRING_PROFILES_ACTIVE=prod`, `BACKEND_PORT` for the platform listener (`8080` matches the backend image's exposed port) |
| PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; tune `DB_POOL_MAX_SIZE` and `DB_POOL_MIN_IDLE` against the database connection budget |
| Redis | `REDIS_HOST`, `REDIS_PORT`; when required use Spring runtime properties such as `SPRING_DATA_REDIS_USERNAME`, `SPRING_DATA_REDIS_PASSWORD`, and `SPRING_DATA_REDIS_SSL_ENABLED=true` |
| Object storage | `S3_ENDPOINT`, HTTPS `S3_PUBLIC_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET` |
| Web/security | unpredictable `APP_JWT_SECRET` of at least 64 bytes, `APP_SECURE_COOKIES=true`, exact HTTPS `APP_CORS_ALLOWED_ORIGINS`, HTTPS `APP_WEB_BASE_URL` |
| Identity/email | `APP_EMAIL_VERIFICATION_REQUIRED=true`, `APP_EMAIL_DELIVERY_ENABLED=true`, verified `APP_EMAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, and provider authentication/TLS settings; production refuses disabled delivery |
| Commerce | `APP_MOCK_PAYMENT_ENABLED=false`, `APP_MOCK_SHIPPING_ENABLED=false`, `APP_RATE_LIMIT_ENABLED=true`, reviewed rate thresholds, `APP_PLATFORM_FEE_RATE`, `APP_TIME_ZONE=Asia/Ho_Chi_Minh` |
| Operations | leave `APP_OPENAPI_ENABLED=false` unless a separately authenticated private operator route requires it |

For authenticated SMTP, Spring Boot also accepts runtime properties such as `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, and `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`. Match these to the chosen provider; never put them in a `NEXT_PUBLIC_*` or `VITE_*` variable.

Secrets must come from the deployment platform's secret manager, not committed `.env` files, container layers, build logs, or client bundles. Plan rotation for the database, object-storage, SMTP, payment, signing, and Android-signing credentials. Rotating `APP_JWT_SECRET` invalidates existing access tokens; coordinate it with session/re-authentication policy.

## Web and Android build configuration

Public web values are baked into artifacts and must be supplied before the relevant build:

| Client | Build configuration |
| --- | --- |
| Storefront | `NEXT_PUBLIC_API_URL=https://<api-origin>` and `NEXT_PUBLIC_MEDIA_ORIGIN=https://<media-origin-or-prefix>` |
| Seller Center | `VITE_API_URL=https://<api-origin>` |
| Admin | `VITE_API_URL=https://<api-origin>` and `VITE_STOREFRONT_URL=https://<storefront-origin>` |
| Android release | Gradle property `SHOPPEW_API_BASE_URL=https://<api-origin>/` |

Changing a public variable requires a rebuild/redeploy. Do not inject secrets into frontend or Android builds. The Android project does not include a repository-owned release signing identity; configure signing in a protected external build environment. Production push also requires Firebase application configuration, backend device-token lifecycle, provider credentials, logout/revocation, and delivery verification that are not implemented yet.

## External-service readiness

| Capability | Repository boundary | Required before claiming production readiness |
| --- | --- | --- |
| Database | PostgreSQL 17 schema, Flyway V1–V11 | HA/backup policy, restore drill, monitoring, least-privilege roles; ensure `pg_trgm` can be installed before V11 |
| Redis | Cache/rate-limit coordination | Protected managed instance and capacity/eviction monitoring; during outage the bounded fallback enforces per replica rather than globally |
| Media | S3-compatible provider interface and MinIO local implementation | Private write credentials, public/CDN read policy, CORS, lifecycle/retention, malware/content moderation policy |
| Email | SMTP sender, locally verified with Mailpit | Authenticated TLS SMTP, verified sender/domain, bounce/complaint handling and real delivery test |
| Payment | COD and credentialed-signature local mock path; production disables the mock and clients use backend capabilities | A real payment/refund provider and credentials if online payment is offered; keep the mock provider disabled |
| Shipping | Local `MOCK_STANDARD` provider; production disables it and checkout blocks cleanly when no real method is registered | Real quotation/label/tracking provider and credentials before advertising production shipping integration |
| Push | In-app notifications plus local `SKIPPED` push adapter | Authenticated device registration/rotation/revocation, FCM credentials, retry policy and real-device delivery evidence |
| Observability | readiness/liveness/info and authenticated Prometheus endpoint | Central logs, metrics scraper, dashboards, alerting, tracing/retention policy and on-call ownership |

COD can be tested without an online payment credential, but the local mock shipping provider is still not a production carrier integration. `GET /api/v1/public/commerce-capabilities` is the source for checkout choices; Storefront renders only those methods, and Android exposes COD only until another real method is integrated. Production fails fast if either local mock is enabled.

## Database migration, backup, and rollback

1. Take and verify a restorable PostgreSQL backup before schema or application rollout.
2. Ensure the deployment database role can run the pending Flyway migrations. V11 creates `pg_trgm`; pre-install it with an authorized DBA when the application role cannot create extensions.
3. Run one backend instance against the target database and wait for Flyway plus readiness before increasing replica count.
4. Verify the Flyway version and schema validation in logs without exposing connection strings or credentials.
5. Use forward-compatible application/database changes for rolling releases. Flyway files are append-only; do not edit an applied migration.
6. Roll application code back only when the migrated schema remains backward compatible. Otherwise use a reviewed forward repair or restore plan, never an automatic destructive downgrade.

## Proxy, TLS, and browser security

- Terminate TLS at a trusted proxy and preserve the original scheme/host in forwarded headers. The production backend uses native forwarded-header handling.
- Redirect HTTP to HTTPS and set HSTS only after every production subdomain works over HTTPS.
- Preserve `Set-Cookie`; do not cache authenticated API responses; configure request body limits consistently with the backend's 5 MB upload limit.
- Restrict backend ingress to the intended proxy and protect management endpoints. Health/info are public by application policy; Prometheus and non-public application APIs require authentication unless infrastructure adds a separate boundary.
- Keep the image-provided CSP, frame, MIME-sniffing, opener, referrer, and permissions policies intact. If the edge proxy replaces headers, reproduce the same or stricter policy and verify the final public response rather than assuming the container response survived.
- Serve media only from the configured origin. Production Storefront image optimization deliberately rejects private-network media addresses.

## Rollout and verification checklist

- [ ] All backend, web, contract, E2E, and physical-device gates pass for the exact release commit.
- [ ] CI and security definitions have successful evidence on the exact release commit and actual hosted runners; their presence alone is not proof.
- [ ] Production startup validation passes with secret values supplied only by the secret manager.
- [ ] PostgreSQL backup restore, Redis degradation, object upload/download, SMTP delivery, and provider failure paths are exercised in staging.
- [ ] Storefront, Seller Center, Admin, API, media, and Android use the intended HTTPS origins and exact CORS list.
- [ ] Readiness/liveness, central logs, metrics, alerts, and audit-log access work without leaking secrets or personal data.
- [ ] Checkout totals, inventory non-negativity, idempotent payment/refund callbacks, immutable order snapshots, seller finance, and multi-shop isolation pass against production-like services.
- [ ] Rate limits and upload limits are tested at the proxy and application layers.
- [ ] Database migration and application rollback procedures are rehearsed.
- [ ] Real payment, shipping, email, and push capabilities are either credentialed and verified or explicitly disabled in product UI and operations.

After deployment, run a non-destructive release smoke with dedicated test accounts. `scripts/smoke-catalog.ps1` creates durable data, writes catalog/media/commerce/finance state, and uses local mock providers, so do not point it at production without an explicit approved cleanup and provider-isolation plan.
