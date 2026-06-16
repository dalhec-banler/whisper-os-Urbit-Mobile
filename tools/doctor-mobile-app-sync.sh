#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${NP_PAIRING_ENV:-$ROOT_DIR/secrets/urbit/pairing.env}"
DESKS=(groups webterm landscape grove kin)

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
}

extract_bundle_json() {
  python3 -c '
import sys
text = sys.stdin.read()
start = text.find("json=")
if start < 0:
    raise SystemExit(1)
start = text.find("{", start)
if start < 0:
    raise SystemExit(1)
depth = 0
in_string = False
escaped = False
for i, ch in enumerate(text[start:], start):
    if in_string:
        if escaped:
            escaped = False
        elif ch == "\\":
            escaped = True
        elif ch == "\"":
            in_string = False
    else:
        if ch == "\"":
            in_string = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                print(text[start:i + 1])
                raise SystemExit(0)
raise SystemExit(1)
'
}

login_to_ship() {
  local url="$1"
  local code="$2"
  local cookie_jar="$3"

  local form
  form="$(mktemp "$tmp_dir/form.XXXXXX")"
  python3 - "$code" > "$form" <<'PY'
import sys
import urllib.parse

print("password=" + urllib.parse.quote_plus(sys.argv[1]), end="")
PY
  chmod 600 "$form"

  curl --silent --show-error --location \
    --connect-timeout 10 \
    --max-time 30 \
    --cookie-jar "$cookie_jar" \
    --cookie "$cookie_jar" \
    --request POST \
    --header 'content-type: application/x-www-form-urlencoded' \
    --data-binary "@$form" \
    --output /dev/null \
    "$url/~/login"
}

http_probe() {
  local url="$1"
  local cookie_jar="${2:-}"
  if [[ -n "$cookie_jar" ]]; then
    curl --silent --show-error --location \
      --connect-timeout 5 \
      --max-time 10 \
      --cookie "$cookie_jar" \
      --output /dev/null \
      --write-out '%{http_code}' \
      "$url" || true
  else
    curl --silent --show-error --location \
      --connect-timeout 5 \
      --max-time 10 \
      --output /dev/null \
      --write-out '%{http_code}' \
      "$url" || true
  fi
}

require adb
require curl
require jq
require node
require python3

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
chmod 700 "$tmp_dir"

echo "== Device =="
adb devices | sed -n '1,3p'
echo

runtime_json="$(
  adb shell content call --uri content://io.nativeplanet.controller --method getRuntime 2>/dev/null |
    extract_bundle_json
)"
status_json="$(
  adb shell content call --uri content://io.nativeplanet.controller --method getStatus 2>/dev/null |
    extract_bundle_json
)"

echo "== Runtime =="
echo "$runtime_json" | jq '{state, shipName, version, connSockAvailable, lastError}'
echo

pier_path="$(echo "$status_json" | jq -r '.bootPackage.pierPath // empty')"
if [[ -z "$pier_path" ]]; then
  echo "controller status did not include bootPackage.pierPath" >&2
  exit 1
fi

echo "== conn.sock / Ames =="
info="$("$ROOT_DIR/tools/conn-client.js" --adb peel info 2>/dev/null | jq -r '.value')"
printf '%s\n' "$info" |
  grep -oE '%(can-send|can-scry|stun-working|crashed|lane-scry-fails) [^]]+' |
  sed 's/^%/  /'
echo

code="$(
  adb shell content call --uri content://io.nativeplanet.controller --method getWebLoginCode 2>/dev/null |
    sed -n 's/.*"code":"\([^"]*\)".*/\1/p'
)"
if [[ -z "$code" ]]; then
  echo "could not get phone web login code" >&2
  exit 1
fi

adb forward tcp:18080 tcp:8080 >/dev/null
phone_cookie="$tmp_dir/phone-cookies.txt"
login_to_ship "http://127.0.0.1:18080" "$code" "$phone_cookie"

phone_pikes="$tmp_dir/phone-pikes.json"
curl --silent --show-error \
  --cookie "$phone_cookie" \
  --output "$phone_pikes" \
  "http://127.0.0.1:18080/~/scry/hood/kiln/pikes.json"

