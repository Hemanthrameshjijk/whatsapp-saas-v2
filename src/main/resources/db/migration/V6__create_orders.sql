CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    customer_id UUID REFERENCES customers(id),
    customer_phone VARCHAR NOT NULL,
    status VARCHAR DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')),
    total_amount NUMERIC(10,2),
    delivery_type VARCHAR CHECK (delivery_type IN ('DELIVERY','PICKUP')),
    delivery_address_text TEXT,
    delivery_lat DOUBLE PRECISION,
    delivery_lng DOUBLE PRECISION,
    address_source VARCHAR CHECK (address_source IN ('TEXT','PIN')),
    payment_method VARCHAR CHECK (payment_method IN ('UPI','COD')),
    payment_status VARCHAR DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING','PAID','REFUNDED')),
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT now(),
    confirmed_at TIMESTAMP,
    delivered_at TIMESTAMP
);
CREATE INDEX idx_orders_business ON orders(business_id);
CREATE INDEX idx_orders_customer_phone ON orders(business_id, customer_phone);
