CREATE TABLE roles (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL UNIQUE,
    active    BOOLEAN NOT NULL DEFAULT TRUE,
    create_at TIMESTAMPTZ(0) NOT NULL DEFAULT NOW(),
    update_at TIMESTAMPTZ(0)
);

CREATE TABLE accounts (
    id            UUID PRIMARY KEY,
    identifier    VARCHAR(255) NOT NULL UNIQUE,           -- email
    password_hash VARCHAR(255) NOT NULL,
    account_type  VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER'
                  CHECK (account_type IN ('EMPLOYEE','CUSTOMER')),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    create_at     TIMESTAMPTZ(0) NOT NULL DEFAULT NOW(),
    update_at     TIMESTAMPTZ(0)
);

CREATE TABLE customers (
    account_id      UUID PRIMARY KEY REFERENCES accounts(id),
    document_type   VARCHAR(2) CHECK (document_type IN ('CC','CE','TI')),
    document_number VARCHAR(50),
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL
);

CREATE TABLE employees (
    account_id      UUID PRIMARY KEY REFERENCES accounts(id),
    role_id         BIGINT NOT NULL REFERENCES roles(id),
    document_type   VARCHAR(2) CHECK (document_type IN ('CC','CE','TI')),
    document_number VARCHAR(50),
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE colors (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE sizes (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    create_at   TIMESTAMPTZ(0) NOT NULL DEFAULT NOW(),
    update_at   TIMESTAMPTZ(0)
);
CREATE INDEX idx_products_name ON products (LOWER(name));
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE product_variants (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    color_id   BIGINT NOT NULL REFERENCES colors(id),
    size_id    BIGINT NOT NULL REFERENCES sizes(id),
    sku        VARCHAR(100) NOT NULL UNIQUE,
    price      NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock      INTEGER NOT NULL CHECK (stock >= 0),
    CONSTRAINT uq_variant UNIQUE (product_id, color_id, size_id)
);

CREATE TABLE product_images (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    url        TEXT NOT NULL
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      UUID NOT NULL REFERENCES customers(account_id),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','PAID','SHIPPED','CANCELLED')),
    total            NUMERIC(12,2) NOT NULL,
    shipping_address TEXT,
    create_at        TIMESTAMPTZ(0) NOT NULL DEFAULT NOW(),
    update_at        TIMESTAMPTZ(0)
);
CREATE INDEX idx_orders_customer ON orders (customer_id);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL          -- precio al momento de la compra
);