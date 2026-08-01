#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${ROOT_DIR:-/Users/diegoabelardo.soto/Repos/IdeaProjects}"
TOKEN_DIR="${TOKEN_DIR:-$ROOT_DIR/quarkus/token-svc}"
ONBOARDING_DIR="${ONBOARDING_DIR:-$ROOT_DIR/quarkus/onboarding-svc}"
PROFILE_DIR="${PROFILE_DIR:-$ROOT_DIR/profile-service}"

FRONTEND_PUBLIC_URL="${FRONTEND_PUBLIC_URL:-http://localhost:8000}"
JWT_ISSUER="${JWT_ISSUER:-https://apis.internal.dsoto.cl}"
ONBOARDING_SVC_CLIENT_SECRET="${ONBOARDING_SVC_CLIENT_SECRET:-test-onboarding-secret}"

SKIP_BUILD="${SKIP_BUILD:-false}"
RUN_SMOKE="${RUN_SMOKE:-true}"

log() {
  printf '[dev-stack] %s\n' "$*"
}

fail() {
  printf '[dev-stack] FAIL: %s\n' "$*" >&2
  exit 1
}

require_dir() {
  local dir="$1"
  [ -d "$dir" ] || fail "missing directory: $dir"
}

package_service() {
  local name="$1"
  local dir="$2"

  log "packaging $name"
  (cd "$dir" && ./mvnw -q package -DskipTests)
}

compose_up() {
  local name="$1"
  local dir="$2"
  shift 2

  log "starting $name"
  (cd "$dir" && "$@" docker compose up -d --build --force-recreate)
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local expected="${3:-200}"
  local status

  log "waiting for $name"
  for _ in $(seq 1 60); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)"
    if [ "$status" = "$expected" ]; then
      return 0
    fi
    sleep 1
  done

  fail "$name did not become ready at $url"
}

require_dir "$TOKEN_DIR"
require_dir "$ONBOARDING_DIR"
require_dir "$PROFILE_DIR"

if [ "$SKIP_BUILD" != "true" ]; then
  package_service "token-svc" "$TOKEN_DIR"
  package_service "profile-service" "$PROFILE_DIR"
  package_service "onboarding-svc" "$ONBOARDING_DIR"
else
  log "skipping Maven package step"
fi

log "using FRONTEND_PUBLIC_URL=$FRONTEND_PUBLIC_URL"

compose_up "token-svc" "$TOKEN_DIR" env \
  FRONTEND_PUBLIC_URL="$FRONTEND_PUBLIC_URL" \
  JWT_ISSUER="$JWT_ISSUER" \
  JWT_AUDIENCE="identity-svc" \
  JWT_ACCESS_AUDIENCES="identity-svc,onboarding-svc,profile-service" \
  ONBOARDING_SVC_CLIENT_SECRET="$ONBOARDING_SVC_CLIENT_SECRET"

wait_for_http "token-svc JWKS" \
  "http://localhost:9091/token-service/api/.well-known/jwks.json"

compose_up "profile-service" "$PROFILE_DIR" env \
  JWT_PUBLIC_KEY_LOCATION="http://host.docker.internal:9091/token-service/api/.well-known/jwks.json" \
  JWT_ISSUER="$JWT_ISSUER" \
  JWT_AUDIENCE="profile-service"

wait_for_http "profile-service locations" \
  "http://localhost:9092/profile-service/locations/regions"

compose_up "onboarding-svc" "$ONBOARDING_DIR" env \
  JWT_PUBLIC_KEY_LOCATION="http://host.docker.internal:9091/token-service/api/.well-known/jwks.json" \
  JWT_ISSUER="$JWT_ISSUER" \
  JWT_AUDIENCE="onboarding-svc" \
  TOKEN_SERVICE_BASE_URL="http://host.docker.internal:9091/token-service" \
  PROFILE_SERVICE_BASE_URL="http://host.docker.internal:9092/profile-service" \
  TOKEN_SERVICE_CLIENT_SECRET="$ONBOARDING_SVC_CLIENT_SECRET" \
  IDENTITY_EVENTS_FEED_ENABLED="true" \
  PROFILE_EVENTS_FEED_ENABLED="true"

wait_for_http "onboarding-svc train" \
  "http://localhost:9093/onboarding-service/api/onboarding/train"

if [ "$RUN_SMOKE" = "true" ]; then
  log "running smoke test"
  (cd "$ONBOARDING_DIR" && FRONTEND_PUBLIC_URL="$FRONTEND_PUBLIC_URL" ./scripts/smoke-onboarding-flow.sh)
else
  log "smoke test skipped"
fi

log "ready"
log "token-svc:       http://localhost:9091/token-service"
log "profile-service: http://localhost:9092/profile-service"
log "onboarding-svc:  http://localhost:9093/onboarding-service"
log "mailpit:         http://localhost:8025"
