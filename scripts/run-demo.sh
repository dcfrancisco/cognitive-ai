#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
JQ=${JQ:-jq}

echo "Demo script for Cognitive AI (base: $BASE_URL)"

echo "\n1) Capture a memory (explicit remember)"
curl -sS -X POST "$BASE_URL/api/observe" \
  -H "Content-Type: application/json" \
  -d '{"source":"user","content":"I need to wake up tomorrow early. around 5am","explicitRemember":true}' | ${JQ} || true

echo "\n2) Ask a recall question"
curl -sS -X POST "$BASE_URL/api/observe" \
  -H "Content-Type: application/json" \
  -d '{"source":"user","content":"What did I ask you to remember?","explicitRemember":false}' | ${JQ} || true

echo "\n3) Make a general statement (should be silent)"
curl -sS -X POST "$BASE_URL/api/observe" \
  -H "Content-Type: application/json" \
  -d '{"source":"user","content":"It is sunny today.","explicitRemember":false}' -w "\nHTTP_STATUS:%{http_code}\n" | ${JQ} || true

echo "\n4) List pending memory candidates"
curl -sS "$BASE_URL/api/memory/candidates" | ${JQ} || true

cat <<'EOF'

5) Accept a candidate (replace <CANDIDATE_UUID> with actual id from previous output):

curl -sS -X POST "$BASE_URL/api/memory/candidates/<CANDIDATE_UUID>/accept" \
  -H "Content-Type: application/json" \
  -d '{"note":"Keep this"}' -w "\nHTTP_STATUS:%{http_code}\n"

Or to reject:

curl -sS -X POST "$BASE_URL/api/memory/candidates/<CANDIDATE_UUID>/reject" \
  -H "Content-Type: application/json" \
  -d '{"note":"Not important enough"}' -w "\nHTTP_STATUS:%{http_code}\n"

6) Inspect working memory (demo-only):

curl -sS "$BASE_URL/api/demo/working-memory" | ${JQ} || true

7) Open the demo UI in your browser:

http://localhost:8080/demo

8) Tail app logs (docker):

docker logs -f cognitive-ai-app-1

EOF
