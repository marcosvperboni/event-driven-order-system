CREATE TABLE products (
    id                 UUID PRIMARY KEY,
    sku                VARCHAR(64)     NOT NULL UNIQUE,
    name               VARCHAR(255)    NOT NULL,
    available_quantity INTEGER         NOT NULL CHECK (available_quantity >= 0),
    unit_price         NUMERIC(19, 2)  NOT NULL CHECK (unit_price >= 0),
    created_at         TIMESTAMPTZ     NOT NULL,
    updated_at         TIMESTAMPTZ     NOT NULL,
    version            BIGINT          NOT NULL DEFAULT 0
);
