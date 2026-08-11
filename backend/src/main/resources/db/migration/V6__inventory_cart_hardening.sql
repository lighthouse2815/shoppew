INSERT INTO inventories (
    variant_id,
    available_quantity,
    reserved_quantity,
    sold_quantity,
    low_stock_threshold,
    updated_at,
    version
)
SELECT id, 0, 0, 0, 5, now(), 0
FROM product_variants
ON CONFLICT (variant_id) DO NOTHING;

ALTER TABLE product_variants
    ADD CONSTRAINT uq_product_variants_identity UNIQUE (id, product_id, shop_id);

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_variant_product_shop
        FOREIGN KEY (variant_id, product_id, shop_id)
        REFERENCES product_variants(id, product_id, shop_id),
    ADD CONSTRAINT ck_cart_items_quantity_limit CHECK (quantity <= 999);

CREATE INDEX idx_inventories_low_stock
    ON inventories(available_quantity, low_stock_threshold);

CREATE INDEX idx_product_variants_shop_created
    ON product_variants(shop_id, created_at DESC);
