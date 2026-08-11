# Authentication and session security

shoppew uses short-lived signed access tokens and server-recorded rotating refresh sessions. This document describes the implemented browser and native Android contracts.

## Browser strategy

- Access tokens are HS512 JWTs with issuer `shoppew`, a 15-minute default lifetime, and `sub`, `sid`, `jti`, `email`, and `roles` claims.
- The browser keeps the access token in application memory. It must not persist it in `localStorage`.
- The refresh credential is a 48-byte random value delivered only through the `shoppew_refresh` cookie. The cookie is `HttpOnly`, `SameSite=Strict`, scoped to `/api/v1/auth`, and `Secure` when `APP_SECURE_COOKIES=true`.
- PostgreSQL stores only a SHA-256 hash of each refresh credential. Sessions record their device, user agent, family, expiry, last use, revocation, and rotation ancestry.
- Every refresh rotates the credential. Reusing an already-rotated credential revokes the complete token family.
- JWT validation checks signature, HS512 algorithm, issuer, timestamps, user/session ownership, expiry, and current session revocation. Revoking a session therefore invalidates its existing access token immediately.
- Cookie-mutating refresh and logout requests reject untrusted `Origin` values and cross-site Fetch Metadata in addition to the strict SameSite cookie policy.
- CORS uses an explicit credentialed origin allow-list. Deployed origins must be supplied through `APP_CORS_ALLOWED_ORIGINS`.

## Android strategy

- The short-lived access token is process-memory only and is attached by an OkHttp interceptor.
- The backend refresh cookie is captured by a dedicated cookie jar, encrypted with AES-GCM, and stored in application-private preferences. Its AES key is created and retained by `AndroidKeyStore`.
- Session restoration uses the encrypted refresh cookie. A synchronized authenticator performs one refresh retry after `401`; failed refresh clears both access and refresh state.
- Debug network logging is `BASIC` and redacts `Authorization`, `Cookie`, and `Set-Cookie`; release logging is disabled.
- Logout calls the backend first when possible and always clears local credentials. Cart, checkout, order, address, and account mutations are never queued for later replay by the offline cache.

The storage codec, expired/corrupt-cookie cleanup, refresh retry, and repository session behavior have current automated coverage. The integrated flow was also verified on a physical Samsung `SM-G988B` running Android 13; no emulator was installed or used. See [ANDROID.md](ANDROID.md) for the commands and evidence boundary.

## Public endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Create a customer account and profile. |
| `POST` | `/api/v1/auth/login` | Authenticate and start a refresh session. |
| `POST` | `/api/v1/auth/refresh` | Rotate the refresh cookie and issue a new access token. |
| `POST` | `/api/v1/auth/logout` | Revoke the cookie session and expire its cookie. |
| `POST` | `/api/v1/auth/verify-email/request` | Send a generic, non-enumerating verification response. |
| `POST` | `/api/v1/auth/verify-email/confirm` | Consume a one-time verification token. |
| `POST` | `/api/v1/auth/forgot-password` | Send a generic, non-enumerating password-reset response. |
| `POST` | `/api/v1/auth/reset-password` | Consume a one-time token, change the password, and revoke all sessions. |

Authenticated endpoints include `/me`, `/sessions`, session revocation by ID, and logout from all devices. All responses use the common API envelope and never return password or token hashes.

## Email verification and password reset

Action tokens are random high-entropy values. Only their SHA-256 hashes are stored in `auth_action_tokens`; tokens are type-bound, expire, and can be consumed once. Issuing a replacement consumes outstanding tokens of the same type. Verification defaults to 24 hours and password reset to 30 minutes.

Committed actions publish an after-commit delivery event so a message is never sent for a transaction that later rolls back. Local Docker delivery uses Mailpit at `http://localhost:8025`. Delivery can be disabled for isolated tests without exposing action tokens in logs.

Set `APP_EMAIL_VERIFICATION_REQUIRED=true` in environments where users must verify before login. Local development defaults to `false` for quick bootstrapping; production configuration must enable it and supply a real SMTP provider.

## Account and password controls

- Account states are `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, and `BANNED`.
- Roles are `CUSTOMER`, `SELLER`, `SHOP_STAFF`, `MODERATOR`, `ADMIN`, and `SUPER_ADMIN`.
- Passwords are BCrypt-hashed with cost 12 and must contain at least one lowercase letter, uppercase letter, and digit, with a minimum length of 10.
- Login uses a dummy password hash for unknown email addresses to reduce timing-based account discovery.
- Five failed password attempts temporarily lock login for 15 minutes; successful login clears the counter.
- Password changes revoke every active refresh session and their linked access tokens.

## Production checklist

Use unique secrets and configure at minimum:

```text
APP_JWT_SECRET=<at least 64 unpredictable UTF-8 bytes>
APP_SECURE_COOKIES=true
APP_EMAIL_VERIFICATION_REQUIRED=true
APP_EMAIL_DELIVERY_ENABLED=true
APP_EMAIL_FROM=<verified sender>
APP_WEB_BASE_URL=https://<storefront-host>
APP_CORS_ALLOWED_ORIGINS=https://<storefront>,https://<seller>,https://<admin>
```

Terminate TLS at a trusted proxy, keep forwarded-header handling restricted to that proxy, and never reuse the development secret. Production startup now fails fast for known development/undersized secrets, insecure cookie/email/rate-limit/mock-payment settings, and non-HTTPS public origins. Redis-backed fixed-window limits cover authentication/recovery and other abuse-sensitive routes, with a bounded per-instance fallback during Redis failure. See [SECURITY.md](SECURITY.md) and [DEPLOYMENT.md](DEPLOYMENT.md) for remaining deployment gaps.
