-- V5: Create orders, order_items, and order_events tables
-- Requirements: 15, 32, 51, 54, 60, 67, 80, 81, 86, 87, 89

-- Create orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    external_order_id VARCHAR(255) NOT NULL,
    usp_order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    shipping_address TEXT NOT NULL,
    customer_phone VARCHAR(20),
    customer_email VARCHAR(255),
    ordered_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    tracking_number VARCHAR(255),
    courier_partner VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_external_order_id UNIQUE (external_order_id),
    CONSTRAINT uk_orders_usp_order_id UNIQUE (usp_order_id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_platform FOREIGN KEY (platform_id) REFERENCES seller_platforms(id) ON DELETE CASCADE
);

-- Create indexes on orders for faster lookups
CREATE INDEX idx_orders_seller_id ON orders(seller_id);
CREATE INDEX idx_orders_platform_id ON orders(platform_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_external_order_id ON orders(external_order_id);

-- Create order_items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(255),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
);

-- Create index on order_id for faster lookups
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Create order_events table
CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    notes TEXT,
    triggered_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Create index on order_id for faster lookups
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
