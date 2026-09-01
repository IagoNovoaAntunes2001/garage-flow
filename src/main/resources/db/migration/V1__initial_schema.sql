CREATE TABLE customers (
    id UUID PRIMARY KEY,
    document_type VARCHAR(4) NOT NULL,
    document_value VARCHAR(14) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_customers_document_type CHECK (document_type IN ('CPF', 'CNPJ')),
    CONSTRAINT ck_customers_document_length CHECK (
        (document_type = 'CPF' AND char_length(document_value) = 11)
        OR (document_type = 'CNPJ' AND char_length(document_value) = 14)
    ),
    CONSTRAINT uq_customers_document UNIQUE (document_value)
);

CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    license_plate VARCHAR(7) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(100) NOT NULL,
    production_year INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_vehicles_license_plate UNIQUE (license_plate),
    CONSTRAINT ck_vehicles_year CHECK (production_year >= 1886)
);

CREATE INDEX ix_vehicles_customer_id ON vehicles(customer_id);

CREATE TABLE catalog_services (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    current_price NUMERIC(19, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_catalog_services_price CHECK (current_price >= 0)
);

CREATE UNIQUE INDEX uq_catalog_services_active_name
    ON catalog_services(lower(name)) WHERE active;

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    item_type VARCHAR(6) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    available_quantity BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inventory_items_type CHECK (item_type IN ('PART', 'SUPPLY')),
    CONSTRAINT ck_inventory_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_inventory_items_quantity CHECK (available_quantity >= 0)
);

CREATE UNIQUE INDEX uq_inventory_items_active_name
    ON inventory_items(lower(name)) WHERE active;
CREATE INDEX ix_inventory_items_type_active ON inventory_items(item_type, active);

CREATE TABLE administrators (
    id UUID PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_administrators_username UNIQUE (username)
);

CREATE TABLE administrator_roles (
    administrator_id UUID NOT NULL REFERENCES administrators(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (administrator_id, role),
    CONSTRAINT ck_administrator_roles_role CHECK (role IN ('ADMIN'))
);

CREATE TABLE service_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    customer_document_type VARCHAR(4) NOT NULL,
    customer_document_masked VARCHAR(18) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    vehicle_license_plate VARCHAR(7) NOT NULL,
    vehicle_brand VARCHAR(80) NOT NULL,
    vehicle_model VARCHAR(100) NOT NULL,
    vehicle_year INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    tracking_token_hash VARCHAR(64) NOT NULL,
    tracking_expires_at TIMESTAMPTZ,
    tracking_revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_service_orders_tracking_token_hash UNIQUE (tracking_token_hash),
    CONSTRAINT ck_service_orders_document_type CHECK (customer_document_type IN ('CPF', 'CNPJ')),
    CONSTRAINT ck_service_orders_status CHECK (
        status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'IN_EXECUTION', 'FINISHED', 'DELIVERED')
    )
);

CREATE INDEX ix_service_orders_customer_id ON service_orders(customer_id);
CREATE INDEX ix_service_orders_vehicle_id ON service_orders(vehicle_id);
CREATE INDEX ix_service_orders_status ON service_orders(status);
CREATE INDEX ix_service_orders_created_at ON service_orders(created_at);

CREATE TABLE service_order_items (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL REFERENCES service_orders(id) ON DELETE RESTRICT,
    source_type VARCHAR(7) NOT NULL,
    source_id UUID NOT NULL,
    description_snapshot VARCHAR(1000) NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price_snapshot NUMERIC(19, 2) NOT NULL,
    consumed_quantity BIGINT NOT NULL DEFAULT 0,
    additional_repair BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_service_order_items_type CHECK (source_type IN ('SERVICE', 'PART', 'SUPPLY')),
    CONSTRAINT ck_service_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_service_order_items_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT ck_service_order_items_consumed CHECK (
        consumed_quantity >= 0 AND consumed_quantity <= quantity
    )
);

CREATE INDEX ix_service_order_items_order_id ON service_order_items(service_order_id);

