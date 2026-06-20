-- V11: Create Autopilot Tables (agent_runs, proposed_changes, change_impact, entity_snapshots)

-- Each time the agent system runs for a store
CREATE TABLE agent_runs (
  run_id         UUID PRIMARY KEY,
  store_id       VARCHAR(255) NOT NULL,
  triggered_by   VARCHAR(50) CHECK (triggered_by IN ('SCHEDULED','MANUAL','WEBHOOK')),
  triggered_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status         VARCHAR(50) CHECK (status IN ('QUEUED','RUNNING','COMPLETE','FAILED')),
  
  -- Orchestrator's full reasoning (the "thinking" trace)
  orchestrator_reasoning  JSONB,
  
  -- Summary stats for display
  proposals_generated  INTEGER,
  estimated_impact_inr NUMERIC,
  
  completed_at  TIMESTAMP,
  error_message TEXT
);

-- Each individual proposed change
CREATE TABLE proposed_changes (
  change_id         UUID PRIMARY KEY,
  store_id          VARCHAR(255) NOT NULL,
  run_id            UUID REFERENCES agent_runs(run_id) ON DELETE CASCADE,
  
  -- What agent made this
  agent_type        VARCHAR(100), -- LISTING_DOCTOR, SEO_COMMANDER, CART_RECOVERY, etc.
  
  -- What to change
  change_type       VARCHAR(100), -- PRODUCT_TITLE, PRODUCT_DESCRIPTION, META_TITLE, etc.
  shopify_entity_type  VARCHAR(100), -- product, variant, metafield, discount
  shopify_entity_id    VARCHAR(255),
  
  -- The actual change
  current_value     JSONB NOT NULL,
  proposed_value    JSONB NOT NULL,
  
  -- Reasoning (what the agent "thought")
  agent_reasoning   TEXT,
  
  -- Impact estimate
  estimated_impact  JSONB, -- { metric, changePercent, revenueLift, confidence }
  
  -- Risk
  risk_level        VARCHAR(20) CHECK (risk_level IN ('LOW','MEDIUM','HIGH')),
  risk_notes        TEXT,
  
  -- Human-in-the-loop state machine
  status            VARCHAR(50) CHECK (status IN (
    'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 
    'APPLYING', 'APPLIED', 'APPLY_FAILED', 'ROLLED_BACK'
  )),
  approved_by       VARCHAR(255),
  approved_at       TIMESTAMP,
  applied_at        TIMESTAMP,
  rollback_available_until TIMESTAMP, -- approved_at + 30 days
  
  -- For tests with revert
  is_test           BOOLEAN DEFAULT FALSE,
  test_revert_at    TIMESTAMP,
  test_revert_condition JSONB -- { metric, threshold, direction }
);

-- Impact measurement after changes go live
CREATE TABLE change_impact (
  impact_id         UUID PRIMARY KEY,
  change_id         UUID REFERENCES proposed_changes(change_id) ON DELETE CASCADE,
  store_id          VARCHAR(255) NOT NULL,
  
  -- What we're measuring
  metric_type       VARCHAR(100), -- CONVERSION_RATE, CART_ABANDONMENT, ORGANIC_TRAFFIC, REVENUE
  
  -- Baseline: 7 days before change
  baseline_period_start  DATE,
  baseline_period_end    DATE,
  baseline_value         NUMERIC,
  
  -- Measurement: 7 days after change
  measurement_period_start  DATE,
  measurement_period_end    DATE,
  measured_value            NUMERIC,
  
  -- Computed delta
  delta_absolute    NUMERIC,
  delta_percent     NUMERIC,
  
  -- How confident are we this change caused the delta
  attribution_confidence  VARCHAR(20) CHECK (attribution_confidence IN ('LOW','MEDIUM','HIGH')),
  attribution_notes       TEXT,
  
  -- INR revenue impact
  estimated_revenue_impact_inr  NUMERIC,
  
  computed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Snapshots of what a product/entity looked like before changes
-- (For rollback reconstruction)
CREATE TABLE entity_snapshots (
  snapshot_id     UUID PRIMARY KEY,
  change_id       UUID REFERENCES proposed_changes(change_id) ON DELETE CASCADE,
  entity_type     VARCHAR(100),
  entity_id       VARCHAR(255),
  snapshot_data   JSONB, -- full Shopify API response at time of change
  snapshotted_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
