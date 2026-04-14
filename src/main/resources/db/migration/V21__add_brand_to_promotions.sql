-- Add brand-based targeting to promotions
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS brand VARCHAR(100);
