CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY NOT NULL,
    response TEXT,
    status_code INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    marked_for_deletion_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_id ON idempotency_keys(id);
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at ON idempotency_keys(created_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_marked_date ON idempotency_keys(marked_for_deletion_date);
