CREATE TABLE wishlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wishlists_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX idx_wishlists_user ON wishlists(user_id, created_at DESC);

CREATE TABLE recently_viewed_products (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    view_count BIGINT NOT NULL DEFAULT 1 CHECK (view_count > 0),
    PRIMARY KEY (user_id, product_id)
);
CREATE INDEX idx_recently_viewed_user ON recently_viewed_products(user_id, viewed_at DESC);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    shop_id UUID NOT NULL REFERENCES shops(id),
    product_id UUID NOT NULL REFERENCES products(id),
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED',
    seller_reply TEXT,
    seller_replied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reviews_order_item UNIQUE (order_item_id),
    CONSTRAINT ck_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED'))
);
CREATE INDEX idx_reviews_product ON reviews(product_id, status, created_at DESC);
CREATE INDEX idx_reviews_shop ON reviews(shop_id, status, created_at DESC);
CREATE INDEX idx_reviews_user ON reviews(user_id, created_at DESC);

CREATE TABLE review_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    object_key VARCHAR(700) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_review_images_object_key UNIQUE (object_key)
);
CREATE INDEX idx_review_images_review ON review_images(review_id, sort_order);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    notification_type VARCHAR(24) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_type CHECK (notification_type IN ('ORDER', 'PAYMENT', 'PROMOTION', 'SYSTEM', 'CHAT'))
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, created_at DESC) WHERE read_at IS NULL;
CREATE INDEX idx_notifications_user_all ON notifications(user_id, created_at DESC);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(180),
    attempted_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_deliveries_channel UNIQUE (notification_id, channel),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    CONSTRAINT ck_notification_deliveries_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'SKIPPED'))
);

CREATE TABLE push_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    platform VARCHAR(24) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    encrypted_token TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_push_devices_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_push_devices_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);
CREATE INDEX idx_push_devices_user_active ON push_devices(user_id, active);

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id),
    customer_id UUID NOT NULL REFERENCES app_users(id),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_conversations_shop_customer UNIQUE (shop_id, customer_id),
    CONSTRAINT ck_conversations_status CHECK (status IN ('ACTIVE', 'CLOSED', 'BLOCKED'))
);
CREATE INDEX idx_conversations_customer ON conversations(customer_id, last_message_at DESC);
CREATE INDEX idx_conversations_shop ON conversations(shop_id, last_message_at DESC);

CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    participant_type VARCHAR(24) NOT NULL,
    last_read_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at TIMESTAMPTZ,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT ck_conversation_participants_type CHECK (participant_type IN ('CUSTOMER', 'SHOP_MEMBER'))
);
CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id, conversation_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES app_users(id),
    message_type VARCHAR(24) NOT NULL,
    text_content TEXT,
    media_url VARCHAR(1000),
    product_id UUID REFERENCES products(id),
    order_id UUID REFERENCES orders(id),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_messages_type CHECK (message_type IN ('TEXT', 'IMAGE', 'PRODUCT', 'ORDER')),
    CONSTRAINT ck_messages_content CHECK (
        (message_type = 'TEXT' AND text_content IS NOT NULL) OR
        (message_type = 'IMAGE' AND media_url IS NOT NULL) OR
        (message_type = 'PRODUCT' AND product_id IS NOT NULL) OR
        (message_type = 'ORDER' AND order_id IS NOT NULL)
    )
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, sent_at DESC);

