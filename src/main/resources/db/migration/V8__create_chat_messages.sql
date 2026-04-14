CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    customer_phone VARCHAR NOT NULL,
    direction VARCHAR CHECK (direction IN ('IN','OUT')),
    content TEXT,
    message_type VARCHAR DEFAULT 'TEXT',
    session_id VARCHAR,
    guardrail_triggered BOOLEAN DEFAULT false,
    guardrail_reason VARCHAR,
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_chat_business_phone ON chat_messages(business_id, customer_phone, created_at DESC);
