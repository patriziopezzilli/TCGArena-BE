-- V33: Create user_rating_streak table for tracking daily rating streaks
CREATE TABLE user_rating_streak (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    total_rating_days INT NOT NULL DEFAULT 0,
    total_votes INT NOT NULL DEFAULT 0,
    streak_breaks INT NOT NULL DEFAULT 0,
    last_rating_date DATE,
    streak_start_date DATE,
    CONSTRAINT user_rating_streak_user_unique UNIQUE(user_id)
);

-- Index for efficient lookups by user
CREATE INDEX idx_user_rating_streak_user_id ON user_rating_streak(user_id);
