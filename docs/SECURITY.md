# shoppew security baseline

Security is a release criterion. This document records the intended controls; only controls marked verified in `IMPLEMENTATION_STATUS.md` may be treated as working.

## Trust boundaries

- Client-provided user, shop, role, price, discount, stock, order status, and payment-success fields are untrusted.
- Ownership derives from authenticated server identity and repository relationships.
- The backend recalculates checkout totals and validates state transitions.
- External callbacks require provider authentication/signature validation and idempotent processing.
- Browser, Android, provider, object-storage, SMTP, proxy, database, Redis, and operator networks are separate trust boundaries. Public frontend variables are never a secret channel.

## Authentication and sessions

- BCrypt cost-12 passwords, generic credential errors, and temporary account lockout after repeated failures.
- HS512 access tokens with issuer/timestamp validation and a database-backed active-session check.
- HttpOnly, SameSite-strict rotating refresh cookies; only token hashes are stored.
- Refresh-token reuse revokes the whole session family; session/password revocation also invalidates current access tokens.
- Trusted-Origin and Fetch Metadata checks protect cookie-mutating authentication endpoints.
- One-time, hashed, expiring email-verification and password-reset tokens with non-enumerating request responses.
- Android keeps access tokens in memory and encrypts the rotating refresh cookie with AES-GCM under an `AndroidKeyStore` key; network logs redact authorization and cookie headers.

The web applications keep access tokens in memory rather than local storage. Credential forms render as `method="post"` even before hydration, preventing credentials from falling back into a query string. Login return paths use a strict same-origin path sanitizer and reject scheme-relative, backslash, control-character, and explicitly schemed values.

## Authorization and commerce integrity

- Spring Security has an explicit public allow-list; every other route requires authentication. Stable JSON responses distinguish unauthenticated and access-denied requests without exposing internals.
- Role, active shop membership, customer ownership, and resource relationships are derived by backend services. Identifier substitution cannot make a client the owner.
- Checkout recalculates product/variant eligibility, price, promotion, voucher, shipping, stock, and totals. Client-supplied owner, role, money, availability, status, and provider success are ignored.
- Inventory reservation uses atomic/locked transaction boundaries and non-negative constraints. Order items and addresses are immutable snapshots.
- Order changes are explicit state-machine commands. Checkout, provider callbacks, promotion/voucher usage, refund processing, and seller-ledger effects use unique idempotency/deduplication boundaries.
- Critical admin moderation and operations are written to the audit trail with actor/resource/request metadata.

## Request, browser, and WebSocket boundaries

- CORS is a credentialed exact-origin allow-list. Cookie-mutating auth endpoints also validate Origin and Fetch Metadata.
- JSON/multipart validation is applied at controller DTO boundaries. URL inputs accept only well-formed HTTP(S) URLs; arbitrary schemes such as `javascript:` are rejected.
- STOMP requires bearer authentication, permits only the current user's private `/user/queue/chat` subscription, and rejects arbitrary client SEND frames/destinations.
- Request correlation accepts or generates a bounded `X-Request-Id`. Logs do not include credentials; unexpected errors log the exception type rather than a raw message that may contain sensitive input.
- Production responses omit stack traces and raw internal exception text. Production OpenAPI/Swagger is disabled by default.

## Upload and object-storage controls

Product/review uploads are capped at 5 MB and accept JPEG, PNG, or WebP only when the declared MIME type matches the file signature. JPEG/PNG must decode to readable dimensions; WebP RIFF/chunk structure and dimensions are parsed. Images over 10,000 pixels on either axis or 25 million pixels total are rejected. Object keys are generated server-side and seller/customer ownership is checked before metadata mutation.

Production must still provide least-privilege object credentials, HTTPS public media, bucket CORS/read policy, retention, moderation/malware policy, and credential rotation. Local MinIO's anonymous-read bucket is development behavior, not the production policy.

## Abuse controls

Redis-backed fixed-window limits cover login, registration, account recovery, search, and sensitive checkout/upload/webhook mutations. Limits return the normal `429` API envelope. During Redis failure, a bounded per-instance fixed-window fallback continues enforcing limits for up to 10,000 identities and fails closed for new identities when that capacity is full.

The degraded fallback is weaker across multiple replicas because counters are then per instance rather than distributed. Production should monitor Redis/fallback warnings, enforce edge limits where appropriate, and size policies against real traffic without relying on client identifiers.

## Production fail-fast configuration

With `SPRING_PROFILES_ACTIVE=prod`, startup refuses known development/default or undersized JWT/database/object-storage secrets; insecure cookies; disabled email verification, email delivery, or rate limiting; enabled mock payment or mock shipping; empty/non-HTTPS/localhost CORS and web URLs; and a non-HTTPS public media endpoint. The relevant settings are documented in [DEPLOYMENT.md](DEPLOYMENT.md).

This validator is a last guardrail, not a secret manager. Secrets must be injected at runtime. Production still needs TLS/proxy configuration, protected management access, backup/restore, external-provider credential rotation, and deployment-level browser headers.

## Verification expectations

- Run focused authentication, origin, redirect, rate-limit/fallback, URL, upload, WebSocket, and production-startup tests.
- Run seller/customer/admin object-authorization and identifier-substitution tests.
- Run inventory/voucher/promotion concurrency, checkout/payment/refund idempotency, multi-shop isolation, immutable snapshot, and finance/audit tests.
- Exercise the same controls behind the actual production proxy and origins; a MockMvc or unit result alone does not prove deployed headers/cookies/CORS.
- Review logs, traces, Playwright output, Android output, and generated runtime state for secrets before retaining artifacts.

## Known release gaps

- Hosted CI run `32328281486` proves backend, web, Android, and live API-contract jobs on GitHub's runner. Software-composition analysis, secret scanning, and container-image vulnerability scanning are still absent.
- Seller/Admin static-host CSP and deployment security headers are not represented or live-verified in this repository. Apply and test CSP, frame, MIME, referrer, permissions, HSTS, and cache policy at the hosting/proxy layer.
- Rate-limit fallback protects one process during Redis failure but loses globally coordinated counts across replicas; edge protection and Redis availability monitoring remain necessary.
- Application rate-limit identity is currently `request.remoteAddr`. Configure forwarded-header trust only at the real proxy and review shared-NAT/IPv6 behavior; combine it with edge policy rather than treating an untrusted forwarded IP as identity.
- Only COD and local mock payment/shipping adapters exist. Production rejects both mocks, and Storefront reads the public backend capability list so unavailable methods are not presented. Real provider signatures, secrets, callback network policy, refunds, failure handling, reconciliation, and compliance still need a provider-specific review before exposure.
- Android production push lacks authenticated backend device-token registration/rotation/revocation and credentialed FCM delivery.
- In-process after-commit notification/provider effects are not a durable outbox. Introduce persisted retry/idempotent workers before promising crash-safe external delivery.
- A final attack-oriented review of every required scenario and a real deployment penetration/security review remain acceptance work; do not infer production security from local green tests.
