CREATE TABLE ai_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID UNIQUE NOT NULL REFERENCES businesses(id),
    tone_style VARCHAR DEFAULT 'friendly',
    language_style VARCHAR DEFAULT 'auto',
    upsell_mode VARCHAR DEFAULT 'soft',
    greeting_template TEXT,
    active_model VARCHAR DEFAULT 'llama3.2:3b',
    open_time TIME,
    close_time TIME,
    delivery_enabled BOOLEAN DEFAULT true,
    delivery_charge NUMERIC(10,2) DEFAULT 0,
    free_delivery_above NUMERIC(10,2),
    estimated_delivery_days INT DEFAULT 2,
    upi_id VARCHAR,
    shop_address TEXT,
    guardrail_allowlist TEXT,
    updated_at TIMESTAMP DEFAULT now()
);
