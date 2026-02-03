-- Update tcg_type check constraints to include all new TCG types
-- New types: RIFTBOUND, UNION_ARENA, DRAGON_BALL_SUPER_FUSION_WORLD, FLESH_AND_BLOOD

-- Full list of TCG types:
-- POKEMON, ONE_PIECE, MAGIC, YUGIOH, DIGIMON, LORCANA, RIFTBOUND, UNION_ARENA, DRAGON_BALL_SUPER_FUSION_WORLD, FLESH_AND_BLOOD

DO $$
DECLARE
    tcg_type_values TEXT := '''POKEMON'', ''ONE_PIECE'', ''MAGIC'', ''YUGIOH'', ''DIGIMON'', ''LORCANA'', ''RIFTBOUND'', ''UNION_ARENA'', ''DRAGON_BALL_SUPER_FUSION_WORLD'', ''FLESH_AND_BLOOD''';
BEGIN
    -- 1. expansions table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'expansions') THEN
        ALTER TABLE expansions DROP CONSTRAINT IF EXISTS expansions_tcg_type_check;
        EXECUTE 'ALTER TABLE expansions ADD CONSTRAINT expansions_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 2. tcg_sets table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tcg_sets') THEN
        ALTER TABLE tcg_sets DROP CONSTRAINT IF EXISTS tcg_sets_tcg_type_check;
        EXECUTE 'ALTER TABLE tcg_sets ADD CONSTRAINT tcg_sets_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 3. card_templates table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'card_templates') THEN
        ALTER TABLE card_templates DROP CONSTRAINT IF EXISTS card_templates_tcg_type_check;
        EXECUTE 'ALTER TABLE card_templates ADD CONSTRAINT card_templates_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 4. import_progress table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'import_progress') THEN
        ALTER TABLE import_progress DROP CONSTRAINT IF EXISTS import_progress_tcg_type_check;
        EXECUTE 'ALTER TABLE import_progress ADD CONSTRAINT import_progress_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 5. community_pulls table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'community_pulls') THEN
        ALTER TABLE community_pulls DROP CONSTRAINT IF EXISTS community_pulls_tcg_type_check;
        EXECUTE 'ALTER TABLE community_pulls ADD CONSTRAINT community_pulls_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 6. community_threads table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'community_threads') THEN
        ALTER TABLE community_threads DROP CONSTRAINT IF EXISTS community_threads_tcg_type_check;
        EXECUTE 'ALTER TABLE community_threads ADD CONSTRAINT community_threads_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 7. community_events table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'community_events') THEN
        ALTER TABLE community_events DROP CONSTRAINT IF EXISTS community_events_tcg_type_check;
        EXECUTE 'ALTER TABLE community_events ADD CONSTRAINT community_events_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 8. decks table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'decks') THEN
        ALTER TABLE decks DROP CONSTRAINT IF EXISTS decks_tcg_type_check;
        EXECUTE 'ALTER TABLE decks ADD CONSTRAINT decks_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 9. tournaments table
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tournaments') THEN
        ALTER TABLE tournaments DROP CONSTRAINT IF EXISTS tournaments_tcg_type_check;
        EXECUTE 'ALTER TABLE tournaments ADD CONSTRAINT tournaments_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;

    -- 10. users table - favorite_game column (nullable)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_favorite_game_check;
        EXECUTE 'ALTER TABLE users ADD CONSTRAINT users_favorite_game_check CHECK (favorite_game IS NULL OR favorite_game IN (' || tcg_type_values || '))';
    END IF;
END $$;
