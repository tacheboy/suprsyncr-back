-- V6: Create inventory and inventory_transactions tables
-- Requirements: 9, 10, 11, 32, 55, 56, 71, 82, 89

-- Create inventory table
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_variant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_variant_warehouse UNIQUE (product_variant_id, warehouse_id),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES seller_warehouses(id) ON DELETE CASCADE
);

-- Create indexes on inventory for faster lookups
CREATE INDEX idx_inventory_product_variant_id ON inventory(product_variant_id);
CREATE INDEX idx_inventory_warehouse_id ON inventory(warehouse_id);

-- Create inventory_transactions table
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_transactions_inventory FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE CASCADE
);

-- Create index on inventory_id for faster lookups
CREATE INDEX idx_inventory_transactions_inventory_id ON inventory_transactions(inventory_id);
