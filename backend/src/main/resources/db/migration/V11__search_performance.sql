CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_trgm
    ON products USING GIN (name gin_trgm_ops);

CREATE INDEX idx_product_variants_active_price
    ON product_variants(product_id, price)
    WHERE status = 'ACTIVE';
