CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    phone VARCHAR NOT NULL,
    name VARCHAR,
    is_blocked BOOLEAN DEFAULT false,
    total_orders INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(business_id, phone)
);
