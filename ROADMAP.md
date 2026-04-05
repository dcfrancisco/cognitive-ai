# ROADMAP

## Vision
Build an ambient, partner-style cognitive AI that senses context, reasons over curated memory, and speaks only when appropriate.

## Goals
- Curated memory pipeline (candidate → summary → rule review → episodic store).
- Reliable `should_speak` decision service with explainability and confidence.
- Production-ready backend with tests, CI, deployment artifacts, and a lightweight demo experience.
- Modular cognition loop with clear boundaries between perception, decision, routing, memory, and action.
- Optional LLM-assisted cognition layer for reflection, summarization, and memory review.
- Multimodal perception (text, audio, vision) unified into a single Observation model.
- Extensible sensor adapters for new input types.

## Milestones
- **MVP (v0.1):** core cognition loop, working memory, basic rules for `should_speak`, intent routing, basic agent layer, unit tests.
- **v0.2:** episodic store persistence (Postgres + pgvector), Flyway migrations, memory candidate pipeline, review endpoints.
- **v0.3a: Multimodal perception foundation:** text, audio, and vision sensor interfaces, perception normalization layer, audio (STT) prototype integration, basic vision caption/detection integration, unified Observation pipeline.
- **v0.3:** simple UI / demo page for observing the cognitive loop in action.
- **v0.5:** semantic memory (RAG), Spring AI integration, basic TTS/STT adapter demo, optional LLM-assisted reflection. ✅ **Voice conversation** added to demo UI (OpenAI Whisper STT + TTS; Web Speech API fallback). `GET /api/ai/status` returns `voiceEnabled` flag. `POST /api/voice/transcribe` and `POST /api/voice/speak` endpoints live. ✅ **Ambient room listening** — always-on VAD in demo UI captures speech from anyone in the room and feeds it into the cognitive pipeline as `source: "room"` observations.
- **v0.6:** structured episodic memory retrieval — pgvector semantic recall, `EmbeddingProvider` interface (swap OpenAI for custom LLM), `MemoryRetrievalService`, recall tracking (`recall_count`, `last_recalled_at`), ILIKE keyword fallback for null-embedding rows. `MemoryRecallAgent` now uses both working memory and long-term episodic memory.
- **v0.7:** richer orchestration, improved recall, LLM-assisted memory review, stronger observability.
- **v1.0:** hardened release — CI/CD, integration tests, deployable container, docs, examples, and public-project polish.

## Near-term (next 4–8 weeks)
- Add Flyway migrations for episodic memory tables and vector embeddings.
- Implement and harden `CognitionService.shouldSpeak()` contract: decision, confidence, why.
- Wire a simple RAG proof-of-concept using Spring AI client.
- Add CI with `mvn -DskipTests=false test` and basic linting.
- ~~Add a **simple UI / demo page**~~ ✅ Done — demo UI live at `/demo`.
- ~~Add a minimal architecture diagram~~ ✅ Done — see `diagrams/`.
- Add `.env.example`, sample `curl` commands, and expected responses.
- ~~Introduce Perception Layer abstraction (Text, Audio, Vision)~~ ✅ Done.
- ~~Implement Perception Normalizer~~ ✅ Done.
- ~~Add stub/mock `AudioSensor` and `VisionSensor`~~ ✅ Done.
- ~~Extend demo console to simulate audio/vision observations~~ ✅ Done — mic button + TTS playback in demo UI.
  - ~~Add ambient room listening~~ ✅ Done — always-on VAD captures room audio; observations tagged `source: "room"` enter the cognitive loop.
### Conversation / Interactive MVP

- **Text conversational MVP (priority):** wire the decision + routing pipeline to a response generator so the system can produce multi-turn replies. Tasks: session/turn state, simple dialog manager, intent-to-agent response mapping, response content generation (rule-based or LLM stub), demo UI wiring. Estimate: 2–4 weeks (text-only, provider & infra dependent).
  
  Status: interactive console implemented (CognitiveLoopRunner). A temporary `ForcedShouldSpeakPolicy` forces responses so you can exercise the full loop immediately. Conversation turns are persisted to episodic memory via `CuratedMemoryService.storeInteraction()` for recall testing.

  Next tasks:
  - Replace forced policy with the intelligent `ShouldSpeakPolicy` by re-enabling `RuleBasedShouldSpeakPolicy` or improving the decision heuristics.
  - Add an LLM-stub → LLM integration switch for reflection and richer responses.
  - Provide a lightweight web demo/UI that wires `/api/observe` to a console-like front-end.
