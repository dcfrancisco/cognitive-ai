-- pgvector extension for episodic memory embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Working memory is intentionally NOT persisted.

-- Memory candidates are reviewed/curated before being accepted into episodic memory.
CREATE TABLE IF NOT EXISTS memory_candidate (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source TEXT NOT NULL,
    summary TEXT NOT NULL,
    rationale TEXT NOT NULL,
    tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    reviewer_note TEXT NULL
);

-- Episodic memory stores meaning (summaries) and an optional embedding.
-- The embedding can be populated later once provider + policy is ready.
CREATE TABLE IF NOT EXISTS episodic_memory (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    summary TEXT NOT NULL,
    rationale TEXT NOT NULL,
    tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    embedding vector(1536) NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_candidate_status_created_at ON memory_candidate (status, created_at DESC);
