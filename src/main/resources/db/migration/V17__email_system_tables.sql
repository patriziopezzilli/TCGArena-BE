-- Email System Tables
-- PostgreSQL Migration
-- Run this migration to add email tracking and preferences

-- User Email Preferences
CREATE TABLE IF NOT EXISTS user_email_preferences (
    user_id BIGINT PRIMARY KEY,
    welcome_email BOOLEAN DEFAULT TRUE,
    password_reset BOOLEAN DEFAULT TRUE,
    trade_notifications BOOLEAN DEFAULT TRUE,
    event_notifications BOOLEAN DEFAULT TRUE,
    tournament_notifications BOOLEAN DEFAULT TRUE,
    reservation_confirmations BOOLEAN DEFAULT TRUE,
    security_alerts BOOLEAN DEFAULT TRUE,
    shop_notifications BOOLEAN DEFAULT TRUE,
    inactivity_reminders BOOLEAN DEFAULT TRUE,
    import_summaries BOOLEAN DEFAULT FALSE,
    daily_digest BOOLEAN DEFAULT FALSE,
    marketing_emails BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_email_prefs FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Trigger for updated_at in user_email_preferences
CREATE OR REPLACE FUNCTION update_user_email_preferences_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_user_email_preferences_updated_at
    BEFORE UPDATE ON user_email_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_user_email_preferences_updated_at();

-- Import History
CREATE TABLE IF NOT EXISTS import_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    import_type VARCHAR(50) NOT NULL, -- 'JUSTTCG', 'CSV', 'MANUAL'
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL, -- 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'
    total_cards_processed INTEGER DEFAULT 0,
    cards_added INTEGER DEFAULT 0,
    cards_updated INTEGER DEFAULT 0,
    cards_skipped INTEGER DEFAULT 0,
    errors INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_import ON import_history(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_status ON import_history(status);

-- Import Deltas (card changes)
CREATE TABLE IF NOT EXISTS import_deltas (
    id BIGSERIAL PRIMARY KEY,
    import_history_id BIGINT NOT NULL,
    card_template_id BIGINT NOT NULL,
    card_name VARCHAR(255),
    set_name VARCHAR(255),
    quantity_before INTEGER DEFAULT 0,
    quantity_after INTEGER NOT NULL,
    change_type VARCHAR(20) NOT NULL, -- 'ADDED', 'INCREASED', 'DECREASED'
    CONSTRAINT fk_import_deltas_history FOREIGN KEY (import_history_id) REFERENCES import_history(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_import_history ON import_deltas(import_history_id);

-- User Daily Stats
CREATE TABLE IF NOT EXISTS user_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    new_cards INTEGER DEFAULT 0,
    new_trades INTEGER DEFAULT 0,
    completed_trades INTEGER DEFAULT 0,
    messages_received INTEGER DEFAULT 0,
    profile_views INTEGER DEFAULT 0,
    events_nearby INTEGER DEFAULT 0,
    collection_value_change DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_daily_stats_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_date UNIQUE (user_id, stat_date)
);

CREATE INDEX IF NOT EXISTS idx_user_date ON user_daily_stats(user_id, stat_date);

-- Platform Daily Stats
CREATE TABLE IF NOT EXISTS platform_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    stat_date DATE NOT NULL UNIQUE,
    new_users INTEGER DEFAULT 0,
    active_trades INTEGER DEFAULT 0,
    completed_trades INTEGER DEFAULT 0,
    upcoming_events INTEGER DEFAULT 0,
    new_shops INTEGER DEFAULT 0,
    total_cards_added INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stat_date ON platform_daily_stats(stat_date);

-- Email Verification Tokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_token ON email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_user_verified ON email_verification_tokens(user_id, verified);

-- Security Login History (for device detection)
CREATE TABLE IF NOT EXISTS user_login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    location_country VARCHAR(100),
    location_city VARCHAR(100),
    is_new_device BOOLEAN DEFAULT FALSE,
    success BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_user_login_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_login ON user_login_history(user_id, login_time);
CREATE INDEX IF NOT EXISTS idx_device ON user_login_history(user_id, device_fingerprint);
