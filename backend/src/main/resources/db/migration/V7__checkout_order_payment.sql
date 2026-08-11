ALTER TABLE checkout_groups
    ADD COLUMN request_hash VARCHAR(64);

UPDATE checkout_groups
SET request_hash = encode(digest(idempotency_key, 'sha256'), 'hex')
WHERE request_hash IS NULL;

ALTER TABLE checkout_groups
    ALTER COLUMN request_hash SET NOT NULL;

CREATE UNIQUE INDEX uq_inventory_reservations_order_variant
    ON inventory_reservations(order_id, variant_id)
    WHERE order_id IS NOT NULL;

INSERT INTO shipping_methods (provider, code, name, active)
VALUES ('MOCK', 'MOCK_STANDARD', 'Giao hàng tiêu chuẩn (mô phỏng)', true)
ON CONFLICT (provider, code) DO UPDATE
SET name = EXCLUDED.name,
    active = true,
    updated_at = now();
