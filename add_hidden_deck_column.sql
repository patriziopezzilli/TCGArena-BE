-- Add isHidden column to decks table for "Hide Deck" feature
-- This allows competitive players to strategically hide their decks from public view

ALTER TABLE decks ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for efficient filtering
CREATE INDEX IF NOT EXISTS idx_decks_is_hidden ON decks(is_hidden);

-- Verify the column was added
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'decks' AND column_name = 'is_hidden';
