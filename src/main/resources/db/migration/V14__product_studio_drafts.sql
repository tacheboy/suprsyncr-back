-- V14: Product Studio drafts.
--
-- A draft is the seller's in-progress listing being authored by the AI engine.
-- Lifecycle: PENDING_AI → AI_COMPLETE → (edited inline) → PUBLISHED.
-- On PUBLISHED a real `products` row is created and (if enabled) the listing
-- is pushed to Shopify; the draft remains for audit/provenance.

CREATE TABLE product_studio_drafts (
  draft_id           UUID PRIMARY KEY,
  store_id           VARCHAR(255) NOT NULL,
  seller_id          BIGINT,
  status             VARCHAR(32) NOT NULL
                       CHECK (status IN ('PENDING_AI','AI_COMPLETE','PUBLISHED','FAILED')),

  -- seller input
  image_url          TEXT NOT NULL,
  claimed_title      VARCHAR(512) NOT NULL,
  posture            VARCHAR(32) DEFAULT 'balanced',

  -- 3-column AI output (filled when status=AI_COMPLETE)
  copy_column        JSONB,            -- { title, bullets, description }
  seo_column         JSONB,            -- { handle, tags, search_terms, meta_title, meta_description }
  metadata_column    JSONB,            -- { product_type, vendor, attributes, condition }

  -- vision identification result and mismatch warning (if any)
  identified_product JSONB,            -- { title, brand, model, attributes, confidence }
  mismatch_warning   JSONB,            -- { mismatch: bool, seller_claim, identified, recommendation }

  -- engine provenance
  model_path         TEXT,             -- e.g. "gpt-4o→gemma-4-26b-a4b-it"
  cost_inr           NUMERIC(12,6),
  confidence         NUMERIC(4,3),
  plan_reasoning     TEXT,

  -- publish linkage
  published_product_id  BIGINT,        -- references products(id) once published
  shopify_product_id    VARCHAR(64),   -- Shopify gid suffix once pushed

  error_message      TEXT,
  created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ai_completed_at    TIMESTAMP,
  published_at       TIMESTAMP,
  updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_studio_drafts_store_id      ON product_studio_drafts(store_id);
CREATE INDEX idx_studio_drafts_seller_id     ON product_studio_drafts(seller_id);
CREATE INDEX idx_studio_drafts_status        ON product_studio_drafts(status);
CREATE INDEX idx_studio_drafts_created_at    ON product_studio_drafts(created_at DESC);
