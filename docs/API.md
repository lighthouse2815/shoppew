# shoppew HTTP API

## Contract

All application routes are versioned under `/api/v1`. Successful and failed responses share one envelope:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-08-11T00:00:00Z"
}
```

Errors use a stable machine-readable `error.code`, a user-facing `error.message`, and optional field `details`. The backend accepts or creates `X-Request-Id` and returns it on the response. Bearer access tokens protect customer, seller, and admin routes. Refresh credentials are rotating HttpOnly cookies and are never returned in JSON.

The live OpenAPI document is available at `/v3/api-docs` in local development. Swagger UI is available at `/swagger-ui.html`.

## Calling the API

JSON requests use `Content-Type: application/json`; image endpoints use `multipart/form-data`. Protected calls send `Authorization: Bearer <access-token>`. The access token is short-lived. Browser login/refresh also maintains the rotating `shoppew_refresh` HttpOnly cookie; that refresh secret is never present in the JSON response.

PowerShell example using a generated local demo account:

```powershell
$body = @{
    email = 'catalog-buyer-<runId>@example.test'
    password = '<password from ignored test-results/e2e-runtime.json>'
    deviceName = 'PowerShell API QA'
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri 'http://localhost:28080/api/v1/auth/login' `
    -ContentType 'application/json' `
    -Body $body `
    -SessionVariable shoppewSession

$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
Invoke-RestMethod `
    -Uri 'http://localhost:28080/api/v1/auth/me' `
    -Headers $headers `
    -WebSession $shoppewSession
```

Do not write access/refresh tokens to source, screenshots, shell history, or logs. Cookie-authenticated refresh/logout requests must also satisfy the backend's trusted Origin and Fetch Metadata checks.

## Requests and responses

An authentication request is shaped as:

```json
{
  "email": "customer@example.test",
  "password": "local-development-value",
  "deviceName": "Chrome on QA workstation"
}
```

Its response keeps the same global envelope:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresAt": "2026-08-11T04:15:00Z",
    "requiresEmailVerification": false,
    "user": {
      "id": "247b77ba-86bf-4f65-96d8-7b183b6292e8",
      "email": "customer@example.test",
      "displayName": "Khách hàng QA",
      "roles": ["CUSTOMER"],
      "status": "ACTIVE",
      "emailVerified": true
    }
  },
  "error": null,
  "timestamp": "2026-08-11T04:00:00Z"
}
```

This is a shape example, not a seeded credential or guaranteed fixed identifier. Swagger is authoritative for required fields and each endpoint's concrete DTO.

## Pagination

Paged endpoints use zero-based `page` and a positive `size`; current controllers cap `size` at 100 and use endpoint-specific defaults (normally 20, or 50 for message/inventory timelines). Filters and sort values are endpoint-specific and documented in OpenAPI.

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  },
  "error": null,
  "timestamp": "2026-08-11T04:00:00Z"
}
```

Clients must use `totalElements`/`totalPages`, not infer the final page from a short response. A page beyond the end is an empty page, while malformed or out-of-range parameters return a validation error.

## Errors, status codes, and correlation

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": [
      {
        "field": "email",
        "code": "Email",
        "message": "must be a well-formed email address"
      }
    ]
  },
  "timestamp": "2026-08-11T04:00:00Z"
}
```

| HTTP status | Meaning |
| --- | --- |
| `400` | Malformed JSON, validation failure, unsupported option, or invalid command input |
| `401` | Missing/expired/invalid credential or revoked session |
| `403` | Authenticated actor lacks the required role, membership, or object access |
| `404` | Resource is absent or deliberately not exposed to this actor |
| `409` | State transition, idempotency-key reuse, inventory/voucher concurrency, or uniqueness conflict |
| `413` | Multipart request exceeds the configured upload boundary |
| `429` | Rate-limited request; retry only after backoff and do not bypass the policy |
| `500` | Generic unexpected failure; the response does not expose a stack trace or internal exception text |

