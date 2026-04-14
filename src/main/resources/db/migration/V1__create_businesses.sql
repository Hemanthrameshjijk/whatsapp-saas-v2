CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL,
    whatsapp_number VARCHAR UNIQUE NOT NULL,
    city VARCHAR,
    business_type VARCHAR,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
