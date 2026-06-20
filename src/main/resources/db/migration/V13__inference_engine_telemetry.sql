-- V13: Inference Engine — telemetry, budget ledger, and proposal provenance.
-- Backs the orchestrator/supervisor/router so every run records what was
-- decided, which model ran each task, what it cost, and how confident it was.

CREATE TABLE IF NOT EXISTS evidence_packs (
    id           UUID PRIMARY KEY,
    run_id       UUID NOT NULL,
    store_id     VARCHAR(50) NOT NULL,
    window_from  DATE,
    window_to    DATE,
    pack         JSONB,
    data_quality NUMERIC(5,4),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One row per planned task (the supervisor's admitted DAG).
CREATE TABLE IF NOT EXISTS agent_tasks (
    task_id              UUID PRIMARY KEY,
    run_id               UUID NOT NULL,
    store_id             VARCHAR(50) NOT NULL,
    agent_type           VARCHAR(100) NOT NULL,
    kind                 VARCHAR(20) NOT NULL,   -- deterministic | llm
    complexity           VARCHAR(20),            -- simple | moderate | complex
    status               VARCHAR(20),            -- COMPLETE | DEGRADED | FAILED | SKIPPED
    revenue_at_stake_inr NUMERIC(14,2),
    accuracy_bar         NUMERIC(5,4),
    model_path           VARCHAR(255),           -- e.g. "qwen3.6-27b→gpt-4o" | "deterministic"
    cost_inr             NUMERIC(14,6),
    confidence           NUMERIC(5,4),
    note                 TEXT,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_run ON agent_tasks(run_id);

-- One row per model call (the cost/accuracy ledger; powers ROI + calibration).
CREATE TABLE IF NOT EXISTS model_invocations (
    id              UUID PRIMARY KEY,
    run_id          UUID NOT NULL,
    task_id         UUID,
    model           VARCHAR(100) NOT NULL,
    tier            SMALLINT,
    purpose         VARCHAR(30),                 -- draft | escalate | verify | deterministic
    prompt_tokens   INTEGER DEFAULT 0,
    output_tokens   INTEGER DEFAULT 0,
    latency_ms      INTEGER DEFAULT 0,
    cost_inr        NUMERIC(14,6) DEFAULT 0,
    verifier_passed BOOLEAN,
    escalated_from  VARCHAR(100),
    confidence      NUMERIC(5,4),
    ok              BOOLEAN DEFAULT TRUE,
    error           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_model_invocations_run ON model_invocations(run_id);

-- Per-agent realised track record → calibration + routing feedback (Phase 4).
CREATE TABLE IF NOT EXISTS agent_performance (
    id                    UUID PRIMARY KEY,
    store_id              VARCHAR(50) NOT NULL,
    agent_type            VARCHAR(100) NOT NULL,
    change_type           VARCHAR(100),
    proposals             INTEGER DEFAULT 0,
    approved              INTEGER DEFAULT 0,
    applied               INTEGER DEFAULT 0,
    predicted_impact_inr  NUMERIC(14,2) DEFAULT 0,
    realised_impact_inr   NUMERIC(14,2) DEFAULT 0,
    calibration_error     NUMERIC(8,4),
    window_end            DATE
);

-- SERP / market-intel cache (Phase 3).
CREATE TABLE IF NOT EXISTS market_intel_cache (
    id          UUID PRIMARY KEY,
    store_id    VARCHAR(50) NOT NULL,
    query_hash  VARCHAR(64) NOT NULL,
    payload     JSONB,
    expires_at  TIMESTAMP NOT NULL
);

-- Proposal provenance (which evidence, which models, what it cost, how sure).
ALTER TABLE proposed_changes ADD COLUMN IF NOT EXISTS evidence_ids JSONB;
ALTER TABLE proposed_changes ADD COLUMN IF NOT EXISTS model_path VARCHAR(255);
ALTER TABLE proposed_changes ADD COLUMN IF NOT EXISTS cost_inr NUMERIC(14,6);
ALTER TABLE proposed_changes ADD COLUMN IF NOT EXISTS confidence NUMERIC(5,4);

-- Run-level budget ledger.
ALTER TABLE agent_runs ADD COLUMN IF NOT EXISTS total_cost_inr NUMERIC(14,6);
ALTER TABLE agent_runs ADD COLUMN IF NOT EXISTS engine_version VARCHAR(30);
