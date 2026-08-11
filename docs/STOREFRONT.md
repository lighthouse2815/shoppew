# Customer storefront

## Stack and contract

The customer application lives in `web/storefront` and uses Next.js 16 App Router, React 19, TypeScript, TanStack Query, React Hook Form, Zod, and the repository-scoped shoppew design system. `web/packages/api-client/src/schema.d.ts` is generated from the live Spring Boot OpenAPI document; it is not a hand-maintained duplicate contract.

```powershell
corepack pnpm --filter @shoppew/api-client generate
corepack pnpm --filter @shoppew/storefront dev
```

`NEXT_PUBLIC_API_URL` defaults to `http://localhost:28080`. `NEXT_PUBLIC_MEDIA_ORIGIN` identifies the browser-visible S3/CDN origin or prefix and defaults locally to `http://localhost:9000`. Both are public build configuration: change them before dev/build and never store secrets in them. The normal storefront origin is `http://localhost:3000`; the backend CORS list must contain that exact origin.

Next image optimization accepts local MinIO/private addresses only in development. Production rejects private-network image origins and must use the configured HTTPS media origin. Restart/rebuild after changing either public value.

## Authentication

- Login and registration return a short-lived access token held only in React memory.
- The browser sends the rotating refresh cookie with `credentials: include`; JavaScript never reads or persists it.
- Initial load attempts one refresh, and an authenticated request retries once after a `401` through the same deduplicated refresh path.
- Protected routes display a session-loading state before redirecting anonymous users to `/login?returnTo=...`; the return path sanitizer accepts only a same-origin absolute path and rejects scheme-relative/backslash/control/schemed redirects.
- Login/register/recovery forms render with `method="post"` before hydration, so a JavaScript startup failure cannot place credentials in the URL. Search forms deliberately use `GET`.
- Account navigation exposes current-session logout. Local auth/query state is cleared only after the backend confirms revocation; pending and error states remain visible.
- `/account/security` lists server-owned sessions and can revoke individual or all refresh sessions.

## Customer routes

- Discovery: `/`, `/search`, `/category/[slug]`, `/product/[slug]`, `/shop/[slug]`
- Commerce: `/cart`, `/checkout`, `/order/success`
- Identity: `/login`, `/register`, `/forgot-password`, `/reset-password`
- Account: `/account`, `/account/profile`, `/account/addresses`, `/account/orders`, `/account/orders/[id]`, `/account/wishlist`, `/account/reviews`, `/account/notifications`, `/account/messages`, `/account/security`

All catalog prices and seller data come from public APIs. Cart eligibility, checkout price, stock, shipping, voucher allocation, and order totals are recalculated by the backend. The client supplies an idempotency key only when placing checkout.

## UI behavior

The storefront follows `docs/DESIGN_SYSTEM.md`: original lowercase shoppew identity, compact Vietnamese commerce hierarchy, VND formatting, explicit Asia/Ho_Chi_Minh date rendering, mobile-first layout, keyboard focus, and 44-pixel controls. Loading, empty, error, validation, pending, disabled, success, unavailable-stock, and broken-media fallback states are implemented where applicable.

Responsive browser QA is required at 360, 768, and 1280 pixels. Production verification is:

```powershell
corepack pnpm --filter @shoppew/storefront lint
corepack pnpm --filter @shoppew/storefront typecheck
corepack pnpm --filter @shoppew/storefront test
corepack pnpm --filter @shoppew/storefront build
```
