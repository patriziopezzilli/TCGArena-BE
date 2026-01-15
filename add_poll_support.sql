-- Add thread_type column to community_threads table
ALTER TABLE community_threads ADD COLUMN thread_type VARCHAR(20) NOT NULL DEFAULT 'DISCUSSION';

-- Create poll_options table
CREATE TABLE poll_options (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (thread_id) REFERENCES community_threads(id) ON DELETE CASCADE
);

-- Create poll_votes table
CREATE TABLE poll_votes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    poll_option_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (poll_option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    UNIQUE (user_id, poll_option_id)
);

-- Create indexes for better performance
CREATE INDEX idx_poll_options_thread_id ON poll_options(thread_id);
CREATE INDEX idx_poll_votes_user_id ON poll_votes(user_id);
CREATE INDEX idx_poll_votes_poll_option_id ON poll_votes(poll_option_id);