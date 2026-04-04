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
- `agents/VoiceService` — Voice I/O service. `transcribe(MultipartFile)` → `Optional<String>` via OpenAI Whisper. `speak(String)` → `Optional<byte[]>` MP3 via OpenAI TTS. Returns `Optional.empty()` gracefully when no API key is configured.
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

- AI health endpoint: `GET /api/ai/status` — returns `available`, `clientPresent`, `apiKeySet`, `voiceEnabled`, and `model`.

## Voice conversation

A voice mode is available in the demo UI at `/demo`.

**Backend:**
- `POST /api/voice/transcribe` — multipart `audio` file → `{"text": "..."}`. Uses OpenAI Whisper. Returns 503 if not configured.
- `POST /api/voice/speak` — `{"text": "..."}` → `audio/mpeg` bytes. Uses OpenAI TTS (Nova voice). Returns 503 if not configured.

**Frontend (demo UI):**
- "🎙 Voice mode" toggle reveals a mic button.
- Click 🎤 to start recording (MediaRecorder / audio/webm). Click 🔴 to stop and transcribe.
- Transcribed text auto-fills the textarea and submits to `/api/observe`.
- Avery's response is read aloud via `/api/voice/speak`.
- **Fallback:** if `voiceEnabled` is false, the UI falls back to the browser's `SpeechRecognition` (STT) and `speechSynthesis` (TTS) APIs, which work in Chrome/Edge without any API key.

**Requirements for full voice:**
```
SPRING_AI_OPENAI_API_KEY=sk-...   # enables both Whisper + TTS
```
Without the key, browser-native voice still works in Chrome/Edge.

## Ambient room listening

Always-on passive VAD mode is available in the demo UI.

**How it works:**
- `AudioContext` + `AnalyserNode` computes RMS volume continuously.
- A state machine (`silence` → `speaking` → `silence`) starts recording when RMS exceeds `VAD_THRESHOLD = 0.018`.
- `MediaRecorder` captures the segment; recording stops after 900 ms of silence or 25 s max.
- The audio blob is sent to `POST /api/voice/transcribe` (Whisper), then the transcript is submitted to `POST /api/observe` with `source: "room"`.
- Browser `SpeechRecognition` (continuous mode) is used as fallback when Whisper is unavailable.
- A fixed red privacy banner is displayed while ambient mode is active.
- Responses from agents are optionally spoken aloud (TTS or `speechSynthesis`).
- All ambient observations enter the same cognitive loop (ShouldSpeakPolicy → IntentRouter → AgentOrchestrator → memory).

**Key classes:** all in `demo.html` frontend JS (no backend-specific ambient component — uses existing `/api/voice/transcribe` and `/api/observe`).

## Forced speak policy (testing)

- `ForcedShouldSpeakPolicy` exists in `src/main/java/ph/francisco/cognition/ForcedShouldSpeakPolicy.java` but is not registered by default.
- To enable forced responses you can register it as a bean (quick options):
  - Add `@Component` and `@Primary` to the class (temporary). This forces the `DecisionEngine` to always speak.
  - To disable again, remove `@Component`/`@Primary` (the repo currently leaves the file present but not registered).

## Next steps (suggested)

- Run integration with a valid OpenAI key and test `ReflectionAgent` and `MemoryRecallAgent` behaviours.
- Add a small acceptance test for console loop using `System.in` injection or a harness.
- Replace `ForcedShouldSpeakPolicy` with a toggleable profile or property to avoid code edits for enabling the forced policy.
- Replace placeholder stub agents with richer LLM-driven responses.
- Enable `RuleBasedShouldSpeakPolicy` heuristics for more natural silence decisions.
- Add RAG / vector memory for semantic recall.

---

Last updated: April 2026 (voice conversation feature added).
