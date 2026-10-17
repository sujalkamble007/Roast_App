CREATE TABLE IF NOT EXISTS shares (
    id VARCHAR(16) PRIMARY KEY,
    roast_id UUID NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    roast_text TEXT,
    tone VARCHAR(32),
    sources VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_shares_roast_id ON shares(roast_id);
CREATE INDEX IF NOT EXISTS idx_shares_revoked_at ON shares(revoked_at);
