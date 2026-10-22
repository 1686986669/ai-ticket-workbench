#!/usr/bin/env sh
set -eu
base="${BASE_URL:-http://127.0.0.1:8080}"
curl --fail --silent "$base/api/health"
curl --fail --silent -H 'Content-Type: application/json' -d '{"title":"API timeout","description":"upstream timeout"}' "$base/api/tickets"
curl --fail --silent "$base/api/tickets?page=1&size=5"
