-- Add opening_hours_json column to shops table for structured opening hours
ALTER TABLE shops ADD COLUMN IF NOT EXISTS opening_hours_json TEXT;

-- Add comment to explain the column
COMMENT ON COLUMN shops.opening_hours_json IS 'JSON structure containing weekly opening hours: {monday: {open, close, closed}, tuesday: {...}, ...}';

-- Optional: Migrate existing data from legacy fields to new structure
-- This is a basic migration that creates a simple schedule from opening_hours string
-- You can enhance this based on your actual data patterns
UPDATE shops 
SET opening_hours_json = jsonb_build_object(
    'monday', CASE WHEN opening_days LIKE '%Mon%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'tuesday', CASE WHEN opening_days LIKE '%Tue%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'wednesday', CASE WHEN opening_days LIKE '%Wed%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'thursday', CASE WHEN opening_days LIKE '%Thu%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'friday', CASE WHEN opening_days LIKE '%Fri%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'saturday', CASE WHEN opening_days LIKE '%Sat%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END,
    'sunday', CASE WHEN opening_days LIKE '%Sun%' THEN 
        jsonb_build_object('open', split_part(opening_hours, '-', 1), 'close', split_part(opening_hours, '-', 2), 'closed', false)
        ELSE jsonb_build_object('closed', true) END
)::text
WHERE opening_hours IS NOT NULL 
  AND opening_hours <> '' 
  AND opening_hours_json IS NULL;
