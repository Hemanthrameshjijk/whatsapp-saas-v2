CREATE TABLE customer_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    customer_phone VARCHAR NOT NULL,
    pref_key VARCHAR NOT NULL,
    pref_value TEXT,
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE(business_id, customer_phone, pref_key)
);
