-- Add Offer fields to promotions (offers = auto-apply promotions without code)
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS auto_apply BOOLEAN DEFAULT FALSE;
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS offer_label VARCHAR(100);
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS category VARCHAR(100);
