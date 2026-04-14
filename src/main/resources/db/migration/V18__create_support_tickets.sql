CREATE TABLE IF NOT EXISTS support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    customer_id UUID REFERENCES customers(id),
    customer_phone VARCHAR NOT NULL,
    order_id UUID REFERENCES orders(id),
    product_id UUID REFERENCES products(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('RETURN', 'WARRANTY')),
    status VARCHAR(20) DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'RESOLVED')),
    reason TEXT NOT NULL,
    product_name VARCHAR,
    policy_applied TEXT,
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    resolved_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tickets_business ON support_tickets(business_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON support_tickets(business_id, status);
