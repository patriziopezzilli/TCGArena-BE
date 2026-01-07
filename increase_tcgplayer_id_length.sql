-- Increase length of tcgplayer_id column to accommodate Scryfall UUIDs (36 chars)
-- Scryfall IDs are UUIDs like '0000579f-7b35-4ed3-b44c-db2a538066fe'

ALTER TABLE card_templates ALTER COLUMN tcgplayer_id TYPE VARCHAR(100);