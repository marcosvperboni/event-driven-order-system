CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    channel VARCHAR(10) NOT NULL DEFAULT 'EMAIL',
    message TEXT NOT NULL,
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notifications_order_id ON notifications (order_id);
