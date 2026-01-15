-- Add release_date_modified_manually column to preserve manual date changes during batch imports
ALTER TABLE tcg_sets ADD COLUMN release_date_modified_manually BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing records to have the default value
UPDATE tcg_sets SET release_date_modified_manually = FALSE WHERE release_date_modified_manually IS NULL;