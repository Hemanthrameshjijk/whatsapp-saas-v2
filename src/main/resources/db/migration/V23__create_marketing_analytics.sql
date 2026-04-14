CREATE TABLE IF NOT EXISTS marketing_links (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    original_url TEXT NOT NULL,
    short_code TEXT UNIQUE NOT NULL,
    click_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS marketing_clicks (
    id UUID PRIMARY KEY,
    link_id UUID NOT NULL,
    customer_phone TEXT,
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_links_camp ON marketing_links(campaign_id);
CREATE INDEX IF NOT EXISTS idx_clicks_link ON marketing_clicks(link_id);
