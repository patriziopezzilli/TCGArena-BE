-- V35: Update user_activities activity_type check constraint to include all ActivityType values

DO $$
DECLARE
    activity_type_values TEXT := '''CARD_ADDED_TO_COLLECTION'', ''CARD_REMOVED_FROM_COLLECTION'', ''DECK_CREATED'', ''DECK_UPDATED'', ''DECK_DELETED'', ''TOURNAMENT_JOINED'', ''TOURNAMENT_LEFT'', ''TOURNAMENT_WON'', ''TOURNAMENT_LOST'', ''SHOP_FAVORITED'', ''SHOP_UNFAVORITED'', ''USER_REGISTERED'', ''USER_PROFILE_UPDATED'', ''USER_PREFERENCES_UPDATED'', ''CARD_TRADE_INITIATED'', ''CARD_TRADE_COMPLETED'', ''POINTS_EARNED'', ''REWARD_REDEEMED'', ''DECK_LIKED'', ''PROFILE_APPRECIATED''';
BEGIN
    -- Update user_activities table activity_type check constraint
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_activities') THEN
        ALTER TABLE user_activities DROP CONSTRAINT IF EXISTS user_activities_activity_type_check;
        EXECUTE 'ALTER TABLE user_activities ADD CONSTRAINT user_activities_activity_type_check CHECK (activity_type IN (' || activity_type_values || '))';
    END IF;
END $$;