# shoppew inventory and cart correctness

## Source of truth

PostgreSQL is authoritative for `available_quantity`, `reserved_quantity`, and `sold_quantity`. Redis is not part of the correctness boundary. Every variant receives an inventory row when it is created; Flyway V6 backfills variants created before that rule existed.

The invariant is that all three counters stay non-negative. Inventory history is append-only in `inventory_transactions` and records before/after available and reserved quantities, a transaction type, actor, reference, note, and timestamp.

## Seller adjustments

Seller adjustments require an active shop membership and verify that the variant belongs to that shop. The service obtains a PostgreSQL pessimistic write lock on the inventory row before applying one of:

- `INCREASE`: adds available stock and records `STOCK_IN`.
- `DECREASE`: subtracts only available stock and records `STOCK_OUT`; reserved stock is untouched.
- `SET`: sets an exact available quantity and records `ADJUSTMENT`.

The low-stock threshold may be changed in the same command. Adjustments use decimal-safe product prices elsewhere but integer quantities here; supported cart quantities are 1–999.

## Checkout reservation strategy

Reservation uses one conditional PostgreSQL update:

```sql
UPDATE inventories
SET available_quantity = available_quantity - :quantity,
    reserved_quantity = reserved_quantity + :quantity
WHERE variant_id = :variantId
  AND available_quantity >= :quantity;
```

Exactly one row must be updated. Zero rows becomes `INSUFFICIENT_STOCK`. The counter update, reservation row, and `RESERVE` transaction are committed in one Spring transaction, so any later failure rolls everything back. This avoids a read-then-write race and remains correct across multiple backend instances.

Releasing a reservation conditionally moves reserved stock back to available stock and records `RELEASE`. Consuming a paid reservation moves reserved stock to sold stock and records `SALE`. Terminal reservation states make retries no-ops. A scheduled bounded scan locks up to 100 expired active reservations and releases them; the TTL is configured by `APP_INVENTORY_RESERVATION_TTL` and defaults to 15 minutes.

## Concurrency evidence

`ShoppewBackendApplicationTests.atomicInventoryReservationAllowsOnlyOneWinnerForStockOfOne` creates stock of one and starts 100 reservation attempts together through the transactional service. Current evidence is exactly one success, 99 `INSUFFICIENT_STOCK` results, zero available, one reserved, no sold stock, and one reservation/ledger entry. The same test expires that reservation and verifies one release restores stock without duplication.

## Cart revalidation

The cart stores only owned references, quantity, and selection state. Product prices are never accepted from a client. Each response reloads and groups items by seller, then recalculates:

- current variant unit price and line total;
- current inventory and stock status;
- active shop, product, and variant eligibility;
- selected item counts and eligible subtotals;
- stable issue codes such as `SHOP_INACTIVE`, `PRODUCT_UNAVAILABLE`, `OUT_OF_STOCK`, and `INSUFFICIENT_STOCK`.

Flyway V6 adds a composite foreign key proving that every cart item's variant, product, and shop belong together. Checkout will revalidate again and create reservations; viewing or editing a cart never holds stock.
