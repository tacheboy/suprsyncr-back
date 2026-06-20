-- V4: Create listings and listing_errors tables
-- Requirements: 12, 32, 57, 58, 70, 89

-- Create listings table
CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    external_product_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    published_at TIMESTAMP,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_listings_product_platform UNIQUE (product_id, platform_id),
    CONSTRAINT fk_listings_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_listings_platform FOREIGN KEY (platform_id) REFERENCES seller_platforms(id) ON DELETE CASCADE
);

-- Create indexes on listings for faster lookups
CREATE INDEX idx_listings_product_id ON listings(product_id);
CREATE INDEX idx_listings_platform_id ON listings(platform_id);

-- Create listing_errors table
CREATE TABLE listing_errors (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    error_message TEXT NOT NULL,
    error_details TEXT,
    resolved BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_errors_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Create index on listing_id for faster lookups
CREATE INDEX idx_listing_errors_listing_id ON listing_errors(listing_id);
