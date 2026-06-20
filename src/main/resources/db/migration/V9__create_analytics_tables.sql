-- =============================================================
-- V9: Analytics Module — Core Tables
-- Suprsyncr Analytics: dummy D2C store data + snapshot store
-- =============================================================

-- Dummy D2C Stores (Phase 0 seed targets)
CREATE TABLE dummy_stores (
    store_id        VARCHAR(50)  PRIMARY KEY,
    store_name      VARCHAR(255) NOT NULL,
    category        VARCHAR(100) NOT NULL,      -- fashion / electronics / home
    monthly_traffic INTEGER      NOT NULL,
    avg_order_value NUMERIC(10,2) NOT NULL,      -- INR
    primary_traffic_source VARCHAR(50) NOT NULL  -- instagram / google / direct
);

-- Products with full funnel metrics + SEO proxies
CREATE TABLE dummy_products (
    product_id             VARCHAR(50)   PRIMARY KEY,
    store_id               VARCHAR(50)   NOT NULL REFERENCES dummy_stores(store_id) ON DELETE CASCADE,
    name                   VARCHAR(255)  NOT NULL,
    category               VARCHAR(100),
    price                  NUMERIC(10,2) NOT NULL,   -- INR
    stock                  INTEGER       NOT NULL,

    -- Funnel counts (monthly)
    monthly_page_views     INTEGER NOT NULL DEFAULT 0,
    add_to_cart_count      INTEGER NOT NULL DEFAULT 0,
    checkout_count         INTEGER NOT NULL DEFAULT 0,
    purchase_count         INTEGER NOT NULL DEFAULT 0,

    -- Search Console proxy
    organic_impressions    INTEGER NOT NULL DEFAULT 0,
    organic_clicks         INTEGER NOT NULL DEFAULT 0,
    avg_position           NUMERIC(5,2)  DEFAULT NULL,

    primary_traffic_source VARCHAR(50)
);

CREATE INDEX idx_dp_store ON dummy_products(store_id);

-- Daily orders (last 30 days rolling window)
CREATE TABLE dummy_daily_orders (
    order_id        VARCHAR(50)   PRIMARY KEY,
    store_id        VARCHAR(50)   NOT NULL REFERENCES dummy_stores(store_id) ON DELETE CASCADE,
    product_id      VARCHAR(50)   NOT NULL REFERENCES dummy_products(product_id) ON DELETE CASCADE,
    revenue         NUMERIC(10,2) NOT NULL,
    order_date      DATE          NOT NULL,
    channel         VARCHAR(50),         -- instagram / google / direct / referral
    customer_city   VARCHAR(100),
    customer_gender VARCHAR(20),
    age_group       VARCHAR(20)
);

CREATE INDEX idx_ddo_store_date  ON dummy_daily_orders(store_id, order_date);
CREATE INDEX idx_ddo_product     ON dummy_daily_orders(product_id);

-- Session funnel events (daily, per source)
CREATE TABLE dummy_session_events (
    id                      BIGSERIAL    PRIMARY KEY,
    event_date              DATE         NOT NULL,
    store_id                VARCHAR(50)  NOT NULL REFERENCES dummy_stores(store_id) ON DELETE CASCADE,
    source                  VARCHAR(50)  NOT NULL,  -- google / instagram / direct / referral
    sessions                INTEGER      NOT NULL DEFAULT 0,
    add_to_cart_sessions    INTEGER      NOT NULL DEFAULT 0,
    checkout_init_sessions  INTEGER      NOT NULL DEFAULT 0,
    purchased_sessions      INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_dse_store_date  ON dummy_session_events(store_id, event_date);

-- Keyword-level SEO data per product
CREATE TABLE dummy_product_keywords (
    id          BIGSERIAL    PRIMARY KEY,
    product_id  VARCHAR(50)  NOT NULL REFERENCES dummy_products(product_id) ON DELETE CASCADE,
    query       VARCHAR(255) NOT NULL,
    impressions INTEGER      NOT NULL DEFAULT 0,
    clicks      INTEGER      NOT NULL DEFAULT 0,
    position    NUMERIC(5,2) NOT NULL,
    ctr         NUMERIC(6,4) NOT NULL DEFAULT 0
);

CREATE INDEX idx_dpk_product ON dummy_product_keywords(product_id);

-- Analytics snapshot store (Phase 1+)
-- One row per (store, type, date) — payload is JSONB with computed metrics + Gemini narrative
CREATE TABLE analytics_snapshots (
    id           BIGSERIAL    PRIMARY KEY,
    store_id     VARCHAR(50)  NOT NULL,
    type         VARCHAR(50)  NOT NULL,   -- REVENUE_LEAK | PRODUCT_HEALTH | SEO_GAP | FULL
    computed_at  TIMESTAMP    NOT NULL,
    payload      JSONB        NOT NULL,
    data_source  VARCHAR(20)  NOT NULL DEFAULT 'dummy'  -- dummy | live
);

CREATE INDEX idx_as_store_type_date ON analytics_snapshots(store_id, type, computed_at DESC);
CREATE UNIQUE INDEX idx_as_store_type_today
    ON analytics_snapshots(store_id, type, DATE(computed_at));
