-- Add nationality field to deck_cards table
ALTER TABLE deck_cards ADD COLUMN nationality VARCHAR(10) DEFAULT 'EN';

-- Add nationality field to inventory_cards table
ALTER TABLE inventory_cards ADD COLUMN nationality VARCHAR(10) DEFAULT 'EN';

-- Add nationality field to user_cards table
ALTER TABLE user_cards ADD COLUMN nationality VARCHAR(10) DEFAULT 'EN';