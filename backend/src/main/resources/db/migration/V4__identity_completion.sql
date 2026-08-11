CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_type VARCHAR(32) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_auth_action_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_auth_action_tokens_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT ck_auth_action_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX idx_auth_action_tokens_active
    ON auth_action_tokens(user_id, token_type, expires_at)
    WHERE consumed_at IS NULL;

CREATE TABLE shop_settings (
    shop_id UUID PRIMARY KEY REFERENCES shops(id) ON DELETE CASCADE,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    order_auto_cancel_minutes INTEGER NOT NULL DEFAULT 1440 CHECK (order_auto_cancel_minutes BETWEEN 15 AND 10080),
    return_window_days INTEGER NOT NULL DEFAULT 7 CHECK (return_window_days BETWEEN 0 AND 90),
    chat_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vacation_mode BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
