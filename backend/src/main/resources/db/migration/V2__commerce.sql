CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_carts_user UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES shops(id),
    product_id UUID NOT NULL REFERENCES products(id),
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_items_variant UNIQUE (cart_id, variant_id)
);
CREATE INDEX idx_cart_items_cart_shop ON cart_items(cart_id, shop_id, created_at);

CREATE TABLE checkout_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checkout_number VARCHAR(40) NOT NULL,
    user_id UUID NOT NULL REFERENCES app_users(id),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    items_subtotal NUMERIC(19,2) NOT NULL CHECK (items_subtotal >= 0),
    shipping_total NUMERIC(19,2) NOT NULL CHECK (shipping_total >= 0),
    discount_total NUMERIC(19,2) NOT NULL CHECK (discount_total >= 0),
    grand_total NUMERIC(19,2) NOT NULL CHECK (grand_total >= 0),
    status VARCHAR(24) NOT NULL DEFAULT 'CREATED',
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_checkout_groups_number UNIQUE (checkout_number),
    CONSTRAINT uq_checkout_groups_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_checkout_groups_status CHECK (status IN ('CREATED', 'PAYMENT_PENDING', 'CONFIRMED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_checkout_groups_total CHECK (grand_total = items_subtotal + shipping_total - discount_total)
);
CREATE INDEX idx_checkout_groups_user ON checkout_groups(user_id, created_at DESC);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(40) NOT NULL,
    checkout_group_id UUID NOT NULL REFERENCES checkout_groups(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    shop_id UUID NOT NULL REFERENCES shops(id),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    items_subtotal NUMERIC(19,2) NOT NULL CHECK (items_subtotal >= 0),
    shipping_total NUMERIC(19,2) NOT NULL CHECK (shipping_total >= 0),
    shop_discount_total NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (shop_discount_total >= 0),
    platform_discount_total NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (platform_discount_total >= 0),
    grand_total NUMERIC(19,2) NOT NULL CHECK (grand_total >= 0),
    customer_note VARCHAR(500),
    placed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    CONSTRAINT uq_orders_checkout_shop UNIQUE (checkout_group_id, shop_id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'PROCESSING', 'READY_TO_SHIP', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUND_REQUESTED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    CONSTRAINT ck_orders_total CHECK (grand_total = items_subtotal + shipping_total - shop_discount_total - platform_discount_total)
);
CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_shop ON orders(shop_id, status, created_at DESC);
CREATE INDEX idx_orders_checkout ON orders(checkout_group_id);

CREATE TABLE order_addresses (
    order_id UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    ward VARCHAR(120),
    address_line VARCHAR(255) NOT NULL,
    postal_code VARCHAR(24)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(255) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    image_url VARCHAR(1000),
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(19,2) NOT NULL CHECK (subtotal >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_items_subtotal CHECK (subtotal = unit_price * quantity)
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id) WHERE product_id IS NOT NULL;

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_id UUID REFERENCES app_users(id),
    actor_type VARCHAR(24) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_history_actor CHECK (actor_type IN ('SYSTEM', 'CUSTOMER', 'SELLER', 'ADMIN', 'PAYMENT_PROVIDER'))
);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, created_at);

ALTER TABLE inventory_reservations
    ADD CONSTRAINT fk_inventory_reservations_order FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checkout_group_id UUID NOT NULL REFERENCES checkout_groups(id),
    provider VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(160),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    idempotency_key VARCHAR(128) NOT NULL,
    failure_code VARCHAR(80),
    failure_message VARCHAR(500),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payments_checkout UNIQUE (checkout_group_id),
    CONSTRAINT uq_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_payments_provider_reference UNIQUE (provider, provider_reference),
    CONSTRAINT ck_payments_provider CHECK (provider IN ('COD', 'MOCK_ONLINE', 'VNPAY', 'MOMO', 'ZALOPAY', 'STRIPE')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED'))
);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(180) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ,
    processing_error VARCHAR(1000),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_events_provider_event UNIQUE (provider, provider_event_id)
);
CREATE INDEX idx_payment_events_payment ON payment_events(payment_id, received_at DESC);

CREATE TABLE shipping_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(40) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_shipping_methods_provider_code UNIQUE (provider, code)
);

CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    shipping_method_id UUID NOT NULL REFERENCES shipping_methods(id),
    provider_reference VARCHAR(160),
    tracking_number VARCHAR(160),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    fee NUMERIC(19,2) NOT NULL CHECK (fee >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    estimated_delivery_from DATE,
    estimated_delivery_to DATE,
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_shipments_order UNIQUE (order_id),
    CONSTRAINT uq_shipments_tracking UNIQUE (provider_reference, tracking_number),
    CONSTRAINT ck_shipments_status CHECK (status IN ('PENDING', 'READY', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNING', 'RETURNED'))
);

CREATE TABLE shipment_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(500),
    location VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_shipment_tracking_shipment ON shipment_tracking(shipment_id, occurred_at DESC);

CREATE TABLE vouchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(24) NOT NULL,
    shop_id UUID REFERENCES shops(id),
    code CITEXT NOT NULL,
    name VARCHAR(160) NOT NULL,
    voucher_type VARCHAR(24) NOT NULL,
    discount_type VARCHAR(24) NOT NULL,
    discount_value NUMERIC(19,2) NOT NULL CHECK (discount_value > 0),
    max_discount NUMERIC(19,2) CHECK (max_discount IS NULL OR max_discount > 0),
    minimum_spend NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (minimum_spend >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    total_quantity BIGINT NOT NULL CHECK (total_quantity >= 0),
    used_quantity BIGINT NOT NULL DEFAULT 0 CHECK (used_quantity >= 0),
    per_user_limit INTEGER NOT NULL DEFAULT 1 CHECK (per_user_limit > 0),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_vouchers_code UNIQUE (code),
    CONSTRAINT ck_vouchers_owner CHECK ((owner_type = 'PLATFORM' AND shop_id IS NULL) OR (owner_type = 'SHOP' AND shop_id IS NOT NULL)),
    CONSTRAINT ck_vouchers_type CHECK (voucher_type IN ('PLATFORM', 'SHOP', 'SHIPPING', 'PRODUCT', 'CATEGORY')),
    CONSTRAINT ck_vouchers_discount_type CHECK (discount_type IN ('FIXED', 'PERCENTAGE')),
    CONSTRAINT ck_vouchers_percentage CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT ck_vouchers_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_vouchers_quantity CHECK (used_quantity <= total_quantity),
    CONSTRAINT ck_vouchers_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'EXPIRED', 'ARCHIVED'))
);
CREATE INDEX idx_vouchers_active_window ON vouchers(status, starts_at, ends_at);
CREATE INDEX idx_vouchers_shop ON vouchers(shop_id, status) WHERE shop_id IS NOT NULL;

CREATE TABLE voucher_products (
    voucher_id UUID NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    PRIMARY KEY (voucher_id, product_id)
);

CREATE TABLE voucher_categories (
    voucher_id UUID NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (voucher_id, category_id)
);

CREATE TABLE voucher_payment_methods (
    voucher_id UUID NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    payment_provider VARCHAR(32) NOT NULL,
    PRIMARY KEY (voucher_id, payment_provider)
);

CREATE TABLE voucher_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id UUID NOT NULL REFERENCES vouchers(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    discount_amount NUMERIC(19,2) NOT NULL CHECK (discount_amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(24) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    consumed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    CONSTRAINT uq_voucher_usages_voucher_order UNIQUE (voucher_id, order_id),
    CONSTRAINT ck_voucher_usages_status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED'))
);
CREATE INDEX idx_voucher_usages_user ON voucher_usages(voucher_id, user_id, status);

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(24) NOT NULL,
    shop_id UUID REFERENCES shops(id),
    name VARCHAR(180) NOT NULL,
    promotion_type VARCHAR(32) NOT NULL,
    discount_type VARCHAR(24) NOT NULL,
    discount_value NUMERIC(19,2) NOT NULL CHECK (discount_value > 0),
    max_discount NUMERIC(19,2),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_promotions_owner CHECK ((owner_type = 'PLATFORM' AND shop_id IS NULL) OR (owner_type = 'SHOP' AND shop_id IS NOT NULL)),
    CONSTRAINT ck_promotions_type CHECK (promotion_type IN ('PRODUCT_DISCOUNT', 'SHOP_DISCOUNT', 'PLATFORM_CAMPAIGN', 'FLASH_SALE')),
    CONSTRAINT ck_promotions_discount_type CHECK (discount_type IN ('FIXED', 'PERCENTAGE')),
    CONSTRAINT ck_promotions_percentage CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT ck_promotions_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_promotions_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'ENDED', 'ARCHIVED'))
);
CREATE INDEX idx_promotions_active_window ON promotions(status, starts_at, ends_at);

CREATE TABLE promotion_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id UUID REFERENCES product_variants(id) ON DELETE CASCADE,
    promotional_price NUMERIC(19,2) CHECK (promotional_price IS NULL OR promotional_price >= 0),
    quantity_limit BIGINT CHECK (quantity_limit IS NULL OR quantity_limit >= 0),
    sold_quantity BIGINT NOT NULL DEFAULT 0 CHECK (sold_quantity >= 0),
    CONSTRAINT uq_promotion_products_scope UNIQUE NULLS NOT DISTINCT (promotion_id, product_id, variant_id)
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES app_users(id),
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INTEGER,
    response_body JSONB,
    resource_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_records_actor_operation_key UNIQUE NULLS NOT DISTINCT (actor_id, operation, idempotency_key)
);
CREATE INDEX idx_idempotency_records_expiry ON idempotency_records(expires_at);
