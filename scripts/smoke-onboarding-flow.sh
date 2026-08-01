#!/usr/bin/env bash
set -euo pipefail

TOKEN_BASE_URL="${TOKEN_BASE_URL:-http://localhost:9091/token-service}"
ONBOARDING_BASE_URL="${ONBOARDING_BASE_URL:-http://localhost:9093/onboarding-service}"
PROFILE_BASE_URL="${PROFILE_BASE_URL:-http://localhost:9092/profile-service}"
MAILPIT_BASE_URL="${MAILPIT_BASE_URL:-http://localhost:8025}"
FRONTEND_PUBLIC_URL="${FRONTEND_PUBLIC_URL:-http://localhost:8000}"

EMAIL="${SMOKE_EMAIL:-smoke.$(date +%s)@example.com}"
PASSWORD="${SMOKE_PASSWORD:-secret123}"

log() {
  printf '[smoke] %s\n' "$*"
}

fail() {
  printf '[smoke] FAIL: %s\n' "$*" >&2
  exit 1
}

require_status() {
  local expected="$1"
  local actual="$2"
  local label="$3"
  if [ "$actual" != "$expected" ]; then
    fail "$label returned HTTP $actual, expected $expected"
  fi
}

wait_for_onboarding_next_action() {
  local expected="$1"
  local output_file="$2"
  local status_file="$TMP_DIR/onboarding-start-status.txt"
  local next_action

  for _ in $(seq 1 30); do
    http_request POST "$ONBOARDING_BASE_URL/api/onboarding/start" "$START_BODY" "" "$output_file" > "$status_file"
    if [ "$(cat "$status_file")" = "200" ]; then
      next_action="$(json_get nextAction < "$output_file")"
      if [ "$next_action" = "$expected" ]; then
        return 0
      fi
    fi
    sleep 1
  done

  return 1
}

wait_for_onboarding_state() {
  local expected="$1"
  local output_file="$2"
  local status_file="$TMP_DIR/onboarding-state-status.txt"
  local state

  for _ in $(seq 1 30); do
    http_request POST "$ONBOARDING_BASE_URL/api/onboarding/start" "$START_BODY" "" "$output_file" > "$status_file"
    if [ "$(cat "$status_file")" = "200" ]; then
      state="$(json_get state < "$output_file")"
      if [ "$state" = "$expected" ]; then
        return 0
      fi
    fi
    sleep 1
  done

  return 1
}

json_get() {
  local expression="$1"
  python3 -c 'import json, sys
data = json.load(sys.stdin)
value = data
for part in sys.argv[1].split("."):
    if part:
        value = value[part]
print(value)' "$expression"
}

decode_jwt_payload() {
  python3 -c 'import base64, json, sys
payload = sys.argv[1].split(".")[1]
payload += "=" * (-len(payload) % 4)
print(json.dumps(json.loads(base64.urlsafe_b64decode(payload)), indent=2, sort_keys=True))' "$1"
}

extract_confirmation_token() {
  python3 -c 'import json, re, sys, urllib.request
base_url, email = sys.argv[1], sys.argv[2]
messages = json.load(urllib.request.urlopen(base_url + "/api/v1/messages"))
for message in messages.get("messages", []):
    recipients = [recipient.get("Address") for recipient in message.get("To", [])]
    if email in recipients:
        detail = json.load(urllib.request.urlopen(base_url + "/api/v1/message/" + message["ID"]))
        match = re.search(r"confirmEmailToken=([^\s)]+)", detail.get("Text", ""))
        if match:
            print(match.group(1))
            sys.exit(0)
sys.exit(1)' "$MAILPIT_BASE_URL" "$EMAIL"
}

http_request() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local token="${4:-}"
  local output_file="$5"

  local args=(-s -o "$output_file" -w '%{http_code}' -X "$method" "$url" -H 'Accept: application/json')
  if [ -n "$body" ]; then
    args+=(-H 'Content-Type: application/json' --data "$body")
  fi
  if [ -n "$token" ]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  curl "${args[@]}"
}

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

log "checking services"
TOKEN_STATUS="$(http_request GET "$TOKEN_BASE_URL/api/.well-known/jwks.json" "" "" "$TMP_DIR/jwks.json")"
require_status 200 "$TOKEN_STATUS" "token-svc JWKS"

ONBOARDING_STATUS="$(http_request GET "$ONBOARDING_BASE_URL/api/onboarding/train" "" "" "$TMP_DIR/onboarding-train.json")"
require_status 200 "$ONBOARDING_STATUS" "onboarding-svc train"

PROFILE_STATUS="$(http_request GET "$PROFILE_BASE_URL/locations/regions" "" "" "$TMP_DIR/profile-regions.json")"
require_status 200 "$PROFILE_STATUS" "profile-service public locations"

