-- Create shop_suggestion table for user-submitted shop suggestions
CREATE TABLE shop_suggestion (
    id BIGSERIAL PRIMARY KEY,
    shop_name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shop_suggestion_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_shop_suggestion_status CHECK (status IN ('PENDING', 'CONTACTED', 'REJECTED'))
);

-- Create indexes for common queries
CREATE INDEX idx_shop_suggestion_status ON shop_suggestion(status);
CREATE INDEX idx_shop_suggestion_user_id ON shop_suggestion(user_id);
CREATE INDEX idx_shop_suggestion_created_at ON shop_suggestion(created_at DESC);

-- Add comment to table
COMMENT ON TABLE shop_suggestion IS 'User-submitted suggestions for new shops to be added to the platform';
COMMENT ON COLUMN shop_suggestion.status IS 'Workflow status: PENDING (new suggestion), CONTACTED (shop contacted by admin), REJECTED (not suitable)';
