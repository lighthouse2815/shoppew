# shoppew repository instructions

## Non-negotiable product scope

- Build a working multi-vendor marketplace, not a mockup or disconnected scaffold.
- Complete customer, seller, and admin web flows; Spring Boot backend; PostgreSQL, Redis, object storage; and the native Kotlin/Compose Android client as integrated vertical slices.
- Vietnamese is the demonstrated locale, VND is the default currency, and Asia/Ho_Chi_Minh is the business timezone. Store timestamps consistently and render local business time intentionally.
- Use `shoppew` consistently. Do not copy Shopee or another marketplace's protected identity, assets, layout, copy, or trade dress.

## UI and design skill policy

Do not use the user's current global UI/interface design skill for shoppew. Use the shoppew repo-scoped UI/design workflow instead.

- For any storefront, Seller Center, Admin, or Android interface task, read and follow `.agents/skills/shoppew-ui/SKILL.md` and `docs/DESIGN_SYSTEM.md`.
- Do not delete, modify, disable, or otherwise affect the user's global skills.
- Keep all shoppew-specific design decisions and reusable rules inside this repository.
- Every important screen needs real loading, empty, error, validation, disabled, pending, and success behavior where applicable.

## Architecture

- Keep the backend a modular monolith under `com.shoppew`, packaged by business feature rather than project-wide controller/service/repository buckets.
- Expose versioned APIs under `/api/v1`; never return JPA entities directly.
- Treat the backend and its OpenAPI document as the source of truth for web and Android contracts.
- Use PostgreSQL as the source of truth for commerce data. Redis is optional acceleration and coordination, never the sole authority for stock, money, orders, or voucher usage.
- Use Flyway for schema changes. Production uses schema validation, never automatic create/update.
- Keep external systems behind provider interfaces and supply explicit local implementations so missing credentials do not block development.
- Do not introduce microservices, Kafka, Kubernetes, Redux, or other infrastructure without a demonstrated need.

## Commerce and security invariants

- Calculate money with decimal-safe types and explicit currency; never float/double.
- Recalculate product price, availability, shipping, vouchers, and totals on the server during checkout.
- Enforce customer, shop, seller, staff, moderator, and admin ownership on the backend. Never trust a client-provided owner or role.
- Reserve inventory transactionally and prove that concurrent purchases cannot create negative stock.
- Persist immutable order-item snapshots.
- Express order changes as authorized state-machine commands, not arbitrary status patches.
- Make checkout/payment/refund callbacks idempotent where retries can occur.
- Store images in S3-compatible object storage, validate uploads, and keep credentials and tokens out of logs.
- Use short-lived access credentials and revocable, rotated refresh sessions. Protect browser and Android credential storage appropriately.
- Record critical administrative actions in an audit trail.

## Implementation and verification

- Trace the real controller/service/repository/client path before changing behavior.
- Preserve unrelated user changes in a dirty worktree.
- Implement one usable vertical slice at a time and run the narrowest relevant checks immediately.
- A feature is `IMPLEMENTED` when integrated code exists and `VERIFIED` only when the relevant build, automated tests, and runtime behavior have current evidence.
- Keep `docs/IMPLEMENTATION_STATUS.md` accurate. Never turn unchecked requirements into claims.
- Review every TODO, FIXME, HACK, placeholder, dummy, and mock-data occurrence before release. Local provider implementations are allowed only when clearly named and documented.
- Prefer PowerShell-ready scripts on Windows and deterministic, non-interactive commands in CI.

## Repository navigation

- If a `.codegraph/` directory exists, use CodeGraph before grep/find or broad file reading to locate symbols and call paths. Do not create or refresh an index unless the user asks.
- Otherwise use `rg` and `rg --files` for text and file discovery.
