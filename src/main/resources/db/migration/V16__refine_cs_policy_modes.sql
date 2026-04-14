-- Refine Product Policy Modes
ALTER TABLE products ADD COLUMN IF NOT EXISTS warranty_mode VARCHAR(20) DEFAULT 'GLOBAL';
ALTER TABLE products ADD COLUMN IF NOT EXISTS return_mode VARCHAR(20) DEFAULT 'GLOBAL';
ALTER TABLE products ADD COLUMN IF NOT EXISTS warranty_details TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS warranty_claim_rules TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS custom_return_policy TEXT;

-- Handle existing boolean columns if they exist from V14 (safety check)
-- We will migrate their data then drop or ignore them.
-- For now we just add the new structure.

-- Refine AI Settings Policies
ALTER TABLE ai_settings ADD COLUMN IF NOT EXISTS global_warranty_details TEXT;
ALTER TABLE ai_settings ADD COLUMN IF NOT EXISTS global_warranty_claim_rules TEXT;
ALTER TABLE ai_settings ADD COLUMN IF NOT EXISTS global_warranty_mode VARCHAR(20) DEFAULT 'ENABLED';
ALTER TABLE ai_settings ADD COLUMN IF NOT EXISTS global_return_mode VARCHAR(20) DEFAULT 'ENABLED';
ALTER TABLE ai_settings ADD COLUMN IF NOT EXISTS general_faq TEXT;

-- Update Customer for Handoff
ALTER TABLE customers ADD COLUMN IF NOT EXISTS requires_human BOOLEAN DEFAULT FALSE;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS requires_human_reason TEXT;
