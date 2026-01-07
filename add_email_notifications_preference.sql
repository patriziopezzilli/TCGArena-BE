-- Add email notifications preference to users table
-- Default to true for existing users to maintain current behavior

ALTER TABLE users
ADD COLUMN email_notifications_enabled BOOLEAN NOT NULL DEFAULT true;

-- Add comment for documentation
COMMENT ON COLUMN users.email_notifications_enabled IS 'Whether the user wants to receive email notifications. Default: true';