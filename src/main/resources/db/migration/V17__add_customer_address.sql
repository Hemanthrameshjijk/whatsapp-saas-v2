-- Add Delivery Address fields to Customer
ALTER TABLE customers ADD COLUMN IF NOT EXISTS last_delivery_address TEXT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS last_lat DOUBLE PRECISION;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS last_lng DOUBLE PRECISION;

-- Add Delivery Address fields to Orders (just in case they are missing from V6)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address_text TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_lat DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_lng DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS address_source VARCHAR(20);
