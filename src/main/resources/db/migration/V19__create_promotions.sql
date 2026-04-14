-- Promotions / Promo Codes
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    code VARCHAR(50) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FLAT')),
    discount_value NUMERIC(10,2) NOT NULL,
    max_discount NUMERIC(10,2),
    min_order_amount NUMERIC(10,2),
    product_id UUID REFERENCES products(id),
    customer_phone VARCHAR,
    first_order_only BOOLEAN DEFAULT FALSE,
    max_uses INTEGER,
    used_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    starts_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(business_id, code)
);
CREATE INDEX IF NOT EXISTS idx_promotions_business ON promotions(business_id);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(business_id, code);

-- Add promo tracking fields to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS promo_code VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(10,2) DEFAULT 0;
