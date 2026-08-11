ALTER TABLE refund_requests
    ADD COLUMN previous_order_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    ADD CONSTRAINT ck_refund_requests_previous_order_status
        CHECK (previous_order_status IN ('COMPLETED', 'PARTIALLY_REFUNDED'));

ALTER TABLE refund_request_items
    ADD COLUMN seller_charge_amount NUMERIC(19,2) NOT NULL DEFAULT 0
        CHECK (seller_charge_amount >= 0);

ALTER TABLE refunds
    ADD COLUMN seller_charge_amount NUMERIC(19,2) NOT NULL DEFAULT 0
        CHECK (seller_charge_amount >= 0);

ALTER TABLE payouts
    ADD COLUMN idempotency_key VARCHAR(128);

UPDATE payouts SET idempotency_key = id::text WHERE idempotency_key IS NULL;

ALTER TABLE payouts ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE payouts ADD CONSTRAINT uq_payouts_idempotency UNIQUE (idempotency_key);

CREATE INDEX idx_refund_request_items_order_item
    ON refund_request_items(order_item_id, refund_request_id);
CREATE INDEX idx_refunds_payment_status
    ON refunds(payment_id, status, created_at DESC);
CREATE INDEX idx_audit_logs_created
    ON audit_logs(created_at DESC);
