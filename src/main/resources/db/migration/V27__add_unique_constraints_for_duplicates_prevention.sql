-- Migration to add unique constraints for preventing duplicates in import
-- Author: Copilot
-- Date: 2025-12-20

-- Add unique constraint on Expansion (title, tcg_type)
-- This prevents duplicate expansions for the same TCG type
ALTER TABLE expansions 
ADD CONSTRAINT uk_expansion_title_tcg_type 
UNIQUE (title, tcg_type);

-- Add unique constraint on CardTemplate (name, set_code, card_number)
-- This prevents duplicate card templates with same name, set, and number
ALTER TABLE card_templates 
ADD CONSTRAINT uk_card_template_name_set_number 
UNIQUE (name, set_code, card_number);

-- Note: TCGSet already has unique constraint on set_code (defined in model)
