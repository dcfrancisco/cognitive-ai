-- Add indexes to improve lookup performance for candidate summary checks
-- Index on (status, summary) speeds up queries like:
--   SELECT 1 FROM memory_candidate WHERE status = 'PENDING' AND summary = ? LIMIT 1

CREATE INDEX IF NOT EXISTS idx_memory_candidate_status_summary ON memory_candidate (status, summary);

-- If text comparisons become more fuzzy in the future, consider adding
-- a trigram GIN index on lower(summary) for similarity searches:
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_memory_candidate_summary_trgm ON memory_candidate USING gin (lower(summary) gin_trgm_ops) WHERE status = 'PENDING';
