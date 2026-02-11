-- V34: Update TCG type check constraints to include POKEMON_JAPAN
-- Update user_stats favoritetcgtype, users favorite_game, and user_favorite_tcgs tcg_type constraints

DO $$
DECLARE
    tcg_type_values TEXT := '''POKEMON'', ''ONE_PIECE'', ''MAGIC'', ''YUGIOH'', ''DIGIMON'', ''LORCANA'', ''RIFTBOUND'', ''UNION_ARENA'', ''DRAGON_BALL_SUPER_FUSION_WORLD'', ''FLESH_AND_BLOOD'', ''POKEMON_JAPAN''';
BEGIN
    -- Update user_stats table favoritetcgtype check constraint
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stats') THEN
        ALTER TABLE user_stats DROP CONSTRAINT IF EXISTS user_stats_favoritetcgtype_check;
        EXECUTE 'ALTER TABLE user_stats ADD CONSTRAINT user_stats_favoritetcgtype_check CHECK (favoritetcgtype IN (' || tcg_type_values || '))';
    END IF;

    -- Update users table favorite_game check constraint
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS users_favorite_game_check;
        EXECUTE 'ALTER TABLE users ADD CONSTRAINT users_favorite_game_check CHECK (favorite_game IS NULL OR favorite_game IN (' || tcg_type_values || '))';
    END IF;

    -- Update user_favorite_tcgs table tcg_type check constraint
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_favorite_tcgs') THEN
        ALTER TABLE user_favorite_tcgs DROP CONSTRAINT IF EXISTS user_favorite_tcgs_tcg_type_check;
        EXECUTE 'ALTER TABLE user_favorite_tcgs ADD CONSTRAINT user_favorite_tcgs_tcg_type_check CHECK (tcg_type IN (' || tcg_type_values || '))';
    END IF;
END $$;