log "registering $EMAIL"
REGISTER_BODY="$(printf '{"email":"%s","password":"%s"}' "$EMAIL" "$PASSWORD")"
REGISTER_STATUS="$(http_request POST "$TOKEN_BASE_URL/api/users/register" "$REGISTER_BODY" "" "$TMP_DIR/register.json")"
require_status 202 "$REGISTER_STATUS" "token-svc registration"
REGISTRATION_ID="$(json_get registrationId < "$TMP_DIR/register.json")"
log "registrationId=$REGISTRATION_ID"

log "checking onboarding resume after registration"
START_BODY="$(printf '{"email":"%s"}' "$EMAIL")"
if ! wait_for_onboarding_next_action "SHOW_EMAIL_VERIFICATION_PENDING" "$TMP_DIR/onboarding-start.json"; then
  NEXT_ACTION="$(json_get nextAction < "$TMP_DIR/onboarding-start.json")"
  fail "onboarding nextAction was $NEXT_ACTION after waiting, expected SHOW_EMAIL_VERIFICATION_PENDING"
fi

log "reading confirmation token from Mailpit"
CONFIRMATION_TOKEN="$(extract_confirmation_token || true)"
if [ -z "$CONFIRMATION_TOKEN" ]; then
  fail "could not find confirmation email for $EMAIL in Mailpit"
fi

log "confirming email through token-svc"
CONFIRM_STATUS="$(curl -s -G -o "$TMP_DIR/confirm.json" -w '%{http_code}' \
  --data-urlencode "token=$CONFIRMATION_TOKEN" \
  "$TOKEN_BASE_URL/api/users/confirm-email")"
require_status 200 "$CONFIRM_STATUS" "token-svc email confirmation"
ACCESS_TOKEN="$(json_get access_token < "$TMP_DIR/confirm.json")"

log "validating access token claims"
decode_jwt_payload "$ACCESS_TOKEN" > "$TMP_DIR/access-token.json"
python3 -c 'import json, sys
claims = json.load(open(sys.argv[1]))
aud = claims.get("aud", [])
missing = [value for value in ["identity-svc", "onboarding-svc", "profile-service"] if value not in aud]
if missing:
    print("missing audiences: " + ",".join(missing), file=sys.stderr)
    sys.exit(1)
if "USER" not in claims.get("groups", []):
    print("missing USER group", file=sys.stderr)
    sys.exit(1)' "$TMP_DIR/access-token.json" || fail "access token claims are not valid for all local services"

log "calling profile-service /profiles/me before profile exists"
PROFILE_ME_STATUS="$(http_request GET "$PROFILE_BASE_URL/profiles/me" "" "$ACCESS_TOKEN" "$TMP_DIR/profile-me-before.json")"
require_status 404 "$PROFILE_ME_STATUS" "profile-service profiles/me before profile creation"

log "creating profile"
PROFILE_BODY='{
  "displayName": "Smoke Provider",
  "description": "Local smoke test profile",
  "birthDate": "1990-01-01",
  "countryCode": "CL",
  "regionCode": "13",
  "communeCode": "13101",
  "services": [
    {
      "name": "Massage",
      "description": "Relax",
      "active": true
    }
  ],
  "rates": [
    {
      "label": "1 Hour",
      "amount": 50000,
      "currency": "CLP",
      "durationAmount": 1,
      "durationUnit": "HOURS",
      "displayOrder": 1,
      "active": true
    }
  ]
}'
CREATE_PROFILE_STATUS="$(http_request POST "$PROFILE_BASE_URL/profiles" "$PROFILE_BODY" "$ACCESS_TOKEN" "$TMP_DIR/create-profile.json")"
require_status 201 "$CREATE_PROFILE_STATUS" "profile-service create profile"

log "calling profile-service /profiles/me after profile creation"
PROFILE_ME_AFTER_STATUS="$(http_request GET "$PROFILE_BASE_URL/profiles/me" "" "$ACCESS_TOKEN" "$TMP_DIR/profile-me-after.json")"
require_status 200 "$PROFILE_ME_AFTER_STATUS" "profile-service profiles/me after profile creation"
PROFILE_USER_ID="$(json_get userId < "$TMP_DIR/profile-me-after.json")"
if [ "$PROFILE_USER_ID" != "$EMAIL" ]; then
  fail "profile userId was $PROFILE_USER_ID, expected $EMAIL"
fi

log "checking registration status in onboarding-svc"
REGISTRATION_STATUS="$(http_request GET "$ONBOARDING_BASE_URL/api/onboarding/registrations/$REGISTRATION_ID/status" "" "" "$TMP_DIR/registration-status.json")"
require_status 200 "$REGISTRATION_STATUS" "onboarding-svc registration status"

log "checking onboarding completion after profile creation"
if ! wait_for_onboarding_state "PROFILE_CREATED" "$TMP_DIR/onboarding-complete.json"; then
  STATE="$(json_get state < "$TMP_DIR/onboarding-complete.json")"
  fail "onboarding state was $STATE after waiting, expected PROFILE_CREATED"
fi

log "OK"
log "email=$EMAIL"
log "frontend=$FRONTEND_PUBLIC_URL"