CREATE TABLE refund_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_number VARCHAR(40) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    shop_id UUID NOT NULL REFERENCES shops(id),
    reason VARCHAR(32) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    requested_amount NUMERIC(19,2) NOT NULL CHECK (requested_amount >= 0),
    approved_amount NUMERIC(19,2) CHECK (approved_amount IS NULL OR approved_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    reviewed_by UUID REFERENCES app_users(id),
    review_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_refund_requests_number UNIQUE (request_number),
    CONSTRAINT ck_refund_requests_reason CHECK (reason IN ('DAMAGED', 'WRONG_ITEM', 'MISSING_ITEM', 'NOT_AS_DESCRIBED', 'OTHER')),
    CONSTRAINT ck_refund_requests_status CHECK (status IN ('REQUESTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'REFUNDING', 'REFUNDED', 'CANCELLED'))
);
CREATE INDEX idx_refund_requests_user ON refund_requests(user_id, created_at DESC);
CREATE INDEX idx_refund_requests_shop ON refund_requests(shop_id, status, created_at DESC);

CREATE TABLE refund_request_items (
    refund_request_id UUID NOT NULL REFERENCES refund_requests(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    requested_amount NUMERIC(19,2) NOT NULL CHECK (requested_amount >= 0),
    PRIMARY KEY (refund_request_id, order_item_id)
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_request_id UUID NOT NULL REFERENCES refund_requests(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    provider_reference VARCHAR(180),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(128) NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refunds_request UNIQUE (refund_request_id),
    CONSTRAINT uq_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_refunds_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED'))
);

CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_number VARCHAR(40) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    refund_request_id UUID REFERENCES refund_requests(id),
    opened_by UUID NOT NULL REFERENCES app_users(id),
    reason VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID REFERENCES app_users(id),
    resolution VARCHAR(1000),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_disputes_number UNIQUE (dispute_number),
    CONSTRAINT ck_disputes_status CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'AWAITING_CUSTOMER', 'AWAITING_SELLER', 'RESOLVED', 'CLOSED'))
);
CREATE INDEX idx_disputes_status ON disputes(status, created_at DESC);

CREATE TABLE dispute_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_users(id),
    content TEXT NOT NULL,
    attachments JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_dispute_messages_dispute ON dispute_messages(dispute_id, created_at);

CREATE TABLE seller_balances (
    shop_id UUID PRIMARY KEY REFERENCES shops(id),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    pending_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (pending_amount >= 0),
    available_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (available_amount >= 0),
    held_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (held_amount >= 0),
    paid_out_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (paid_out_amount >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE seller_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id),
    transaction_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount <> 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    balance_bucket VARCHAR(24) NOT NULL,
    order_id UUID REFERENCES orders(id),
    refund_id UUID REFERENCES refunds(id),
    payout_id UUID,
    reference_key VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_seller_transactions_reference UNIQUE (shop_id, transaction_type, reference_key),
    CONSTRAINT ck_seller_transactions_type CHECK (transaction_type IN ('SALE_PENDING', 'SALE_AVAILABLE', 'PLATFORM_FEE', 'DISCOUNT_ALLOCATION', 'REFUND', 'ADJUSTMENT', 'PAYOUT')),
    CONSTRAINT ck_seller_transactions_bucket CHECK (balance_bucket IN ('PENDING', 'AVAILABLE', 'HELD', 'PAID_OUT'))
);
CREATE INDEX idx_seller_transactions_shop ON seller_transactions(shop_id, created_at DESC);
CREATE INDEX idx_seller_transactions_order ON seller_transactions(order_id) WHERE order_id IS NOT NULL;

CREATE TABLE payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_number VARCHAR(40) NOT NULL,
    shop_id UUID NOT NULL REFERENCES shops(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    destination_snapshot JSONB NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    provider_reference VARCHAR(180),
    failure_message VARCHAR(500),
    CONSTRAINT uq_payouts_number UNIQUE (payout_number),
    CONSTRAINT ck_payouts_status CHECK (status IN ('REQUESTED', 'APPROVED', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED'))
);
CREATE INDEX idx_payouts_shop ON payouts(shop_id, requested_at DESC);

ALTER TABLE seller_transactions
    ADD CONSTRAINT fk_seller_transactions_payout FOREIGN KEY (payout_id) REFERENCES payouts(id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES app_users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id UUID,
    before_state JSONB,
    after_state JSONB,
    ip_address INET,
    user_agent VARCHAR(1000),
    request_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at DESC);

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_name VARCHAR(80) NOT NULL,
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    shop_id UUID REFERENCES shops(id) ON DELETE SET NULL,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    session_key VARCHAR(128),
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_events_name_time ON analytics_events(event_name, occurred_at DESC);
CREATE INDEX idx_analytics_events_shop_time ON analytics_events(shop_id, occurred_at DESC) WHERE shop_id IS NOT NULL;
