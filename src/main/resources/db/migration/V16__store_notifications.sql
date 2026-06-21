CREATE TABLE store_notifications (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    webhook_topic VARCHAR(100) NOT NULL,
    external_order_id VARCHAR(255),
    customer_name VARCHAR(255),
    order_total DECIMAL(12, 2),
    currency VARCHAR(10),
    payment_status VARCHAR(100),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_notifications_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
    CONSTRAINT fk_store_notifications_platform FOREIGN KEY (platform_id) REFERENCES seller_platforms(id) ON DELETE CASCADE
);

CREATE INDEX idx_store_notifications_seller_created ON store_notifications(seller_id, created_at DESC);
CREATE INDEX idx_store_notifications_seller_unread ON store_notifications(seller_id, read_at) WHERE read_at IS NULL;

CREATE TABLE shopify_webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    platform_id BIGINT,
    delivery_id VARCHAR(255) UNIQUE,
    topic VARCHAR(100),
    shop_domain VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    raw_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shopify_webhook_deliveries_platform FOREIGN KEY (platform_id) REFERENCES seller_platforms(id) ON DELETE SET NULL
);

CREATE INDEX idx_shopify_webhook_deliveries_platform_created ON shopify_webhook_deliveries(platform_id, created_at DESC);
