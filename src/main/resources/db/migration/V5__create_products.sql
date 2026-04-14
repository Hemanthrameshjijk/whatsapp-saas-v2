CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    name VARCHAR NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    category VARCHAR,
    stock_qty INT DEFAULT 0,
    photo_url VARCHAR,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_products_business ON products(business_id);
CREATE INDEX idx_products_name_fts ON products USING gin(to_tsvector('english', name));
