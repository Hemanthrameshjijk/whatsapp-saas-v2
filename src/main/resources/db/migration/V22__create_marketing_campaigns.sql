CREATE TABLE IF NOT EXISTS marketing_campaigns (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    name TEXT,
    message TEXT NOT NULL,
    audience TEXT NOT NULL,
    promo_id UUID,
    sent_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_campaigns_biz ON marketing_campaigns(business_id);
