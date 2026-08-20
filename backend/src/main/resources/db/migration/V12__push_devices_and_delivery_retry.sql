ALTER TABLE push_devices RENAME COLUMN token_hash TO target_hash;
ALTER TABLE push_devices RENAME COLUMN encrypted_token TO encrypted_target;
ALTER TABLE push_devices ADD COLUMN target_type VARCHAR(24) NOT NULL DEFAULT 'TOKEN';
ALTER TABLE push_devices ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE push_devices ADD CONSTRAINT ck_push_devices_target_type
    CHECK (target_type IN ('FID', 'TOKEN'));
ALTER TABLE push_devices ALTER COLUMN target_type SET DEFAULT 'FID';

ALTER TABLE notification_deliveries ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE notification_deliveries ADD COLUMN next_attempt_at TIMESTAMPTZ;
ALTER TABLE notification_deliveries ADD CONSTRAINT ck_notification_deliveries_attempt_count
    CHECK (attempt_count >= 0);
CREATE INDEX idx_notification_deliveries_retry
    ON notification_deliveries(status, next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED', 'SENT');
