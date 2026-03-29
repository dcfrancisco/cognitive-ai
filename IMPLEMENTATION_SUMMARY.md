# Implementation Summary — cognitive-ai

This document summarises the runtime components currently implemented in the repository and short instructions for running and toggling the test policy.

## Overview

The project implements a minimal cognition-first conversational loop (console + agents). The core flow is:

Observation -> ShouldSpeakPolicy -> IntentRouter -> AgentOrchestrator -> Agent -> Response -> Memory

## Key components

- `runner/CognitiveLoopRunner` — Console loop (implements `CommandLineRunner`). Reads input, creates `Observation`, sends it to `DecisionEngine`, and prints agent responses. Also stores interactions in `ConversationMemoryService` and `CuratedMemoryService`.
- `cognition/DecisionEngine` — Calls the configured `ShouldSpeakPolicy` and `IntentRouter`, returns a `DecisionEngineResult` (decision + routed intent).
- `cognition/ShouldSpeakPolicy` — Interface for the speak policy. Implementations:
  - `RuleBasedShouldSpeakPolicy` — default rule-based decision logic.
  - `ForcedShouldSpeakPolicy` — temporary file (kept in code base). When registered as a bean it forces SPEAK for every observation (used for interactive testing).
- `agents/IntentRouter` — Routes `Observation` → `CognitiveIntent` values.
- `agents/AgentOrchestrator` — Picks the first `CognitiveAgent` that `supports(intent)` and forwards the `Observation`.
- `agents/CognitiveAgent` implementations:
  - `ReflectionAgent` — general responses; uses `SpringAiResponseService` when available.
  - `MemoryRecallAgent` — answers recall requests by grounding replies in `CuratedMemoryService` working memory.
  - `MemoryCaptureAgent` — acknowledges memory-capture intents.
- `agents/SpringAiResponseService` — LLM integration. Uses Spring AI `ChatClient` when available or falls back to a direct OpenAI HTTP call (Responses API). Produces reflection/recall/summarization outputs.
- `memory/CuratedMemoryService` — Working memory, candidate extraction, and persistence into episodic stores. Provides `workingSnapshot(sessionId)` used by agents.
- `memory/ConversationMemoryService` — In-memory recent conversation entries for quick retrieval + forwards entries to `CuratedMemoryService` for persistence.

## How to run

- Using Maven locally (dev):

```bash
export OPENAI_API_KEY="sk-..."   # optional for LLM responses
mvn -DskipTests spring-boot:run
```

- Using Docker Compose (repo includes `docker-compose.yml`):

```bash
docker compose up -d app --build
```

## LLM / API key

- The LLM client is enabled when either `spring.ai.openai.api-key` or `OPENAI_API_KEY` / `SPRING_AI_OPENAI_API_KEY` is available in the environment.
- Model selection can be controlled via `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL` or `AI_MODEL` env var. Defaults to `gpt-4o-mini` in fallback.

## Health & status

- AI health endpoint: `GET /api/ai/status` (controller logs whether ChatClient is available and whether an API key is set).

## Forced speak policy (testing)

- `ForcedShouldSpeakPolicy` exists in `src/main/java/ph/francisco/cognition/ForcedShouldSpeakPolicy.java` but is not registered by default.
- To enable forced responses you can register it as a bean (quick options):
  - Add `@Component` and `@Primary` to the class (temporary). This forces the `DecisionEngine` to always speak.
  - To disable again, remove `@Component`/`@Primary` (the repo currently leaves the file present but not registered).

## Next steps (suggested)

- Run integration with a valid OpenAI key and test `ReflectionAgent` and `MemoryRecallAgent` behaviours.
- Add a small acceptance test for console loop using `System.in` injection or a harness.
- Replace `ForcedShouldSpeakPolicy` with a toggleable profile or property to avoid code edits for enabling the forced policy.

---

File created by Copilot-assisted audit on branch `feature/force-speak-policy`.
