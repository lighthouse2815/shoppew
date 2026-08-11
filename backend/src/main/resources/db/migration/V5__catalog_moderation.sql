ALTER TABLE products
    ADD COLUMN moderation_note VARCHAR(1000);

CREATE INDEX idx_products_pending_review
    ON products(created_at)
    WHERE status = 'PENDING_REVIEW';
