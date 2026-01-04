-- Add agreementReached column to chat_conversations table
-- This column tracks whether a completed trade was successful (true) or closed without agreement (false)
-- NULL values indicate active trades that haven't been completed yet

ALTER TABLE chat_conversations 
ADD COLUMN IF NOT EXISTS agreement_reached BOOLEAN DEFAULT NULL;

-- Optional: Set existing completed conversations to true (assuming they were successful before this feature)
-- Uncomment the following line if you want to mark all existing completed trades as successful:
-- UPDATE chat_conversations SET agreement_reached = true WHERE status = 'COMPLETED' AND type = 'TRADE';

COMMENT ON COLUMN chat_conversations.agreement_reached IS 'TRUE if trade completed successfully, FALSE if closed without agreement, NULL if active or not applicable';
