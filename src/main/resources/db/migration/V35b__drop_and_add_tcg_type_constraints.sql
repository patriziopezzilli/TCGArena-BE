-- V35b: Drop and re-add only existing tcg_type check constraints (expansions, tcg_sets, card_templates, decks, tournaments, users, user_stats, user_favorite_tcgs)
-- Questo script elimina e riaggiunge i constraint solo sulle tabelle che sicuramente esistono in produzione.

ALTER TABLE expansions DROP CONSTRAINT IF EXISTS expansions_tcg_type_check;
ALTER TABLE expansions ADD CONSTRAINT expansions_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE tcg_sets DROP CONSTRAINT IF EXISTS tcg_sets_tcg_type_check;
ALTER TABLE tcg_sets ADD CONSTRAINT tcg_sets_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE card_templates DROP CONSTRAINT IF EXISTS card_templates_tcg_type_check;
ALTER TABLE card_templates ADD CONSTRAINT card_templates_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE decks DROP CONSTRAINT IF EXISTS decks_tcg_type_check;
ALTER TABLE decks ADD CONSTRAINT decks_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE tournaments DROP CONSTRAINT IF EXISTS tournaments_tcg_type_check;
ALTER TABLE tournaments ADD CONSTRAINT tournaments_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_favorite_game_check;
ALTER TABLE users ADD CONSTRAINT users_favorite_game_check CHECK (favorite_game IS NULL OR favorite_game IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE user_stats DROP CONSTRAINT IF EXISTS user_stats_favoritetcgtype_check;
ALTER TABLE user_stats ADD CONSTRAINT user_stats_favoritetcgtype_check CHECK (favoritetcgtype IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));

ALTER TABLE user_favorite_tcgs DROP CONSTRAINT IF EXISTS user_favorite_tcgs_tcg_type_check;
ALTER TABLE user_favorite_tcgs ADD CONSTRAINT user_favorite_tcgs_tcg_type_check CHECK (tcg_type IN ('POKEMON', 'ONE_PIECE', 'MAGIC', 'YUGIOH', 'DIGIMON', 'LORCANA', 'RIFTBOUND', 'UNION_ARENA', 'DRAGON_BALL_SUPER_FUSION_WORLD', 'FLESH_AND_BLOOD', 'POKEMON_JAPAN'));
