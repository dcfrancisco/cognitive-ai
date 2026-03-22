# ROADMAP

Vision
- Build an ambient, partner-style cognitive AI that senses context, reasons over curated memory, and speaks only when appropriate.

Goals
- Curated memory pipeline (candidate → summary → rule review → episodic store).
- Reliable should_speak decision service with explainability and confidence.
- Production-ready backend with tests, CI, and deployment artifacts.

Milestones
- MVP (v0.1): core cognition loop, working memory, basic rules for `should_speak`, unit tests.
- v0.2: episodic store persistence (Postgres + pgvector), Flyway migrations, memory candidate pipeline.
- v0.5: semantic memory (RAG), Spring AI integration, basic TTS/STT adapter demo.
- v1.0: hardened release — CI/CD, integration tests, deployable container, docs and examples.

Near-term (next 4–8 weeks)
- Add Flyway migrations for episodic memory tables and vector embeddings.
- Implement `CognitionService.shouldSpeak()` contract: decision, confidence, why.
- Wire a simple RAG proof-of-concept using Spring AI client.
- Add CI with `mvn -DskipTests=false test` and basic linting.

Medium-term (3–6 months)
- Improve memory-review rules; add LLM-assisted review workflow.
- Add TTS/STT integration and a voice-first demo loop.
- Add metrics and observability for decisions and memory retention.

Long-term (6+ months)
- External integrations (mobile, desktop voice agents), privacy/consent flows.
- Evaluate federated/local embeddings for privacy-preserving memory.
- Production-grade scaling and RBAC for multi-user scenarios.

Success metrics
- False-positive speak rate < X% in pilot tests.
- Memory retention precision (meaningful saved items) > Y% after review.
- End-to-end pipeline reliability and CI test coverage > 70%.

How to contribute
- Open small, focused PRs with tests and rationale for behavioral changes.
- Document new memory rules and their behavioral impact in `docs/`.
