CREATE TABLE IF NOT EXISTS roasts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tone VARCHAR(32) NOT NULL,
    sources VARCHAR(255) NOT NULL,
    stats_json JSONB NOT NULL,
    stats_hash VARCHAR(64) NOT NULL,
    roast_text TEXT,
    error_message TEXT,
    prompt_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_roasts_user_id ON roasts(user_id);
CREATE INDEX IF NOT EXISTS idx_roasts_status ON roasts(status);
CREATE INDEX IF NOT EXISTS idx_roasts_stats_hash ON roasts(stats_hash);
