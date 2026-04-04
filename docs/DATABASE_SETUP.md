# Database Setup

## Prerequisites

PostgreSQL with the `pgvector` extension enabled. Create the extension once:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

## Flyway migrations

Migrations live in `src/main/resources/db/migration` and run automatically on startup.

To run manually:

```bash
mvn -Dflyway.url=${DATABASE_URL} \
    -Dflyway.user=${DATABASE_USER} \
    -Dflyway.password=${DATABASE_PASSWORD} \
    flyway:migrate
```

### Migration files

| File | Purpose |
|------|---------|
| `V1__init_memory_tables.sql` | Creates `memory_candidate`, `episodic_memory`, and the `vector` extension |
| `V2__add_indexes_for_candidate_summary.sql` | Index for exact pending-summary duplicate checks |
| `V3__add_pg_trgm_and_trigram_index.sql` | Enables `pg_trgm`, adds GIN index for fuzzy duplicate detection on `lower(summary)` |

## V3 — pg_trgm (fuzzy duplicate detection)

`V3` is optional. The system works without it (falls back to exact-match duplicate checks), but with it you get faster fuzzy similarity searches.

Some managed Postgres providers require elevated permissions for `CREATE EXTENSION`. If yours blocks it, skip V3 — the application still runs correctly.

### Checking if pg_trgm is available

```sql
SELECT extname FROM pg_extension WHERE extname = 'pg_trgm';
-- or in psql: \dx
```

### Enabling pg_trgm manually

```bash
# psql (local or direct connection)
psql "$DATABASE_URL" -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

# Docker container
docker exec -it <pg-container> psql -U <user> -d <db> -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

# Heroku
heroku pg:psql -a <app-name> -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
```

If your provider blocks `CREATE EXTENSION`, ask your DBA or open a support ticket with them. RDS, Cloud SQL, and Azure Database for PostgreSQL all support `pg_trgm` but may require enabling it through their console rather than via SQL.

### Verifying the trigram index

```sql
SELECT indexname
FROM pg_indexes
WHERE tablename = 'memory_candidate'
  AND indexname = 'idx_memory_candidate_summary_trgm';
```

## Similarity threshold

Fuzzy duplicate detection uses a configurable similarity threshold (default: `0.45`).

Set via Spring property `memory.duplicate.similarity.threshold` or environment variable:

```bash
export MEMORY_DUPLICATE_SIMILARITY_THRESHOLD=0.5
```

## Environment variables

| Variable | Example | Purpose |
|----------|---------|---------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/cognitive_ai` | JDBC connection URL |
| `DATABASE_USER` | `postgres` | DB username |
| `DATABASE_PASSWORD` | `postgres` | DB password |
