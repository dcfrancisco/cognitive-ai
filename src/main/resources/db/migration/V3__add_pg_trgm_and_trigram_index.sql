-- Install pg_trgm extension (if permitted) and create a GIN trigram index
-- This enables similarity(...) and faster fuzzy lookups for candidate summaries.

-- Create extension (requires superuser privileges in some hosted environments).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create a GIN trigram index on the lower(summary) for pending candidates only
CREATE INDEX IF NOT EXISTS idx_memory_candidate_summary_trgm
    ON memory_candidate USING gin (lower(summary) gin_trgm_ops)
    WHERE status = 'PENDING';

-- Note: Some managed Postgres services restrict CREATE EXTENSION; if you cannot
-- create pg_trgm in your environment, the similarity-based check will gracefully
-- fall back to exact-match checks only. See README for migration guidance.
