CREATE TABLE ai_requests (
    id UUID PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    feature VARCHAR(50) NOT NULL,
    input_summary TEXT,
    gemini_model VARCHAR(50),
    prompt_tokens INTEGER,
    output_tokens INTEGER,
    latency_ms INTEGER,
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_req_feature_date ON ai_requests(feature, created_at);
CREATE INDEX idx_ai_req_seller_date ON ai_requests(seller_id, created_at);

CREATE TABLE ai_insights_cache (
    id UUID PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    insight_type VARCHAR(20) NOT NULL,
    period_start DATE,
    period_end DATE,
    content JSONB,
    generated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (seller_id, insight_type, period_start)
);

CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    session_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_chat_seller ON ai_chat_sessions(seller_id);

CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_chat_msg_session ON ai_chat_messages(session_id, created_at);
