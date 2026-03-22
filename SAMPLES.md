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
