CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email CITEXT NOT NULL,
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_users_email UNIQUE (email),
    CONSTRAINT uq_app_users_phone UNIQUE (phone),
    CONSTRAINT ck_app_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BANNED'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('CUSTOMER', 'SELLER', 'SHOP_STAFF', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN'))
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(1000),
    date_of_birth DATE,
    gender VARCHAR(24),
    locale VARCHAR(16) NOT NULL DEFAULT 'vi-VN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_profiles_gender CHECK (gender IS NULL OR gender IN ('FEMALE', 'MALE', 'NON_BINARY', 'UNDISCLOSED'))
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_family_id UUID NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    device_name VARCHAR(160),
    ip_address INET,
    user_agent VARCHAR(1000),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(120),
    rotated_from_session_id UUID REFERENCES user_sessions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_sessions_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at)
);
CREATE INDEX idx_user_sessions_active ON user_sessions(user_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_user_sessions_family ON user_sessions(token_family_id);

CREATE TABLE user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    label VARCHAR(80),
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    ward VARCHAR(120),
    address_line VARCHAR(255) NOT NULL,
    postal_code VARCHAR(24),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_user_addresses_default ON user_addresses(user_id) WHERE is_default;
CREATE INDEX idx_user_addresses_user ON user_addresses(user_id, created_at DESC);

CREATE TABLE shops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_users(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    logo_url VARCHAR(1000),
    banner_url VARCHAR(1000),
    rating_average NUMERIC(3,2) NOT NULL DEFAULT 0 CHECK (rating_average BETWEEN 0 AND 5),
    review_count BIGINT NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_shops_slug UNIQUE (slug),
    CONSTRAINT ck_shops_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'BANNED'))
);
CREATE INDEX idx_shops_owner ON shops(owner_id);
CREATE INDEX idx_shops_status ON shops(status, created_at DESC);

CREATE TABLE shop_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    member_role VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_shop_members_user UNIQUE (shop_id, user_id),
    CONSTRAINT ck_shop_members_role CHECK (member_role IN ('OWNER', 'MANAGER', 'PRODUCT', 'ORDER', 'FINANCE', 'SUPPORT')),
    CONSTRAINT ck_shop_members_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED'))
);
CREATE INDEX idx_shop_members_user ON shop_members(user_id, status);

CREATE TABLE shop_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    address_type VARCHAR(24) NOT NULL,
    contact_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    ward VARCHAR(120),
    address_line VARCHAR(255) NOT NULL,
    postal_code VARCHAR(24),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_shop_addresses_type CHECK (address_type IN ('BUSINESS', 'PICKUP', 'RETURN'))
);
CREATE UNIQUE INDEX uq_shop_addresses_default_type ON shop_addresses(shop_id, address_type) WHERE is_default;

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID REFERENCES categories(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    image_url VARCHAR(1000),
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX idx_categories_parent ON categories(parent_id, sort_order, name);

CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    logo_url VARCHAR(1000),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_brands_name UNIQUE (name),
    CONSTRAINT uq_brands_slug UNIQUE (slug),
    CONSTRAINT ck_brands_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    brand_id UUID REFERENCES brands(id),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    short_description VARCHAR(500),
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    rating_average NUMERIC(3,2) NOT NULL DEFAULT 0 CHECK (rating_average BETWEEN 0 AND 5),
    review_count BIGINT NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    sold_count BIGINT NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT uq_products_id_shop UNIQUE (id, shop_id),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'REJECTED', 'HIDDEN', 'ARCHIVED'))
);
CREATE INDEX idx_products_shop_status ON products(shop_id, status, created_at DESC);
CREATE INDEX idx_products_category_status ON products(category_id, status, created_at DESC);
CREATE INDEX idx_products_brand_status ON products(brand_id, status) WHERE brand_id IS NOT NULL;
CREATE INDEX idx_products_search ON products USING GIN (to_tsvector('simple', name || ' ' || coalesce(short_description, '')));

CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    object_key VARCHAR(700) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_images_object_key UNIQUE (object_key)
);
CREATE UNIQUE INDEX uq_product_images_primary ON product_images(product_id) WHERE is_primary;
CREATE INDEX idx_product_images_sort ON product_images(product_id, sort_order);

CREATE TABLE product_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_options_name UNIQUE (product_id, name)
);

CREATE TABLE product_option_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id UUID NOT NULL REFERENCES product_options(id) ON DELETE CASCADE,
    value VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_option_values_value UNIQUE (option_id, value)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    shop_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(19,2) NOT NULL CHECK (price >= 0),
    compare_at_price NUMERIC(19,2) CHECK (compare_at_price IS NULL OR compare_at_price >= price),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    weight_grams INTEGER CHECK (weight_grams IS NULL OR weight_grams > 0),
    length_mm INTEGER CHECK (length_mm IS NULL OR length_mm > 0),
    width_mm INTEGER CHECK (width_mm IS NULL OR width_mm > 0),
    height_mm INTEGER CHECK (height_mm IS NULL OR height_mm > 0),
    image_url VARCHAR(1000),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_variants_product_shop FOREIGN KEY (product_id, shop_id) REFERENCES products(id, shop_id) ON DELETE CASCADE,
    CONSTRAINT uq_product_variants_shop_sku UNIQUE (shop_id, sku),
    CONSTRAINT uq_product_variants_id_product UNIQUE (id, product_id),
    CONSTRAINT ck_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);
CREATE INDEX idx_product_variants_product ON product_variants(product_id, status);

CREATE TABLE product_variant_option_values (
    variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    option_value_id UUID NOT NULL REFERENCES product_option_values(id) ON DELETE CASCADE,
    PRIMARY KEY (variant_id, option_value_id)
);

CREATE TABLE product_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES categories(id),
    name VARCHAR(120) NOT NULL,
    value_type VARCHAR(24) NOT NULL DEFAULT 'TEXT',
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_attributes_category_name UNIQUE NULLS NOT DISTINCT (category_id, name),
    CONSTRAINT ck_product_attributes_type CHECK (value_type IN ('TEXT', 'NUMBER', 'BOOLEAN', 'SELECT'))
);

CREATE TABLE product_attribute_values (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_id UUID NOT NULL REFERENCES product_attributes(id),
    value_text VARCHAR(1000) NOT NULL,
    PRIMARY KEY (product_id, attribute_id)
);

CREATE TABLE inventories (
    variant_id UUID PRIMARY KEY REFERENCES product_variants(id) ON DELETE CASCADE,
    available_quantity BIGINT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity BIGINT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    sold_quantity BIGINT NOT NULL DEFAULT 0 CHECK (sold_quantity >= 0),
    low_stock_threshold BIGINT NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    transaction_type VARCHAR(24) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    available_before BIGINT NOT NULL CHECK (available_before >= 0),
    available_after BIGINT NOT NULL CHECK (available_after >= 0),
    reserved_before BIGINT NOT NULL CHECK (reserved_before >= 0),
    reserved_after BIGINT NOT NULL CHECK (reserved_after >= 0),
    reference_type VARCHAR(40),
    reference_id UUID,
    note VARCHAR(500),
    actor_id UUID REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventory_transactions_type CHECK (transaction_type IN ('STOCK_IN', 'STOCK_OUT', 'RESERVE', 'RELEASE', 'SALE', 'RETURN', 'ADJUSTMENT'))
);
CREATE INDEX idx_inventory_transactions_variant ON inventory_transactions(variant_id, created_at DESC);
CREATE INDEX idx_inventory_transactions_reference ON inventory_transactions(reference_type, reference_id);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    order_id UUID,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventory_reservations_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_inventory_reservations_expiry CHECK (expires_at > created_at)
);
CREATE INDEX idx_inventory_reservations_expiry ON inventory_reservations(status, expires_at);
CREATE INDEX idx_inventory_reservations_variant ON inventory_reservations(variant_id, status);
