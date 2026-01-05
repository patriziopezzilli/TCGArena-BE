-- Add is_partner column to shops table for the Shop Rewards feature
-- This marks shops that have active rewards and are official partners

ALTER TABLE shops ADD COLUMN IF NOT EXISTS is_partner BOOLEAN DEFAULT FALSE;

-- Update existing shops to be partners if they already have rewards
-- (run this after creating the shop_rewards table)
-- UPDATE shops SET is_partner = TRUE WHERE id IN (SELECT DISTINCT shop_id FROM shop_rewards WHERE is_active = TRUE);
