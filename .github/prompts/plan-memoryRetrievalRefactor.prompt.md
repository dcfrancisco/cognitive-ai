# Plan: v0.4 — Memory Retrieval & Usage

**TL;DR:** The `episodic_memory` table gets written to but is never read. v0.4 makes memory real: structured columns, keyword retrieval ranked by importance and recency, usage tracking, and agent integration. No vector search yet — that is v0.5. Goal is correct → predictable → explainable.

**Core principle:** Structured + keyword retrieval first. Vector is an optional enhancement after this works.

---

## Phase 1 — Database Migration

**Depends on nothing. Blocks everything else.**

Create `V4__add_structured_memory_fields.sql`:

- Add `memory_type VARCHAR(50) NOT NULL DEFAULT 'INTERACTION'`, `importance INT NOT NULL DEFAULT 1`, `source VARCHAR(255) NULL`, `last_recalled_at TIMESTAMPTZ NULL`, `recall_count INT NOT NULL DEFAULT 0`, `metadata JSONB NULL` to `episodic_memory`
- Backfill: `UPDATE episodic_memory SET source = 'system'` for existing rows
- Add index for retrieval ranking: `(importance DESC, last_recalled_at DESC NULLS LAST, created_at DESC)`
- Keep `summary` as the content field (no rename); keep `embedding` column untouched (v0.5)

---

## Phase 2 — EpisodicMemory Record _(depends on Phase 1)_

New `EpisodicMemory.java` — plain Java record:

```java
public record EpisodicMemory(
    UUID id,
    String summary,
    String memoryType,
    int importance,
    String source,
    Instant createdAt,
    Instant lastRecalledAt,
    int recallCount
) {}
```

Include a static `RowMapper<EpisodicMemory>` constant for use in all JdbcTemplate queries.

---

## Phase 3 — Repository Updates _(depends on Phase 2)_

Modify `EpisodicMemoryRepository`:

1. **`insert()` signature update** — add `String source`, `String memoryType` params; include in INSERT. No embedding call.
2. **`findRelevant(String input, int limit)`** — primary retrieval:
   ```sql
   WHERE summary ILIKE '%' || ? || '%'
   ORDER BY importance DESC, last_recalled_at DESC NULLS LAST, created_at DESC
   LIMIT ?
   ```
3. **`findRecent(int limit)`** — fallback when keyword returns nothing:
   ```sql
   ORDER BY importance DESC, created_at DESC LIMIT ?
   ```
4. **`markRecalled(UUID id)`**:
   ```sql
   UPDATE episodic_memory SET recall_count = recall_count + 1, last_recalled_at = NOW() WHERE id = ?
   ```

---

## Phase 4 — MemoryRetrievalService _(depends on Phase 3)_

New `MemoryRetrievalService.java` interface:

```java
List<EpisodicMemory> findRelevant(String input, int limit);
```

New `MemoryRetrievalServiceImpl.java` `@Service`:

1. Call `repo.findRelevant(input, limit)` — keyword path
2. If result is empty, call `repo.findRecent(limit)` — recency fallback
3. Call `repo.markRecalled(m.id())` for each result
4. Return list (max 5)

No embedding calls. No external API dependency.

---

## Phase 5 — MemoryRecallAgent Update _(depends on Phase 4)_

Modify `MemoryRecallAgent`:

- Inject `MemoryRetrievalService` via constructor (alongside existing `CuratedMemoryService`)
- In `handle()`:
  1. Fetch working memory: `curatedMemoryService.workingSnapshot(obs.sessionId())`
  2. Fetch episodic memory: `memoryRetrievalService.findRelevant(obs.content(), 5)`
  3. Case A — both exist: combine, pass to Spring AI (or fallback formatter)
  4. Case B — working empty, episodic exists: use episodic only, drop "not enough memory" guard
  5. Case C — both empty: existing fallback response
- Episodic entries formatted as: `"From what I remember: - <summary>"`
- Add `"episodic memory recalled"` to reasons when episodic results are used

---

## Phase 6 — CuratedMemoryService Write-Path _(depends on Phase 3, parallel with Phase 5)_

Modify `CuratedMemoryService`:

- `storeInteraction()`: pass `source = obs.source()`, `memoryType = "INTERACTION"` to `episodicMemoryRepository.insert()`
- `acceptCandidate()`: pass `source = found.source()`, `memoryType = "CANDIDATE_ACCEPTED"` to `episodicMemoryRepository.insert()`

No embedding. No external call. Structured fields only.

---

## Phase 7 — Tests _(depends on Phases 4 + 5)_

New `MemoryRetrievalServiceTest.java`:

- `findRelevant_callsMarkRecalledForEachResult` — stub repo returns 2 results, verify `markRecalled` called twice
- `findRelevant_fallsBackToRecentWhenKeywordReturnsEmpty` — keyword returns empty, verify `findRecent` called
- `findRelevant_returnsEmptyWhenBothPathsEmpty` — `markRecalled` never called

New `MemoryRecallAgentTest.java`:

- `handle_combinesEpisodicAndWorkingMemory` — both sources return data, verify both appear in response
- `handle_usesEpisodicWhenWorkingIsEmpty` — working empty + episodic has data → uses episodic, not "not enough memory" guard
- `handle_fallbackWhenBothEmpty` — both empty → fallback response

---

## Relevant Files

| File                                                                   | Action                              |
| ---------------------------------------------------------------------- | ----------------------------------- |
| `src/main/resources/db/migration/V4__add_structured_memory_fields.sql` | NEW                                 |
| `src/main/java/ph/francisco/memory/EpisodicMemory.java`                | NEW                                 |
| `src/main/java/ph/francisco/memory/MemoryRetrievalService.java`        | NEW — interface                     |
| `src/main/java/ph/francisco/memory/MemoryRetrievalServiceImpl.java`    | NEW — keyword impl                  |
| `src/main/java/ph/francisco/memory/EpisodicMemoryRepository.java`      | MODIFY — insert + 3 new methods     |
| `src/main/java/ph/francisco/memory/CuratedMemoryService.java`          | MODIFY — structured fields on write |
| `src/main/java/ph/francisco/agents/MemoryRecallAgent.java`             | MODIFY — inject + use episodic      |
| `src/test/java/ph/francisco/memory/MemoryRetrievalServiceTest.java`    | NEW                                 |
| `src/test/java/ph/francisco/agents/MemoryRecallAgentTest.java`         | NEW                                 |

---

## Verification

After Phase 5, run this sanity check before tests:

1. Send any message that asks the system to remember a specific personal fact (e.g. a preference, habit, or detail about yourself)
2. Send a follow-up question that is contextually related but does not repeat the exact words
3. Expected: the response references the remembered fact — if it does, memory retrieval is working

Then:

1. Run `./mvnw flyway:migrate` — V4 applies cleanly
2. Run `./mvnw test` — all existing + new tests pass
3. Confirm `recall_count` increments and `last_recalled_at` updates in DB after each recall

Common pitfalls:

- Results too broad → limit to 3–5
- Agent ignores episodic → add log at start of `handle()` to confirm retrieval result
- JSON still used in logic → structured fields only for filtering/ranking

---

## Deferred to v0.5 (After This Works)

- `EmbeddingProvider` interface + `SpringAiEmbeddingProvider` impl
- Populate `embedding` column on write
- Vector similarity retrieval (`embedding <=> ?::vector`) as primary path, keyword as fallback
- Hybrid retrieval: keyword narrows candidates, vector ranks them
- Embedding dimension migration when switching from OpenAI to custom LLM
