# Cognitive AI

![Java](https://img.shields.io/badge/java-21-blue)
![License](https://img.shields.io/badge/license-MIT-green)

A cognition-first ambient AI framework that observes context, decides whether to speak, routes intent to specialized agents, and remembers selectively through curated memory.

This project is not designed as a conventional chatbot. It explores how AI can behave more like a restrained partner: aware of context, capable of silence, deliberate about memory, and explainable in how it acts.

## Current status

Early-stage but runnable foundation.

What works now:
- Observation intake via REST API
- Working memory capture
- Curated memory candidate pipeline
- Rule-based should-speak decision
- Intent routing
- Agent orchestration
- Explainable JSON response showing:
  - decision
  - confidence
  - intent
  - selected agent
  - reasons

What is planned next:
- episodic recall beyond working memory
- semantic memory with embeddings
- LLM-assisted summarization and reflection
- voice-first loop (STT → cognition → TTS)

## Core ideas

- **Cognition before interaction**  
  Decide whether to speak before generating a reply.

- **Curated memory, not raw logging**  
  Store meaning selectively. Do not persist everything by default.

- **Explainable behavior**  
  Every intervention should have a reason.

- **Partner stance**  
  The system avoids command-style, judgmental, or owner/slave behavior.

- **Modular cognition**  
  Perception, decision, memory, routing, and agents stay separate and auditable.

## Architecture

Implemented request flow:

Observation intake (`POST /api/observe`) → `CuratedMemoryService` (working memory + candidate curation) → `DecisionEngine` (`ShouldSpeakPolicy` + `IntentRouter`) → `AgentOrchestrator` → selected agent response

Long-term memory is review-first: observations may become pending `memory_candidate` records, and only accepted candidates are materialized into `episodic_memory`.

Current behavior is intentionally rule-based and explainable. Live recall is grounded in recent working memory today; episodic retrieval and model-assisted cognition remain extension points.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full component diagram and runtime details.

## Agent layer (current)

* **MemoryCaptureAgent** — handles explicit “remember this” style observations.
* **MemoryRecallAgent** — answers simple recall requests from recent working memory.
* **ReflectionAgent** — produces restrained reflective responses for questions and general prompts.

## Tech stack

* Java 21
* Spring Boot
* Spring Validation
* Spring JDBC
* PostgreSQL
* pgvector
* Flyway
* Spring AI

## Run locally

### Prerequisites

* Java 21
* Maven
* PostgreSQL with `pgvector` extension

### Database

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Run DB migrations

- Flyway migrations live in `src/main/resources/db/migration`.
- Spring Boot will run Flyway automatically on startup if the database is reachable.
- To run migrations manually with Maven (example):

```bash
mvn -Dflyway.url=${DATABASE_URL} -Dflyway.user=${DATABASE_USER} -Dflyway.password=${DATABASE_PASSWORD} flyway:migrate
```

Additional migration notes
-------------------------

This project includes an optional migration that enables Postgres trigram fuzzy matching (pg_trgm) and creates a GIN trigram index to speed up similarity searches for memory candidate summaries:

- `V3__add_pg_trgm_and_trigram_index.sql` — installs `pg_trgm` and creates a GIN index on `lower(summary)` for rows where `status = 'PENDING'`.

Some managed Postgres providers require elevated permissions to run `CREATE EXTENSION`. If your hosting provider blocks `CREATE EXTENSION pg_trgm`, you can either ask your DBA/operator to enable it or skip applying V3 — the system will still work using exact-match checks but without fast fuzzy matching.

Configuration: similarity threshold
-----------------------------------

Fuzzy duplicate detection uses a configurable similarity threshold. Default is `0.45`.

Set via Spring property `memory.duplicate.similarity.threshold` or environment variable `MEMORY_DUPLICATE_SIMILARITY_THRESHOLD` (e.g. `0.55` for stricter matching).

Example env var:

```bash
export MEMORY_DUPLICATE_SIMILARITY_THRESHOLD=0.5
```

Applying V3 on managed Postgres — step-by-step
----------------------------------------------

1. Verify whether `pg_trgm` is already available

  Connect to the database (use `psql`, provider CLI, or your DB admin tools) and run:

  ```sql
  -- shows installed extensions
  SELECT extname FROM pg_extension WHERE extname = 'pg_trgm';
  -- or list all extensions in psql: \dx
  ```

2. If `pg_trgm` is available, apply the V3 migration

  - Let the app run migrations automatically on startup, or run Flyway manually:

  ```bash
  mvn -Dflyway.url=${DATABASE_URL} -Dflyway.user=${DATABASE_USER} -Dflyway.password=${DATABASE_PASSWORD} flyway:migrate
  ```

3. If `pg_trgm` is not available or `CREATE EXTENSION` fails

  - Many managed Postgres providers allow `CREATE EXTENSION pg_trgm`; some require operator intervention.
  - Example commands you (or your DBA) can run (replace connection/instance placeholders):

  ```bash
  # psql (local or direct connection)
  psql "$DATABASE_URL" -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

  # Docker (when Postgres runs in a container)
  docker exec -it <pg-container> psql -U <user> -d <db> -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"

  # Heroku example
  heroku pg:psql -a <app-name> -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
  ```

  - If you cannot run `CREATE EXTENSION` due to provider restrictions, open a support ticket or ask your DBA to enable `pg_trgm` for your database instance. The application will continue to work without V3, but fuzzy duplicate detection will be disabled and only exact-match checks will run.

4. Verify the trigram index exists

  ```sql
  SELECT indexname
  FROM pg_indexes
  WHERE tablename = 'memory_candidate' AND indexname = 'idx_memory_candidate_summary_trgm';
  ```

5. Adjust similarity threshold if desired

  - Tune `memory.duplicate.similarity.threshold` via `application.yml` or environment variable `MEMORY_DUPLICATE_SIMILARITY_THRESHOLD` and restart the app.

If you'd like, I can add a short troubleshooting section for common provider errors (RDS/GCP/Azure/Heroku) or create a small Testcontainers-based integration test to validate fuzzy detection locally.


### Environment variables

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/cognitive_ai
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
```

### Start

```bash
mvn spring-boot:run
```

## Example API

### Observe

```bash
curl -X POST http://localhost:8080/api/observe \
  -H "Content-Type: application/json" \
  -d '{
    "source": "user",
    "content": "Please remember that I prefer meetings after 10am",
    "explicitRemember": true
  }'
```

### Example response

```json
{
  "decision": "SPEAK",
  "confidence": 0.9,
  "intent": "MEMORY_CAPTURE",
  "agent": "MemoryCaptureAgent",
  "message": "I’ll treat that as something worth remembering and reviewing.",
  "reasons": [
    "Explicit remember request present",
    "Intent routed to MEMORY_CAPTURE",
    "Observation was routed to memory capture",
    "This supports curated memory rather than raw logging"
  ]
}
```

### Review pending memory candidates

```bash
curl http://localhost:8080/api/memory/candidates
```

### Accept a memory candidate

```bash
curl -X POST http://localhost:8080/api/memory/candidates/<candidate-id>/accept \
  -H "Content-Type: application/json" \
  -d '{
    "note": "Looks useful for long-term memory"
  }'
```

### Reject a memory candidate

```bash
curl -X POST http://localhost:8080/api/memory/candidates/<candidate-id>/reject \
  -H "Content-Type: application/json" \
  -d '{
    "note": "Too transient to keep"
  }'
```

## Why this repo is called Cognitive AI

This project aims to demonstrate a minimal cognitive loop:

* observe
* decide
* route
* act
* remember selectively

It is still early-stage, but it already goes beyond plain request/response by separating:

* should the system respond at all?
* what kind of intent is present?
* which agent should handle it?
* what should be remembered?

## Roadmap

* richer intent classification
* episodic memory retrieval
* semantic memory search
* values-and-boundaries enforcement in orchestration
* voice-first interface
* LLM-backed reflection and summarization
* policy-driven memory acceptance

## Contribution

Small, auditable pull requests are preferred.

Good contributions:

* clearer routing rules
* better tests
* safer memory policies
* stronger explainability
* improved recall behavior

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.