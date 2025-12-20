-- Script to check for existing duplicates before applying V27 migration
-- Run this BEFORE applying the unique constraint migration
-- Author: Copilot
-- Date: 2025-12-20

-- 1. Check for duplicate Expansions (same title and tcg_type)
SELECT 
    title, 
    tcg_type, 
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id) as expansion_ids
FROM expansions
GROUP BY title, tcg_type
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC;

-- 2. Check for duplicate CardTemplates (same name, set_code, card_number)
SELECT 
    name, 
    set_code, 
    card_number,
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id) as card_template_ids
FROM card_templates
GROUP BY name, set_code, card_number
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC;

-- 3. If duplicates are found, you can clean them up with these queries:

-- Clean duplicate Expansions (keeps the oldest one based on ID)
-- CAUTION: Review the duplicates first before running this!
-- DELETE e1 FROM expansions e1
-- INNER JOIN expansions e2 
-- WHERE e1.title = e2.title 
--   AND e1.tcg_type = e2.tcg_type 
--   AND e1.id > e2.id;

-- Clean duplicate CardTemplates (keeps the oldest one based on ID)
-- CAUTION: Review the duplicates first before running this!
-- DELETE ct1 FROM card_templates ct1
-- INNER JOIN card_templates ct2 
-- WHERE ct1.name = ct2.name 
--   AND ct1.set_code = ct2.set_code 
--   AND ct1.card_number = ct2.card_number 
--   AND ct1.id > ct2.id;

-- 4. After cleaning duplicates, you can safely apply V27 migration