echo "== Phone Kiln =="
jq -r '
  to_entries |
  sort_by(.key) |
  .[] |
  "\(.key)\t\(.value.zest // "missing")\t\(.value.hash // "0v0")\t\(.value.sync.ship // "-")\t%\(.value.sync.desk // "-")"
' "$phone_pikes" |
  awk 'BEGIN { printf "%-22s %-8s %-18s %-30s %-12s\n", "desk", "zest", "hash", "sync ship", "sync desk" }
       { printf "%-22s %-8s %-18s %-30s %-12s\n", $1, $2, $3, $4, $5 }'
echo

echo "== Phone Routes =="
for desk in "${DESKS[@]}"; do
  code_http="$(http_probe "http://127.0.0.1:18080/apps/$desk/" "$phone_cookie")"
  printf '%-12s /apps/%s/ -> HTTP %s\n' "$desk" "$desk" "$code_http"
done
charges_code="$(http_probe "http://127.0.0.1:18080/~/scry/docket/charges.json" "$phone_cookie")"
printf '%-12s /~/scry/docket/charges.json -> HTTP %s\n' "docket" "$charges_code"
echo

if [[ -n "${NP_PAIRING_URL:-}" && -n "${NP_PAIRING_CODE:-}" ]]; then
  parent_url="${NP_PAIRING_URL%/}"
  case "$parent_url" in
    http://*|https://*) ;;
    *) parent_url="https://$parent_url" ;;
  esac

  parent_cookie="$tmp_dir/parent-cookies.txt"
  login_to_ship "$parent_url" "$NP_PAIRING_CODE" "$parent_cookie"

  parent_pikes="$tmp_dir/parent-pikes.json"
  curl --silent --show-error \
    --cookie "$parent_cookie" \
    --output "$parent_pikes" \
    "$parent_url/~/scry/hood/kiln/pikes.json"

  echo "== Parent Comparison =="
  for desk in "${DESKS[@]}"; do
    parent_zest="$(jq -r --arg desk "$desk" '.[$desk].zest // "missing"' "$parent_pikes")"
    parent_hash="$(jq -r --arg desk "$desk" '.[$desk].hash // "0v0"' "$parent_pikes")"
    parent_sync_ship="$(jq -r --arg desk "$desk" '.[$desk].sync.ship // "-"' "$parent_pikes")"
    phone_zest="$(jq -r --arg desk "$desk" '.[$desk].zest // "missing"' "$phone_pikes")"
    phone_hash="$(jq -r --arg desk "$desk" '.[$desk].hash // "0v0"' "$phone_pikes")"
    route_code="$(http_probe "$parent_url/apps/$desk/" "$parent_cookie")"
    printf '%-12s parent=%-6s %-18s source=%-30s route=%s phone=%-6s %-18s\n' \
      "$desk" "$parent_zest" "$parent_hash" "$parent_sync_ship" "$route_code" "$phone_zest" "$phone_hash"
  done
  echo
else
  echo "== Parent Comparison =="
  echo "skipped: set NP_PAIRING_URL and NP_PAIRING_CODE or create secrets/urbit/pairing.env"
  echo
fi

echo "== Diagnosis Hints =="
held_count="$(jq '[.[] | select(.zest == "held" and .hash == "0v0")] | length' "$phone_pikes")"
if [[ "$held_count" -gt 0 ]]; then
  echo "- $held_count phone desk(s) are held with hash 0v0: install requests were accepted, but desk data has not arrived."
fi
if grep -q '%can-scry 0' <<<"$info"; then
  echo "- Ames reports can-scry=false; remote desk sync and Fine scries are expected to stall."
fi
if grep -q '%can-send 0' <<<"$info"; then
  echo "- Ames reports can-send=false; check Urbit network reachability before debugging Launcher UI."
fi
if [[ "$charges_code" != "200" ]]; then
  echo "- Docket charges are unavailable locally; launcher should not mark local WebView routes open."
fi
