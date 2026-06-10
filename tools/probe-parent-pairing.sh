#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${NP_PAIRING_ENV:-$ROOT_DIR/secrets/urbit/pairing.env}"

if [[ "${1:-}" == "--code-stdin" ]]; then
  NP_PAIRING_CODE="$(cat)"
  export NP_PAIRING_CODE
fi

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

if [[ -n "${NP_PAIRING_URL:-}" && -z "${NP_PAIRING_CODE:-}" && -t 0 ]]; then
  read -r -s -p "Parent +code: " NP_PAIRING_CODE
  printf '\n' >&2
  export NP_PAIRING_CODE
fi

if [[ -z "${NP_PAIRING_URL:-}" || -z "${NP_PAIRING_CODE:-}" ]]; then
  cat >&2 <<'EOF'
Missing pairing inputs.

Create a local ignored file:

  secrets/urbit/pairing.env

With:

  NP_PAIRING_URL=https://example.tlon.network
  NP_PAIRING_CODE=<plus-code>

Or pass the code through stdin:

  NP_PAIRING_URL=https://example.tlon.network ./tools/probe-parent-pairing.sh --code-stdin

Or run from an interactive terminal and type the code at the hidden prompt:

  NP_PAIRING_URL=https://example.tlon.network ./tools/probe-parent-pairing.sh

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
login_form="$tmp_dir/login-form.txt"

chmod 700 "$tmp_dir"
printf '%s' "$NP_PAIRING_CODE" |
  python3 -c 'import sys, urllib.parse; print("password=" + urllib.parse.quote_plus(sys.stdin.read()), end="")' \
  > "$login_form"
chmod 600 "$login_form"

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
    --data-binary "@$login_form" \
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

printf 'Parent ship: '
host_body="$tmp_dir/host.txt"
host_code="$(
  curl "${curl_common[@]}" \
    --output "$host_body" \
    --write-out '%{http_code}' \
    "$url/~/host" || true
)"
parent_ship="$(tr -d '\r\n ' < "$host_body" | sed 's/^~//')"
if [[ "$host_code" == "200" && "$parent_ship" =~ ^[a-z0-9-]+$ ]]; then
  printf '~%s\n' "$parent_ship"
else
  printf 'HTTP %s, not confirmed\n' "$host_code"
fi

printf 'Artemis /moons channel: '
channel_id="nativeplanet-probe-$(date +%s)-$RANDOM"
channel_put="$tmp_dir/channel-put.json"
channel_body="$tmp_dir/channel-body.txt"
if [[ "$host_code" == "200" && "$parent_ship" =~ ^[a-z0-9-]+$ ]]; then
  python3 - "$parent_ship" > "$channel_put" <<'PY'
import json
import sys

parent = sys.argv[1]
print(json.dumps([{
    "id": 1,
    "action": "subscribe",
    "ship": parent,
    "app": "artemis",
    "path": "/moons",
}]))
PY
  channel_put_code="$(
    curl "${curl_common[@]}" \
      --request PUT \
      --header 'content-type: application/json' \
      --data-binary "@$channel_put" \
      --output "$body" \
      --write-out '%{http_code}' \
      "$url/~/channel/$channel_id" || true
  )"
  channel_get_code="$(
    curl "${curl_common[@]}" \
      --header 'accept: text/event-stream' \
      --max-time 10 \
      --output "$channel_body" \
      --write-out '%{http_code}' \
      "$url/~/channel/$channel_id" || true
  )"
  if [[ "$channel_put_code" =~ ^2 && "$channel_get_code" == "200" ]] && grep -q '"response":"diff"' "$channel_body" && grep -q '"moons"' "$channel_body"; then
    printf 'subscription fact received\n'
  else
    printf 'PUT HTTP %s, GET HTTP %s, fact not confirmed\n' "$channel_put_code" "$channel_get_code"
  fi

  delete_body="$tmp_dir/channel-delete.json"
  printf '[{"id":3,"action":"delete"}]' > "$delete_body"
  curl "${curl_common[@]}" \
    --request POST \
    --header 'content-type: application/json' \
    --data-binary "@$delete_body" \
    --output "$body" \
    "$url/~/channel/$channel_id" >/dev/null 2>&1 || true
else
  printf 'skipped, parent ship not confirmed\n'
fi

printf 'Result: '
if grep -q 'urbauth' "$cookie_jar" && [[ "$scry_code" == "200" && "$artemis_code" == "200" ]] &&
    [[ "${channel_put_code:-000}" =~ ^2 && "${channel_get_code:-000}" == "200" ]] &&
    grep -q '"response":"diff"' "${channel_body:-/dev/null}" && grep -q '"moons"' "${channel_body:-/dev/null}"; then
  printf 'authenticated transport available, Artemis mobile provisioning channel reachable\n'
  exit 0
fi

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
