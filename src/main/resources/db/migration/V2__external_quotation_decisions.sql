ALTER TABLE approvals DROP CONSTRAINT ck_approvals_channel;
ALTER TABLE approvals ADD CONSTRAINT ck_approvals_channel CHECK (channel IN ('CUSTOMER_ACCESS_TOKEN', 'EXTERNAL_INTEGRATION'));

CREATE TABLE external_decision_receipts (
    event_id VARCHAR(150) PRIMARY KEY,
    payload_hash CHAR(64) NOT NULL,
    service_order_id UUID NOT NULL REFERENCES service_orders(id),
    quotation_version INTEGER NOT NULL,
    decision VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