Send a valid `X-Request-Id` to correlate a call, or retain the value generated and returned by the backend. Error handling should branch on `error.code`, not parse localized `message` text. A retryable network error is not permission to repeat a non-idempotent mutation; follow the endpoint's `Idempotency-Key` contract.

## Audience and authorization

- `/api/v1/public/**` is anonymous catalog/system discovery.
- Customer routes derive the customer from the bearer identity; user/address/cart/order/chat/refund identifiers are checked against that identity.
- `/api/v1/seller/shops/{shopId}/**` requires an active shop owner/member with the operation's role. A client-provided `shopId` never grants access.
- `/api/v1/admin/**` uses backend role checks for staff/moderator/admin operations and records critical mutations in the audit trail.
- STOMP connects at `/api/v1/ws` with bearer authentication. Clients may subscribe only to their private `/user/queue/chat`; arbitrary SEND destinations are rejected.

## Swagger/OpenAPI lifecycle

In local development, open `http://localhost:28080/swagger-ui.html`, call login, and use **Authorize** with the access token. OpenAPI declares `bearerAuth` globally for protected operations and explicitly clears security for public/auth-entry operations, so Swagger sends the token where required without mislabeling login or public catalog calls. Generate TypeScript types only from the matching running backend:

```powershell
corepack pnpm --filter @shoppew/api-client generate
```

Production profile disables `/v3/api-docs` and Swagger by default. `APP_OPENAPI_ENABLED=true` only enables the application endpoints; it does not authenticate them. If operators need production OpenAPI, place it behind a separately authenticated private network/proxy and do not expose it as public product documentation.

## Authentication and account

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Create a customer account and session |
| `POST` | `/api/v1/auth/login` | Create a session and access token |
| `POST` | `/api/v1/auth/refresh` | Rotate the refresh-token family |
| `POST` | `/api/v1/auth/logout` | Revoke the current session |
| `GET` | `/api/v1/auth/me` | Read the current identity |
| `GET/DELETE` | `/api/v1/auth/sessions` | List or revoke sessions |
| `POST` | `/api/v1/auth/verify-email/request` | Request a one-time verification link |
| `POST` | `/api/v1/auth/verify-email/confirm` | Consume a verification token |
| `POST` | `/api/v1/auth/forgot-password` | Request a non-enumerating reset link |
| `POST` | `/api/v1/auth/reset-password` | Consume a reset token and revoke sessions |

Profile and owned address resources are exposed under `/api/v1/users/me/profile` and `/api/v1/users/me/addresses`.

