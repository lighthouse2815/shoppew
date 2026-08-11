ALTER TABLE voucher_usages
    ADD COLUMN checkout_group_id UUID REFERENCES checkout_groups(id);

UPDATE voucher_usages usage
SET checkout_group_id = orders.checkout_group_id
FROM orders
WHERE orders.id = usage.order_id;

ALTER TABLE voucher_usages
    ALTER COLUMN checkout_group_id SET NOT NULL;

CREATE INDEX idx_voucher_usages_checkout
    ON voucher_usages(checkout_group_id, status);

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_product_id UUID NOT NULL REFERENCES promotion_products(id),
    checkout_group_id UUID NOT NULL REFERENCES checkout_groups(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    status VARCHAR(24) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    consumed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    CONSTRAINT uq_promotion_usages_item_scope UNIQUE (promotion_product_id, order_item_id),
    CONSTRAINT ck_promotion_usages_status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED'))
);

CREATE INDEX idx_promotion_usages_checkout
    ON promotion_usages(checkout_group_id, status);
CREATE INDEX idx_promotion_usages_order
    ON promotion_usages(order_id, status);
