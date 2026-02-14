-- Add profile_image_url column to global_chat_messages table
-- This column stores the user's profile image URL for display in chat messages

ALTER TABLE global_chat_messages
ADD COLUMN IF NOT EXISTS profile_image_url TEXT;

-- Add comment for documentation
COMMENT ON COLUMN global_chat_messages.profile_image_url IS 'URL of the user profile image for display in chat messages';