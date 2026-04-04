# Sample curl commands

## Observe (explicit remember)

```bash
curl -X POST http://localhost:8080/api/observe \
  -H "Content-Type: application/json" \
  -d '{
    "source": "user",
    "content": "Please remember that I prefer meetings after 10am",
    "explicitRemember": true
  }'
```

Expected response (SPEAK):

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

## Observe (recall request)

```bash
curl -X POST http://localhost:8080/api/observe \
  -H "Content-Type: application/json" \
  -d '{
    "source": "user",
    "content": "What do you remember about my preferences?"
  }'
```

Expected response (SPEAK or SILENCE depending on memory):

```json
{
  "decision": "SPEAK",
  "confidence": 0.75,
  "intent": "MEMORY_RECALL",
  "agent": "MemoryRecallAgent",
  "message": "From recent working memory, here’s what seems relevant:\n- I prefer tea in the morning",
  "reasons": [
    "User asked for recall",
    "Response was grounded in recent working memory"
  ]
}
```

## List pending memory candidates

```bash
curl http://localhost:8080/api/memory/candidates
```

## Accept a candidate

```bash
curl -X POST http://localhost:8080/api/memory/candidates/<CANDIDATE_UUID>/accept \
  -H "Content-Type: application/json" \
  -d '{ "note": "Accepted during review" }'
```

## Reject a candidate

```bash
curl -X POST http://localhost:8080/api/memory/candidates/<CANDIDATE_UUID>/reject \
  -H "Content-Type: application/json" \
  -d '{ "note": "Not relevant" }'
```

## Transcribe audio (Whisper)

```bash
curl -X POST http://localhost:8080/api/voice/transcribe \
  -F "audio=@recording.webm"
```

Expected response:

```json
{ "text": "I need to wake up early tomorrow" }
```

Returns 503 when no `OPENAI_API_KEY` is set.

## Text to speech

```bash
curl -X POST http://localhost:8080/api/voice/speak \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello, how can I help?"}' \
  --output response.mp3
```

Returns `audio/mpeg` bytes. Returns 503 when not configured.

## AI health check

```bash
curl http://localhost:8080/api/ai/status
```

Expected response:

```json
{
  "available": true,
  "clientPresent": true,
  "apiKeySet": true,
  "voiceEnabled": true,
  "model": "gpt-4o-mini"
}
```

## Ambient room observation (submitted automatically by demo UI)

```bash
curl -X POST http://localhost:8080/api/observe \
  -H "Content-Type: application/json" \
  -d '{
    "source": "room",
    "content": "We should reschedule Thursday meeting to Friday",
    "explicitRemember": false
  }'
```
