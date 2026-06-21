-- V15: Sale Attribution (Scenario 3 — order-triggered attribution).
--
-- One row per attribution attempt. The unique (order_id) constraint is also
-- the dedup key for the poller: if a row exists for an order, the gate has
-- already been run on it and we never re-attempt.
--
-- Lifecycle / status semantics:
--   PENDING          → gate passed, engine call queued
--   ATTRIBUTED       → engine returned attributed=true
--   NOT_ATTRIBUTABLE → engine ran but verdict.attributed=false (no recent
--                      change explained the sale)
--   GATE_SKIPPED     → trigger gate declined (no recent change) — recorded
--                      so the poller doesn't keep checking
--   FAILED           → engine errored or output unparseable

CREATE TABLE attributions (
  attribution_id        UUID PRIMARY KEY,
  order_id              BIGINT NOT NULL UNIQUE,
  store_id              VARCHAR(255) NOT NULL,
  product_id            BIGINT,

  status                VARCHAR(32) NOT NULL
                          CHECK (status IN ('PENDING','ATTRIBUTED',
                                            'NOT_ATTRIBUTABLE',
                                            'GATE_SKIPPED','FAILED')),

  -- engine verdict (filled when status=ATTRIBUTED or NOT_ATTRIBUTABLE)
  causal_change_id      UUID,                -- references proposed_changes(change_id)
  causal_change_type    VARCHAR(100),
  confidence            NUMERIC(4,3),
  reasoning             TEXT,
  pattern               JSONB,               -- { what, scope }

  -- revenue tracking
  order_revenue_inr     NUMERIC(12,2) NOT NULL DEFAULT 0,
  realized_revenue_inr  NUMERIC(12,2) NOT NULL DEFAULT 0,
  forecasted_lift_inr   NUMERIC(12,2) NOT NULL DEFAULT 0,

  -- propagation
  proposals_generated   INTEGER NOT NULL DEFAULT 0,
  -- references proposed_changes(change_id) that were created from this run
  generated_change_ids  JSONB,

  -- engine telemetry
  total_cost_inr        NUMERIC(12,6),
  model_path            TEXT,
  plan_reasoning        TEXT,
  invocations           JSONB,
  verifier              JSONB,

  error_message         TEXT,
  triggered_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at          TIMESTAMP
);

CREATE INDEX idx_attributions_store_id      ON attributions(store_id);
CREATE INDEX idx_attributions_status        ON attributions(status);
CREATE INDEX idx_attributions_product_id    ON attributions(product_id);
CREATE INDEX idx_attributions_causal_change ON attributions(causal_change_id);
CREATE INDEX idx_attributions_triggered_at  ON attributions(triggered_at DESC);
