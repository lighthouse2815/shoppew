# Native Android client

The customer application lives in `mobile/android` and uses Kotlin, Jetpack Compose, ViewModels/StateFlow, Retrofit/OkHttp, Hilt, Coil, and Room. It consumes the same versioned `/api/v1` contract as the web clients and does not use a fake repository when the backend is available.

## Supported customer flow

The current application covers session restore, login, registration, home, category and search browsing, product detail and variants, cart, address management, authoritative checkout, COD order placement, order success/list/detail, wishlist, notifications, reviews, profile, and security settings. Product/category reads may fall back to the Room cache after a network failure; cart, checkout, order, payment, address, and account mutations are never silently queued offline.

## Run against the local backend on a real device

The verified workflow uses a USB-connected Android phone. It does not install or start an emulator. If no physical device is connected, stop and ask the user to connect one with USB debugging enabled.

From the repository root, confirm the exact device first:

```powershell
adb devices -l
```

Replace `<serial>` below with the listed device serial. `adb reverse` makes the host backend and MinIO object URLs available as `127.0.0.1`/`localhost` from the phone:

```powershell
adb -s <serial> reverse tcp:28080 tcp:28080
adb -s <serial> reverse tcp:9000 tcp:9000

Set-Location .\mobile\android
.\gradlew.bat assembleDebug -PSHOPPEW_DEBUG_API_BASE_URL=http://127.0.0.1:28080/
adb -s <serial> install -r .\app\build\outputs\apk\debug\app-debug.apk
```

The debug base URL is a build-time setting. Release builds use `SHOPPEW_API_BASE_URL`, must use HTTPS, and must not embed provider secrets.

## Session and network security

- The short-lived access token is held only in process memory.
- The rotating refresh cookie is stored in private preferences only after AES-GCM encryption. The AES key is created and retained by `AndroidKeyStore` and is not exportable from the application.
- OkHttp redacts `Authorization`, `Cookie`, and `Set-Cookie` headers. Release logging is disabled.
- A synchronized authenticator performs at most one refresh retry and clears both token stores if refresh fails.
- Debug cleartext traffic exists only for local development; release traffic disables it.

## Notifications and push boundary

In-app notifications come from the authenticated backend timeline. Android 13+ notification permission is requested only from the Notifications screen after an explicit tap; the application does not prompt on launch. The screen distinguishes granted, denied, permanently blocked, and settings-return states.

The application contains the FCM receiving service, notification channel, Firebase Installation ID (FID) lifecycle, and typed deep-link routing for product, order, and notification destinations. After refresh/login/registration succeeds, Android resolves its current FID and idempotently registers it through the authenticated API. Logout sends the cached FID in a validated JSON body and revokes it before the refresh session is cleared, so the identifier is not exposed in a URL. FCM rotates its lower-level registration token independently; the backend targets the stable FID, and the FID itself is never treated as authentication.

The backend stores only a SHA-256 target hash plus an AES-256-GCM encrypted target, transfers a reused installation to the currently authenticated account, limits active targets per account, and deactivates unregistered FIDs reported by FCM. Outbound email/push failures are persisted and retried with bounded exponential delay up to five attempts.

This closes the repository-owned lifecycle, but it is not proof of production push delivery. A release still needs an actual Firebase Android application configuration, FCM HTTP v1 enabled, `APP_PUSH_DELIVERY_ENABLED=true`, a project ID, Application Default Credentials with least-privilege messaging access, and a real-device end-to-end delivery. When delivery is disabled, the local sender records `SKIPPED` explicitly.

## Verification

Run the deterministic local gates from `mobile/android`:

```powershell
.\gradlew.bat testDebugUnitTest --rerun-tasks
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug -PSHOPPEW_DEBUG_API_BASE_URL=http://127.0.0.1:28080/
.\gradlew.bat compileDebugAndroidTestKotlin
.\gradlew.bat connectedDebugAndroidTest -PSHOPPEW_DEBUG_API_BASE_URL=http://127.0.0.1:28080/
```

The earlier physical-device evidence below is from 2026-08-11. Current FID registration/revocation changes are covered by JVM/backend tests and a debug build; they have not been installed on the phone without a new explicit device-QA authorization.

Current earlier device evidence:

- 24 JVM tests passed across secure session storage, notification permission state, real repository parsing/error handling, formatters, commerce ViewModels, and session restoration.
- `lintDebug`, debug assembly, and Android-test compilation completed successfully.
- 7 connected tests passed on the physical Samsung `SM-G988B`, Android 13; no emulator was installed or used.
- Real-device QA used the live Spring Boot/PostgreSQL backend for login/register, browsing/search, product detail, cart, address, COD checkout/order detail, process/session restoration, deep links, and Room offline fallback.
- The contextual permission flow changed from `Chưa bật` to `Đã bật`, and Android reported `POST_NOTIFICATIONS: granted=true`. Evidence images are in `.artifacts/android/notification-permission-before.png` and `.artifacts/android/notification-permission-after.png`.
