-- Update customer_requests type constraint to match new enum values
ALTER TABLE customer_requests 
DROP CONSTRAINT IF EXISTS customer_requests_type_check;

ALTER TABLE customer_requests 
ADD CONSTRAINT customer_requests_type_check 
CHECK (type IN ('AVAILABILITY', 'EVALUATION', 'SELL', 'BUY', 'TRADE', 'GENERAL'));
