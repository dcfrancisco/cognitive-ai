# Cognitive AI (Ambient Companion)

A partner-style ambient cognitive AI that:
- Observes context
- Reasons over memory
- Chooses when to speak
- Respects silence
- Remembers selectively and deliberately

This project is not a chatbot, not command-based, and not owner/master-style. It explores how AI should behave around humans, not how much it can say.

Core principles
- Cognition before interaction: decide whether to respond; silence is often preferred.
- Memory is curated, not logged: store meaning, not raw transcripts; persist only if it recurs, changes future behavior, or is explicitly marked "remember this".
- Clear memory layers: working (in-session), episodic (summarized vectors), semantic (RAG), values & boundaries (explicit rules, never learned).
- Non-hierarchical behavior: the AI is a partner; defer to human agency; avoid authoritative language and judgments.
- Modality-agnostic cognition: cognitive core independent of UI; voice-first loop with chat as fallback/debug.
- Explainability and restraint: every remembered item and intervention should have a clear why; default to silence when unsure.

Technical direction
- Backend: Spring Boot + Spring AI
- Memory: PostgreSQL + pgvector
- AI: LLM via Spring AI, RAG for semantic memory
- Memory review: rule-based first, LLM-assisted later
- Interfaces: minimal chat, voice-first loop (STT → cognition → TTS)

Architecture (high level)
- Perception → Cognition (should_speak gate) → Memory (working/episodic/semantic/values) → Interface adapters.
- Keep components small and auditable; separate layers cleanly.

Getting started
- Prerequisites: Java 21+, Maven or Gradle; PostgreSQL with pgvector extension.
- Database setup (example):
  - CREATE EXTENSION IF NOT EXISTS vector;
- Environment variables (examples):
  - DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
  - AI_PROVIDER_KEY (e.g., OpenAI or Azure OpenAI)
- Suggested next steps:
  - Add Flyway migrations for episodic memory tables and embeddings.
  - Configure Spring AI client and a simple RAG service for semantic memory.
  - Implement the curated memory pipeline: candidate → summary → rule review → episodic store.
  - Implement a should_speak decision service that outputs decision, confidence, and why; prefer silence on uncertainty.

Project structure (proposed)
- interface/
- perception/
- cognition/
- memory/
- values/
- config/

Voice-first loop
- STT → perception → cognition (gate) → memory access → TTS
- Default to silence when uncertainty is high; record explainability briefly (no raw transcripts).

Contribution
- Prefer small, auditable changes; include tests where possible.
- Explain how changes respect cognition-first and curated memory.
- Document any new memory review rules and their behavioral impact.

License
- To be decided (e.g., MIT or Apache-2.0). Add a LICENSE file when chosen.
