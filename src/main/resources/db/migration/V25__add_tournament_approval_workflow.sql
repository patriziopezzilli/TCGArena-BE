-- Add tournament approval workflow columns
-- This enables customers to request tournaments and shop owners to approve/reject them

-- Add new statuses to tournament_status enum (if using enum type)
-- Note: PostgreSQL doesn't allow adding values to existing enum in a transaction
-- So we'll rely on the String-based @Enumerated in JPA

-- Add approval workflow columns
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS approved_by_user_id BIGINT;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS approval_date TIMESTAMP;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- Add comments to explain the columns
COMMENT ON COLUMN tournaments.created_by_user_id IS 'User ID of the customer who created/requested the tournament';
COMMENT ON COLUMN tournaments.approved_by_user_id IS 'User ID of the shop owner/merchant who approved the tournament';
COMMENT ON COLUMN tournaments.approval_date IS 'Timestamp when the tournament was approved';
COMMENT ON COLUMN tournaments.rejection_reason IS 'Reason provided by merchant if tournament request was rejected';

-- Add index for filtering pending tournaments by organizer (shop)
CREATE INDEX IF NOT EXISTS idx_tournaments_status_organizer ON tournaments(status, organizer_id);

-- Add index for filtering tournaments by creator
CREATE INDEX IF NOT EXISTS idx_tournaments_created_by ON tournaments(created_by_user_id);

-- Update existing tournaments to have createdByUserId same as organizerId (for merchants)
-- This ensures backward compatibility - merchant-created tournaments were both created and organized by them
UPDATE tournaments 
SET created_by_user_id = organizer_id 
WHERE created_by_user_id IS NULL 
  AND status NOT IN ('PENDING_APPROVAL', 'REJECTED');
