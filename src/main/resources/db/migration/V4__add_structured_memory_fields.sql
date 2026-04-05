-- Add structured fields to episodic_memory for ranked retrieval and usage tracking.
-- The embedding column from V1 is intentionally left unchanged (populated in v0.5).

ALTER TABLE episodic_memory
    ADD COLUMN IF NOT EXISTS memory_type   VARCHAR(50)  NOT NULL DEFAULT 'INTERACTION',
    ADD COLUMN IF NOT EXISTS importance    INT          NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS source        VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS last_recalled_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS recall_count  INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS metadata      JSONB        NULL;

-- Backfill source for rows that existed before this migration.
UPDATE episodic_memory SET source = 'system' WHERE source IS NULL;

-- Index optimised for the retrieval ranking order used by MemoryRetrievalService.
CREATE INDEX IF NOT EXISTS idx_episodic_memory_ranking
    ON episodic_memory (importance DESC, last_recalled_at DESC NULLS LAST, created_at DESC);
