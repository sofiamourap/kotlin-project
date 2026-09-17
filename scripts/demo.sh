#!/usr/bin/env bash
# Demo: register → login → BUY → wait → portfolio.
# Needs: running API on :8080, jq.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
EMAIL="${EMAIL:-demo@example.com}"
PASSWORD="${PASSWORD:-secret}"
REQUEST_ID="${REQUEST_ID:-demo-$(date +%s)}"

echo "== health"
curl -sf "$BASE/health" | jq .

echo "== register (409 if email exists is ok)"
curl -s -o /tmp/mp-register.json -w "%{http_code}" -X POST "$BASE/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
echo
jq . /tmp/mp-register.json || true

echo "== login"
TOKEN="$(curl -sf -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r .token)"
echo "token length: ${#TOKEN}"

echo "== BUY AAPL (requestId=$REQUEST_ID)"
curl -sf -D /tmp/mp-order.headers -o /tmp/mp-order.json -X POST "$BASE/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H "X-Request-Id: $REQUEST_ID" \
  -d '{"symbol":"AAPL","side":"BUY","quantity":"1"}'
grep -i x-request-id /tmp/mp-order.headers || true
jq . /tmp/mp-order.json

echo "== wait for consumer"
sleep 2

echo "== portfolio"
curl -sf "$BASE/portfolio" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Request-Id: $REQUEST_ID" | jq .

echo "Grep API logs for: $REQUEST_ID"
