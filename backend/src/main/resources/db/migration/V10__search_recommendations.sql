DROP INDEX IF EXISTS idx_products_search;

CREATE INDEX idx_products_search ON products USING GIN (
    to_tsvector(
        'simple',
        coalesce(name, '') || ' ' || coalesce(short_description, '') || ' ' || coalesce(description, '')
    )
);

CREATE TABLE product_views (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    view_count BIGINT NOT NULL DEFAULT 1 CHECK (view_count > 0),
    first_viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

CREATE INDEX idx_product_views_user_recent
    ON product_views(user_id, last_viewed_at DESC);

CREATE INDEX idx_product_views_product_popularity
    ON product_views(product_id, view_count DESC);
