-- V36: Fix shop_reviews table - change id to bigint and add created_at column

DO $$
BEGIN
    -- Change the id column from integer to bigint
    ALTER TABLE shop_reviews ALTER COLUMN id TYPE bigint;

    -- Update the sequence to handle bigint if it exists
    -- PostgreSQL serial creates a sequence named table_column_seq
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'shop_reviews_id_seq') THEN
        -- Change sequence to bigint
        ALTER SEQUENCE shop_reviews_id_seq AS bigint;
    END IF;

    -- Add created_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'shop_reviews' AND column_name = 'created_at') THEN
        ALTER TABLE shop_reviews ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
END $$;