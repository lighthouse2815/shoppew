# Payment flow

## Provider abstraction

Checkout selects a registered provider rather than branching on client-calculated behavior. The current local providers are:

| Provider | Checkout result | Inventory effect |
| --- | --- | --- |
| `COD` | checkout/order confirmed, payment remains pending with `PAY_ON_DELIVERY` action | reservation is consumed into sold stock immediately |
| `MOCK_ONLINE` | checkout/payment pending and orders `PENDING_PAYMENT` | stock remains reserved until a provider event |

The mock-online initiation returns a provider reference. Local tests can complete it through `POST /api/v1/payments/mock/webhook` with the configured `X-Shoppew-Mock-Signature` shared secret. The secret belongs in runtime configuration and must never be exposed to browser or Android clients.

## Callback processing

1. Compare the callback secret in constant time.
2. Lock the payment by provider reference and require the expected provider.
3. Hash the normalized event ID, provider reference, and result.
4. Return the prior result for an identical provider/event ID and payload.
5. Reject the same event ID with a different payload.
6. Apply the payment, checkout, order, and inventory effects in one transaction.

Success changes payment to `SUCCEEDED`, checkout to `CONFIRMED`, each pending order to `PAID`, and consumes each reservation exactly once. Failure changes payment to `FAILED`, checkout to `FAILED`, cancels pending orders, and releases reservations exactly once.

## Production replacement boundary

`MOCK_ONLINE` is deliberately local. A real provider adapter must keep the same server-owned amount/currency, provider-reference lookup, signature verification, event deduplication, transactional state transition, and replay guarantees. Browser redirect state is never sufficient proof of payment.
