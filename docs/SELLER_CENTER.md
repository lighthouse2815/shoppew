# Seller Center

The Seller Center is the Vietnamese seller operations client at `web/seller`. It is a Vite, React 19, TypeScript, TanStack Query application that consumes the same `/api/v1` and generated OpenAPI contract as the storefront and Android client.

## Run locally

Start the healthy backend first, then run:

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/seller dev
```

Open http://localhost:3001. `VITE_API_URL` defaults to `http://localhost:28080` and can be overridden in `web/seller/.env.local`. The backend CORS allowlist must include the exact Seller Center origin.

Verification commands:

```powershell
corepack pnpm --filter @shoppew/seller lint
corepack pnpm --filter @shoppew/seller typecheck
corepack pnpm --filter @shoppew/seller test
corepack pnpm --filter @shoppew/seller build
```

## Authentication and shop scope

The access token remains in memory. The backend-issued rotating refresh credential stays in an HttpOnly cookie and is retried through the shared API client. Logout revokes the current session. Shop queries are keyed by authenticated user ID so changing accounts cannot display the previous seller's cached shops. Every mutation still verifies active shop membership and resource ownership in the backend; a selected client-side shop ID is never an authorization decision.

## Seller workflows

| Area | Working flow |
| --- | --- |
| Onboarding | Login, create a first shop, see moderation status, and switch among authorized shops |
| Dashboard | Completed revenue, average order value, available/pending balance, recent orders, and low-stock alerts from live APIs |
| Products | List/filter, create/edit content, category and brand, upload/delete media, category attributes, option/value CRUD, variant/SKU/price/dimensions/image/status CRUD, submit for moderation, and archive |
| Inventory | Search, filter low stock, increase/decrease/set stock and warning threshold, and inspect the immutable ledger with signed available-stock deltas |
| Orders | Filter and open immutable snapshots, inspect address/shipment/history, and issue only the server-supported fulfillment or COD-cancel commands |
| Refunds and disputes | Review owned refund requests; inspect persisted dispute timelines and append seller messages |
| Discounts | Create/edit/state-manage vouchers and multi-target product, shop, or flash-sale promotions |
| Reviews | Read shop reviews and publish an authorized seller reply |
| Finance and analytics | Read authoritative balance/ledger data and bounded sales, order, top-product, and low-stock metrics |
| Shop operations | Manage pickup/return addresses, public shop profile, currency/timezone, and shipping/return policies |

All major pages expose loading, empty, error, disabled/pending, validation, and success behavior where applicable. Tables collapse into scrollable or disclosed mobile layouts, and a mobile logout control remains available when the desktop sidebar becomes horizontal navigation.

## Current verification evidence

On 2026-08-11:

- ESLint and TypeScript completed without errors; Vitest passed 11/11 tests; the production Vite build completed with a 372.38 kB JavaScript asset and 29.00 kB CSS asset before gzip.
- PostgreSQL Testcontainers passed the two focused catalog/inventory regressions, including authorized/nonmember attribute-definition reads, OpenAPI discovery, and the inventory list without a search term.
- The rebuilt Compose backend became healthy with the current Flyway V1–V11 schema.
- Browser QA authenticated as a real seller and loaded dashboard, products/detail, inventory/history, orders/detail, refunds, disputes, vouchers, promotions, reviews, finance, analytics, addresses, and settings with live smoke data and no application error state.
- A browser-driven inventory write increased available stock `23 -> 24`, then decreased it `24 -> 23`; both notes appeared in the ledger and the original stock was restored.
- The final HMR regression pass confirmed signed ledger deltas (`-1` stock-out and `+1` stock-in), one-based review-image labels (`Xem ảnh 1`), and non-destructive multi-target promotion editing with save disabled until every new target is valid.
- Responsive checks at 360, 768, and 1280 pixels reported no document-level horizontal overflow. The 360-pixel layout retained shop switching, horizontal module navigation, storefront access, and logout.

Browser QA found and fixed three runtime defects that static checks had not proved: a Spring Data shop query derived from a non-persistent getter, PostgreSQL `lower(bytea)` inference for a null inventory search, and stale shop query data after account switching. It also corrected cancellation-reason state timing, inventory ledger signs, review-image numbering, destructive single-target promotion editing, and missing category-attribute/option-value/dimension controls.
