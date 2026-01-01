-- Script per creare la tabella community_pulls
-- Esegui questo script sul database PostgreSQL

CREATE TABLE IF NOT EXISTS community_pulls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tcg_type VARCHAR(50) NOT NULL,
    image_base64 TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_community_pulls_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indice per migliorare le performance delle query
CREATE INDEX IF NOT EXISTS idx_community_pulls_user_id ON community_pulls(user_id);
CREATE INDEX IF NOT EXISTS idx_community_pulls_created_at ON community_pulls(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_community_pulls_tcg_type ON community_pulls(tcg_type);

-- Tabella per i like
CREATE TABLE IF NOT EXISTS pull_likes (
    id BIGSERIAL PRIMARY KEY,
    pull_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pull_likes_pull FOREIGN KEY (pull_id) REFERENCES community_pulls(id) ON DELETE CASCADE,
    CONSTRAINT fk_pull_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_pull_user_like UNIQUE (pull_id, user_id)
);

-- Indice per i like
CREATE INDEX IF NOT EXISTS idx_pull_likes_pull_id ON pull_likes(pull_id);
CREATE INDEX IF NOT EXISTS idx_pull_likes_user_id ON pull_likes(user_id);
