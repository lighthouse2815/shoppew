# Search, recommendations, and chat

This document records the implemented Phase 14 contracts. PostgreSQL remains the local source of truth; neither OpenSearch nor a machine-learning service is required for a working development environment.

## Search

`SearchService` is the application boundary and `PostgresSearchService` is the active implementation. `GET /api/v1/public/products` accepts:

| Parameter | Contract |
| --- | --- |
| `q` | Optional keyword, up to 200 characters |
| `shopId`, `categoryId`, `brandId` | Optional exact UUID filters |
| `minPrice`, `maxPrice` | Optional non-negative decimal bounds |
| `minRating` | Optional decimal from 0 through 5 |
| `sort` | `RELEVANCE`, `NEWEST`, `PRICE_ASC`, `PRICE_DESC`, `BEST_SELLING`, or `RATING` |
| `page`, `size` | Zero-based page and 1–100 rows |

Search only returns active products with an active variant and active shop. V10 installs a full-text GIN index over product name, short description, and description. V11 adds `pg_trgm`, a trigram GIN product-name index for the partial-name `ILIKE` search/suggestion path, and a partial active-variant `(product_id, price)` index. Relevance uses PostgreSQL full-text rank with sold count/rating tie-breakers; the partial-name fallback keeps incomplete names discoverable. `GET /api/v1/public/search/suggestions?q=...&size=...` returns up to 10 distinct active product names and requires at least two query characters.

## Recommendations

`RecommendationService` keeps the strategy replaceable without coupling the clients to an implementation:

| Route | Strategy |
| --- | --- |
| `GET /api/v1/public/recommendations/popular` | Current best-selling order |
| `GET /api/v1/public/recommendations/trending` | Non-cancelled/non-refunded order quantity from the last seven days, then sold count and rating |
| `GET /api/v1/public/recommendations/products/{productId}/related` | Same category first, then same shop and trending fallbacks with duplicates removed |
| `GET /api/v1/public/recommendations/shops/{shopId}` | Newest active products from one active shop, with optional exclusion |
| `POST /api/v1/recommendations/recently-viewed/{productId}` | Upsert the authenticated user's durable view count/timestamps |
| `GET /api/v1/recommendations/recently-viewed` | Most recently viewed active products for the authenticated user |

V10 stores one `product_views` row per user/product and indexes user recency and aggregate product popularity. The Android Room recently-viewed list is a local browsing aid and does not replace this authenticated server record.

## Customer and seller chat

Customer routes live under `/api/v1/chat/conversations`; seller routes live under `/api/v1/seller/shops/{shopId}/chat/conversations`. The customer starts one durable conversation per customer/shop pair. Both clients can page their authorized conversations and messages and append `TEXT`, `IMAGE`, `PRODUCT`, or `ORDER` messages.

Authorization is enforced in the service and repository path:

- a customer can read/send only when the conversation's `customer_id` matches the authenticated user;
- a seller must be an active member of the exact path shop and the conversation must belong to that same shop;
- product messages must reference an active product in the conversation shop;
- order messages must reference an order belonging to both the conversation customer and shop;
- disabled shop chat and inactive conversations reject writes.

Every message is persisted before a `CHAT` in-app notification and realtime event are produced. STOMP connects at `/api/v1/ws`, requires a bearer token on `CONNECT`, and permits subscriptions only to `/user/queue/chat`. The simple broker is suitable for the current single-backend local environment; scaling it across replicas would require a shared broker or another explicit transport design.

## Current verification

- `Phase14IntegrationTests` passes 2/2 with PostgreSQL/Testcontainers. It covers the combined keyword/category/brand/price/rating/price-sort query and invalid range, suggestions, popular/trending/related/recently-viewed recommendations, OpenAPI paths, durable messages, customer isolation, seller membership/shop isolation, STOMP broker bootstrap, and `CHAT` notification persistence.
- Browser QA verified real Storefront suggestions and a search narrowed by brand, minimum rating, and rating sort.
- Browser QA created conversation `bd4df27d-66d9-4600-a56f-7ad2b2600a52`, sent a buyer message from Storefront, read and replied from Seller Center, then confirmed the reply and history after returning to Storefront. All reads and writes used the live backend; no mock conversation data was used.
- The generated TypeScript API contract completed successfully after the backend contract changes.
