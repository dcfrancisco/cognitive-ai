# Architecture

This document describes the architecture implemented in the current codebase. It focuses on the `ph.francisco` Spring Boot application under `src/main/java` and calls out where the design is intentionally rule-based today versus where the project is prepared for future model-assisted behavior.

## Overview
- Purpose: an ambient cognitive companion that accepts observations, decides whether to respond, routes the interaction to a cognitive agent, and curates long-term memory through human review.
- Style: modular monolith built with Spring Boot, Spring MVC, Spring JDBC, Flyway, and PostgreSQL.
- Current runtime character: rule-based cognition and routing, in-process working memory, database-backed candidate review and episodic memory.
- Extension points: agent implementations, should-speak policy, intent routing heuristics, and optional model-assisted summarization or retrieval.

## Implemented package map
- `ph.francisco.interfaceadapters` — HTTP controllers and demo page.
- `ph.francisco.perception` — inbound observation shape.
- `ph.francisco.cognition` — should-speak policy and decision engine.
- `ph.francisco.agents` — intent routing, orchestrator, and agent implementations.
- `ph.francisco.memory` — working memory, curation logic, candidate persistence, episodic persistence.
- `ph.francisco.config` — Spring Boot configuration support, including conditional environment setup for provider configuration.
- `ph.francisco.values` — shared value objects.

## Multimodal perception layer
- **Multimodal Perception Layer** introduces multiple sensors that capture input from different modalities.
  - `TextSensor`
  - `AudioSensor`
  - `VisionSensor`
- **Perception Normalizer / Adapter** converts every sensor signal into the unified `Observation` model.
  - Audio -> speech-to-text -> `Observation`
  - Vision -> caption/detection -> `Observation`
  - Text -> direct `Observation`

### Multimodal perception flow (normalized before cognition)
```
TextSensor  \
AudioSensor  >-- PerceptionNormalizer --> Observation
VisionSensor /

Observation -> ShouldSpeakPolicy -> IntentRouter -> AgentOrchestrator -> AgentResponse -> Memory
```

## Runtime component diagram

```mermaid
flowchart TD
  subgraph HTTP[HTTP / MVC layer]
    OC[ObservationController\nPOST /api/observe]
    MRC[MemoryReviewController\nreview endpoints]
    DMC[DemoMemoryController\nGET /api/demo/working-memory]
    DPC[DemoPageController\nGET /demo]
  end

  subgraph COG[Cognition layer]
    DE[DecisionEngine]
    SSP[ShouldSpeakPolicy\nRuleBasedShouldSpeakPolicy]
    IR[IntentRouter]
  end

  subgraph AGENTS[Agent layer]
    AO[AgentOrchestrator]
    MCA[MemoryCaptureAgent]
    MRA[MemoryRecallAgent]
    RA[ReflectionAgent]
  end

  subgraph MEMORY[Memory layer]
    CMS[CuratedMemoryService]
    WM[WorkingMemory\nin-process ring buffer]
    MCR[MemoryCandidateRepository]
    EMR[EpisodicMemoryRepository]
  end

  subgraph DB[PostgreSQL]
    MC[(memory_candidate)]
    EM[(episodic_memory)]
    TRG[pg_trgm index on lower(summary)]
    VEC[pgvector embedding column]
  end

  OC --> CMS
  OC --> DE
  DE --> SSP
  DE --> IR
  DE --> AO
  AO --> MCA
  AO --> MRA
  AO --> RA

  CMS --> WM
  CMS --> MCR
  MRC --> CMS
  DMC --> CMS

  MCR --> MC
  EMR --> EM
  CMS -->|accept candidate| EMR
  TRG --> MC
  VEC --> EM
```

## Request and decision flow
0. Sensors or external callers submit signals that are normalized into an `Observation` by the Perception Normalizer.
1. `ObservationController` accepts `POST /api/observe` with an `Observation` payload.
2. The controller first calls `CuratedMemoryService.observe(...)`.
   - The observation is appended to `WorkingMemory`.
   - The service decides whether to create a `memory_candidate` for review.
3. The controller then calls `DecisionEngine.evaluate(...)`.
   - `RuleBasedShouldSpeakPolicy` decides whether the system should `SPEAK` or `SILENCE`.
   - `IntentRouter` classifies the observation into a `CognitiveIntent`.
4. If the decision is `SILENCE`, the controller returns HTTP `204 No Content`.
5. If the decision is `SPEAK`, `AgentOrchestrator` selects the first `CognitiveAgent` that supports the routed intent.
6. The selected agent returns an `AgentResponse`, and the controller responds with JSON containing decision, confidence, intent, agent name, message, and merged reasoning.

