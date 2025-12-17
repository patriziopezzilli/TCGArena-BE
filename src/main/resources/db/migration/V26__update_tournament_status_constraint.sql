-- Update tournament status constraint to include PENDING_APPROVAL and REJECTED

-- Drop existing status constraint
ALTER TABLE tournaments DROP CONSTRAINT IF EXISTS tournaments_status_check;

-- Add updated status constraint with new PENDING_APPROVAL and REJECTED values
ALTER TABLE tournaments ADD CONSTRAINT tournaments_status_check 
    CHECK (status IN ('PENDING_APPROVAL', 'UPCOMING', 'REGISTRATION_OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED'));
