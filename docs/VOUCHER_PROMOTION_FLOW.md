# Voucher and promotion flow

## Promotion pricing

Promotions use explicit owner, type, time window, discount, and product/variant target records. Seller targets must belong to the exact owned shop; platform campaigns are admin-only. The active types are `PRODUCT_DISCOUNT`, `SHOP_DISCOUNT`, `PLATFORM_CAMPAIGN`, and `FLASH_SALE` without a rules DSL.

For each variant, the server selects the lowest eligible single promotion price. A target may declare an exact promotional price or derive one from a fixed/percentage discount and optional cap. The response retains the original price and promotion metadata. Checkout repeats this decision, reserves any quantity-limited target with one conditional database update, and snapshots the effective unit price into the order item.

`promotion_usages` tracks `RESERVED`, `CONSUMED`, and `RELEASED`. COD consumes at placement. Online payment success consumes after the signed callback; failure or eligible cancellation releases both usage and target capacity.

## Voucher evaluation

Voucher ownership is `SHOP` or `PLATFORM`; supported types are `PLATFORM`, `SHOP`, `SHIPPING`, `PRODUCT`, and `CATEGORY`. Every evaluation checks:

- normalized unique code and active time window;
- remaining global quantity and per-user applications;
- owner shop and product/category target scope;
- payment-provider restriction when present;
- checkout currency and minimum spend;
- fixed/percentage discount and optional maximum discount.

Platform vouchers are allocated proportionally across eligible seller orders. Shop-owned discounts remain on their shop order. Aggregate discounts are capped so an order cannot become negative.

## Concurrency and retry

Preview is informational. Placement sorts vouchers, detaches preview snapshots, then obtains a fresh `PESSIMISTIC_WRITE` lock on each voucher row. Under that lock it rechecks rule freshness, availability, and the per-user count before incrementing usage. This serializes concurrent attempts without trusting a stale ORM version.

One voucher application may create multiple per-order usage rows but increments `used_quantity` once per checkout. Identical checkout idempotency replay creates no additional usage. An online failure or full release marks usage rows `RELEASED` and decrements the application counter only after no active allocation remains.
