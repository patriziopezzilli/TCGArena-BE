-- Add vote counters to card_templates for Card Rating Arena feature
ALTER TABLE card_templates
    ADD COLUMN likes_count BIGINT DEFAULT 0 NOT NULL,
    ADD COLUMN dislikes_count BIGINT DEFAULT 0 NOT NULL;

-- Create index for ranking queries
CREATE INDEX idx_card_templates_likes ON card_templates(likes_count);
CREATE INDEX idx_card_templates_dislikes ON card_templates(dislikes_count);