- **Voice prototype (STT → Cognition → TTS):** ✅ Done. `VoiceService` (Whisper transcription + OpenAI TTS), `VoiceController` (`/api/voice/transcribe`, `/api/voice/speak`), voice mode toggle + mic button in demo UI. Web Speech API fallback when no OpenAI key is set.
- **Ambient room listening:** ✅ Done. Always-on VAD in demo UI. AudioContext + AnalyserNode RMS detection → MediaRecorder → Whisper transcribe → `/api/observe` (source: "room"). Browser SpeechRecognition continuous fallback. Red privacy banner while active.
- **Conversational quality & safety:** add turn-level context windowing, transient conversational memory, safety/policy checks, and request auditing. Estimate: ongoing; aim for basic safety checks before public demos.

## Conversation timeline (rough)

- ~~Text-only interactive MVP (demo wired to `/api/observe` + response generator)~~ ✅ Done.
- ~~Voice-first prototype (adds STT/TTS)~~ ✅ Done — demo UI with mic + speaker; OpenAI or browser fallback.
- ~~Ambient room listening~~ ✅ Done — always-on VAD; room observations tagged `source: "room"`.
- Production-grade multi-turn (RAG, longer context, safety, observability): **2–3 months**.


## Current focus (v0.6 — Episodic Memory Retrieval)
- Add structured fields to `episodic_memory` (`memory_type`, `importance`, `source`, `last_recalled_at`, `recall_count`, `metadata`) via Flyway V4.
- Introduce `EmbeddingProvider` interface + `SpringAiEmbeddingProvider` — single swap point for future custom LLM.
- Generate and store embeddings on every episodic memory write (`storeInteraction`, `acceptCandidate`).
- Implement `MemoryRetrievalService`: vector similarity search (pgvector `<=>`) with ILIKE fallback.
- Wire `MemoryRecallAgent` to use both working memory and episodic memory in responses.
- Track memory usage: `recall_count` and `last_recalled_at` updated on every retrieval.


## Medium-term (3–6 months)
- Improve memory-review rules; add LLM-assisted review workflow.
- Add **LLM integration layer** for:
  - reflective responses
  - summarization of candidate memory
  - semantic recall assistance
  - optional decision support (kept auditable and bounded by policy)
- Add TTS/STT integration and a voice-first demo loop.
- Add metrics and observability for decisions and memory retention.
- Improve recall behavior beyond recent working memory. ✅ **In progress** — episodic memory retrieval via pgvector (v0.6).
- Add better policy controls for when LLM assistance is allowed vs disabled.
- Real STT integration for `AudioSensor`.
- Vision captioning or object detection integration.
- Metadata enrichment (tone, confidence, scene context).
- Improved routing using multimodal context.

## Long-term (6+ months)
- External integrations (mobile, desktop voice agents), privacy/consent flows.
- **Build own LLM** — replace `SpringAiEmbeddingProvider` with custom embedding model; retrieval and memory pipeline require no changes due to `EmbeddingProvider` interface.
- Evaluate federated/local embeddings for privacy-preserving memory.
- Production-grade scaling and RBAC for multi-user scenarios.
- Expand from rule-based cognition to hybrid cognition:
  - policy-first deterministic safety rails
  - optional LLM reasoning layer
  - long-term semantic and episodic memory alignment
- Support multiple partner-style agent personas on the same cognitive core.
- Continuous perception streams (microphone, camera).
- Context fusion across modalities (text + audio + vision).
- Attention mechanisms for prioritizing signals.
- Privacy controls for sensor data.

## Success metrics
- False-positive speak rate < X% in pilot tests.
- Memory retention precision (meaningful saved items) > Y% after review.
- End-to-end pipeline reliability and CI test coverage > 70%.
- Demo usability: a new visitor can run the project and see the cognitive loop within 10 minutes.
- Explainability completeness: every non-silent response includes decision, intent, agent, and reasons.

## Public project readiness track
### What to fix first before going public
- Rewrite `README.md` around:
  - current status
  - what works now
  - quick start
  - API examples
  - roadmap
- Add:
  - `.env.example`
  - sample `curl` commands
  - expected responses
- Add GitHub Actions:
  - Java build
  - test run
- Fix code issues:
  - `acceptCandidate()` lookup logic
  - duplicate candidate behavior
  - request validation
- Add one short architecture diagram.
- Add a lightweight UI / demo page so the project is easier to understand at a glance.

## How to contribute
- Open small, focused PRs with tests and rationale for behavioral changes.
- Document new memory rules and their behavioral impact in `docs/`.
- Keep cognition changes auditable and explainable.
- Prefer modular additions over deeply coupled intelligence layers.