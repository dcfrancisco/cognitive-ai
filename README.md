# Cognitive AI

![Java](https://img.shields.io/badge/java-21-blue)
![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)
![Status](https://img.shields.io/badge/status-early--stage-orange)

A cognition-first ambient AI framework that observes context, decides whether to speak, routes intent to specialized agents, and remembers selectively through curated memory.

This project is not designed as a conventional chatbot. It explores how AI can behave more like a restrained partner: aware of context, capable of silence, deliberate about memory, and explainable in how it acts.

## Current status

Early-stage but runnable foundation. Interactive and voice-capable.

**What works now:**

- Observation intake via REST API and demo UI
- Working memory capture
- Curated memory candidate pipeline
- Rule-based should-speak decision
- Intent routing and agent orchestration
- Explainable JSON response (decision, confidence, intent, agent, reasons)
- Interactive console loop (`CognitiveLoopRunner`)
- Voice conversation in demo UI — OpenAI Whisper STT + TTS; Web Speech API fallback
- Ambient room listening — always-on VAD that feeds observations from anyone in the room into the cognitive pipeline
- AI health endpoint (`GET /api/ai/status`) with `voiceEnabled` flag
- LLM-backed reflection and summarization via Spring AI (optional, requires API key)

## Core ideas

- **Cognition before interaction** — decide whether to speak before generating a reply.
- **Curated memory, not raw logging** — store meaning selectively.
- **Explainable behavior** — every intervention has a reason.
- **Partner stance** — no command-style or owner/slave behavior.
- **Modular cognition** — perception, decision, memory, routing, and agents stay separate and auditable.

## Architecture

```
Observation intake (POST /api/observe)
  → CuratedMemoryService    (working memory + candidate curation)
  → DecisionEngine          (ShouldSpeakPolicy + IntentRouter)
  → AgentOrchestrator       (select agent by intent)
  → Agent response
```

Ambient mode adds a parallel path:

```
Room audio (AudioContext VAD in browser)
  → MediaRecorder
  → POST /api/voice/transcribe  (Whisper STT)
  → POST /api/observe           (source: "room")
  → same cognitive loop
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full component diagram and runtime details.

## Agent layer

- **MemoryCaptureAgent** — handles explicit "remember this" observations.
- **MemoryRecallAgent** — answers recall requests from recent working memory.
- **ReflectionAgent** — produces restrained reflective responses; uses Spring AI when available.

## Tech stack

| | |
|---|---|
| Runtime | Java 21, Spring Boot 3.x |
| DB | PostgreSQL + pgvector + Flyway |
| AI | Spring AI 1.1.2 (OpenAI chat, Whisper STT, TTS) |
| Frontend | Thymeleaf demo page; Web Speech API fallback |

## Run locally

### Prerequisites

- Java 21, Maven
- PostgreSQL with `pgvector` extension

### Environment variables

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/cognitive_ai
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export OPENAI_API_KEY=sk-...   # optional — enables LLM responses + Whisper + TTS
```

### Start

```bash
mvn spring-boot:run
# or skip tests
mvn -DskipTests spring-boot:run
```

Open the demo UI at **http://localhost:8080/demo**.

### Docker Compose

```bash
docker compose up --build
```

### Database setup

Flyway migrations run automatically on startup. For pg_trgm (fuzzy duplicate detection) and managed Postgres notes, see [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md).

### Interactive console

The `CognitiveLoopRunner` starts on application startup and provides a prompt loop:

```
User: hello
Avery: Hello. How can I help you?
```

> Note: currently uses `ForcedShouldSpeakPolicy` — the system responds to every input. Re-enabling intelligent filtering is a planned next step.

## Demo UI

Open **http://localhost:8080/demo** after starting the app.

**Text mode** — type an observation, see the full cognitive response (decision, intent, agent, reasons).

**Voice mode** (🎙 toggle):
- Click 🎤 to record, 🔴 to stop and transcribe.
- Requires `OPENAI_API_KEY` for Whisper + TTS; falls back to browser `SpeechRecognition` / `speechSynthesis` in Chrome/Edge without a key.

**Ambient mode** (always-on listening):
- Activates passive room-level VAD — any speech is captured and fed into the cognitive pipeline as `source: "room"`.
- Volume meter, ambient feed of last captured observations, optional TTS responses.
- Red privacy banner is displayed while active.

## API

### POST /api/observe

```bash
curl -X POST http://localhost:8080/api/observe \
  -H "Content-Type: application/json" \
  -d '{
    "source": "user",
    "content": "Please remember that I prefer meetings after 10am",
    "explicitRemember": true
  }'
```

Response (SPEAK):

```json
{
  "decision": "SPEAK",
  "confidence": 0.9,
  "intent": "MEMORY_CAPTURE",
  "agent": "MemoryCaptureAgent",
  "message": "I'll treat that as something worth remembering and reviewing.",
  "reasons": [
    "Explicit remember request present",
    "Intent routed to MEMORY_CAPTURE"
  ]
}
```

### Memory review

```bash
# list pending candidates
curl http://localhost:8080/api/memory/candidates

# accept
curl -X POST http://localhost:8080/api/memory/candidates/<id>/accept \
  -H "Content-Type: application/json" \
  -d '{"note": "Useful for long-term memory"}'

# reject
curl -X POST http://localhost:8080/api/memory/candidates/<id>/reject \
  -H "Content-Type: application/json" \
  -d '{"note": "Too transient"}'
```

### Voice endpoints

```bash
# transcribe audio to text (Whisper)
curl -X POST http://localhost:8080/api/voice/transcribe \
  -F "audio=@recording.webm"

# text to speech (returns audio/mpeg)
curl -X POST http://localhost:8080/api/voice/speak \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello, how can I help?"}' \
  --output response.mp3
```

### AI health

```bash
curl http://localhost:8080/api/ai/status
```

Returns `available`, `clientPresent`, `apiKeySet`, `voiceEnabled`, and `model`.

For the full sample library see [SAMPLES.md](SAMPLES.md).

## Documentation

| File | Content |
|------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Component diagram, request flow, memory architecture |
| [COGNITIVE-AI.md](COGNITIVE-AI.md) | What cognitive AI is, how it differs from traditional AI |
| [DEMO_FLOW.md](DEMO_FLOW.md) | Sequence diagram and demo script |
| [ROADMAP.md](ROADMAP.md) | Milestones and near/medium/long-term plans |
| [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) | Runtime components and how to toggle policies |
| [SAMPLES.md](SAMPLES.md) | curl examples |
| [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md) | pg_trgm, Flyway, managed Postgres notes |

## Contribution

Small, auditable pull requests preferred.

Good contributions: clearer routing rules, better tests, safer memory policies, stronger explainability, improved recall behavior.

## License

MIT