## Admin identity and marketplace oversight

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/admin/users`, `/users/{userId}` | Filter users and inspect roles, status, profile, shops, and active-session count |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Suspend/ban/restore with a required reason and audit record |
| `GET` | `/api/v1/admin/sellers`, `/sellers/{userId}` | Filter sellers and inspect their shops/operational state |
| `GET/PATCH` | `/api/v1/admin/shops`, `/shops/{shopId}/status` | Inspect and moderate marketplace shops |
| `GET` | `/api/v1/admin/orders`, `/orders/{orderId}` | Read order snapshots, status/shipment/payment history across shops |
| `GET` | `/api/v1/admin/payments`, `/payments/{paymentId}` | Filter and inspect payment/provider event state |

These routes require `ADMIN` or `SUPER_ADMIN`; the relevant moderation and finance routes below apply their own method-level roles. Admin settings are an effective read-only view, not a mutation surface.

## Shop and catalog

| Audience | Base route | Capabilities |
| --- | --- | --- |
| Public | `/api/v1/public/categories`, `/brands` | Active category tree and brands |
| Public | `/api/v1/public/products` | Paged approved-product search with keyword, shop/category/brand, decimal price/rating filters, six sorts, and detail by slug |
| Public | `/api/v1/public/shops/{slug}` | Active shop profile |
| Public | `/api/v1/public/commerce-capabilities` | Payment providers and shipping methods registered in the current runtime; checkout clients must not invent unavailable methods |
| Seller | `/api/v1/seller/shops` | Create and manage owned shops |
| Seller | `/api/v1/seller/shops/{shopId}` | Addresses and commerce settings |
| Seller | `/api/v1/seller/shops/{shopId}/products` | Drafts, product content, options, values, variants, typed attributes, media, submission, and archive |
| Seller | `/api/v1/seller/shops/{shopId}/products/attribute-definitions` | Read applicable category attribute definitions after active shop-membership authorization |
| Admin | `/api/v1/admin/categories`, `/brands` | Taxonomy and brand management |
| Admin | `/api/v1/admin/shops/{shopId}/status` | Shop activation and suspension |
| Admin | `/api/v1/admin/products` | Pending queue, approve, reject, hide, and attribute definitions |
| Admin | `/api/v1/admin/settings` | Read effective locale, currency, business time zone, provider availability, object-storage type, and upload limit; there is no mutation endpoint |

Product media accepts multipart field `file` plus optional `altText` and `primary`. JPEG, PNG, and WebP are accepted up to 5 MB only when the declared MIME type matches the file signature. Object bytes live in S3-compatible storage; PostgreSQL retains the durable object key and catalog metadata.

## Inventory and cart

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/seller/shops/{shopId}/inventory` | Paged shop inventory with text and low-stock filters |
| `POST` | `/api/v1/seller/shops/{shopId}/inventory/{variantId}/adjustments` | Increase, decrease, or set available stock and threshold |
| `GET` | `/api/v1/seller/shops/{shopId}/inventory/{variantId}/transactions` | Immutable stock ledger timeline |
| `GET` | `/api/v1/cart` | Revalidate and return the current cart grouped by shop |
| `POST` | `/api/v1/cart/items` | Add a variant by ID and quantity; client price is not accepted |
| `PUT` | `/api/v1/cart/items/{itemId}` | Replace an owned item quantity |
| `PATCH` | `/api/v1/cart/items/{itemId}/selection` | Select or deselect one item |
| `PUT` | `/api/v1/cart/selection` | Select or deselect a provided set, or every item when the set is empty |
| `DELETE` | `/api/v1/cart/items/{itemId}` | Remove one owned item |
| `DELETE` | `/api/v1/cart/items` | Clear the current user's cart |

Every cart read recalculates current unit prices, stock, shop/product/variant eligibility, line totals, selected subtotals, and issue codes. Cart mutations reject unavailable variants and quantities over authoritative stock. Cart rows do not reserve inventory; reservation happens at checkout.

## Checkout, orders, payments, and shipping

