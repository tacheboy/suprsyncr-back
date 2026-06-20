-- V7: Create connector_failures table
-- Requirements: 27, 32, 89

-- Create connector_failures table
CREATE TABLE connector_failures (
    id BIGSERIAL PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    failure_type VARCHAR(50) NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_connector_failures_platform FOREIGN KEY (platform_id) REFERENCES seller_platforms(id) ON DELETE CASCADE
);

-- Create indexes on connector_failures for faster lookups
CREATE INDEX idx_connector_failures_platform_id ON connector_failures(platform_id);
CREATE INDEX idx_connector_failures_resolved ON connector_failures(resolved);
