-- Create scheduler locks table for preventing duplicate batch executions
-- This table is used by SchedulerLockService to manage distributed locks

CREATE TABLE IF NOT EXISTS scheduler_locks (
    lock_key VARCHAR(255) PRIMARY KEY,
    acquired_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    instance_id VARCHAR(255)
);

-- Create index for faster cleanup of expired locks
CREATE INDEX IF NOT EXISTS idx_scheduler_locks_expires_at ON scheduler_locks(expires_at);

-- Add comment to table
COMMENT ON TABLE scheduler_locks IS 'Table for managing distributed locks to prevent duplicate scheduled task executions';