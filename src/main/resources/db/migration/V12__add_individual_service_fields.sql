-- V12: Add individual service run fields to agent_runs

ALTER TABLE agent_runs ADD COLUMN IF NOT EXISTS run_type VARCHAR(50) DEFAULT 'FULL_PIPELINE';
ALTER TABLE agent_runs ADD COLUMN IF NOT EXISTS selected_agents VARCHAR(500);
ALTER TABLE agent_runs ADD COLUMN IF NOT EXISTS product_overrides JSONB;
