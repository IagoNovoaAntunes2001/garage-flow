CREATE TABLE status_notifications (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    history_id UUID NOT NULL UNIQUE REFERENCES service_order_status_history(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    recipient VARCHAR(254),
    state VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_id UUID,
    lease_until TIMESTAMPTZ,
    last_error VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    CONSTRAINT ck_status_notification_state CHECK (state IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'))
);

CREATE INDEX ix_status_notifications_pending ON status_notifications(state, next_attempt_at);
