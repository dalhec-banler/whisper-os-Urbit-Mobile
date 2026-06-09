#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${NP_PAIRING_ENV:-$ROOT_DIR/secrets/urbit/pairing.env}"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

if [[ -z "${NP_PAIRING_URL:-}" || -z "${NP_PAIRING_CODE:-}" ]]; then
  cat >&2 <<'EOF'
Missing pairing inputs.

Create a local ignored file:

  secrets/urbit/pairing.env

With:

  NP_PAIRING_URL=https://example.tlon.network
  NP_PAIRING_CODE=<plus-code>

This script never prints the code.
EOF
  exit 2
fi

url="${NP_PAIRING_URL%/}"
case "$url" in
  http://*|https://*) ;;
  *) url="https://$url" ;;
esac

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

cookie_jar="$tmp_dir/cookies.txt"
headers="$tmp_dir/headers.txt"
body="$tmp_dir/body.bin"

curl_common=(
  --silent
  --show-error
  --location
  --connect-timeout 10
  --max-time 30
  --cookie-jar "$cookie_jar"
  --cookie "$cookie_jar"
)

printf 'Parent URL: %s\n' "$url"
printf 'Login: '

login_code="$(
  curl "${curl_common[@]}" \
    --request POST \
    --header 'content-type: application/x-www-form-urlencoded' \
    --data-urlencode "password=$NP_PAIRING_CODE" \
    --dump-header "$headers" \
    --output "$body" \
    --write-out '%{http_code}' \
    "$url/~/login" || true
)"

if grep -q 'urbauth' "$cookie_jar"; then
  printf 'HTTP %s, session cookie set\n' "$login_code"
else
  printf 'HTTP %s, no session cookie detected\n' "$login_code"
fi

printf 'Webterm: '
webterm_code="$(
  curl "${curl_common[@]}" \
    --output "$body" \
    --write-out '%{http_code}' \
    "$url/apps/webterm/" || true
)"
printf 'HTTP %s\n' "$webterm_code"

printf 'Scry probe: '
scry_code="$(
  curl "${curl_common[@]}" \
    --output "$body" \
    --write-out '%{http_code}' \
    "$url/~/scry/hood/kiln/pikes.json" || true
)"
printf 'HTTP %s\n' "$scry_code"

printf 'Artemis app: '
artemis_code="$(
  curl "${curl_common[@]}" \
    --output "$body" \
    --write-out '%{http_code}' \
    "$url/apps/artemis/" || true
)"
printf 'HTTP %s\n' "$artemis_code"

printf 'Artemis moons scry: '
artemis_moons_code="$(
  curl "${curl_common[@]}" \
    --output "$body" \
    --write-out '%{http_code}' \
    "$url/~/scry/artemis/mons.json" || true
)"
printf 'HTTP %s\n' "$artemis_moons_code"

printf 'Result: '
if grep -q 'urbauth' "$cookie_jar" && [[ "$scry_code" == "200" && "$artemis_code" == "200" && "$artemis_moons_code" == "200" ]]; then
  printf 'authenticated transport available, Artemis mobile provisioning surface reachable\n'
  exit 0
fi

if grep -q 'urbauth' "$cookie_jar" && [[ "$scry_code" == "200" && "$artemis_code" == "200" ]]; then
  printf 'authenticated transport available, Artemis app reachable, moons scry not confirmed\n'
  exit 0
fi

if grep -q 'urbauth' "$cookie_jar" && [[ "$scry_code" == "200" ]]; then
  printf 'authenticated transport available, Artemis not confirmed\n'
  exit 0
fi

printf 'login not confirmed\n'
exit 1