Checkout request bodies contain only owned cart item IDs, an owned address ID, a payment provider, a shipping method code, and an optional customer note. Development can register `MOCK_ONLINE` and `MOCK_STANDARD`; production rejects both mock adapters. Clients read `/api/v1/public/commerce-capabilities` and render only the methods active in that backend runtime. The backend recalculates every monetary value and rejects stale, unavailable, or ineligible input.
Up to five optional `voucherCodes` may be supplied. Promotion prices and voucher discounts are always resolved again inside the placement transaction.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/checkout/preview` | Authoritative per-shop items, shipping, discounts, and grand-total quote |
| `POST` | `/api/v1/checkout` | Place one checkout and split it into one seller order per shop; requires `Idempotency-Key` |
| `GET` | `/api/v1/orders` | Page through the current customer's orders |
| `GET` | `/api/v1/orders/{orderId}` | Owned order detail with immutable items/address, history, shipment, and tracking |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Cancel an eligible pending-online checkout or confirmed COD order |
| `POST` | `/api/v1/orders/{orderId}/complete` | Customer confirms receipt after delivery |
| `GET` | `/api/v1/seller/shops/{shopId}/orders` | Page/filter orders for an authorized shop member |
| `GET` | `/api/v1/seller/shops/{shopId}/orders/{orderId}` | Seller-owned order detail |
| `POST` | `/api/v1/seller/shops/{shopId}/orders/{orderId}/{action}` | Explicit `confirm`, `process`, `ready-to-ship`, `ship`, `deliver`, or `cancel` command |
| `GET` | `/api/v1/payments/{paymentId}` | Customer-owned payment status and next action |
| `POST` | `/api/v1/payments/mock/webhook` | Local mock-online provider callback with `X-Shoppew-Mock-Signature` |

Replaying the same checkout key and request returns the original checkout even after cart rows are removed. Reusing that key with different content returns a conflict. Online provider events are deduplicated by provider/event ID and payload hash; a repeated event has no second inventory or order effect.

See [ORDER_FLOW.md](ORDER_FLOW.md) and [PAYMENT_FLOW.md](PAYMENT_FLOW.md) for transition and retry rules.

## Vouchers and promotions

| Audience | Base route | Capabilities |
| --- | --- | --- |
| Seller | `/api/v1/seller/shops/{shopId}/vouchers` | List, create, update, activate, pause, and archive shop-owned vouchers |
| Admin | `/api/v1/admin/vouchers` | List, create, update, activate, pause, and archive platform vouchers |
| Seller | `/api/v1/seller/shops/{shopId}/promotions` | Manage targeted product/shop/flash-sale promotion foundations for owned products |
| Admin | `/api/v1/admin/promotions` | Manage targeted platform campaigns and product/flash-sale promotions |

Product/variant responses expose effective `price`, `originalPrice`, and optional promotion metadata. Cart items expose effective `unitPrice`, `originalUnitPrice`, and promotion metadata. Checkout preview/placement expose the server discount total plus applied voucher summaries. See [VOUCHER_PROMOTION_FLOW.md](VOUCHER_PROMOTION_FLOW.md) for rules and concurrency behavior.

## Search, recommendations, and chat

| Audience | Route | Capabilities |
| --- | --- | --- |
| Public | `/api/v1/public/products` | PostgreSQL-backed keyword/filter/sort/pagination search over active products, variants, and shops |
| Public | `/api/v1/public/search/suggestions` | Distinct active product-name suggestions for a query of at least two characters |
| Public | `/api/v1/public/recommendations/popular`, `/trending` | Best-selling and seven-day order-activity strategies |
| Public | `/api/v1/public/recommendations/products/{productId}/related` | Same-category, same-shop, and trending fallbacks without duplicates |
| Public | `/api/v1/public/recommendations/shops/{shopId}` | Active products from one shop with an optional product exclusion |
| Customer | `/api/v1/recommendations/recently-viewed` | Record and list durable user/product view history |
| Customer | `/api/v1/chat/conversations` | Start/list owned conversations and page/send persisted messages |
| Seller | `/api/v1/seller/shops/{shopId}/chat/conversations` | Page/send messages after active membership and exact shop/conversation checks |

Chat message types are `TEXT`, `IMAGE`, `PRODUCT`, and `ORDER`. Product and order references are revalidated against the conversation shop/customer. The optional STOMP transport connects at `/api/v1/ws` with a bearer token and exposes only `/user/queue/chat` subscriptions. See [DISCOVERY_CHAT.md](DISCOVERY_CHAT.md) for the search strategies, recommendation fallbacks, authorization boundary, and current verification.

## Wishlist, reviews, and notifications

| Audience | Route | Capabilities |
| --- | --- | --- |
| Customer | `/api/v1/wishlist` | List the current wishlist; add or remove a product by ID with duplicate-safe adds |
| Customer | `/api/v1/reviews`, `/reviews/me` | Create one review for an eligible completed order item, edit owned content/rating, and manage up to five images |
| Public | `/api/v1/public/products/{productId}/reviews` | Page through published verified-purchase reviews |
| Seller | `/api/v1/seller/shops/{shopId}/reviews` | Page reviews belonging to an authorized owned/member shop |
| Seller | `/api/v1/seller/shops/{shopId}/reviews/{reviewId}/reply` | Reply to a review only for an authorized owned/member shop |
| Admin | `/api/v1/admin/reviews/{reviewId}/{action}` | Publish, hide, or remove review content |
| Customer | `/api/v1/notifications` | Page in-app notifications, read unread count, mark one read, or mark all read |
| Customer | `PUT /api/v1/notifications/devices/current` | Idempotently register the current Android FID; ownership is derived from the access token and the target is encrypted at rest |
| Customer | `DELETE /api/v1/notifications/devices/current` | Revoke only the current account's matching FID from a validated JSON body, used before Android logout; the target is never placed in the URL |

Review rating is constrained to 1–5. Eligibility is derived from the authenticated customer's immutable order item and a `COMPLETED` order; clients cannot claim purchase status, product, or shop. Published review mutations and moderation recompute product/shop rating average and count inside the transaction. Order and chat events persist owned in-app notifications and separate per-channel delivery records. SMTP delivery is verified locally; push registration/revocation and bounded persisted retry are implemented, while real FCM credentials and physical-device delivery remain deployment gates. See [ENGAGEMENT_FLOW.md](ENGAGEMENT_FLOW.md).

## Refunds, disputes, finance, audit, and analytics

| Audience | Route | Capabilities |
| --- | --- | --- |
| Customer | `/api/v1/refunds` | Create a server-priced item refund, list/detail owned requests, and cancel a request before review |
| Seller | `/api/v1/seller/shops/{shopId}/refunds` | List authorized shop requests and move a new request under review |
| Admin | `/api/v1/admin/refunds` | Filter requests, approve/reject with notes, and idempotently process an approved refund |
| Customer | `/api/v1/disputes` | Open a dispute for an eligible owned order/refund and append persisted messages |
| Seller | `/api/v1/seller/shops/{shopId}/disputes` | Read and reply only within an authorized shop |
| Admin | `/api/v1/admin/disputes` | Review timelines, reply, assign, change state, and persist a resolution |
| Seller | `/api/v1/seller/shops/{shopId}/finance` | Read the locked balance aggregate and append-only transaction timeline |
| Seller | `/api/v1/seller/shops/{shopId}/analytics` | Real revenue/order/top-product/low-stock metrics over an optional bounded time range |
| Admin | `/api/v1/admin/analytics` | Real GMV-like, order, user, shop, moderation, and refund-volume metrics |
| Admin | `/api/v1/admin/audit-logs` | Page immutable-style critical-operation records with actor/request metadata |

Refund creation accepts item IDs and quantities, never a client amount. The backend checks ownership, completion/partial-refund state, return window, remaining refundable quantity, and prorates immutable order discounts. Admin processing requires `Idempotency-Key`; replay returns the same refund without a second payment, order, balance, or ledger effect. The local synchronous refund implementation persists an explicit request/refund lifecycle that a future provider adapter must preserve. See [REFUND_FINANCE_FLOW.md](REFUND_FINANCE_FLOW.md).

## Local verification

With the Compose stack healthy, run:

```powershell
.\scripts\smoke-catalog.ps1
```

The smoke creates uniquely named development records, performs the admin/seller/public lifecycle through HTTP, downloads uploaded product/review MinIO objects, then runs wishlist idempotency, address and cart revalidation, promotion and seller/platform voucher creation, authoritative discounted checkout preview, idempotent COD placement, explicit fulfillment, shipment tracking, customer completion, notification read state, verified-purchase review and seller reply, seller settlement, refund approval/idempotent processing, dispute resolution, audit/analytics reads, and final PostgreSQL inventory/order/payment/discount/engagement/operations checks. It does not delete existing data.
