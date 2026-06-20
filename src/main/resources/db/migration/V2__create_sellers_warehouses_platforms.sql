-- V2: Create sellers, seller_warehouses, and seller_platforms tables
-- Requirements: 3, 4, 5, 32, 44, 59, 62, 66, 89, 103, 104, 105, 106

-- Create sellers table
CREATE TABLE sellers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    gstin VARCHAR(15),
    business_address TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sellers_user_id UNIQUE (user_id),
    CONSTRAINT fk_sellers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_sellers_user_id ON sellers(user_id);

-- Create seller_warehouses table
CREATE TABLE seller_warehouses (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_warehouses_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE
);

-- Create index on seller_id for faster lookups
CREATE INDEX idx_seller_warehouses_seller_id ON seller_warehouses(seller_id);

-- Create seller_platforms table
CREATE TABLE seller_platforms (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    platform_type VARCHAR(50) NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    encrypted_credentials TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    creation_method VARCHAR(50) NOT NULL,
    external_store_id VARCHAR(255),
    platform_metadata TEXT,
    last_synced_at TIMESTAMP,
    last_sync_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_platforms_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE
);

-- Create indexes on seller_platforms for faster lookups
CREATE INDEX idx_seller_platforms_seller_id ON seller_platforms(seller_id);
CREATE INDEX idx_seller_platforms_external_store_id ON seller_platforms(external_store_id);