CREATE TABLE quotations (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL REFERENCES service_orders(id) ON DELETE RESTRICT,
    version_number INTEGER NOT NULL,
    service_subtotal NUMERIC(19, 2) NOT NULL,
    inventory_subtotal NUMERIC(19, 2) NOT NULL,
    total NUMERIC(19, 2) NOT NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    requested_at TIMESTAMPTZ,
    CONSTRAINT uq_quotations_order_version UNIQUE (service_order_id, version_number),
    CONSTRAINT ck_quotations_version CHECK (version_number > 0),
    CONSTRAINT ck_quotations_subtotals CHECK (service_subtotal >= 0 AND inventory_subtotal >= 0),
    CONSTRAINT ck_quotations_total CHECK (total >= 0 AND total = service_subtotal + inventory_subtotal),
    CONSTRAINT ck_quotations_state CHECK (
        state IN ('DRAFT', 'AWAITING_APPROVAL', 'APPROVED', 'REJECTED', 'SUPERSEDED')
    )
);

CREATE INDEX ix_quotations_service_order_id ON quotations(service_order_id);

CREATE TABLE quotation_lines (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES quotations(id) ON DELETE RESTRICT,
    source_type VARCHAR(7) NOT NULL,
    source_id UUID NOT NULL,
    description_snapshot VARCHAR(1000) NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT ck_quotation_lines_type CHECK (source_type IN ('SERVICE', 'PART', 'SUPPLY')),
    CONSTRAINT ck_quotation_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_quotation_lines_amounts CHECK (
        unit_price >= 0 AND line_total >= 0 AND line_total = unit_price * quantity
    )
);

CREATE INDEX ix_quotation_lines_quotation_id ON quotation_lines(quotation_id);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES quotations(id) ON DELETE RESTRICT,
    decision VARCHAR(8) NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    channel VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT uq_approvals_quotation_id UNIQUE (quotation_id),
    CONSTRAINT ck_approvals_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_approvals_channel CHECK (channel IN ('CUSTOMER_ACCESS_TOKEN'))
);

CREATE TABLE service_order_status_history (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL REFERENCES service_orders(id) ON DELETE RESTRICT,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_type VARCHAR(13) NOT NULL,
    actor_reference VARCHAR(100),
    reason VARCHAR(500),
    CONSTRAINT ck_status_history_from_status CHECK (
        from_status IS NULL OR from_status IN (
            'RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'IN_EXECUTION', 'FINISHED', 'DELIVERED'
        )
    ),
    CONSTRAINT ck_status_history_to_status CHECK (
        to_status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'IN_EXECUTION', 'FINISHED', 'DELIVERED')
    ),
    CONSTRAINT ck_status_history_actor_type CHECK (actor_type IN ('ADMINISTRATOR', 'CUSTOMER'))
);

CREATE INDEX ix_status_history_order_occurred
    ON service_order_status_history(service_order_id, occurred_at);
CREATE INDEX ix_status_history_status_occurred
    ON service_order_status_history(to_status, occurred_at);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    service_order_id UUID REFERENCES service_orders(id) ON DELETE RESTRICT,
    service_order_item_id UUID REFERENCES service_order_items(id) ON DELETE RESTRICT,
    movement_type VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL,
    resulting_quantity BIGINT NOT NULL,
    reason VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id UUID NOT NULL REFERENCES administrators(id) ON DELETE RESTRICT,
    CONSTRAINT ck_inventory_movements_type CHECK (
        movement_type IN ('STOCK_ADDED', 'STOCK_REMOVED', 'ORDER_CONSUMED', 'ORDER_RETURNED')
    ),
    CONSTRAINT ck_inventory_movements_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_movements_result CHECK (resulting_quantity >= 0),
    CONSTRAINT ck_inventory_movements_order_link CHECK (
        movement_type NOT IN ('ORDER_CONSUMED', 'ORDER_RETURNED')
        OR (service_order_id IS NOT NULL AND service_order_item_id IS NOT NULL)
    )
);

CREATE INDEX ix_inventory_movements_item_occurred
    ON inventory_movements(inventory_item_id, occurred_at);
CREATE INDEX ix_inventory_movements_service_order_id
    ON inventory_movements(service_order_id) WHERE service_order_id IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_movements_order_consumption
    ON inventory_movements(service_order_id, service_order_item_id, movement_type)
    WHERE movement_type = 'ORDER_CONSUMED';