## Why multimodal perception
- Multimodal perception matters because context arrives through text, audio, and vision; all are needed for complete situational awareness.
- Normalization is required so cognition operates on a single `Observation` model, keeping decisions explainable and auditable.
- Sensors must not bypass the cognitive loop; every signal must go through `ShouldSpeakPolicy`, `IntentRouter`, and the agent layer to preserve the cognition-first philosophy.

## Memory architecture

### 1. Working memory
- Implemented by `WorkingMemory` and owned by `CuratedMemoryService`.
- Stored in process only; it is intentionally not persisted.
- Used for short-horizon context and demo inspection.
- Current constructor capacity is `25` observations.

### 2. Candidate memory (review queue)
- Backed by the `memory_candidate` table.
- A candidate is created only when one of these conditions is met:
  - `explicitRemember == true`, or
  - the normalized observation content recurs at least 3 times within the recent sliding window.
- Before insert, `CuratedMemoryService` prevents duplicates by:
  - exact-match lookup on pending summaries, and
  - fuzzy similarity lookup with PostgreSQL `pg_trgm` when enabled.
- Candidate status is one of `PENDING`, `ACCEPTED`, or `REJECTED`.

### 3. Episodic memory
- Backed by the `episodic_memory` table.
- Materialized only when a reviewer accepts a pending candidate.
- Stores curated summary, rationale, tags, and an optional `vector(1536)` embedding column.
- In the current implementation, episodic memory is written to but not yet used as a live retrieval source in the request path.

## Review workflow
- `GET /api/memory/candidates` lists pending candidates.
- `POST /api/memory/candidates/{id}/accept` marks a candidate as accepted and inserts a new episodic memory record.
- `POST /api/memory/candidates/{id}/reject` marks a candidate as rejected.
- Review endpoints accept a note payload and return `204 No Content`.

## Agent layer
- `IntentRouter` is currently heuristic and rule-based.
- `AgentOrchestrator` dispatches by supported intent.
- Current agents:
  - `MemoryCaptureAgent` — acknowledges remember-worthy observations and reinforces curated capture.
  - `MemoryRecallAgent` — answers recall-style requests using the recent working-memory snapshot, not the episodic store.
  - `ReflectionAgent` — handles reflective or general-response interactions with restrained replies.

## Persistence and schema notes
- Flyway manages schema creation and evolution.
- `V1__init_memory_tables.sql` creates:
  - `memory_candidate`
  - `episodic_memory`
  - the `vector` extension used by the optional embedding column
- `V2__add_indexes_for_candidate_summary.sql` adds an index for exact pending-summary checks.
- `V3__add_pg_trgm_and_trigram_index.sql` enables `pg_trgm` and adds a trigram GIN index for fuzzy duplicate detection on pending summaries.
- `pg_trgm` may require elevated privileges in managed PostgreSQL environments; the architecture still works with exact-match duplicate checks if similarity support is unavailable at deployment time.

## API surface
- `POST /api/observe` — submit an observation and possibly receive an agent response.
- `GET /api/memory/candidates?limit=25` — list pending memory candidates.
- `POST /api/memory/candidates/{id}/accept` — accept a candidate with reviewer note.
- `POST /api/memory/candidates/{id}/reject` — reject a candidate with reviewer note.
- `GET /api/demo/working-memory` — inspect the in-memory working snapshot for demos.
- `GET /demo` — render the demo page.

## Configuration and deployment
- Application entry point: `ph.francisco.CognitiveAiApplication`.
- Default HTTP port: `8080`.
- Datasource is configured through environment-backed Spring properties:
  - `DATABASE_URL`
  - `DATABASE_USER`
  - `DATABASE_PASSWORD`
- Flyway is enabled by default.
- Spring AI OpenAI support is present and conditionally configured. When enabled, `ReflectionAgent` and `MemoryRecallAgent` use Spring AI for responses, and `CuratedMemoryService` can use Spring AI for candidate summarization, all with rule-based fallbacks.

## Testing coverage relevant to the architecture
- `ObservationControllerTest` verifies the `204` silence path and `200` speak path.
- `IntentRouterTest` verifies routing to memory capture, memory recall, and reflection.
- `RuleBasedShouldSpeakPolicyTest` verifies silence-by-default plus explicit-remember and question-triggered speech.

## Known gaps and intentional extension points
- No live retrieval from `episodic_memory` is wired into agent responses yet.
- Model-assisted summarization and reflection are optional and fall back to conservative rule-based behavior when Spring AI is not configured.
- Working memory is process-local and resets on restart.
- The architecture is prepared for richer provider-backed cognition later, but the current design should be read as a review-first, rule-based cognitive loop.
