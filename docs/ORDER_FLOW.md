# Order flow

## Aggregate boundary

A checkout group belongs to one customer and represents a single payment decision. Checkout creates one order per shop so sellers can fulfill independently. Each order stores immutable product, variant, SKU, option, unit-price, address, customer-note, and monetary snapshots; later catalog/profile edits cannot rewrite purchase history.

Inventory is reserved transactionally while checkout is placed. COD consumes the reservation immediately because the order is confirmed for fulfillment. `MOCK_ONLINE` keeps the reservation while payment is pending and consumes or releases it exactly once when the provider event succeeds or fails.

## Explicit transitions

There is no generic status-patch endpoint. Every command locks the order, validates the current state and actor scope, writes `order_status_history`, and publishes an `OrderStatusChangedEvent`.

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: MOCK_ONLINE checkout
    PENDING_PAYMENT --> PAID: signed payment success
    PENDING_PAYMENT --> CANCELLED: payment failure or customer cancels group
    PAID --> CONFIRMED: seller confirms
    [*] --> CONFIRMED: COD checkout
    CONFIRMED --> PROCESSING: seller processes
    CONFIRMED --> CANCELLED: customer or seller cancels COD
    PROCESSING --> READY_TO_SHIP: seller packs
    PROCESSING --> CANCELLED: seller cancels COD
    READY_TO_SHIP --> SHIPPED: seller hands off
    SHIPPED --> DELIVERED: delivery completes
    DELIVERED --> COMPLETED: customer confirms receipt
    COMPLETED --> REFUND_REQUESTED: customer requests eligible items
    PARTIALLY_REFUNDED --> REFUND_REQUESTED: customer requests remaining items
    REFUND_REQUESTED --> COMPLETED: request rejected/cancelled
    REFUND_REQUESTED --> PARTIALLY_REFUNDED: partial merchandise refund succeeds
    REFUND_REQUESTED --> REFUNDED: all refundable merchandise succeeds
```

Refund states are reachable only through the authorized refund workflow. Rejection or customer cancellation restores the exact previous refundable order state; processing uses a separate idempotency key and writes order history/domain events like every fulfillment command.

## Shipping

Shipping is provider-based. `MOCK_STANDARD` currently produces deterministic server-side quotes and estimated dates. Checkout stores the selected method and quote in a shipment. Ready, ship, and deliver commands update shipment state and append tracking records; the ship command may attach a tracking number and location.

## Authorization and retry rules

- Customers can read only their own orders and payments.
- Sellers must be active members of the order's exact shop; cross-shop IDs return the same not-found boundary.
- The checkout key is scoped to the customer and bound to a normalized request hash.
- Retrying an identical checkout returns the original group and orders without reserving stock twice.
- Cancelling a pending online checkout applies to every order in that checkout group so payment and reservations cannot split-brain.
