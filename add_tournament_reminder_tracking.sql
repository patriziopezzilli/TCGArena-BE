-- Add last_reminder_sent_at column to tournaments table for tracking notification timestamps
-- This prevents sending duplicate reminders for the same tournament

ALTER TABLE tournaments ADD COLUMN last_reminder_sent_at TIMESTAMP NULL;

-- Add index for performance on the new column
CREATE INDEX idx_tournaments_last_reminder_sent_at ON tournaments(last_reminder_sent_at);

-- Add comment to document the column purpose
COMMENT ON COLUMN tournaments.last_reminder_sent_at IS 'Timestamp when the last tournament start reminder notification was sent to participants';